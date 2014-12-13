package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * @author Erik Pragt
 */
public abstract class FormatAsciiDocAction extends AnAction {


  public abstract String getName();

  public abstract String updateSelection(String asciidoc);


  @Override
  public final void actionPerformed(AnActionEvent event) {

    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    final Document document = editor.getDocument();

    SelectionModel selectionModel = editor.getSelectionModel();
    if(!selectionModel.hasSelection()) {
      selectionModel.selectWordAtCaret(false);
    }

      final String newDocumentText = applyMarkup(selectionModel, document);
      updateDocument(project, document, newDocumentText);
  }

  private String applyMarkup(SelectionModel selectionModel, Document document) {


    String allText = document.getText();

    int start = selectionModel.getSelectionStart();
    int end = selectionModel.getSelectionEnd();

    String startText = allText.substring(0, start);
    String endText = allText.substring(end, allText.length());

    String text = selectionModel.getSelectedText();

    final String updatedText = updateSelection(text);
    return startText + updatedText + endText;
  }

  @Override
  public void update(AnActionEvent event) {
    PsiFile file = event.getData(DataKeys.PSI_FILE);
    boolean enabled = false;

    if (file != null) {
      for (String ext : AsciiDocFileType.DEFAULT_ASSOCIATED_EXTENSIONS) {
        if (file.getName().endsWith("." + ext)) {
          enabled = true;
          break;
        }
      }
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

  private void updateDocument(final Project project, final Document document, final String asciiDoc) {
    final Runnable readRunner = new Runnable() {
      @Override
      public void run() {
        document.setText(asciiDoc);
      }
    };
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(readRunner);
          }
        }, getName(), null);
      }
    });
  }

}
