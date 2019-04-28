package org.asciidoc.intellij;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class AsciiDocBundle {
  @NotNull
  private static final String BUNDLE_NAME = "AsciiDocBundle";
  @NotNull
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  @NotNull
  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return CommonBundle.message(BUNDLE, key, params);
  }
}
