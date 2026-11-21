package org.asciidoc.intellij.lexer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detect AsciiDoc files that are FreeMarker templates, like the guides of the Keycloak project.
 * Only for those files, lines that consist of a FreeMarker directive or macro call are handled like comments.
 */
public final class AsciiDocFreemarker {
  /**
   * A directive that is used only in templates, or a call of a macro that is imported from a namespace, each at the start of a line.
   */
  private static final Pattern TEMPLATE_MARKER = Pattern.compile(
    "^[ \\t]*(?:<#(?:import|include|assign|macro|function|ftl|setting|noparse|list|if|switch)\\b|</?@[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_])",
    Pattern.MULTILINE);

  /**
   * Prefix for the target of an include that must be read as a FreeMarker template.
   */
  public static final String INCLUDE_PREFIX = "freemarker:";

  /**
   * The path of an include or import directive.
   */
  public static final Pattern INCLUDE_OR_IMPORT = Pattern.compile("<#(?:include|import)\\s+\"([^\"]+)\"");

  private static final Pattern INCLUDE = Pattern.compile("<#include\\s+\"([^\"]+)\"(?:[^>\"']|\"[^\"]*\"|'[^']*')*+>");

  private AsciiDocFreemarker() {
  }

  /**
   * Resolve the path of an include or import like FreeMarker does with a template loader on the file system.
   * Paths starting with a slash are relative to the template root, which is assumed to be a parent folder of the current file.
   * Other paths are relative to the folder of the current file.
   * Files outside of the boundary folder are never returned.
   */
  public static @Nullable File resolve(@NotNull String path, @NotNull File currentFile, @NotNull File boundary) {
    try {
      File dir = currentFile.getAbsoluteFile().getParentFile();
      if (dir == null || !isInside(dir, boundary)) {
        return null;
      }
      if (path.startsWith("/")) {
        for (File root = dir; root != null && isInside(root, boundary); root = root.getParentFile()) {
          File candidate = new File(root, path.substring(1)).toPath().normalize().toFile();
          if (candidate.isFile() && isInside(candidate, boundary)) {
            return candidate;
          }
        }
        return null;
      }
      File candidate = new File(dir, path).toPath().normalize().toFile();
      return candidate.isFile() && isInside(candidate, boundary) ? candidate : null;
    } catch (InvalidPathException e) {
      return null;
    }
  }

  /**
   * Check if a file is inside the boundary folder, after resolving symbolic links.
   */
  public static boolean isInside(@NotNull File file, @NotNull File boundary) {
    try {
      return file.toPath().toRealPath().startsWith(boundary.toPath().toRealPath());
    } catch (IOException | InvalidPathException e) {
      return false;
    }
  }

  public static boolean isTemplate(@NotNull CharSequence text) {
    // cheap check to avoid scanning the text with a regular expression for all other files
    if (!contains(text, '<', '#') && !contains(text, '<', '@')) {
      return false;
    }
    return TEMPLATE_MARKER.matcher(text).find();
  }

  private static boolean contains(CharSequence text, char first, char second) {
    for (int i = 0; i < text.length() - 1; i++) {
      if (text.charAt(i) == first && text.charAt(i + 1) == second) {
        return true;
      }
    }
    return false;
  }

  private static final String TAG_START = "<(?:/)?(?:#[a-zA-Z_]+|@[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)";

  // a directive or macro call that is the only content of the line(s), possibly spanning multiple lines
  private static final Pattern DIRECTIVE_LINES = Pattern.compile(
    "[ \\t]*" + TAG_START + "(?:[^>\"']|\"[^\"]*\"|'[^']*')*+>[ \\t]*\\r?(?=\\n|\\z)");

  // a directive or macro call within a line
  private static final Pattern INLINE_TAG = Pattern.compile(
    TAG_START + "(?:[^>\"'\\n]|\"[^\"\\n]*\"|'[^'\\n]*')*+>");

  private static final Pattern VERBATIM_DELIMITER = Pattern.compile("(-{4,}|\\.{4,}|\\+{4,}|/{4,})[ \\t]*\\r?");

  /**
   * Prepare a FreeMarker template for the preview, without changing the number of lines.
   * Lines that contain only a directive or macro call are turned into comments, or into empty lines in verbatim blocks.
   * Self-closing macro calls within a line are shown as a placeholder, all other directives within a line are removed.
   *
   * @return the text unchanged if it is not a template
   */
  public static @NotNull String neutralize(@NotNull String text) {
    return neutralize(text, null, null);
  }

  /**
   * Same as {@link #neutralize(String)}, and if the file and the boundary folder are known, includes of other templates are turned into AsciiDoc includes.
   */
  public static @NotNull String neutralize(@NotNull String text, @Nullable File file, @Nullable File boundary) {
    if (!isTemplate(text)) {
      return text;
    }
    StringBuilder out = new StringBuilder(text.length());
    Matcher directive = DIRECTIVE_LINES.matcher(text);
    String verbatimDelimiter = null;
    int pos = 0;
    int length = text.length();
    while (pos <= length) {
      int eol = text.indexOf('\n', pos);
      if (eol == -1) {
        eol = length;
      }
      String line = text.substring(pos, eol);
      Matcher delimiter = VERBATIM_DELIMITER.matcher(line);
      int end = eol;
      if (delimiter.matches()) {
        if (verbatimDelimiter == null) {
          verbatimDelimiter = delimiter.group(1);
        } else if (verbatimDelimiter.equals(delimiter.group(1))) {
          verbatimDelimiter = null;
        }
        out.append(line);
      } else if (directive.region(pos, length).lookingAt()) {
        end = directive.end();
        String[] covered = text.substring(pos, end).split("\n", -1);
        for (int i = 0; i < covered.length; i++) {
          if (i > 0) {
            out.append('\n');
          }
          // a comment after an empty line would end a list, while FreeMarker removes the line completely
          String include = covered.length == 1 && verbatimDelimiter == null ? includeTarget(covered[0], file, boundary) : null;
          if (include != null) {
            out.append("include::").append(INCLUDE_PREFIX).append(include).append("[]");
          } else if (verbatimDelimiter == null && !endsWithEmptyLine(out)) {
            out.append("// ").append(covered[i].strip());
          }
        }
      } else {
        out.append(replaceInline(line, verbatimDelimiter != null));
      }
      if (end >= length) {
        break;
      }
      out.append('\n');
      pos = end + 1;
    }
    return out.toString();
  }

  private static @Nullable String includeTarget(String line, @Nullable File file, @Nullable File boundary) {
    if (file == null || boundary == null) {
      return null;
    }
    Matcher matcher = INCLUDE.matcher(line.strip());
    if (!matcher.matches()) {
      return null;
    }
    File resolved = resolve(matcher.group(1), file, boundary);
    return resolved == null ? null : resolved.getPath().replace('\\', '/');
  }

  private static boolean endsWithEmptyLine(StringBuilder out) {
    int end = out.length();
    if (end > 0 && out.charAt(end - 1) == '\n') {
      end--;
    } else if (end > 0) {
      return out.substring(out.lastIndexOf("\n") + 1).isBlank();
    }
    if (end == 0) {
      return false;
    }
    int last = out.lastIndexOf("\n", end - 1);
    return out.substring(last + 1, end).isBlank();
  }

  private static String replaceInline(String line, boolean verbatim) {
    if (line.indexOf('<') == -1) {
      return line;
    }
    Matcher matcher = INLINE_TAG.matcher(line);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String tag = matcher.group();
      String replacement = "";
      if (!verbatim && tag.startsWith("<@") && tag.endsWith("/>")) {
        String call = tag.substring(2, tag.length() - 2).strip();
        replacement = "pass:[<code>" + call.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
          .replace("]", "&#93;") + "</code>]";
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
