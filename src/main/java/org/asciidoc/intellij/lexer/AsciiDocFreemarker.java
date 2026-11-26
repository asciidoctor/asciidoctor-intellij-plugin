package org.asciidoc.intellij.lexer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
    Context macros = new Context(loadMacros(text, file, boundary), file, boundary, new HashMap<>());
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
        String expanded = verbatimDelimiter == null ? expandDirective(covered, macros) : null;
        if (expanded == null && verbatimDelimiter == null) {
          expanded = callPlaceholder(covered);
        }
        for (int i = 0; i < covered.length; i++) {
          if (i > 0) {
            out.append('\n');
          }
          // a comment after an empty line would end a list, while FreeMarker removes the line completely
          String include = covered.length == 1 && verbatimDelimiter == null ? includeTarget(covered[0], file, boundary) : null;
          if (expanded != null) {
            if (i == 0) {
              out.append(expanded);
            }
          } else if (include != null) {
            out.append("include::").append(INCLUDE_PREFIX).append(include).append("[]");
          } else if (verbatimDelimiter == null && !endsWithEmptyLine(out)) {
            out.append("// ").append(covered[i].strip());
          }
        }
      } else {
        out.append(replaceInline(line, verbatimDelimiter != null, macros));
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

  /**
   * Show a self-closing macro call that is the only content of the line(s) as visible text, as it would produce content that is not known.
   * Directives that are standard FreeMarker and the start and end of macros that wrap content stay hidden.
   */
  private static @Nullable String callPlaceholder(String[] covered) {
    String joined = String.join(" ", Arrays.stream(covered).map(String::strip).toList());
    Matcher call = CALL.matcher(joined);
    if (call.matches() && !call.group(3).isEmpty()) {
      return placeholder(joined);
    }
    return null;
  }

  private static String placeholder(String tag) {
    String call = tag.strip();
    if (call.indexOf('`') == -1 && call.indexOf('+') == -1) {
      // literal monospace, the call is shown as it is written
      return "`+" + call + "+`";
    }
    return "pass:[<code>" + call.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      .replace("]", "&#93;") + "</code>]";
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

  private static String replaceInline(String line, boolean verbatim, Context macros) {
    if (line.indexOf('<') == -1) {
      return line;
    }
    Matcher matcher = INLINE_TAG.matcher(line);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String tag = matcher.group();
      String replacement = "";
      if (!verbatim && tag.startsWith("<@") && tag.endsWith("/>")) {
        String expanded = expandCall(tag, macros);
        replacement = expanded != null ? expanded : placeholder(tag);
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private record Macro(Map<String, String> defaults, String body) {
  }

  private record Context(Map<String, Macro> macros, @Nullable File file, @Nullable File boundary, Map<String, Map<String, File>> guides) {
  }

  // Keycloak defines these attributes in the index of each folder at build time, resolve them to the guide here as the preview doesn't know them
  private static final Pattern GUIDE_LINK = Pattern.compile(
    "link:\\{links_([a-z0-9-]+)_([a-z0-9-]+)_url}(#[^\\[\\s]*)?\\[\\{links_\\1_\\2_name}]");
  private static final Pattern GUIDE_TITLE = Pattern.compile("title=\"([^\"]*)\"");

  private static String resolveGuideLinks(String text, Context context) {
    if (context.file() == null || context.boundary() == null || !text.contains("{links_")) {
      return text;
    }
    Matcher matcher = GUIDE_LINK.matcher(text);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      File guide = findGuide(matcher.group(1), matcher.group(2), context);
      String replacement = matcher.group();
      if (guide != null) {
        File dir = context.file().getAbsoluteFile().getParentFile();
        String relative = dir.toPath().relativize(guide.toPath()).toString().replace('\\', '/');
        replacement = "xref:" + relative + (matcher.group(3) != null ? matcher.group(3) : "") + "[" + guideTitle(guide, matcher.group(2)) + "]";
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static @Nullable File findGuide(String folder, String id, Context context) {
    File file = context.file();
    File boundary = context.boundary();
    if (file == null || boundary == null) {
      return null;
    }
    Map<String, File> guides = context.guides().get(folder);
    if (guides == null) {
      guides = new HashMap<>();
      context.guides().put(folder, guides);
      for (File root = file.getAbsoluteFile().getParentFile(); root != null && isInside(root, boundary); root = root.getParentFile()) {
        File folderDir = new File(root, folder);
        if (folderDir.isDirectory() && isInside(folderDir, boundary)) {
          collectGuides(folderDir.toPath(), guides);
          break;
        }
      }
    }
    return guides.get(id);
  }

  // the id of a guide is the name of its file, prefixed with the name of its parent folder if it is in a subfolder
  private static void collectGuides(java.nio.file.Path folder, Map<String, File> guides) {
    try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(folder, 3)) {
      paths.filter(path -> path.getFileName().toString().endsWith(".adoc") && Files.isRegularFile(path)).forEach(path -> {
        String name = path.getFileName().toString();
        name = name.substring(0, name.length() - ".adoc".length());
        java.nio.file.Path parent = path.getParent();
        if (!folder.equals(parent)) {
          name = parent.getFileName() + "-" + name;
        }
        guides.putIfAbsent(name, path.toFile());
      });
    } catch (IOException | RuntimeException e) {
      // best effort only
    }
  }

  private static String guideTitle(File guide, String fallback) {
    try (java.io.BufferedReader reader = Files.newBufferedReader(guide.toPath(), StandardCharsets.UTF_8)) {
      for (int i = 0; i < 20; i++) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        Matcher title = GUIDE_TITLE.matcher(line);
        if (title.find()) {
          return title.group(1);
        }
      }
    } catch (IOException | RuntimeException e) {
      // use the fallback
    }
    return fallback;
  }

  private static final Pattern IMPORT = Pattern.compile("^[ \\t]*<#import\\s+\"([^\"]+)\"\\s+as\\s+(\\w+)\\s*>", Pattern.MULTILINE);
  private static final Pattern MACRO = Pattern.compile("<#macro\\s+(\\w+)([^>]*)>(.*?)</#macro>", Pattern.DOTALL);
  private static final Pattern MACRO_PARAM = Pattern.compile("(\\w+)(?:\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s\"']+))?");
  private static final Pattern CALL = Pattern.compile("<@(\\w+\\.\\w+)((?:[^>\"']|\"[^\"]*\"|'[^']*')*?)(/?)>");
  private static final Pattern CALL_END = Pattern.compile("</@(\\w+\\.\\w+)\\s*>");
  private static final Pattern ARGUMENT = Pattern.compile("(\\w+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
  private static final Pattern CONDITION = Pattern.compile("<#if\\s+(\\w+)\\s*(!=|==)\\s*\"([^\"]*)\"\\s*>(.*?)</#if>", Pattern.DOTALL);
  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");
  private static final String NESTED = "<#nested>";

  /**
   * Load the macros of the templates that are imported, keyed by namespace and name.
   */
  private static Map<String, Macro> loadMacros(String text, @Nullable File file, @Nullable File boundary) {
    if (file == null || boundary == null) {
      return Map.of();
    }
    Map<String, Macro> macros = new HashMap<>();
    Matcher imports = IMPORT.matcher(text);
    while (imports.find()) {
      File imported = resolve(imports.group(1), file, boundary);
      if (imported == null || imported.length() > 1024 * 1024) {
        continue;
      }
      String content;
      try {
        content = Files.readString(imported.toPath(), StandardCharsets.UTF_8);
      } catch (IOException | RuntimeException e) {
        continue;
      }
      Matcher macro = MACRO.matcher(content);
      while (macro.find()) {
        Map<String, String> defaults = new LinkedHashMap<>();
        Matcher param = MACRO_PARAM.matcher(macro.group(2));
        while (param.find()) {
          String value = param.group(2);
          if (value != null && value.length() >= 2 && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
            value = value.substring(1, value.length() - 1);
          }
          defaults.put(param.group(1), value);
        }
        macros.put(imports.group(2) + "." + macro.group(1), new Macro(defaults, macro.group(3)));
      }
    }
    return macros;
  }

  /**
   * Expand a directive that spans the lines given into a single line, if it is a macro that wraps content and is simple enough.
   */
  private static @Nullable String expandDirective(String[] covered, Context macros) {
    if (macros.macros().isEmpty()) {
      return null;
    }
    String joined = String.join(" ", Arrays.stream(covered).map(String::strip).toList());
    Matcher end = CALL_END.matcher(joined);
    if (end.matches()) {
      Macro macro = macros.macros().get(end.group(1));
      int nested = macro == null ? -1 : macro.body().indexOf(NESTED);
      String after = nested == -1 ? null : singleLine(macro.body().substring(nested + NESTED.length()), Map.of());
      // nothing to show, keep a comment so that paragraphs and lists are not interrupted
      return after == null || after.isEmpty() ? null : after;
    }
    Matcher call = CALL.matcher(joined);
    if (!call.matches()) {
      return null;
    }
    return expandCall(joined, macros);
  }

  /**
   * Expand a call of a macro, which is either self-closing or the start of a macro that wraps content.
   */
  private static @Nullable String expandCall(String tag, Context macros) {
    if (macros.macros().isEmpty()) {
      return null;
    }
    Matcher call = CALL.matcher(tag);
    if (!call.matches()) {
      return null;
    }
    Macro macro = macros.macros().get(call.group(1));
    if (macro == null) {
      return null;
    }
    Map<String, String> values = new HashMap<>(macro.defaults());
    Matcher argument = ARGUMENT.matcher(call.group(2));
    String rest = call.group(2);
    while (argument.find()) {
      values.put(argument.group(1), argument.group(2) != null ? argument.group(2) : argument.group(3));
      rest = rest.replace(argument.group(), "");
    }
    if (!rest.isBlank()) {
      return null;
    }
    String body = macro.body();
    int nested = body.indexOf(NESTED);
    boolean selfClosing = !call.group(3).isEmpty();
    if ((nested == -1) != selfClosing) {
      return null;
    }
    String expanded = singleLine(nested == -1 ? body : body.substring(0, nested), values);
    return expanded == null ? null : resolveGuideLinks(expanded, macros);
  }

  /**
   * Evaluate simple conditions and parameters of a macro body, and return the result as a single line.
   * Returns null if the body is too complex.
   */
  private static @Nullable String singleLine(String body, Map<String, String> values) {
    String result = body;
    Matcher condition = CONDITION.matcher(result);
    while (condition.find()) {
      String value = values.get(condition.group(1));
      if (value == null) {
        return null;
      }
      boolean equal = value.equals(condition.group(3));
      String replacement = equal == "==".equals(condition.group(2)) ? condition.group(4) : "";
      result = result.substring(0, condition.start()) + replacement + result.substring(condition.end());
      condition = CONDITION.matcher(result);
    }
    Matcher placeholder = PLACEHOLDER.matcher(result);
    StringBuilder replaced = new StringBuilder();
    while (placeholder.find()) {
      String value = values.get(placeholder.group(1));
      if (value == null) {
        return null;
      }
      placeholder.appendReplacement(replaced, Matcher.quoteReplacement(value));
    }
    placeholder.appendTail(replaced);
    result = replaced.toString().strip();
    if (result.contains("<#") || result.contains("</#") || result.contains("<@") || result.contains("${") || result.indexOf('\n') != -1) {
      return null;
    }
    return result;
  }
}
