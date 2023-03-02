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

public class AsciiDocInlineMacro extends AsciiDocASTWrapperPsiElement implements HasFileReference, HasAntoraReference {
  public static final Set<String> HAS_FILE_AS_BODY = new HashSet<>(Arrays.asList(
      // standard asciidoctor
      "image", "video", "audio",
      // asciidoctor diagram
      "a2s", "actdiag", "blockdiag", "d2", "dbml", "ditaa", "erd", "graphviz", "meme",
      "mermaid", "msc", "nwdiag", "packetdiag", "plantuml", "rackdiag", "seqdiag",
      "shaape", "svgbob", "syntrax", "umlet", "vega", "vegalite", "wavedrom"
  ));

  public AsciiDocInlineMacro(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    if (HAS_FILE_AS_BODY.contains(getMacroName())) {
      TextRange range = getRangeOfBody(this);
      if (!range.equals(TextRange.EMPTY_RANGE)) {
        String file = range.substring(this.getText());
        ArrayList<PsiReference> references = new ArrayList<>();
        int start = 0;
        int i = 0;
        boolean isAntora = false;
        if (ANTORA_SUPPORTED.contains(getMacroName())) {
          Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(file);
          Matcher matcher = AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(file);
          if (!urlMatcher.find() && matcher.find()) {
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

  @Nullable
  public String getResolvedBody() {
    String text = getRangeOfBody(this).substring(getText());
    return AsciiDocUtil.resolveAttributes(this, text);
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
      while (child != null && child.getNode() != null && child.getNode().getElementType() != AsciiDocTokenTypes.INLINE_MACRO_BODY) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement && range.getEndOffset() <= child.getTextLength()) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        AsciiDocPsiImplUtil.throwExceptionCantHandleContentChange(element, range, newContent);
      }

      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocInlineMacro element) {
      if (element.getMacroName().equals("footnote")) {
        // ensure to return the text, as only that will be checked by the grammar/spell checker for root elements
        return TextRange.from(0, element.getTextLength());
      }
      return getRangeOfBody(element);
    }

  }

  private static TextRange getRangeOfBody(AsciiDocInlineMacro element) {
    PsiElement child = element.getFirstChild();
    // skip over pre-block until macro ID starts
    while (child != null && child.getNode() != null && child.getNode().getElementType() != AsciiDocTokenTypes.INLINE_MACRO_ID) {
      child = child.getNextSibling();
    }
    // skip over macro ID
    while (child != null && child.getNode() != null && child.getNode().getElementType() == AsciiDocTokenTypes.INLINE_MACRO_ID) {
      child = child.getNextSibling();
    }
    if (child == null) {
      return TextRange.EMPTY_RANGE;
    }
    int start = child.getStartOffsetInParent();
    int end = start;
    while (child != null && child.getNode() != null && child.getNode().getElementType() != AsciiDocTokenTypes.INLINE_ATTRS_START) {
      end = child.getStartOffsetInParent() + child.getTextLength();
      child = child.getNextSibling();
    }
    return TextRange.create(start, end);
  }

}
