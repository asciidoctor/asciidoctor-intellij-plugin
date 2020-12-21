package org.asciidoc.intellij.grazie;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationImpl;
import org.jetbrains.annotations.NotNull;

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
    SEPARATE
  }

  // all tokens that contain full sentences that can be checked for grammar and spelling.
  private static final TokenSet NODES_TO_CHECK = TokenSet.create(
    AsciiDocTokenTypes.HEADING_TOKEN,
    AsciiDocTokenTypes.HEADING_OLDSTYLE,
    AsciiDocTokenTypes.TITLE_TOKEN,
    AsciiDocTokenTypes.LINE_COMMENT,
    AsciiDocTokenTypes.BLOCK_COMMENT,
    AsciiDocTokenTypes.LITERAL_BLOCK,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocElementTypes.SECTION,
    AsciiDocElementTypes.BLOCK,
    AsciiDocElementTypes.CELL
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
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END,
    AsciiDocTokenTypes.ASSIGNMENT,
    AsciiDocTokenTypes.CELLSEPARATOR,
    AsciiDocTokenTypes.BULLET,
    AsciiDocTokenTypes.ENUMERATION,
    AsciiDocTokenTypes.ADMONITION,
    AsciiDocTokenTypes.CALLOUT,
    AsciiDocTokenTypes.LT,
    AsciiDocTokenTypes.GT
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
    AsciiDocTokenTypes.LINKTEXT,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocTokenTypes.REFTEXT,
    AsciiDocTokenTypes.MONOITALIC,
    AsciiDocTokenTypes.MONOBOLDITALIC,
    AsciiDocTokenTypes.END_OF_SENTENCE,
    AsciiDocTokenTypes.LPAREN,
    AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.LBRACKET,
    AsciiDocTokenTypes.RBRACKET,
    AsciiDocTokenTypes.BULLET,
    AsciiDocTokenTypes.ATTRIBUTE_VAL, // will only get here if attribute is classified to contain spell checkable content
    // keep the white space in here as blanks are necessary to separate words
    AsciiDocTokenTypes.WHITE_SPACE,
    AsciiDocTokenTypes.WHITE_SPACE_MONO,
    TokenType.WHITE_SPACE,
    AsciiDocElementTypes.URL, // can nest LINKTEXT
    AsciiDocElementTypes.REF, // can nest REFTEXT
    AsciiDocElementTypes.LINK, // can nest LINKTEXT
    AsciiDocElementTypes.MONO, // will nest MONO
    AsciiDocElementTypes.ITALIC // will nest ITALIC
  ), NODES_TO_CHECK);

  public Behavior getElementBehavior(@NotNull PsiElement root, @NotNull PsiElement child) {
    if (root != child && NODES_TO_CHECK.contains(child.getNode().getElementType())) {
      return Behavior.ABSORB;
    } else if (root != child && child instanceof AsciiDocAttributeDeclarationImpl) {
      if (((AsciiDocAttributeDeclarationImpl) child).hasSpellCheckableContent()) {
        return Behavior.ABSORB;
      } else {
        return Behavior.STEALTH;
      }
    } else if (SEPARATOR_TOKENS.contains(child.getNode().getElementType())) {
      return Behavior.SEPARATE;
    } else if (TEXT_TOKENS.contains(child.getNode().getElementType())) {
      return Behavior.TEXT;
    } else {
      return Behavior.STEALTH;
    }
  }

  public boolean isMyContextRoot(@NotNull PsiElement psiElement) {
    if (psiElement instanceof AsciiDocAttributeDeclarationImpl &&
      ((AsciiDocAttributeDeclarationImpl) psiElement).hasSpellCheckableContent()) {
      return true;
    }
    return NODES_TO_CHECK.contains(psiElement.getNode().getElementType())
      || psiElement instanceof PsiComment;
  }

}
