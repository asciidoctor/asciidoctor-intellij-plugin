package org.asciidoc.intellij;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.asciidoc.intellij.grazie.AsciiDocLanguageSupport;
import org.jetbrains.annotations.NotNull;

/**
 * For given {@link PsiElement}s, check if they should be spell checked and run a tokenizer.
 * This removes for example restricted formatting from the text (like <code>**E**quivalent</code>).
 *
 * @author yole
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocSpellcheckingStrategy extends SpellcheckingStrategy {
  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  /**
   * Return a tokenizer it it is a file's top child, or if is a content root.
   * If it is a context root, the tokenizer will analyze if text tokens are separated by non-printable tokens
   * and will combine them to words.
   */
  @SuppressWarnings("rawtypes")
  @NotNull
  @Override
  public Tokenizer getTokenizer(PsiElement element) {
    // run tokenizing on all top level elements in the file and those marked as root elements in the language support.
    if (languageSupport.isMyContextRoot(element) || element.getParent() instanceof PsiFile) {
      return new Tokenizer<>() {
        @Override
        public void tokenize(@NotNull PsiElement root, TokenConsumer consumer) {
          TokenizingElementVisitor visitor = new TokenizingElementVisitor(root, consumer);
          // if an element has children, run the tokenization on the children. Otherwise on the element itself.
          if (root.getFirstChild() != null) {
            root.acceptChildren(visitor);
          } else {
            root.accept(visitor);
          }
          visitor.flush();
        }
      };
    }
    return EMPTY_TOKENIZER;
  }

  /**
   * After each word-separating token, flush the text contents to the token consumer.
   */
  private class TokenizingElementVisitor extends PsiElementVisitor {
    private final PsiElement root;
    private final TokenConsumer consumer;
    private int offset = 0, length = 0;
    private final StringBuilder sb = new StringBuilder();

    TokenizingElementVisitor(PsiElement root, TokenConsumer consumer) {
      this.root = root;
      this.consumer = consumer;
    }

    @Override
    public void visitElement(@NotNull PsiElement child) {
      AsciiDocLanguageSupport.Behavior elementBehavior = languageSupport.getElementBehavior(root, child);
      switch (elementBehavior) {
        case STEALTH:
        case UNKNOWN:
        case ABSORB:
          length += child.getTextLength();
          if (sb.length() == 0) {
            offset += length;
            length = 0;
          }
          break;
        case SEPARATE:
        case TEXT:
          if (child instanceof PsiWhiteSpace || elementBehavior == AsciiDocLanguageSupport.Behavior.SEPARATE) {
            flush();
            length += child.getTextLength();
            offset += length;
            length = 0;
          } else if (child.getFirstChild() != null) {
            child.acceptChildren(this);
          } else {
            sb.append(child.getText());
            length += child.getTextLength();
          }
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + elementBehavior);
      }
    }

    /**
     * Flush the text collected so far to the splitter for spell checking.
     * Ensure to call this method at the end of the cycle to flush the last bits of the content.
     */
    public void flush() {
      if (sb.length() > 0) {
        consumer.consumeToken(root, sb.toString(), false, offset, TextRange.allOf(sb.toString()), PlainTextSplitter.getInstance());
        sb.setLength(0);
      }
    }
  }

}
