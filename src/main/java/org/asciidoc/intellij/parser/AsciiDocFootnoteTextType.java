package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocLazyElementType;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.jetbrains.annotations.NotNull;

/**
 * The text of a footnote is lexed as one chunk by the main lexer, and parsed lazily with a lexer that starts inside a line.
 * This allows cross-references, macros and formatting in footnotes to be parsed.
 */
public class AsciiDocFootnoteTextType extends AsciiDocLazyElementType {
  public AsciiDocFootnoteTextType(String name) {
    super(name);
  }

  @Override
  public ASTNode parseContents(@NotNull ASTNode chameleon) {
    Lexer lexer = AsciiDocLexer.inline();
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
      LanguageParserDefinitions.INSTANCE.forLanguage(AsciiDocLanguage.INSTANCE), lexer, chameleon.getText());
    PsiBuilder.Marker root = builder.mark();
    new AsciiDocParserImpl(builder).parseInline();
    root.done(this);
    return builder.getTreeBuilt().getFirstChildNode();
  }
}
