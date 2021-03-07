package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class CreatePdfFromPreviewAction extends AsciiDocAction implements DumbAware {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreatePdfFromPreviewAction";

  @Override
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    boolean enabled = false;
    if (ApplicationManager.getApplication().isInternal()) {
      Project project = event.getProject();
      if (project != null) {
        FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (editor instanceof AsciiDocSplitEditor) {
          AsciiDocPreviewEditor previewEditor = ((AsciiDocSplitEditor) editor).getSecondEditor();
          enabled = previewEditor.isPrintingSupported();
        }
      }
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }
    FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor();
    if (editor == null) {
      return;
    }
    VirtualFile file = editor.getFile();
    if (file == null) {
      return;
    }

    if (editor instanceof AsciiDocSplitEditor) {
      AsciiDocPreviewEditor previewEditor = ((AsciiDocSplitEditor) editor).getSecondEditor();
      String canonicalPath = file.getCanonicalPath();
      if (canonicalPath != null) {
        String targetString = canonicalPath.replaceAll("\\.(adoc|asciidoc|ad)$", ".pdf");
        previewEditor.printToPdf(targetString, success -> {
          ApplicationManager.getApplication().invokeLater(() -> {
            if (success) {
              ApplicationManager.getApplication().runWriteAction(() -> {
                VirtualFile target = changeFileExtension(file);
                VirtualFile parent = file.getParent();
                updateProjectView(target != null ? target : parent, project);
                if (target != null) {
                  new OpenFileDescriptor(project, target).navigate(true);
                }
              });
            } else {
              Notifications.Bus
                .notify(new Notification("asciidoc", AsciiDocBundle.message("asciidoc.webpdf.failed.title"),
                  AsciiDocBundle.message("asciidoc.webpdf.failed.message", targetString),
                  NotificationType.ERROR));
            }
          });
        });
      }
    }

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

  private void updateProjectView(VirtualFile virtualFile, Project project) {
    //update project view
    ProjectView projectView = ProjectView.getInstance(project);
    projectView.changeView(ProjectViewPane.ID);
    projectView.select(null, virtualFile, true);
  }

}
