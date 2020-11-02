package org.asciidoc.intellij.actions.asciidoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MacroAttribute {

  private final Object value;
  private final String label;
  private final boolean quoted;

  public static MacroAttribute createWithLabel(@Nullable final Object valueOption,
                                               @Nullable final String label) {
    return new MacroAttribute(valueOption, label, false);
  }

  public static MacroAttribute createInQuotesWithLabel(@Nullable final Object valueOption,
                                                       @Nullable final String label) {
    return new MacroAttribute(valueOption, label, true);
  }

  private MacroAttribute(@Nullable final Object value,
                         @Nullable final String label,
                         final boolean quoted) {
    this.value = value;
    this.label = label;
    this.quoted = quoted;
  }

  public @NotNull Optional<String> asAttributeStringOption() {
    return Optional.ofNullable(value)
      .map(Object::toString)
      .map(value -> value.replaceAll("\"", "\\\\\""))
      .map(value -> quoted ? "\"" + value + "\"" : value)
      .map(value -> getPrefixedLabel().concat(value));
  }

  private @NotNull String getPrefixedLabel() {
    return Optional.ofNullable(label).map(label -> label.concat("=")).orElse("");
  }
}
