package org.asciidoc.intellij.indexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.jetbrains.annotations.NotNull;

/**
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocIdIndexer extends LexerBasedIdIndexer {

  @Override
  public int getVersion() {
    return 5;
  }

  public static Lexer createIndexingLexer(OccurrenceConsumer consumer) {
    return new AsciiDocFilterLexer(new AsciiDocLexer(), consumer);
  }

  @NotNull
  @Override
  public Lexer createLexer(@NotNull final OccurrenceConsumer consumer) {
    return createIndexingLexer(consumer);
  }
}
