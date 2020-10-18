package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public final class AttributeService {

  /**
   * Takes a list of attributes and transforms them into an attribute string.
   * Attributes consist of a value-abel {@link com.intellij.openapi.util.Pair}.
   * Empty {@link java.util.Optional} {@code values} will be ignored.
   * Present {@link java.util.Optional} {@code labels} will be prefixed ({@code "<label>="}).
   *
   * @param attributes list of attributes consisting of a value-label {@link com.intellij.openapi.util.Pair}
   * @return list of attribute values and labels if present, concatenated by comma.
   */
  @SafeVarargs
  public final @NotNull String toAttributeString(@NotNull final Pair<Optional<?>, Optional<String>>... attributes) {
    return Stream.of(attributes)
      .filter(labelValuePair -> labelValuePair.first.isPresent())
      .map(labelValuePair -> labelValuePair.second.map(label -> label + "=").orElse("") + labelValuePair.first.get())
      .collect(Collectors.joining(","));
  }
}
