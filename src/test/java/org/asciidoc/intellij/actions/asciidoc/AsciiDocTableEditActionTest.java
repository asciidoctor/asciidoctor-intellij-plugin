package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class AsciiDocTableEditActionTest extends BasePlatformTestCase {

  private void check(String action, String before, String after) {
    myFixture.configureByText("test.adoc", before);
    myFixture.performEditorAction("asciidoc.tableedit." + action);
    myFixture.checkResult(after);
  }

  private void unchanged(String action, String text) {
    check(action, text, text);
  }

  public void testInsertRowBelowPerLineCells() {
    check("insertrowbelow",
      "[cols=\"1,1\"]\n|===\n|a<caret>\n|b\n\n|c\n|d\n|===\n",
      "[cols=\"1,1\"]\n|===\n|a\n|b\n\n|<caret>\n|\n\n|c\n|d\n|===\n");
  }

  public void testInsertRowBelowLastInline() {
    check("insertrowbelow",
      "|===\n|a |b\n\n|c |d<caret>\n|===\n",
      "|===\n|a |b\n\n|c |d\n|\n|<caret>\n|===\n".replace("|\n|<caret>", "| |<caret>"));
  }

  public void testInsertRowAboveHeaderRefused() {
    unchanged("insertrowabove", "|===\n|a<caret> |b\n\n|c |d\n|===\n");
  }

  public void testDeleteRow() {
    check("deleterow",
      "|===\n|a |b\n\n|c<caret> |d\n\n|e |f\n|===\n",
      "|===\n|a |b\n\n|<caret>e |f\n|===\n");
  }

  public void testDeleteHeaderRefused() {
    unchanged("deleterow", "[%header]\n|===\n|a<caret> |b\n|c |d\n|===\n");
  }

  public void testMoveRowDown() {
    check("moverowdown",
      "[cols=\"1,1\"]\n|===\n|a<caret>\n|b\n\n|c\n|d\n|===\n",
      "[cols=\"1,1\"]\n|===\n|c\n|d\n\n|a\n|b\n|===\n".replace("|a\n|b", "|<caret>a\n|b"));
  }

  public void testInsertColumnRightUpdatesCols() {
    check("insertcolumnright",
      "[cols=\"2*\"]\n|===\n|a<caret> |b\n\n|c |d\n|===\n".replace("2*", "1,2"),
      "[cols=\"1,1,2\"]\n|===\n|a | |b\n\n|c | |d\n|===\n".replace("| |b", "|<caret> |b"));
  }

  public void testDeleteColumn() {
    check("deletecolumn",
      "|===\n|a |b<caret> |c\n\n|d |e |f\n|===\n",
      "|===\n|a |<caret>c\n\n|d |f\n|===\n");
  }

  public void testMoveColumnLeft() {
    check("movecolumnleft",
      "[cols=\"1,2\"]\n|===\n|a |b<caret>\n\n|c |d\n|===\n",
      "[cols=\"2,1\"]\n|===\n|<caret>b |a\n\n|d |c\n|===\n");
  }

  public void testSpanRefused() {
    unchanged("deletecolumn", "|===\n|a<caret> |b\n\n2+|c\n|===\n");
  }

  public void testCsvRefused() {
    unchanged("deletecolumn", "[format=csv]\n|===\na<caret>,b\n|===\n");
  }

  public void testOutsideTable() {
    unchanged("deleterow", "text<caret>\n\n----\n|===\n|a\n|===\n----\n");
  }

  public void testIntentionSubMenus() {
    myFixture.configureByText("test.adoc", "|===\n|a<caret> |b\n\n|c |d\n|===\n");
    var rows = new org.asciidoc.intellij.actions.intentions.AsciiDocTableIntention.Rows();
    var columns = new org.asciidoc.intellij.actions.intentions.AsciiDocTableIntention.Columns();
    var rowNames = rows.getAvailableOptions(getProject(), myFixture.getEditor(), myFixture.getFile()).stream()
      .map(a -> a.getText()).toList();
    // header row: can't insert above, delete or move
    assertEquals(java.util.List.of("Insert row below"), rowNames);
    var insert = columns.getAvailableOptions(getProject(), myFixture.getEditor(), myFixture.getFile()).get(1);
    insert.invoke(getProject(), myFixture.getEditor(), myFixture.getFile());
    myFixture.checkResult("|===\n|a |<caret> |b\n\n|c | |d\n|===\n");
  }

  public void testTabMovesToNextCell() {
    myFixture.configureByText("test.adoc", "|===\n|a<caret> |b\n\n|c |d\n|===\n");
    myFixture.performEditorAction("EditorTab");
    myFixture.checkResult("|===\n|a |<caret>b\n\n|c |d\n|===\n");
    myFixture.performEditorAction("EditorTab");
    myFixture.checkResult("|===\n|a |b\n\n|<caret>c |d\n|===\n");
    myFixture.performEditorAction("EditorUnindentSelection");
    myFixture.checkResult("|===\n|a |<caret>b\n\n|c |d\n|===\n");
  }

  public void testTabOutsideTableAndAtEdges() {
    myFixture.configureByText("test.adoc", "text<caret>\n");
    myFixture.performEditorAction("EditorTab");
    assertFalse(myFixture.getEditor().getDocument().getText().equals("text\n"));
    myFixture.configureByText("test.adoc", "|===\n|a |b<caret>\n|===\n");
    myFixture.performEditorAction("EditorUnindentSelection");
    myFixture.performEditorAction("EditorUnindentSelection");
    myFixture.checkResult("|===\n|<caret>a |b\n|===\n");
  }

  public void testTabInLastCellAppendsRow() {
    myFixture.configureByText("test.adoc", "|===\n|a |b\n\n|c |d<caret>\n|===\n");
    myFixture.performEditorAction("EditorTab");
    myFixture.checkResult("|===\n|a |b\n\n|c |d\n|<caret> |\n|===\n");
    myFixture.configureByText("test.adoc", "|===\n|a |b<caret>\n\n|c |d\n|===\n");
    myFixture.performEditorAction("EditorTab");
    myFixture.checkResult("|===\n|a |b\n\n|<caret>c |d\n|===\n");
  }

  public void testMoveLineMovesRowInTableOnly() {
    myFixture.configureByText("test.adoc", "[cols=\"1,1\"]\n|===\n|a<caret>\n|b\n\n|c\n|d\n|===\n");
    myFixture.performEditorAction("MoveStatementDown");
    myFixture.checkResult("[cols=\"1,1\"]\n|===\n|c\n|d\n\n|<caret>a\n|b\n|===\n");
    myFixture.performEditorAction("MoveStatementUp");
    myFixture.checkResult("[cols=\"1,1\"]\n|===\n|<caret>a\n|b\n\n|c\n|d\n|===\n");
    myFixture.performEditorAction("MoveLineDown");
    myFixture.performEditorAction("MoveLineUp");
    myFixture.checkResult("[cols=\"1,1\"]\n|===\n|<caret>a\n|b\n\n|c\n|d\n|===\n");
    myFixture.configureByText("test.adoc", "line1<caret>\n\nline2\n");
    myFixture.performEditorAction("MoveLineDown");
    assertFalse(myFixture.getEditor().getDocument().getText().startsWith("line1\n\nline2"));
  }

  public void testAllAvailableOptionsForSeparatorMenu() {
    myFixture.configureByText("test.adoc", "|===\n|a |b\n\n|c<caret> |d\n|===\n");
    var names = org.asciidoc.intellij.actions.intentions.AsciiDocTableIntention
      .getAllAvailableOptions(getProject(), myFixture.getEditor(), myFixture.getFile()).stream()
      .map(a -> a.getText()).toList();
    assertTrue(names.toString(), names.containsAll(java.util.List.of("Insert row above", "Delete row", "Insert column left", "Move column right")));
  }
}
