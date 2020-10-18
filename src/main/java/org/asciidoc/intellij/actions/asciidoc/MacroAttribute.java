package org.asciidoc.intellij.actions.asciidoc;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class MacroAttribute {

  private final Optional<?> valueOption;
  private final Optional<String> labelOption;
  private final boolean quoted;

  public static MacroAttribute createWithLabel(@NotNull final Optional<?> valueOption,
                                               @NotNull final String label) {
    return new MacroAttribute(valueOption, Optional.of(label), false);
  }


  public static MacroAttribute createInQuotesWithLabel(@NotNull final Optional<?> valueOption,
                                                       @NotNull final String label) {
    return new MacroAttribute(valueOption, Optional.of(label), true);
  }

  public MacroAttribute(@NotNull final Optional<?> valueOption,
                        @NotNull final Optional<String> labelOption,
                        final boolean quoted) {
    this.valueOption = valueOption;
    this.labelOption = labelOption;
    this.quoted = quoted;
  }

  public boolean hasValue() {
    return valueOption.isPresent();
  }

  public @NotNull Optional<String> asAttributeStringOption() {
    return valueOption
      .map(Object::toString)
      .map(value -> value.replaceAll("\"", "\\\\\""))
      .map(value -> quoted ? "\"" + value + "\"" : value)
      .map(value -> getPrefixedLabel().concat(value));
  }

  private @NotNull String getPrefixedLabel() {
    return labelOption.map(label -> label.concat("=")).orElse("");
  }
}
