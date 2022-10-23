package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.parser.AsciiDocParserImpl;
import org.jetbrains.annotations.NotNull;

public class AsciiDocHeading extends AsciiDocASTWrapperPsiElement {
  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocWrapper.class);

  private static final TokenSet HEADINGS = TokenSet.create(AsciiDocTokenTypes.HEADING_TOKEN, AsciiDocTokenTypes.HEADING_OLDSTYLE);

  public AsciiDocHeading(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  private static String trimHeading(String text) {
    if (text.charAt(0) == '=') {
      // new style heading
      text = StringUtil.trimLeading(text, '=').trim();
    } else if (text.charAt(0) == '#') {
      // markdown style heading
      text = StringUtil.trimLeading(text, '#').trim();
    } else {
      // old style heading
      text = text.replaceAll("[-=~^+\n \t]*$", "");
    }
    return text;
  }

  public static final Key<CachedValue<String>> KEY_ASCIIDOC_HEADING_SUBS = new Key<>("asciidoc-heading-nosubs");
  public static final Key<CachedValue<String>> KEY_ASCIIDOC_HEADING_NOSUBS = new Key<>("asciidoc-heading-nosubs");

  public String getHeadingText(boolean substitution) {
    return CachedValuesManager.getCachedValue(this, substitution ? KEY_ASCIIDOC_HEADING_SUBS : KEY_ASCIIDOC_HEADING_NOSUBS,
      () -> {
        StringBuilder sb = new StringBuilder();
        PsiElement child = getFirstChild();
        boolean hasAttribute = false;
        while (child != null) {
          if (child.getNode() != null && HEADINGS.contains(child.getNode().getElementType())) {
            sb.append(child.getText());
          } else if (child instanceof AsciiDocAttributeReference) {
            hasAttribute = true;
            sb.append(child.getText());
          }
          child = child.getNextSibling();
        }
        if (hasAttribute && substitution) {
          try {
            String resolved = AsciiDocUtil.resolveAttributes(this, sb.toString());
            if (resolved != null && resolved.length() > 0) {
              sb.replace(0, sb.length(), resolved);
            }
          } catch (IndexNotReadyException ex) {
            // noop
          }
        }
        if (sb.length() == 0) {
          LOG.error("unable to extract heading text", AsciiDocPsiImplUtil.getRuntimeException("unable to extract heading text", this.getParent(), null));
          sb.replace(0, sb.length(), "???");
        }
        Object dep;
        if (hasAttribute && substitution) {
          dep = PsiModificationTracker.MODIFICATION_COUNT;
        } else {
          // as the calculated value depends only on the PSI node and its subtree, try to be more specific than the PsiElement
          // as using the PsiElement would invalidate the cache on the file level.
          dep = (ModificationTracker) this::getModificationCount;
        }
        return CachedValueProvider.Result.create(trimHeading(sb.toString()), dep);
      }
    );
  }

  public int getHeadingLevel() {
    return AsciiDocParserImpl.headingLevel(getNode().getChars());
  }

  public AsciiDocBlockId getBlockId() {
    AsciiDocBlockId blockId = findLastChildByType(AsciiDocElementTypes.BLOCKID);

    // only if the block ID is the last element in the heading it is the block ID of the section
    if (blockId != null &&
      blockId.getNextSibling() != null &&
      blockId.getNextSibling().getNode() != null &&
      blockId.getNextSibling().getNode().getElementType() == AsciiDocTokenTypes.INLINEIDEND &&
      blockId.getNextSibling().getNextSibling() == null) {
      return blockId;
    }

    return null;
  }

  private long myModificationStamp;
  @Override
  public void subtreeChanged() {
    ++myModificationStamp;
    super.subtreeChanged();
  }
  @Override
  public long getModificationCount() {
    return myModificationStamp;
  }

}
