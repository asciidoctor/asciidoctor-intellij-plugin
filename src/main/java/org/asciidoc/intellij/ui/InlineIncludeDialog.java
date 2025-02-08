package org.asciidoc.intellij.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.inline.InlineOptionsDialog;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InlineIncludeDialog extends InlineOptionsDialog {
  private final PsiFile myResolve;

  public InlineIncludeDialog(@NotNull Project project, PsiElement element, PsiFile resolve) {
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
  public static PsiElement getElement(@NotNull Editor editor, @NotNull PsiFile psiFile) {
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
      // attributes like tags and leveloffset can't be inlined as of now
      // TODO: see https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/437
      if (m.hasAttributes()) {
        return null;
      }
      return element;
    }

    return null;
  }

  @Nullable
  public static PsiFile resolve(PsiElement element) {
    PsiReference[] references = element.getReferences();
    for (int i = references.length - 1; i >= 0; --i) {
      if (!(references[i] instanceof AsciiDocFileReference)) {
        continue;
      }
      AsciiDocFileReference fileReference = (AsciiDocFileReference) references[i];
      if (fileReference.isFolder() || fileReference.isAnchor()) {
        continue;
      }
      PsiElement resolve = fileReference.resolve();
      if (resolve instanceof PsiFile) {
        return (PsiFile) resolve;
      }
    }
    return null;
  }

}
