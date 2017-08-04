package org.asciidoc.intellij.indexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer;
import org.asciidoc.intellij.lexer.AsciiDocLexerAdapter;

/**
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocIdIndexer extends LexerBasedIdIndexer {

  public static Lexer createIndexingLexer(OccurrenceConsumer consumer) {
    return new AsciiDocFilterLexer(new AsciiDocLexerAdapter(), consumer);
  }

  @Override
  public Lexer createLexer(final OccurrenceConsumer consumer) {
    return createIndexingLexer(consumer);
  }
}
