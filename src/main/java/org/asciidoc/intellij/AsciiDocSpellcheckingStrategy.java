package org.asciidoc.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
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
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;

/**
 * For given {@link PsiElement}s, check if they should be spell checked and run a tokenizer.
 * This removes for example restricted formatting from the text (like <code>**E**quivalent</code>).
 *
 * @author yole
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocSpellcheckingStrategy extends SpellcheckingStrategy {
  private static final Logger LOG = Logger.getInstance(AsciiDocSpellcheckingStrategy.class);
  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  /**
   * Return a tokenizer if it is a file's top child, or if is a content root.
   * If it is a context root, the tokenizer will analyze if text tokens are separated by non-printable tokens
   * and will combine them to words.
   */
  @SuppressWarnings("rawtypes")
  @NotNull
  @Override
  public Tokenizer getTokenizer(PsiElement element) {
    // run tokenizing on all top level elements in the file and those marked as root elements in the language support.
    if (languageSupport.isMyContextRoot(element) || element.getParent() instanceof PsiFile) {
      if (element instanceof AsciiDocAttributeDeclarationImpl asciiDocAttributeDeclarationImpl) {
        if (!asciiDocAttributeDeclarationImpl.hasSpellCheckableContent()) {
          // the AsciiDocAttributeDeclarationImpl is special, as it is sometimes a root element, sometimes not
          return EMPTY_TOKENIZER;
        }
      }
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
        case ABSORB:
          length += child.getTextLength();
          if (sb.length() == 0) {
            offset += length;
            length = 0;
          }
          break;
        case UNKNOWN:
        case SEPARATE:
        case TEXT:
          if (child instanceof PsiWhiteSpace || elementBehavior == AsciiDocLanguageSupport.Behavior.SEPARATE
            || elementBehavior == AsciiDocLanguageSupport.Behavior.UNKNOWN) {
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
        consumeToken(sb.toString());
        sb.setLength(0);
      }
    }

    /**
     * Older versions of LanguageTool bundled with Grazie (fixed in IntelliJ 2026.3) keep their languages in non-thread-safe lists,
     * which fail when Grazie initializes languages while another thread spell checks.
     * See <a href="https://github.com/languagetool-org/languagetool/pull/12050">LanguageTool PR 12050</a> for the upstream fix.
     * Retry once for a transient failure, and otherwise skip the token; the next inspection run will check it again.
     */
    private void consumeToken(String text) {
      for (int attempt = 1; ; attempt++) {
        try {
          consumer.consumeToken(root, text, false, offset, TextRange.allOf(text), PlainTextSplitter.getInstance());
          return;
        } catch (ProcessCanceledException e) {
          throw e;
        } catch (RuntimeException e) {
          if (!isLanguageToolRace(e)) {
            throw e;
          }
          if (attempt == 1 && hasCause(e, ConcurrentModificationException.class)) {
            continue;
          }
          LOG.debug("skipping spell check of token due to concurrent LanguageTool initialization", e);
          return;
        }
      }
    }
  }

  /**
   * Detect failures caused by unsynchronized access to LanguageTool's static language lists.
   */
  static boolean isLanguageToolRace(Throwable throwable) {
    for (Throwable t = throwable; t != null; t = t.getCause()) {
      if (t instanceof ProcessCanceledException) {
        return false;
      }
      if (t instanceof ConcurrentModificationException || t instanceof NullPointerException
        || t instanceof ArrayIndexOutOfBoundsException) {
        for (StackTraceElement element : t.getStackTrace()) {
          if ("org.languagetool.Languages".equals(element.getClassName())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
    for (Throwable t = throwable; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }

}
