package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.asciidoc.intellij.psi.AsciiDocUtil;
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
        FileEditor editor = event.getData(LangDataKeys.FILE_EDITOR);
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
    FileEditor editor = event.getData(LangDataKeys.FILE_EDITOR);
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
        previewEditor.printToPdf(targetString, success -> ApplicationManager.getApplication().invokeLater(() -> {
          if (success) {
            ApplicationManager.getApplication().runWriteAction(() -> {
              VirtualFile target = changeFileExtension(file);
              VirtualFile parent = file.getParent();
              VirtualFile virtualFile = target != null ? target : parent;
              AsciiDocUtil.selectFileInProjectView(project, virtualFile);
              if (target != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                  new OpenFileDescriptor(project, target).navigate(true);
                });
              }
            });
          } else {
            Notifications.Bus
              .notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.webpdf.failed.title"),
                AsciiDocBundle.message("asciidoc.webpdf.failed.message", targetString),
                NotificationType.ERROR));
          }
        }));
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

}
