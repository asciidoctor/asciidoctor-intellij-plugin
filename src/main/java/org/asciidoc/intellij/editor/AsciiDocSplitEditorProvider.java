package org.asciidoc.intellij.editor;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleManagerEx;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.ui.SplitTextEditorProvider;
import org.jetbrains.annotations.NotNull;

public class AsciiDocSplitEditorProvider extends SplitTextEditorProvider {
  public AsciiDocSplitEditorProvider() {
    super(new PsiAwareTextEditorProvider(), new AsciiDocPreviewEditorProvider());

    // removing the entry from the history prevents an "Unable to find providerId=text-editor" assertion error
    // in EditorComposite
    // https://youtrack.jetbrains.com/issue/IDEA-289732 - gone in 2022.1 final
    boolean shouldClearEditorHistory = ApplicationInfo.getInstance().getBuild().getBaselineVersion() == 221
       && ApplicationInfo.getInstance().getBuild().getComponents().length > 1
       && ApplicationInfo.getInstance().getBuild().getComponents()[1] < 5080;

    // when this plugin is installed at runtime, check if the existing editors as split editors.
    // If not, close the editor and re-open the files
    // on startup, the list of files is always empty (it is still being restored)
    ApplicationManager.getApplication().invokeLater(() -> {
      // run later, to avoid a cyclic dependency plugin is dynamically loaded
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        FileEditorManager fem = FileEditorManager.getInstance(project);
        PsiManager pm = PsiManager.getInstance(project);
        for (FileEditor editor : fem.getAllEditors()) {
          if (!(editor instanceof AsciiDocSplitEditor)) {
            VirtualFile vFile = editor.getFile();
            if (vFile != null && vFile.isValid()) {
              PsiFile pFile = pm.findFile(vFile);
              if (pFile != null) {
                if (pFile.getLanguage() == AsciiDocLanguage.INSTANCE) {
                  // an AsciiDoc file in a non-split editor, close and re-open the file to enforce split editor
                  ApplicationManager.getApplication().runWriteAction(() -> {
                    // closing the file might trigger a save, therefore wrap in write action
                    fem.closeFile(vFile);
                    if (shouldClearEditorHistory) {
                      if (EditorHistoryManager.getInstance(project).getState(vFile, this) == null) {
                        EditorHistoryManager.getInstance(project).removeFile(vFile);
                      }
                    }
                    fem.openFile(vFile, false);
                  });
                }
              }
            }
          }
        }
        if (shouldClearEditorHistory) {
          DumbService.getInstance(project).runWhenSmart(() -> ApplicationManager.getApplication().invokeLater(() -> {
            ModuleManager manager = ModuleManager.getInstance(project);
            if (manager instanceof ModuleManagerEx) {
              // use this to avoid an exception in RootIndex.java:69
              if (!((ModuleManagerEx) manager).areModulesLoaded()) {
                return;
              }
            }

            // clearing the history after the re-opening of files for all closed files in the history.
            for (VirtualFile vFile : EditorHistoryManager.getInstance(project).getFileList()) {
              if (vFile.isValid()) {
                PsiFile pFile = pm.findFile(vFile);
                if (pFile != null) {
                  if (pFile.getLanguage() == AsciiDocLanguage.INSTANCE) {
                    if (EditorHistoryManager.getInstance(project).getState(vFile, this) == null) {
                      EditorHistoryManager.getInstance(project).removeFile(vFile);
                    }
                  }
                }
              }
            }
          }));
        }
      }
    });
  }

  @Override
  protected FileEditor createSplitEditor(@NotNull final FileEditor firstEditor, @NotNull FileEditor secondEditor) {
    if (!(firstEditor instanceof TextEditor) || !(secondEditor instanceof AsciiDocPreviewEditor)) {
      throw new IllegalArgumentException("Main editor should be TextEditor");
    }
    AsciiDocPreviewEditor asciiDocPreviewEditor = (AsciiDocPreviewEditor) secondEditor;
    asciiDocPreviewEditor.setEditor(((TextEditor) firstEditor).getEditor());
    return new AsciiDocSplitEditor(((TextEditor) firstEditor), ((AsciiDocPreviewEditor) secondEditor));
  }

  @Override
  public void disposeEditor(@NotNull FileEditor fileEditor) {
    // default -- needed for IntelliJ IDEA 15 compatibility
    Disposer.dispose(fileEditor);
  }

}
