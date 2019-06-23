
package org.asciidoc.intellij.parser;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;

public class AsciiDocWordsScanner extends DefaultWordsScanner {

  public AsciiDocWordsScanner() {
    super(new AsciiDocLexer(),
      // identifiers
      TokenSet.create(AsciiDocTokenTypes.BLOCKID, AsciiDocTokenTypes.REF, AsciiDocTokenTypes.LINKANCHOR,
        AsciiDocTokenTypes.ATTRIBUTE_REF),
      // comments
      TokenSet.create(AsciiDocTokenTypes.BLOCK_COMMENT, AsciiDocTokenTypes.LINE_COMMENT),
      // literals
      TokenSet.create(AsciiDocTokenTypes.TEXT, AsciiDocTokenTypes.BOLD, AsciiDocTokenTypes.MONO,
        AsciiDocTokenTypes.MONOBOLD, AsciiDocTokenTypes.MONOITALIC, AsciiDocTokenTypes.BOLDITALIC,
        AsciiDocTokenTypes.MONOBOLDITALIC));
  }
}
