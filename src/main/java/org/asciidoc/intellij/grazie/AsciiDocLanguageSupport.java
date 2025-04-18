package org.asciidoc.intellij.grazie;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationImpl;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocHtmlEntity;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocModificationTracker;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.asciidoc.intellij.psi.AsciiDocUrl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AsciiDocLanguageSupport {

  public enum Behavior {
    /**
     * A PSI element that contains a nested text and should be ignored.
     * <p>
     * Example: The "Headline" is a nested element in the section; it is treated as its own sentence.
     * <p>
     * <pre>
     * == Headline
     * More text
     * </pre>
     * <p>
     */
    ABSORB,
    /**
     * A PSI element that <b>WILL NOT</b> be printed, adjacent text is part of the same word.
     * <p>
     * Example: the "**" would be STEALTH<br>
     * <code>**b**old</code> is one word "bold"
     */
    STEALTH,
    /**
     * A PSI that contains text that should be spell and grammar checked.
     * <p>
     * Example: the "b" and "old" would be TEXT<br>
     * <code>**b**old</code> is one word "bold"
     */
    TEXT,
    /**
     * A PSI that that <b>WILL</b> be printed, adjacent text represents different words.
     * <p>
     * Example: the {@code ->} would be "SEPARATE"<br>
     * {@code one->two} is two words "one" and "two"
     */
    SEPARATE,
    /**
     * A PSI that that <b>WILL</b> be printed, but with unknown text.
     * <p>
     * Example: an attribute {@code a {attr}} would print some text <br>
     * but the contents are unknown.
     */
    UNKNOWN
  }

  // all tokens that contain full sentences that can be checked for grammar and spelling.
  private static final TokenSet NODES_TO_CHECK = TokenSet.create(
    AsciiDocTokenTypes.LINE_COMMENT,
    AsciiDocTokenTypes.BLOCK_COMMENT,
    AsciiDocTokenTypes.LITERAL_BLOCK,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocElementTypes.SECTION,
    AsciiDocElementTypes.TITLE,
    AsciiDocElementTypes.BLOCK,
    AsciiDocElementTypes.DESCRIPTION_ITEM,
    AsciiDocElementTypes.DESCRIPTION_TERM,
    AsciiDocElementTypes.LIST_ITEM,
    AsciiDocElementTypes.CELL,
    AsciiDocElementTypes.HEADING
  );

  /** All tokens that contain full sentences that can be checked for grammar and spelling.
   * The contents of these tokens will not be forwarded to the grammar or spell checker.
   * Example: END_OF_SENTENCE is in the Text category, as it needs to be passed to the grammar checker as a ".", "?" or other text,
   * so that the grammar checker recognizes the end of a sentence.
   */
  private static final TokenSet SEPARATOR_TOKENS = TokenSet.create(
    AsciiDocTokenTypes.ARROW,
    AsciiDocTokenTypes.LBRACKET,
    AsciiDocTokenTypes.RBRACKET,
    AsciiDocTokenTypes.LPAREN,
    AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.DOUBLE_QUOTE,
    AsciiDocTokenTypes.SINGLE_QUOTE,
    AsciiDocTokenTypes.SUBSCRIPT_START,
    AsciiDocTokenTypes.SUPERSCRIPT_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END,
    AsciiDocTokenTypes.ASSIGNMENT,
    AsciiDocTokenTypes.CELLSEPARATOR,
    AsciiDocTokenTypes.BULLET,
    AsciiDocTokenTypes.ENUMERATION,
    AsciiDocTokenTypes.ADMONITION,
    AsciiDocTokenTypes.CALLOUT
  );


  // all tokens that contain text that is part of a sentence and can be a sub-node of the elements above
  private static final TokenSet TEXT_TOKENS = TokenSet.orSet(TokenSet.create(
    AsciiDocTokenTypes.TEXT,
    AsciiDocTokenTypes.ITALIC,
    AsciiDocTokenTypes.BOLD,
    AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.MONO,
    AsciiDocTokenTypes.MONOBOLD,
    AsciiDocTokenTypes.DESCRIPTION,
    AsciiDocTokenTypes.MACROTEXT,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocTokenTypes.REFTEXT,
    AsciiDocTokenTypes.MONOITALIC,
    AsciiDocTokenTypes.MONOBOLDITALIC,
    AsciiDocTokenTypes.END_OF_SENTENCE,
    AsciiDocTokenTypes.PASSTRHOUGH_CONTENT,
    AsciiDocTokenTypes.LT,
    AsciiDocTokenTypes.GT,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END,
    AsciiDocTokenTypes.LPAREN,
    AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.LBRACKET,
    AsciiDocTokenTypes.RBRACKET,
    AsciiDocTokenTypes.BULLET,
    AsciiDocTokenTypes.ATTRIBUTE_VAL, // will only get here if attribute is classified to contain spell checkable content
    AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION,
    AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY,
    // keep the white space in here as blanks are necessary to separate words
    AsciiDocTokenTypes.WHITE_SPACE,
    AsciiDocTokenTypes.WHITE_SPACE_MONO,
    AsciiDocTokenTypes.HEADING_TOKEN,
    AsciiDocTokenTypes.HEADING_OLDSTYLE,
    TokenType.WHITE_SPACE,
    AsciiDocElementTypes.URL, // can nest MACROTEXT, or will show the URL_LINK or URL_EMAIL as default
    AsciiDocElementTypes.REF, // can nest REFTEXT
    AsciiDocElementTypes.LINK, // can nest MACROTEXT
    AsciiDocElementTypes.INLINE_MACRO, // can nest MACROTEXT
    AsciiDocElementTypes.QUOTED // will nest MONO, ITALIC and others
  ), NODES_TO_CHECK);

  // all tokens that contain text that is part of a sentence and can be a sub-node of the elements above
  private static final TokenSet UNKNOWN_TOKENS = TokenSet.create(
    AsciiDocTokenTypes.ATTRIBUTE_REF_START,
    AsciiDocTokenTypes.ATTRIBUTE_REF,
    AsciiDocTokenTypes.ATTRIBUTE_REF_END);

  private static final Set<String> SEPARATING_ATTRIBUTES = new HashSet<>();
  static {
    SEPARATING_ATTRIBUTES.add("sp");
    SEPARATING_ATTRIBUTES.add("zwsp");
    SEPARATING_ATTRIBUTES.add("apos");
    SEPARATING_ATTRIBUTES.add("quot");
    SEPARATING_ATTRIBUTES.add("lsquo");
    SEPARATING_ATTRIBUTES.add("rsquo");
    SEPARATING_ATTRIBUTES.add("ldquo");
    SEPARATING_ATTRIBUTES.add("rdquo");
  }

  private static final Set<String> EMPTY_ATTRIBUTES = new HashSet<>();
  static {
    EMPTY_ATTRIBUTES.add("blank");
    EMPTY_ATTRIBUTES.add("empty");
    EMPTY_ATTRIBUTES.add("wj");
  }

  public Behavior getElementBehavior(@NotNull PsiElement root, @NotNull PsiElement child) {
    if (root != child && NODES_TO_CHECK.contains(getElementType(child))) {
      return Behavior.ABSORB;
    } else if (root == child && child instanceof AsciiDocAttributeDeclarationImpl) {
      if (((AsciiDocAttributeDeclarationImpl) child).hasSpellCheckableContent()) {
        return Behavior.TEXT;
      } else {
        return Behavior.ABSORB;
      }
    } else if (child instanceof AsciiDocAttributeReference) {
      AsciiDocAttributeReference attributeReference = (AsciiDocAttributeReference) child;
      if (SEPARATING_ATTRIBUTES.contains(attributeReference.getName())) {
        return Behavior.SEPARATE;
      } else if (EMPTY_ATTRIBUTES.contains(attributeReference.getName())) {
          return Behavior.ABSORB;
      } else {
        return Behavior.UNKNOWN;
      }
    } else if (child instanceof AsciiDocHtmlEntity) {
      return Behavior.UNKNOWN;
    } else if (child instanceof AsciiDocTextQuoted && ((AsciiDocTextQuoted) child).isMono()) {
      return Behavior.UNKNOWN;
    } else if (SEPARATOR_TOKENS.contains(getElementType(child))) {
      return Behavior.SEPARATE;
    } else if (root != child && child instanceof AsciiDocInlineMacro && ((AsciiDocInlineMacro) child).getMacroName().equals("footnote")) {
      return Behavior.ABSORB;
    } else if (
      // A link or URL can contain either a macro text or no text.
      // AsciiDoc will display the macro text, or the link/email address if no such text is provided.
      // Pass on the content that would be displayed by AsciiDoc to the grammar check.
      ((getElementType(child) == AsciiDocTokenTypes.URL_LINK || getElementType(child) == AsciiDocTokenTypes.URL_EMAIL) &&
        isChildOfLinkOrUrl(child))) {
      boolean macroTextPresent = false;
      ASTNode node = child.getNode();
      while (node != null) {
        if (node.getElementType() == AsciiDocTokenTypes.MACROTEXT) {
          macroTextPresent = true;
          break;
        }
        node = node.getTreeNext();
      }
      if (macroTextPresent) {
        return Behavior.STEALTH;
      } else {
        return Behavior.TEXT;
      }
    } else if (child instanceof AsciiDocInlineMacro
      || (child instanceof AsciiDocLink && ((AsciiDocLink) child).getMacroName().equals("xref"))
      || (child instanceof AsciiDocRef)) {
      if (child instanceof AsciiDocInlineMacro && ((AsciiDocInlineMacro) child).getMacroName().equals("kbd")) {
        // mark keyboard macros as "unknown" to avoid showing spell checker or grammar errors for them
        return Behavior.UNKNOWN;
      }
      // an inline macro or an xref will be treated as unknown if they don't contain text
      LookingForMacroTextVisitor visitor = new LookingForMacroTextVisitor();
      child.accept(visitor);
      if (visitor.hasFound()) {
        return Behavior.TEXT;
      } else {
        return Behavior.UNKNOWN;
      }
    } else if (TEXT_TOKENS.contains(getElementType(child))) {
      return Behavior.TEXT;
    } else if (UNKNOWN_TOKENS.contains(getElementType(child))) {
      return Behavior.UNKNOWN;
    } else {
      return Behavior.STEALTH;
    }
  }

  private static IElementType getElementType(@NotNull PsiElement child) {
    ASTNode node = child.getNode();
    if (node != null) {
      return node.getElementType();
    }
    return null;
  }

  private boolean isChildOfLinkOrUrl(@NotNull PsiElement child) {
    PsiElement parent = child.getParent();
    return parent instanceof AsciiDocLink || parent instanceof AsciiDocUrl;
  }

  private static class LookingForMacroTextVisitor extends PsiElementVisitor {
    private boolean found = false;

    public boolean hasFound() {
      return found;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
      super.visitElement(element);
      if (getElementType(element) == AsciiDocTokenTypes.MACROTEXT || getElementType(element) == AsciiDocTokenTypes.REFTEXT) {
        found = true;
        return;
      }
      PsiElement child = element.getFirstChild();
      while (child != null && !found) {
        visitElement(child);
        child = child.getNextSibling();
      }
    }
  }

  public static boolean containsOnlySpaces(PsiElement child) {
    return child.getNode() != null && child.getNode().getChars().chars().noneMatch(c -> c != ' ');
  }

  private static final Key<CachedValue<Boolean>> KEY_ASCIIDOC_CONTEXT_ROOT = new Key<>("asciidoc-contextroot");

  public boolean isMyContextRoot(@NotNull PsiElement psiElement) {
    return CachedValuesManager.getCachedValue(psiElement, KEY_ASCIIDOC_CONTEXT_ROOT,
      () -> {
        boolean result;
        if (psiElement instanceof AsciiDocAttributeDeclarationImpl) {
          result = ((AsciiDocAttributeDeclarationImpl) psiElement).hasSpellCheckableContent();
        } else if (psiElement instanceof AsciiDocInlineMacro &&
          ((AsciiDocInlineMacro) psiElement).getMacroName().equals("footnote")) {
          result = true;
        } else {
          result = NODES_TO_CHECK.contains(getElementType(psiElement))
            || psiElement instanceof PsiComment;
        }
        // as the calculated value depends only on the PSI node and its subtree, try to be more specific than the PsiElement
        // as using the PsiElement would invalidate the cache on the file level.
        Object dep = psiElement;
        if (psiElement instanceof AsciiDocModificationTracker) {
          dep = (ModificationTracker) () -> ((AsciiDocModificationTracker) psiElement).getModificationCount();
        }
        return CachedValueProvider.Result.create(result, dep);
      }
    );


  }

}
