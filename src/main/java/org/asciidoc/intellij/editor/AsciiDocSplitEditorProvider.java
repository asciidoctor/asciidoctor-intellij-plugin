package org.asciidoc.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.asciidoc.intellij.AsciiDocLanguage;

public class AsciiDocSplitEditorProvider extends TextEditorWithPreviewProvider {
  public AsciiDocSplitEditorProvider() {
    super(new AsciiDocPreviewEditorProvider());

    // when this plugin is installed at runtime, check if the existing editors as split editors.
    // If not, close the editor and re-open the files
    // on startup, the list of files is always empty (it is still being restored)
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      // Run later, to avoid a cyclic dependency plugin is dynamically loaded.
      // Choose background thread as looking up files will use indexes which shouldn't block the AWT.
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        FileEditorManager fem = FileEditorManager.getInstance(project);
        PsiManager pm = PsiManager.getInstance(project);
        for (FileEditor editor : fem.getAllEditors()) {
          if (!(editor instanceof AsciiDocSplitEditor)) {
            ApplicationManager.getApplication().runReadAction(() -> {
              VirtualFile vFile = editor.getFile();
              if (vFile != null && vFile.isValid()) {
                PsiFile pFile = pm.findFile(vFile);
                if (pFile != null) {
                  if (pFile.getLanguage() == AsciiDocLanguage.INSTANCE) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                      // an AsciiDoc file in a non-split editor, close and re-open the file to enforce split editor
                      ApplicationManager.getApplication().runWriteAction(() -> {
                        // closing the file might trigger a save, therefore, wrap in write action
                        if (!project.isDisposed()) {
                          fem.closeFile(vFile);
                        }
                      });
                      // opening the file accesses AWT, must not be wrapped in a write action
                      if (!project.isDisposed()) {
                        fem.openFile(vFile, false);
                      }
                    });
                  }
                }
              }
            });
          }
        }
      }
    });
  }

}
