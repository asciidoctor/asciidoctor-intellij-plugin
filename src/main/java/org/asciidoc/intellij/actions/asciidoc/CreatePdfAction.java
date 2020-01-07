package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CreatePdfAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreatePdfAction";

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

    ApplicationManager.getApplication().runWriteAction(() ->
      ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        ApplicationManager.getApplication().saveAll();
        File fileBaseDir = new File("");
        VirtualFile parent = file.getParent();
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
        }
        Path tempImagesPath = AsciiDoc.tempImagesPath();
        try {
          AsciiDoc asciiDoc = new AsciiDoc(project, fileBaseDir,
            tempImagesPath, file.getName());
          List<String> extensions = AsciiDoc.getExtensions(project);
          String config = AsciiDoc.config(editor.getDocument(), project);
          asciiDoc.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDoc.FileType.PDF);
          VirtualFile virtualFile = VirtualFileManager.getInstance()
            .refreshAndFindFileByUrl(file.getUrl().replaceAll("\\.(adoc|asciidoc|ad)$", ".pdf"));
          updateProjectView(virtualFile != null ? virtualFile : parent);
          if (virtualFile != null) {
            new OpenFileDescriptor(project, virtualFile).navigate(true);
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
      }, new ProgressIndicatorBase()));

  }

  private void updateProjectView(VirtualFile virtualFile) {
    //update project view
    ProjectView projectView = ProjectView.getInstance(project);
    projectView.changeView(ProjectViewPane.ID);
    projectView.select(null, virtualFile, true);
  }

}
