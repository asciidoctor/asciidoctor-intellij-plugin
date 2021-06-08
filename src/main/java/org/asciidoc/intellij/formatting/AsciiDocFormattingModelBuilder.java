package org.asciidoc.intellij.formatting;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocFormattingModelBuilder implements FormattingModelBuilder {
  @NotNull
  @Override
  public FormattingModel createModel(FormattingContext context) {
    final PsiElement element = context.getPsiElement();
    final ASTNode root = TreeUtil.getFileElement((TreeElement) element.getNode());
    final FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(element.getContainingFile());
    return new PsiBasedFormattingModel(element.getContainingFile(), new AsciiDocBlock(root, context.getCodeStyleSettings()), documentModel);
  }

  @Nullable
  @Override
  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }
}
