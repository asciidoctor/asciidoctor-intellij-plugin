package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AsciiDocInlineMacro extends ASTWrapperPsiElement {
  private static final Set<String> HAS_FILE_AS_BODY = new HashSet<>();

  static {
    HAS_FILE_AS_BODY.addAll(Arrays.asList(
      // standard asciidoctor
      "image", "video", "audio",
      // asciidoctor diagram
      "a2s", "actdiag", "blockdiag", "ditaa", "erd", "graphviz", "meme", "mermaid", "msc",
      "nwdiag", "packetdiag", "plantuml", "rackdiag", "seqdiag", "shaape", "svgbob",
      "syntrax", "umlet", "vega", "vegalite", "wavedrom"
    ));
  }

  public AsciiDocInlineMacro(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    if (HAS_FILE_AS_BODY.contains(getMacroName())) {
      ASTNode bodyNode = getNode().findChildByType(AsciiDocTokenTypes.INLINE_MACRO_BODY);
      if (bodyNode != null) {
        String file = bodyNode.getText();
        ArrayList<PsiReference> references = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < file.length(); ++i) {
          if (file.charAt(i) == '/') {
            references.add(
              new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
                TextRange.create(bodyNode.getPsi().getStartOffsetInParent() + start, bodyNode.getPsi().getStartOffsetInParent() + i),
                false)
            );
            start = i + 1;
          }
        }
        references.add(
          new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
            TextRange.create(bodyNode.getPsi().getStartOffsetInParent() + start, bodyNode.getPsi().getStartOffsetInParent() + file.length()),
            false)
        );
        return references.toArray(new PsiReference[0]);
      }
    }
    return super.getReferences();
  }

  public String getMacroName() {
    ASTNode idNode = getNode().findChildByType(AsciiDocTokenTypes.INLINE_MACRO_ID);
    if (idNode == null) {
      throw new IllegalStateException("Parser failure: block macro without ID found: " + getText());
    }
    return StringUtil.trimEnd(idNode.getText(), ":");

  }

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.MACRO;
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocInlineMacro> {

    @Override
    public AsciiDocInlineMacro handleContentChange(@NotNull AsciiDocInlineMacro element,
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
    public TextRange getRangeInElement(@NotNull AsciiDocInlineMacro element) {
      PsiElement child = element.findChildByType(AsciiDocTokenTypes.INLINE_MACRO_BODY);
      if (child != null) {
        return TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength());
      } else {
        return TextRange.EMPTY_RANGE;
      }
    }
  }

}
