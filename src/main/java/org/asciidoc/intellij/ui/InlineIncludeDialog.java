package org.asciidoc.intellij.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.inline.InlineOptionsDialog;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InlineIncludeDialog extends InlineOptionsDialog {
  private final PsiNamedElement myResolve;

  public InlineIncludeDialog(@NotNull Project project, PsiElement element, PsiNamedElement resolve) {
    super(project, false, element);
    this.myResolve = resolve;
    this.myInvokedOnReference = true;
    setTitle("Inline Include");
    init();
  }

  @Override
  protected String getNameLabelText() {
    return AsciiDocBundle.message("asciidoc.inline.label", myResolve.getName());
  }

  @Override
  protected String getBorderTitle() {
    return "Inline";
  }

  @Override
  protected String getInlineAllText() {
    return "Inline all and remove included file";
  }

  @Override
  protected String getKeepTheDeclarationText() {
    return "Inline all and keep included file";
  }

  @Override
  protected String getInlineThisText() {
    return "Inline this only and keep included file";
  }

  @Override
  protected boolean isInlineThis() {
    return false;
  }

  @Override
  protected boolean hasHelpAction() {
    return false;
  }

  @Override
  protected boolean hasPreviewButton() {
    return true;
  }

  @Override
  protected void doAction() {
    invokeRefactoring(
      new InlineIncludeProcessor(myElement, getProject(), myResolve, isInlineThisOnly(), !isKeepTheDeclaration()));
  }

  @Nullable
  public static PsiElement getElement(Editor editor, PsiFile psiFile) {
    PsiElement element = AsciiDocUtil.getStatementAtCaret(editor, psiFile);

    if (element == null) {
      return element;
    }

    while (!(element instanceof AsciiDocBlockMacro) && element.getParent() != null) {
      element = element.getParent();
    }
    if (element instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro m = (AsciiDocBlockMacro) element;
      if (!"include".equals(m.getMacroName())) {
        return null;
      }
      return element;
    }

    return null;
  }

  @Nullable
  public static PsiNamedElement resolve(PsiElement element) {
    PsiReference[] references = element.getReferences();
    if (references.length != 1) {
      return null;
    }
    PsiElement resolve = references[0].resolve();
    if (!(resolve instanceof PsiNamedElement)) {
      return null;
    }
    return (PsiNamedElement) resolve;
  }

}
