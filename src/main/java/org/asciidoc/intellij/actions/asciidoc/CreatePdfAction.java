package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
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

public class CreatePdfAction extends AsciiDocFileAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreatePdfAction";

  private static final Logger LOG = Logger.getInstance(CreatePdfAction.class);
  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);

  private Project project;

  @Override
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }
    if (file.getCanonicalPath() == null) {
      return;
    }

    if (!AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJPdf()) {
      // download all element, as we can't detect if the PDF contains maybe diagrams
      AsciiDocDownloaderUtil.downloadAsciidoctorJPdf(project, () -> {
        Notifications.Bus
          .notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.download.title"),
            AsciiDocBundle.message("asciidoc.download.asciidoctorj-pdf.success"),
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
        if (parent != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          tempImagesPath = AsciiDocWrapper.tempImagesPath(parent.toNioPath(), project);
        }
        AsciiDocWrapper asciiDocWrapper = new AsciiDocWrapper(project, parent, tempImagesPath, file.getName());
        List<String> extensions = extensionService.getExtensions(project);
        String config = AsciiDocWrapper.config(editor.getDocument(), project);
        if (!asciiDocWrapper.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDocWrapper.FileType.PDF)) {
          return false;
        }
        if (Objects.equals("true", asciiDocWrapper.getAttributes().get("asciidoctor-diagram-missing-diagram-extension"))) {
          AsciiDocDownloadNotificationProvider.showNotification();
        }
        return true;
      } finally {
        AsciiDocWrapper.cleanupImagesPath(tempImagesPath);
      }
    }, "Creating PDF", true, project);
    ApplicationManager.getApplication().runWriteAction(() -> {
      VirtualFile virtualFilePdf = changeFileExtension(file);
      VirtualFile virtualFile = virtualFilePdf != null ? virtualFilePdf : parent;
      AsciiDocUtil.selectFileInProjectView(project, virtualFile);
      if (virtualFilePdf != null) {
        if (successful) {
          ApplicationManager.getApplication().invokeLater(() -> {
            new OpenFileDescriptor(project, virtualFilePdf).navigate(true);
          });
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
      path.getParent().resolve(file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".pdf"))
    );
  }

}
