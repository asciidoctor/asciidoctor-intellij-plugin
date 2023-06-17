package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.download.AsciiDocDownloadNotificationProvider;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CreateEpubAction extends AsciiDocFileAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreateEpubAction";
  private static final Logger LOG = Logger.getInstance(CreateEpubAction.class);
  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);


  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
//    LOG.warn("is it even getting in here");
    var project = event.getProject();
    if (project == null) {
      return;
    }

    var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    var file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }
    if (file.getCanonicalPath() == null) {
      return;
    }

    if (!AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJEpub()) {
      // download all element, as we can't detect if the EPUB contains maybe diagrams
      AsciiDocDownloaderUtil.downloadAsciidoctorJEpub(project, () -> {
        Notifications.Bus
          .notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.download.title"),
            AsciiDocBundle.message("asciidoc.download.asciidoctorj-epub3.success"),
            NotificationType.INFORMATION));
        this.actionPerformed(event);
      }, e -> LOG.warn("unable to download", e));
      return;
    }

    if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
            FileDocumentManager.getInstance().saveDocument(unsavedDocument);
          }
        } catch (RuntimeException ex) {
          LOG.warn("Unable to save other file (might be a problem in another plugin", ex);
        }
      });
    }
    VirtualFile parent = file.getParent();
    boolean successful = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      Path tempImagesPath = null;
      try {
        File fileBaseDir = new File("");
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
          tempImagesPath = AsciiDocWrapper.tempImagesPath(fileBaseDir.toPath(), project);
        }
        AsciiDocWrapper asciiDocWrapper = new AsciiDocWrapper(project, fileBaseDir, tempImagesPath, file.getName());
        List<String> extensions = extensionService.getExtensions(project);
        String config = AsciiDocWrapper.config(editor.getDocument(), project);
        asciiDocWrapper.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDocWrapper.FileType.EPUB);
        if (Objects.equals("true", asciiDocWrapper.getAttributes().get("asciidoctor-diagram-missing-diagram-extension"))) {
          AsciiDocDownloadNotificationProvider.showNotification();
        }
      } finally {
        AsciiDocWrapper.cleanupImagesPath(tempImagesPath);
      }
    }, "Creating EPUB", true, project);
    ApplicationManager.getApplication().runWriteAction(() -> {
      VirtualFile virtualFileEpub = changeFileExtension(file);
      VirtualFile virtualFile = virtualFileEpub != null ? virtualFileEpub : parent;
      AsciiDocUtil.selectFileInProjectView(project, virtualFile);
      if (virtualFileEpub != null) {
        if (successful) {
          new OpenFileDescriptor(project, virtualFileEpub).navigate(true);
        }
      }
    });
  }

  @Nullable
  private VirtualFile changeFileExtension(VirtualFile file) {
    Path path = file.getFileSystem().getNioPath(file);
    if (path == null) {
      return null;
    }
    return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(
      path.getParent().resolve(file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".epub"))
    );
  }

}
