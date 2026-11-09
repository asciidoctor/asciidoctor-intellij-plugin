package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Model of an AsciiDoc table built from the PSI tree, supporting structural edits and rendering back to text.
 * Cells are kept as raw text; each row remembers if its cells are on one line and if a blank line follows.
 * Tables that can't be edited safely (spans, other formats, includes, nested tables) are not parsed.
 */
public final class AsciiDocTable {
  private static final Pattern OPEN_DELIMITER = Pattern.compile("\\|={3,}");
  private static final Pattern SAFE_SPEC = Pattern.compile("[<^>]?(\\.[<^>])?[a-z]?");
  private static final Pattern UNSAFE_BODY = Pattern.compile("(?m)^(include::|ifdef::|ifndef::|ifeval::|endif::)|!===");

  static final class Cell {
    final String spec;
    final String text;

    Cell(String spec, String text) {
      this.spec = spec;
      this.text = text;
    }
  }

  static final class Row {
    private List<Cell> cells;
    private final boolean singleLine;
    private boolean blankAfter;

    Row(List<Cell> cells, boolean singleLine, boolean blankAfter) {
      this.cells = cells;
      this.singleLine = singleLine;
      this.blankAfter = blankAfter;
    }

    List<Cell> getCells() {
      return cells;
    }

    void setCells(List<Cell> cells) {
      this.cells = cells;
    }

    boolean isSingleLine() {
      return singleLine;
    }

    boolean isBlankAfter() {
      return blankAfter;
    }

    void setBlankAfter(boolean blankAfter) {
      this.blankAfter = blankAfter;
    }
  }

  private final List<Row> rows = new ArrayList<>();
  private int columns;
  private boolean headerOption;
  private boolean footerOption;
  private boolean noHeader;
  private TextRange body = TextRange.EMPTY_RANGE;
  private @Nullable TextRange colsRange;
  private @Nullable List<String> colSpecs;
  private int row;
  private int col;

  private AsciiDocTable() {
  }

  public static @Nullable AsciiDocTable parse(@NotNull PsiFile file, int offset) {
    String text = file.getText();
    PsiElement leaf = file.findElementAt(offset);
    if (leaf == null && offset > 0) {
      leaf = file.findElementAt(offset - 1);
    }
    for (PsiElement el = leaf; el != null && !(el instanceof PsiFile); el = el.getParent()) {
      ASTNode node = el.getNode();
      if (node == null) {
        continue;
      }
      ASTNode open = node.findChildByType(AsciiDocTokenTypes.BLOCK_DELIMITER);
      if (open != null && OPEN_DELIMITER.matcher(open.getText()).matches()) {
        return parseBlock(node, open, text, offset);
      }
    }
    return null;
  }

  private static @Nullable AsciiDocTable parseBlock(ASTNode block, ASTNode open, String text, int offset) {
    ASTNode close = null;
    List<ASTNode> cellNodes = new ArrayList<>();
    for (ASTNode child : block.getChildren(null)) {
      if (child.getElementType() == AsciiDocElementTypes.CELL) {
        cellNodes.add(child);
      } else if (child != open && child.getElementType() == AsciiDocTokenTypes.BLOCK_DELIMITER
        && child.getText().equals(open.getText())) {
        close = child;
      }
    }
    if (close == null || cellNodes.isEmpty()) {
      return null;
    }
    int bodyStart = open.getTextRange().getEndOffset();
    int bodyEnd = close.getTextRange().getStartOffset();
    if (offset < bodyStart || offset > bodyEnd) {
      return null;
    }
    if (UNSAFE_BODY.matcher(text.substring(bodyStart, bodyEnd)).find()) {
      return null;
    }
    AsciiDocTable table = new AsciiDocTable();
    table.body = new TextRange(bodyStart, bodyEnd);
    if (!table.parseAttributes(block)) {
      return null;
    }

    // raw cells with their trailing whitespace ("gap") up to the next separator
    List<Cell> cells = new ArrayList<>();
    List<String> gaps = new ArrayList<>();
    int caretCell = -1;
    int previousEnd = bodyStart;
    for (int i = 0; i < cellNodes.size(); i++) {
      ASTNode sepNode = cellNodes.get(i).getFirstChildNode();
      if (sepNode == null || sepNode.getElementType() != AsciiDocTokenTypes.CELLSEPARATOR) {
        return null;
      }
      int sepStart = sepNode.getTextRange().getStartOffset();
      if (i == 0 && !text.substring(bodyStart, sepStart).isBlank()) {
        return null;
      }
      String sep = sepNode.getText();
      String spec = sep.substring(0, sep.length() - 1);
      if (!sep.endsWith("|") || !SAFE_SPEC.matcher(spec).matches()) {
        return null;
      }
      int contentStart = sepNode.getTextRange().getEndOffset();
      int contentEnd = i + 1 < cellNodes.size()
        ? cellNodes.get(i + 1).getFirstChildNode().getTextRange().getStartOffset() : bodyEnd;
      if (contentEnd < contentStart || contentStart < previousEnd) {
        return null;
      }
      String raw = text.substring(contentStart, contentEnd);
      String content = stripTrailing(raw);
      cells.add(new Cell(spec, content));
      gaps.add(raw.substring(content.length()));
      if (caretCell == -1 && offset < contentEnd + (i + 1 < cellNodes.size() ? 0 : 1) && offset >= sepStart) {
        caretCell = i;
      }
      previousEnd = contentEnd;
    }
    if (caretCell == -1) {
      // caret between the opening delimiter and the first cell, or on whitespace before it
      caretCell = 0;
    }

    table.columns = table.colSpecs != null ? table.colSpecs.size() : countFirstLine(cells, gaps);
    if (table.columns < 1 || cells.size() % table.columns != 0) {
      return null;
    }
    for (int start = 0; start < cells.size(); start += table.columns) {
      List<Cell> rowCells = new ArrayList<>(cells.subList(start, start + table.columns));
      boolean singleLine = true;
      for (int c = 0; c < table.columns; c++) {
        if (rowCells.get(c).text.contains("\n")
          || (c < table.columns - 1 && gaps.get(start + c).contains("\n"))) {
          singleLine = false;
        }
      }
      boolean blankAfter = start + table.columns < cells.size()
        && countNewlines(gaps.get(start + table.columns - 1)) >= 2;
      table.rows.add(new Row(rowCells, singleLine, blankAfter));
    }
    table.row = caretCell / table.columns;
    table.col = caretCell % table.columns;
    return table;
  }

  private static int countFirstLine(List<Cell> cells, List<String> gaps) {
    int count = 1;
    while (count < cells.size() && !gaps.get(count - 1).contains("\n") && !cells.get(count - 1).text.contains("\n")) {
      count++;
    }
    return count;
  }

  private static int countNewlines(String s) {
    int n = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '\n') {
        n++;
      }
    }
    return n;
  }

  private static String stripTrailing(String s) {
    int end = s.length();
    while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
      end--;
    }
    return s.substring(0, end);
  }

  private boolean parseAttributes(ASTNode block) {
    for (ASTNode child : block.getChildren(null)) {
      if (child.getElementType() != AsciiDocElementTypes.BLOCK_ATTRIBUTES) {
        continue;
      }
      for (ASTNode attr : child.getChildren(null)) {
        if (attr.getElementType() != AsciiDocElementTypes.ATTRIBUTE_IN_BRACKETS) {
          continue;
        }
        ASTNode nameNode = attr.findChildByType(AsciiDocTokenTypes.ATTR_NAME);
        ASTNode valueNode = attr.findChildByType(AsciiDocTokenTypes.ATTR_VALUE);
        String name = nameNode == null ? "" : nameNode.getText();
        String value = valueNode == null ? "" : valueNode.getText();
        switch (name) {
          case "%header" -> setOption("header");
          case "%footer" -> setOption("footer");
          case "%noheader" -> setOption("noheader");
          case "options", "opts" -> {
            for (String option : value.split(",", -1)) {
              setOption(option.trim());
            }
          }
          case "format", "separator" -> {
            return false;
          }
          case "cols" -> {
            colSpecs = valueNode == null ? null : expandCols(value);
            if (colSpecs == null) {
              return false;
            }
            colsRange = valueNode.getTextRange();
          }
          default -> {
          }
        }
      }
    }
    return true;
  }

  private void setOption(String option) {
    if ("header".equals(option)) {
      headerOption = true;
    } else if ("footer".equals(option)) {
      footerOption = true;
    } else if ("noheader".equals(option)) {
      noHeader = true;
    }
  }

  private static @Nullable List<String> expandCols(String value) {
    if (value.contains("{")) {
      return null;
    }
    List<String> result = new ArrayList<>();
    String[] tokens = value.split(",", -1);
    for (String token : tokens) {
      token = token.trim();
      if (token.isEmpty()) {
        return null;
      }
      if (tokens.length == 1 && token.matches("\\d+")) {
        int n = Integer.parseInt(token);
        if (n < 1 || n > 100) {
          return null;
        }
        for (int i = 0; i < n; i++) {
          result.add("1");
        }
        return result;
      }
      java.util.regex.Matcher m = Pattern.compile("(\\d+)\\*(.*)").matcher(token);
      if (m.matches()) {
        int n = Integer.parseInt(m.group(1));
        if (n < 1 || n > 100) {
          return null;
        }
        for (int i = 0; i < n; i++) {
          result.add(m.group(2));
        }
      } else {
        result.add(token);
      }
    }
    return result;
  }

  private boolean hasHeader() {
    if (headerOption) {
      return true;
    }
    if (noHeader || rows.isEmpty()) {
      return false;
    }
    Row first = rows.get(0);
    return first.isSingleLine() && first.isBlankAfter() && rows.size() > 1;
  }

  private boolean isHeader(int r) {
    return r == 0 && hasHeader();
  }

  private boolean isFooter(int r) {
    return footerOption && r == rows.size() - 1 && rows.size() > 1;
  }

  private boolean cellsOnly() {
    return colSpecs != null || rows.get(0).isSingleLine();
  }

  public int getRow() {
    return row;
  }

  public int getColumn() {
    return col;
  }

  public boolean canInsertRowAbove() {
    return !isHeader(row);
  }

  public boolean canInsertRowBelow() {
    return !isFooter(row);
  }

  public boolean canDeleteRow() {
    return rows.size() > 1 && !isHeader(row) && !isFooter(row);
  }

  public boolean canMoveRowUp() {
    return row > 0 && !isHeader(row) && !isHeader(row - 1) && !isFooter(row);
  }

  public boolean canMoveRowDown() {
    return row < rows.size() - 1 && !isHeader(row) && !isFooter(row) && !isFooter(row + 1);
  }

  public boolean canInsertColumn() {
    return cellsOnly();
  }

  public boolean canDeleteColumn() {
    return columns > 1 && cellsOnly();
  }

  public boolean canMoveColumnLeft() {
    return col > 0 && cellsOnly();
  }

  public boolean canMoveColumnRight() {
    return col < columns - 1 && cellsOnly();
  }

  public void insertRow(boolean above) {
    int target = above ? row : row + 1;
    int template = isHeader(row) && rows.size() > 1 ? 1 : row;
    Row layout = rows.get(template);
    boolean singleLine = layout.isSingleLine();
    boolean blankAfter;
    if (template < rows.size() - 1) {
      blankAfter = layout.isBlankAfter();
    } else if (template > 0 && !isHeader(template - 1)) {
      blankAfter = rows.get(template - 1).isBlankAfter();
    } else {
      blankAfter = !singleLine;
    }
    if (isHeader(row) && rows.size() == 1) {
      singleLine = false;
      blankAfter = true;
    }
    List<Cell> cells = new ArrayList<>();
    for (int c = 0; c < columns; c++) {
      cells.add(new Cell("", ""));
    }
    if (target == rows.size()) {
      rows.get(target - 1).setBlankAfter(blankAfter);
    }
    rows.add(target, new Row(cells, singleLine, blankAfter));
    row = target;
  }

  public boolean isLastCell() {
    return row == rows.size() - 1 && col == columns - 1;
  }

  public boolean canAppendRow() {
    return isLastCell() && canInsertRowBelow();
  }

  public void appendRow() {
    insertRow(false);
    col = 0;
  }

  public void deleteRow() {
    rows.remove(row);
    if (row >= rows.size()) {
      row = rows.size() - 1;
    }
  }

  public void moveRow(boolean up) {
    int other = up ? row - 1 : row + 1;
    List<Cell> tmp = rows.get(row).getCells();
    rows.get(row).setCells(rows.get(other).getCells());
    rows.get(other).setCells(tmp);
    row = other;
  }

  public void insertColumn(boolean left) {
    int target = left ? col : col + 1;
    for (Row r : rows) {
      r.getCells().add(target, new Cell("", ""));
    }
    if (colSpecs != null) {
      colSpecs.add(target, "1");
    }
    columns++;
    col = target;
  }

  public void deleteColumn() {
    for (Row r : rows) {
      r.getCells().remove(col);
    }
    if (colSpecs != null) {
      colSpecs.remove(col);
    }
    columns--;
    if (col >= columns) {
      col = columns - 1;
    }
  }

  public void moveColumn(boolean left) {
    int other = left ? col - 1 : col + 1;
    for (Row r : rows) {
      Cell tmp = r.getCells().get(col);
      r.getCells().set(col, r.getCells().get(other));
      r.getCells().set(other, tmp);
    }
    if (colSpecs != null) {
      String tmp = colSpecs.get(col);
      colSpecs.set(col, colSpecs.get(other));
      colSpecs.set(other, tmp);
    }
    col = other;
  }

  /**
   * Replacement for the region between the delimiters.
   */
  public @NotNull String renderBody() {
    return render(null);
  }

  /**
   * Offset relative to the start of the body where the caret should be placed.
   */
  public int caretOffsetInBody() {
    IdentityHashMap<Cell, Integer> offsets = new IdentityHashMap<>();
    render(offsets);
    Integer offset = offsets.get(rows.get(row).getCells().get(col));
    return offset == null ? 0 : offset;
  }

  private String render(@Nullable Map<Cell, Integer> offsets) {
    StringBuilder sb = new StringBuilder("\n");
    for (int r = 0; r < rows.size(); r++) {
      Row rw = rows.get(r);
      for (int c = 0; c < rw.getCells().size(); c++) {
        Cell cell = rw.getCells().get(c);
        sb.append(cell.spec).append('|');
        if (offsets != null) {
          offsets.put(cell, sb.length());
        }
        sb.append(cell.text);
        if (c < rw.getCells().size() - 1) {
          sb.append(rw.isSingleLine() ? " " : "\n");
        }
      }
      sb.append('\n');
      if (rw.isBlankAfter() && r < rows.size() - 1) {
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  public @NotNull TextRange getBodyRange() {
    return body;
  }

  public @Nullable TextRange getColsRange() {
    return colsRange;
  }

  public @Nullable String renderCols() {
    return colSpecs == null ? null : String.join(",", colSpecs);
  }

  /**
   * Find the offset where the caret should go when moving to the next or previous cell of the table at the offset.
   * This works for all tables with parsed cells, including tables with spans.
   *
   * @return the new offset, or null if there is no table or no such cell
   */
  public static @Nullable Integer findNeighborCellOffset(@NotNull PsiFile file, int offset, boolean forward) {
    PsiElement leaf = file.findElementAt(offset);
    if (leaf == null && offset > 0) {
      leaf = file.findElementAt(offset - 1);
    }
    for (PsiElement el = leaf; el != null && !(el instanceof PsiFile); el = el.getParent()) {
      ASTNode node = el.getNode();
      ASTNode open = node == null ? null : node.findChildByType(AsciiDocTokenTypes.BLOCK_DELIMITER);
      if (open == null || !OPEN_DELIMITER.matcher(open.getText()).matches()) {
        continue;
      }
      List<ASTNode> separators = new ArrayList<>();
      ASTNode close = null;
      for (ASTNode child : node.getChildren(null)) {
        if (child.getElementType() == AsciiDocElementTypes.CELL && child.getFirstChildNode() != null) {
          separators.add(child.getFirstChildNode());
        } else if (child != open && child.getElementType() == AsciiDocTokenTypes.BLOCK_DELIMITER) {
          close = child;
        }
      }
      if (close == null || offset < open.getTextRange().getEndOffset() || offset > close.getTextRange().getStartOffset()) {
        return null;
      }
      int current = -1;
      for (int i = 0; i < separators.size(); i++) {
        if (separators.get(i).getTextRange().getStartOffset() <= offset) {
          current = i;
        }
      }
      int target = forward ? current + 1 : current - 1;
      if (target < 0 || target >= separators.size()) {
        return null;
      }
      int start = separators.get(target).getTextRange().getEndOffset();
      CharSequence text = file.getViewProvider().getContents();
      return start < text.length() && text.charAt(start) == ' ' ? start + 1 : start;
    }
    return null;
  }
}
