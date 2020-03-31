package org.asciidoc.intellij.indexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.todo.LexerBasedTodoIndexer;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocTodoIndexer extends LexerBasedTodoIndexer {

  @Override
  public int getVersion() {
    return 2;
  }

  @NotNull
  @Override
  public Lexer createLexer(@NotNull OccurrenceConsumer consumer) {
    return AsciiDocIdIndexer.createIndexingLexer(consumer);
  }
}
