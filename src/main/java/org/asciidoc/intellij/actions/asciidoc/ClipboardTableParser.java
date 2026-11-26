package org.asciidoc.intellij.actions.asciidoc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses table data from the clipboard as spreadsheets provide it: rows are separated by line breaks, and cells
 * containing separators, line breaks or quotes are enclosed in double quotes, with quotes inside doubled.
 */
public final class ClipboardTableParser {

  private ClipboardTableParser() {
  }

  public static @NotNull List<List<String>> parse(@NotNull String data, @NotNull String separator) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    int length = data.length();
    int pos = 0;
    boolean rowOpen = false;
    while (pos < length) {
      rowOpen = true;
      String cell = null;
      if (data.charAt(pos) == '"') {
        StringBuilder sb = new StringBuilder();
        int end = readQuoted(data, pos, sb);
        if (end != -1 && isEndOfCell(data, end, separator)) {
          cell = sb.toString();
          pos = end;
        }
      }
      if (cell == null) {
        int end = pos;
        while (end < length && !data.startsWith(separator, end) && !isLineBreak(data, end)) {
          end++;
        }
        cell = data.substring(pos, end);
        pos = end;
      }
      row.add(cell);
      if (pos >= length) {
        break;
      }
      if (data.startsWith(separator, pos)) {
        pos += separator.length();
        if (pos >= length) {
          // trailing separator at the end of the data is followed by an empty cell
          row.add("");
        }
      } else {
        pos += data.startsWith("\r\n", pos) ? 2 : 1;
        rows.add(row);
        row = new ArrayList<>();
        rowOpen = false;
      }
    }
    if (rowOpen) {
      rows.add(row);
    }
    return rows;
  }

  /**
   * @return the position after the closing quote, or -1 if there is no closing quote
   */
  private static int readQuoted(String data, int start, StringBuilder sb) {
    int pos = start + 1;
    while (pos < data.length()) {
      char c = data.charAt(pos);
      if (c == '"') {
        if (pos + 1 < data.length() && data.charAt(pos + 1) == '"') {
          sb.append('"');
          pos += 2;
          continue;
        }
        return pos + 1;
      }
      sb.append(c);
      pos++;
    }
    return -1;
  }

  private static boolean isEndOfCell(String data, int pos, String separator) {
    return pos >= data.length() || data.startsWith(separator, pos) || isLineBreak(data, pos);
  }

  private static boolean isLineBreak(String data, int pos) {
    char c = data.charAt(pos);
    return c == '\n' || c == '\r';
  }
}
