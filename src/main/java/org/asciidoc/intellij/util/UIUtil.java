package org.asciidoc.intellij.util;

import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.StyleSheet;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class UIUtil {

  @NotNull
  public static StyleSheet loadStyleSheet(@NotNull URL url) {
    try {
      StyleSheet styleSheet = new StyleSheet();
      styleSheet.loadRules(new InputStreamReader(url.openStream(), CharsetToolkit.UTF8), url);
      return styleSheet;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
