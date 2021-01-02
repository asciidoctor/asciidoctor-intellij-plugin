package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
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
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.download.AsciiDocDownloadNotificationProvider;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class CreatePdfAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreatePdfAction";

  private static final Logger LOG = Logger.getInstance(CreatePdfAction.class);
  private final AsciiDocExtensionService extensionService = ServiceManager.getService(AsciiDocExtensionService.class);

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
          .notify(new Notification("asciidoc", AsciiDocBundle.message("asciidoc.download.title"),
            AsciiDocBundle.message("asciidoc.download.asciidoctorj-pdf.success"),
            NotificationType.INFORMATION));
        this.actionPerformed(event);
      }, e -> LOG.warn("unable to download", e));
      return;
    }

    if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
          FileDocumentManager.getInstance().saveDocument(unsavedDocument);
        }
      });
    }
    VirtualFile parent = file.getParent();
    boolean successful = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      Path tempImagesPath = AsciiDoc.tempImagesPath();
      try {
        File fileBaseDir = new File("");
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
        }
        AsciiDoc asciiDoc = new AsciiDoc(project, fileBaseDir, tempImagesPath, file.getName());
        List<String> extensions = extensionService.getExtensions(project);
        String config = AsciiDoc.config(editor.getDocument(), project);
        asciiDoc.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDoc.FileType.PDF);
        if (Objects.equals("true", asciiDoc.getAttributes().get("asciidoctor-diagram-missing-diagram-extension"))) {
          AsciiDocDownloadNotificationProvider.showNotification();
        }
      } finally {
        if (tempImagesPath != null) {
          try {
            FileUtils.deleteDirectory(tempImagesPath.toFile());
          } catch (IOException _ex) {
            Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", _ex);
          }
        }
      }
    }, "Creating PDF", true, project);
    ApplicationManager.getApplication().runWriteAction(() -> {
      VirtualFile virtualFile = VirtualFileManager.getInstance()
        .refreshAndFindFileByUrl(file.getUrl().replaceAll("\\.(adoc|asciidoc|ad)$", ".pdf"));
      updateProjectView(virtualFile != null ? virtualFile : parent);
      if (virtualFile != null) {
        if (successful) {
          new OpenFileDescriptor(project, virtualFile).navigate(true);
        }
      }
    });
  }

  private void updateProjectView(VirtualFile virtualFile) {
    if (!LightEdit.owns(project)) {
      //update project view
      ProjectView projectView = ProjectView.getInstance(project);
      projectView.changeView(ProjectViewPane.ID);
      projectView.select(null, virtualFile, true);
    }
  }

}
