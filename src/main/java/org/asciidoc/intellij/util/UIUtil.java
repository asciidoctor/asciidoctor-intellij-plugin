package org.asciidoc.intellij.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.StyleSheet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UIUtil {

  @NotNull
  public static StyleSheet loadStyleSheet(@NotNull URL url) {
    try (InputStream stream = url.openStream()) {
      StyleSheet styleSheet = new StyleSheet();
      try (Reader is = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        styleSheet.loadRules(is, url);
      }
      return styleSheet;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
