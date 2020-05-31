package org.asciidoc.intellij;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AsciiDocBundle {
  @NotNull
  private static final String BUNDLE_NAME = "AsciiDocBundle";
  @NotNull
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  public static final String BUILTIN_ATTRIBUTE_PREFIX = "asciidoc.attributes.builtin.";

  @NotNull
  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
    return AbstractBundle.message(BUNDLE, key, params);
  }

  public static List<String> getBuiltInAttributesList() {
    return BUNDLE.keySet().stream()
      .filter(key -> key.startsWith(BUILTIN_ATTRIBUTE_PREFIX))
      .map(key -> key.substring(BUILTIN_ATTRIBUTE_PREFIX.length(), key.lastIndexOf(".")))
      .distinct()
      .collect(Collectors.toList());
  }
}
