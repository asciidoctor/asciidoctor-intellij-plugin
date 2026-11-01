package org.asciidoc.intellij.actions.asciidoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Derives defaults for a pasted image from the text selected in the editor.
 */
public final class PasteImageSelection {

  private static final int MAX_FILE_NAME_LENGTH = 100;
  private static final Pattern NOT_ALLOWED = Pattern.compile("[^A-Za-z0-9._-]+");
  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

  private PasteImageSelection() {
  }

  /**
   * The selected text to be used as alt text, only if it is on a single line.
   */
  public static Optional<String> toAltText(@Nullable String selectedText) {
    if (selectedText == null) {
      return Optional.empty();
    }
    String trimmed = selectedText.trim();
    if (trimmed.isEmpty() || trimmed.indexOf('\n') != -1 || trimmed.indexOf('\r') != -1) {
      return Optional.empty();
    }
    return Optional.of(trimmed);
  }

  /**
   * The selected text as a file name with the given extension, restricted to characters that are valid on all platforms.
   */
  public static Optional<String> toFileName(@Nullable String selectedText, @NotNull String extension) {
    return toAltText(selectedText).map(text -> {
      String name = DIACRITICS.matcher(Normalizer.normalize(text.replace("ß", "ss"), Normalizer.Form.NFD)).replaceAll("");
      name = NOT_ALLOWED.matcher(name).replaceAll("-");
      name = name.toLowerCase(Locale.ROOT).replaceAll("^[-.]+|[-.]+$", "");
      String suffix = "." + extension;
      if (name.endsWith(suffix)) {
        name = name.substring(0, name.length() - suffix.length());
      }
      if (name.length() > MAX_FILE_NAME_LENGTH) {
        name = name.substring(0, MAX_FILE_NAME_LENGTH).replaceAll("[-.]+$", "");
      }
      return name;
    }).filter(name -> !name.isEmpty()).map(name -> name + "." + extension);
  }
}
