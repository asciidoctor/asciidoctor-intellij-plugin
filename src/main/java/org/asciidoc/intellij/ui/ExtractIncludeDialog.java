package org.asciidoc.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.util.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ExtractIncludeDialog extends RefactoringDialog {
  private final Editor myEditor;
  private final PsiFile myFile;

  private NameSuggestionsField myFilename;

  public static PsiElement getElementToExtract(@NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement element = AsciiDocUtil.getStatementAtCaret(editor, file);

    if (element == null) {
      return null;
    }

    if (element.getParent() != null && element.getParent() instanceof AsciiDocBlockMacro) {
      element = element.getParent();
    }

    if (element instanceof AsciiDocBlockMacro && "include".equals(((AsciiDocBlockMacro) element).getMacroName())) {
      return null;
    }

    while ((element instanceof AsciiDocBlockMacro || (!(element instanceof AsciiDocBlock) && !(element instanceof AsciiDocSection)))
      && element.getParent() != null) {
      element = element.getParent();
    }
    if (element instanceof AsciiDocBlock || element instanceof AsciiDocSection) {
      return element;
    }

    return null;
  }

  public ExtractIncludeDialog(@NotNull Project project, Editor editor, PsiFile file) {
    super(project, false);
    this.myEditor = editor;
    this.myFile = file;
    String filename = "include";
    PsiElement element = getElementToExtract(editor, file);
    if (element != null) {
      AsciiDocBlockId id = PsiTreeUtil.findChildOfType(element, AsciiDocBlockId.class);
      if (id != null) {
        filename = id.getName();
      }
    }
    myFilename = new NameSuggestionsField(new String[]{filename + "." + AsciiDocFileType.INSTANCE.getDefaultExtension()},
      myProject, AsciiDocFileType.INSTANCE, myEditor);
    myFilename.selectNameWithoutExtension();
    setTitle("Extract Include Directive");
    init();
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (myFilename.getEnteredName().trim().length() == 0) {
      return new ValidationInfo("Please enter include file name!", myFilename);
    }
    if (!FilenameUtils.getName(getFilename()).equals(getFilename())) {
      return new ValidationInfo("Specifying a different directory is currently not supported.");
    }
    try {
      myFile.getContainingDirectory().checkCreateFile(getFilename());
    } catch (IncorrectOperationException e) {
      return new ValidationInfo("Unable to create file: " + e.getMessage(), myFilename);
    }
    return super.doValidate();
  }

  @Override
  protected boolean postponeValidation() {
    return false;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 0));

    JPanel addFilename = new JPanel(new BorderLayout());
    JLabel filenameLabel = new JLabel("File Name: ");
    filenameLabel.setLabelFor(myFilename);
    addFilename.add(filenameLabel, BorderLayout.LINE_START);
    myFilename.setPreferredSize(new Dimension(200, 0));
    addFilename.add(myFilename, BorderLayout.LINE_END);
    panel.add(addFilename);

    return panel;
  }

  @Override
  protected boolean hasHelpAction() {
    return false;
  }

  @Override
  protected boolean hasPreviewButton() {
    return false;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myFilename.getFocusableComponent();
  }

  @Override
  protected void doAction() {
    ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(myProject,
      () -> {
        try {
          TextRange range = getTextRange(myEditor, myFile);

          String newFileName = getFilename();
          PsiFile asciiDocFile = PsiFileFactory.getInstance(myProject).createFileFromText(newFileName,
            AsciiDocFileType.INSTANCE, myEditor.getDocument().getText(range));

          PsiFile newFile = (PsiFile) myFile.getContainingDirectory().add(asciiDocFile);
          StringBuilder sb = new StringBuilder();
          sb.append("include::");
          int offset = sb.length();
          sb.append(newFileName).append("[]");
          int start = range.getStartOffset();
          int end = range.getEndOffset();

          // the include block macro needs to be on its own line.
          // if we can't reuse the newlines of the current selection, add newlines as necessary
          if (start != 0 && !myEditor.getDocument().getText(TextRange.create(start - 1, start)).equals("\n")) {
            sb.insert(0, "\n");
            offset++;
            while (start > 0 && myEditor.getDocument().getText(TextRange.create(start - 1, start)).equals(" ")) {
              start--;
            }
          }
          if (end != myEditor.getDocument().getTextLength() && !myEditor.getDocument().getText(TextRange.create(end, end + 1)).equals("\n")) {
            sb.append("\n");
            while (end != myEditor.getDocument().getTextLength() && myEditor.getDocument().getText(TextRange.create(end, end + 1)).equals(" ")) {
              end++;
            }
          }

          myEditor.getDocument().replaceString(start, end, sb.toString());
          myEditor.getCaretModel().moveToOffset(start + offset);
          myEditor.getCaretModel().getPrimaryCaret().removeSelection();

          newFile.navigate(true);
          close(DialogWrapper.OK_EXIT_CODE);
        } catch (Exception e) {
          setErrorText("Unable to create include");
        }
      }, getTitle(), getGroupId(), UndoConfirmationPolicy.REQUEST_CONFIRMATION));
  }

  private static TextRange getTextRange(@NotNull Editor myEditor, @NotNull PsiFile myFile) {
    SelectionModel selectionModel = myEditor.getSelectionModel();
    int start, end;
    if (selectionModel.getSelectionStart() != selectionModel.getSelectionEnd()) {
      start = selectionModel.getSelectionStart();
      end = selectionModel.getSelectionEnd();
    } else {
      PsiElement element = getElementToExtract(myEditor, myFile);
      if (element != null) {
        start = element.getTextOffset();
        end = element.getTextOffset() + element.getTextLength();
      } else {
        element = AsciiDocUtil.getStatementAtCaret(myEditor, myFile);
        if (element != null) {
          // use start/end of current element and expand to begin/end of line
          start = element.getTextOffset();
          end = element.getTextOffset() + element.getTextLength();
          while (start > 0 && !myEditor.getDocument().getText(TextRange.create(start - 1, start)).equals("\n")) {
            start--;
          }
          while (end != myEditor.getDocument().getTextLength() && !myEditor.getDocument().getText(TextRange.create(end, end + 1)).equals("\n")) {
            end++;
          }
        } else {
          return TextRange.EMPTY_RANGE;
        }
      }
    }
    return TextRange.create(start, end);
  }

  private String getGroupId() {
    return AsciiDocFileType.INSTANCE.getName();
  }

  @Override
  protected boolean areButtonsValid() {
    if (doValidate() != null) {
      return false;
    }
    return super.areButtonsValid();
  }

  private String getFilename() {
    return myFilename.getEnteredName().trim();
  }

}
