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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_SUPPORTED;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;

/**
 * @author yole
 */
public class AsciiDocBlockMacro extends AsciiDocStandardBlock implements HasFileReference, HasAntoraReference {
  private static final Set<String> HAS_FILE_AS_BODY = new HashSet<>();

  static {
    HAS_FILE_AS_BODY.addAll(Arrays.asList(
      // standard asciidoctor
      "image", "include", "video", "audio",
      // asciidoctor diagram
      "a2s", "actdiag", "blockdiag", "ditaa", "erd", "graphviz", "meme", "mermaid", "msc",
      "nwdiag", "packetdiag", "plantuml", "rackdiag", "seqdiag", "shaape", "svgbob",
      "syntrax", "umlet", "vega", "vegalite", "wavedrom"
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
        int i = 0;
        boolean isAntora = false;
        if (ANTORA_SUPPORTED.contains(getMacroName())) {
          Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(file);
          if (!urlMatcher.find()) {
            Matcher matcher = AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(file);
            if (matcher.find()) {
              VirtualFile examplesDir = AsciiDocUtil.findAntoraModuleDir(this);
              if (examplesDir != null) {
                i += matcher.end();
                isAntora = true;
                references.add(
                  new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
                    TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i - 1),
                    true, isAntora, 1)
                );
                start = i;
              }
            }
            matcher = AsciiDocUtil.ANTORA_FAMILY_PATTERN.matcher(file.substring(start));
            if (matcher.find()) {
              VirtualFile examplesDir = AsciiDocUtil.findAntoraModuleDir(this);
              if (examplesDir != null) {
                i += matcher.end();
                isAntora = true;
                references.add(
                  new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
                    TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i - 1),
                    true, isAntora, 1)
                );
                start = i;
              }
            }
          }
        }
        for (; i < file.length(); ++i) {
          if (file.charAt(i) == '/') {
            references.add(
              new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
                TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i),
                true, isAntora)
            );
            start = i + 1;
          }
        }
        references.add(
          new AsciiDocFileReference(this, getMacroName(), file.substring(0, start),
            TextRange.create(range.getStartOffset() + start, range.getStartOffset() + file.length()),
            false, isAntora)
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
    }
    return super.getReferences();
  }

  @Override
  public String getDefaultTitle() {
    return getMacroName();
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

  @Nullable
  public String getResolvedBody() {
    String text = getRangeOfBody(this).substring(getText());
    return AsciiDocUtil.resolveAttributes(this, text);
  }

  public AsciiDocAttributeInBrackets getAttribute(String attribute) {
    PsiElement child = this.getFirstChild();
    while (child != null) {
      if (child instanceof AsciiDocAttributeInBrackets) {
        if (((AsciiDocAttributeInBrackets) child).getAttrName().equals(attribute)) {
          return (AsciiDocAttributeInBrackets) child;
        }
      }
      child = child.getNextSibling();
    }
    return null;
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

  public boolean hasAttributes() {
    if (getAttributeRange().getLength() == 0) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (getAttributeRange().substring(getText()).trim().length() == 0) {
      return false;
    }
    return true;
  }

  public TextRange getAttributeRange() {
    PsiElement child = this.getFirstChild();
    // skip over pre-block until macro ID starts
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.BLOCK_MACRO_ID) {
      child = child.getNextSibling();
    }
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRS_START) {
      child = child.getNextSibling();
    }
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.ATTRS_START) {
      child = child.getNextSibling();
    }
    if (child == null) {
      return TextRange.EMPTY_RANGE;
    }
    int start = child.getStartOffsetInParent();
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.ATTRS_START) {
      child = child.getNextSibling();
    }
    int end = start;
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRS_END) {
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
