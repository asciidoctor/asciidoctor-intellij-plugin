package org.asciidoc.intellij.indexer;

import com.intellij.lexer.EmptyLexer;
import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer;

/**
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocIdIndexer extends LexerBasedIdIndexer {

  public static Lexer createIndexingLexer(OccurrenceConsumer consumer) {
    return new AsciiDocFilterLexer(new EmptyLexer(), consumer);
  }

  @Override
  public Lexer createLexer(final OccurrenceConsumer consumer) {
    return createIndexingLexer(consumer);
  }
}
