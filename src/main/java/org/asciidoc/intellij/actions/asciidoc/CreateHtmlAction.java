package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Balasubramanian Naagarajan(balabarath)
 */
public class CreateHtmlAction extends AsciiDocAction {

  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreateHtmlAction";

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
        String config = AsciiDoc.config(editor.getDocument(), project);
        List<String> extensions = extensionService.getExtensions(project);
        asciiDoc.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDoc.FileType.HTML);
      } finally {
        if (tempImagesPath != null) {
          try {
            FileUtils.deleteDirectory(tempImagesPath.toFile());
          } catch (IOException _ex) {
            Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", _ex);
          }
        }
      }
    }, "Creating HTML", true, project);
    ApplicationManager.getApplication().runWriteAction(() -> {
      VirtualFile virtualFile = VirtualFileManager.getInstance()
        .refreshAndFindFileByUrl(changeFileExtensionToHtml(file.getUrl()));
      updateProjectView(virtualFile != null ? virtualFile : parent);
      if (virtualFile != null) {
        if (successful) {
          BrowserUtil.browse(virtualFile);
        }
      }
    });
  }

  private String changeFileExtensionToHtml(String filePath) {
    return filePath.replaceAll("\\.(adoc|asciidoc|ad)$", ".html");
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
