package org.asciidoc.intellij.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AsciiDocDiffTest {
  @Test
  public void shouldCreateDiffFromLexerResult() throws DiffException {
    List<LexerElement> oldText = toList("Hello world! This **a** test!");
    List<LexerElement> newText = toList("Hello to the world! This a test!");
    Patch<LexerElement> diff = DiffUtils.diff(oldText, newText);
    System.out.println(diff.getDeltas());
    List<LexerElement> diffText = new ArrayList<>();
    int start = 0;
    for (AbstractDelta<LexerElement> delta : diff.getDeltas()) {
      diffText.addAll(oldText.subList(start, delta.getSource().getPosition()));
      start = delta.getSource().getPosition();
      List<LexerElement> deletions = delta.getSource().getLines();
      List<LexerElement> inserts = delta.getTarget().getLines();
      for (LexerElement insert : inserts) {
        insert.setDeltaType(DeltaType.INSERT);
      }
      for (LexerElement delete : deletions) {
        delete.setDeltaType(DeltaType.DELETE);
      }
      diffText.addAll(deletions);
      diffText.addAll(inserts);
      start += deletions.size();
    }
    diffText.addAll(oldText.subList(start, oldText.size()));
    System.out.println(diffText);
    StringBuilder result = new StringBuilder();
    DeltaType state = null;
    for (LexerElement element : diffText) {
      if (element.getDeltaType() != state) {
        if (state != null) {
          result.append("##");
        }
        state = element.getDeltaType();
        if (state == DeltaType.INSERT) {
          result.append("[.added]##");
        } else if (state == DeltaType.DELETE) {
          result.append("[.removed]##");
        }
      }
      result.append(element.getToken());
    }
    if (state != null) {
      result.append("##");
    }
    System.out.println(result.toString());
  }

  private List<LexerElement> toList(String doc) {
    List<LexerElement> result = new ArrayList<>();
    Lexer lexer = createLexer();
    lexer.start(doc, 0, doc.length());
    IElementType tokenType;
    while ((tokenType = lexer.getTokenType()) != null) {
      result.add(new LexerElement(tokenType, doc.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
      lexer.advance();
    }
    return result;
  }

  protected Lexer createLexer() {
    return new AsciiDocLexer();
  }

  public static class LexerElement {
    private final IElementType element;
    private final String token;
    private DeltaType deltaType;

    private LexerElement(IElementType element, String token) {
      this.element = element;
      this.token = token;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LexerElement that = (LexerElement) o;
      return element.equals(that.element) &&
        token.equals(that.token);
    }

    @Override
    public int hashCode() {
      return Objects.hash(element, token);
    }

    @Override
    public String toString() {
      return (deltaType != null ? deltaType.toString() + ":" : "") + element + "[" + token + "]";
    }

    public String getToken() {
      return token;
    }

    public DeltaType getDeltaType() {
      return deltaType;
    }

    public void setDeltaType(DeltaType deltaType) {
      this.deltaType = deltaType;
    }
  }

}
