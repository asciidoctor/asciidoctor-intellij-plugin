package org.asciidoc.intellij.indexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer;

/**
 *
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocTodoIndexer extends LexerBasedTodoIndexer {


  @Override
  public Lexer createLexer(OccurrenceConsumer consumer) {
    return AsciiDocIdIndexer.createIndexingLexer(consumer);
  }
}
