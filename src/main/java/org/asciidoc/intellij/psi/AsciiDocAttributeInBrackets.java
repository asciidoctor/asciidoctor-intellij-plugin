package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class AsciiDocAttributeInBrackets extends ASTWrapperPsiElement {

  public AsciiDocAttributeInBrackets(@NotNull ASTNode node) {
    super(node);
  }

  private static final Pattern LINK = Pattern.compile("^(https?|file|ftp|irc)://");

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    if ("link".equals(getAttrName())) {
      TextRange rangeOfBody = getRangeOfBody(this);
      if (!TextRange.EMPTY_RANGE.equals(rangeOfBody)) {
        String file = rangeOfBody.substring(this.getText());
        if (!LINK.matcher(file).find()) {
          ArrayList<PsiReference> references = new ArrayList<>();
          int start = 0;
          for (int i = 0; i < file.length(); ++i) {
            if (file.charAt(i) == '/') {
              references.add(
                new AsciiDocFileReference(this, "link-attr", file.substring(0, start),
                  TextRange.create(rangeOfBody.getStartOffset() + start, rangeOfBody.getStartOffset() + i),
                  true)
              );
              start = i + 1;
            }
          }
          references.add(
            new AsciiDocFileReference(this, "link-attr", file.substring(0, start),
              TextRange.create(rangeOfBody.getStartOffset() + start, rangeOfBody.getStartOffset() + file.length()),
              false)
          );
          return references.toArray(new PsiReference[0]);
        }
      }
    }
    return super.getReferences();
  }

  public String getAttrName() {
    ASTNode idNode = getNode().findChildByType(AsciiDocTokenTypes.ATTR_NAME);
    if (idNode == null) {
      return null;
    }
    return idNode.getText();

  }

  public String getAttrValue() {
    TextRange bodyRange = getRangeOfBody(this);
    if (bodyRange.isEmpty()) {
      return null;
    }
    String value = bodyRange.substring(this.getText());
    String resolvedValue = AsciiDocUtil.resolveAttributes(this, value);
    if (resolvedValue != null) {
      value = resolvedValue;
    }
    return value;
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocAttributeInBrackets> {

    @Override
    public AsciiDocAttributeInBrackets handleContentChange(@NotNull AsciiDocAttributeInBrackets element,
                                                           @NotNull TextRange range,
                                                           String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.INLINE_MACRO_BODY) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        throw new IncorrectOperationException("Bad child");
      }

      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocAttributeInBrackets element) {
      return getRangeOfBody(element);
    }

  }

  private static TextRange getRangeOfBody(AsciiDocAttributeInBrackets element) {
    PsiElement child = element.getFirstChild();
    // skip over pre-block until macro ID starts
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ASSIGNMENT) {
      child = child.getNextSibling();
    }
    // skip over macro ID
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.ASSIGNMENT) {
      child = child.getNextSibling();
    }
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.DOUBLE_QUOTE) {
      child = child.getNextSibling();
    }
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.SINGLE_QUOTE) {
      child = child.getNextSibling();
    }
    if (child == null) {
      return TextRange.EMPTY_RANGE;
    }
    int start = child.getStartOffsetInParent();
    int end = start;
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.DOUBLE_QUOTE
      && child.getNode().getElementType() != AsciiDocTokenTypes.SINGLE_QUOTE) {
      end = child.getStartOffsetInParent() + child.getTextLength();
      child = child.getNextSibling();
    }
    return TextRange.create(start, end);
  }

}
