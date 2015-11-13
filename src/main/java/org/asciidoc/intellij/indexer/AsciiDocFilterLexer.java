package org.asciidoc.intellij.indexer;

import com.intellij.lexer.EmptyLexer;
import com.intellij.psi.impl.cache.impl.BaseFilterLexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.search.UsageSearchContext;

/**
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocFilterLexer extends BaseFilterLexer  {

  public AsciiDocFilterLexer(EmptyLexer emptyLexer, OccurrenceConsumer consumer) {
    super(emptyLexer, consumer);
  }

  @Override
  public void advance() {
    scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT, false, false);
    advanceTodoItemCountsInToken();
    myDelegate.advance();
  }

}
