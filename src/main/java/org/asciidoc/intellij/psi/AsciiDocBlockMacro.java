package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
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

/**
 * @author yole
 */
public class AsciiDocBlockMacro extends AsciiDocStandardBlock {
  private static final Set<String> HAS_FILE_AS_BODY = new HashSet<>();
  private static final Set<String> HAS_ATTRIBUTE_BODY = new HashSet<>();

  static {
    HAS_FILE_AS_BODY.addAll(Arrays.asList(
      // standard asciidoctor
      "image", "include", "video", "audio",
      // asciidoctor diagram
      "a2s", "actdiag", "blockdiag", "ditaa", "erd", "graphviz", "meme", "mermaid", "msc",
      "nwdiag", "packetdiag", "plantuml", "rackdiag", "seqdiag", "shaape", "svgbob",
      "syntrax", "umlet", "vega", "vegalite", "wavedrom"
    ));
    HAS_ATTRIBUTE_BODY.addAll(Arrays.asList(
      // standard asciidoctor
      "ifdef", "ifndef"
    ));
  }

  public AsciiDocBlockMacro(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    if (HAS_FILE_AS_BODY.contains(getMacroName())) {
      TextRange range = getRangeOfBody(this);
      if (!range.isEmpty()) {
        String file = this.getText().substring(range.getStartOffset(), range.getEndOffset());
        ArrayList<PsiReference> references = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < file.length(); ++i) {
          if (file.charAt(i) == '/') {
            references.add(
              new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
                TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i),
                false)
            );
            start = i + 1;
          }
        }
        references.add(
          new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
            TextRange.create(range.getStartOffset() + start, range.getStartOffset() + file.length()),
            false)
        );
        return references.toArray(new PsiReference[0]);
      }
    } else if ("operation".equals(getMacroName())) {
      TextRange range = getRangeOfBody(this);
      VirtualFile springRestDocSnippets = AsciiDocUtil.findSpringRestDocSnippets(this);
      if (!range.isEmpty() && springRestDocSnippets != null) {
        String file = this.getText().substring(range.getStartOffset(), range.getEndOffset());
        ArrayList<PsiReference> references = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < file.length(); ++i) {
          if (file.charAt(i) == '/') {
            references.add(
              new AsciiDocFileReference(this, getMacroName(),
                springRestDocSnippets.getPath() + "/" + file.substring(0, start),
                TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i),
                true)
            );
            start = i + 1;
          }
        }
        references.add(
          new AsciiDocFileReference(this, getMacroName(),
            springRestDocSnippets.getPath() + "/" + file.substring(0, start),
            TextRange.create(range.getStartOffset() + start, range.getStartOffset() + file.length()),
            true)
        );
        return references.toArray(new PsiReference[0]);
      }
    } else if (HAS_ATTRIBUTE_BODY.contains(getMacroName())) {
      ASTNode bodyNode = getNode().findChildByType(AsciiDocTokenTypes.BLOCK_MACRO_BODY);
      if (bodyNode != null) {
        String file = bodyNode.getText();
        ArrayList<PsiReference> references = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < file.length(); ++i) {
          if (file.charAt(i) == ',' || file.charAt(i) == '+') {
            references.add(
              new AsciiDocAttributeDeclarationReference(this,
                TextRange.create(bodyNode.getPsi().getStartOffsetInParent() + start, bodyNode.getPsi().getStartOffsetInParent() + i)
              )
            );
            start = i + 1;
          }
        }
        references.add(
          new AsciiDocAttributeDeclarationReference(this,
            TextRange.create(bodyNode.getPsi().getStartOffsetInParent() + start, bodyNode.getPsi().getStartOffsetInParent() + file.length())
          )
        );
        return references.toArray(new PsiReference[0]);
      }
    }
    return super.getReferences();
  }

  @NotNull
  @Override
  public String getDescription() {
    String title = getTitle();
    String style = getStyle();
    if (title == null) {
      if (style == null) {
        title = getMacroName();
      } else {
        title = "";
      }
    }
    if (style != null) {
      return "[" + style + "]" + (title.isEmpty() ? "" : " ") + title;
    }
    return title;
  }

  public String getMacroName() {
    ASTNode idNode = getNode().findChildByType(AsciiDocTokenTypes.BLOCK_MACRO_ID);
    if (idNode == null) {
      throw new IllegalStateException("Parser failure: block macro without ID found: " + getText());
    }
    return StringUtil.trimEnd(idNode.getText(), "::");

  }

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.MACRO;
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocBlockMacro> {

    @Override
    public AsciiDocBlockMacro handleContentChange(@NotNull AsciiDocBlockMacro element,
                                                  @NotNull TextRange range,
                                                  String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && range.getStartOffset() >= child.getTextLength()) {
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
    public TextRange getRangeInElement(@NotNull AsciiDocBlockMacro element) {
      return getRangeOfBody(element);
    }
  }

  private static TextRange getRangeOfBody(AsciiDocBlockMacro element) {
    PsiElement child = element.getFirstChild();
    // skip over pre-block until macro ID starts
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.BLOCK_MACRO_ID) {
      child = child.getNextSibling();
    }
    // skip over macro ID
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_MACRO_ID) {
      child = child.getNextSibling();
    }
    if (child == null) {
      return TextRange.EMPTY_RANGE;
    }
    int start = child.getStartOffsetInParent();
    int end = start;
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRS_START) {
      end = child.getStartOffsetInParent() + child.getTextLength();
      child = child.getNextSibling();
    }
    return TextRange.create(start, end);
  }

  @Override
  public Type getType() {
    return Type.BLOCKMACRO;
  }
}
