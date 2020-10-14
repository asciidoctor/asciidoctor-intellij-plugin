package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public final class AttributeService {

  @SafeVarargs
  public final @NotNull String toAttributeString(@NotNull final Pair<String, Optional<?>>... attributes) {
    return Stream.of(attributes)
      .filter(labelValuePair -> labelValuePair.second.isPresent())
      .map(labelValuePair -> labelValuePair.first + "=" + labelValuePair.second.get())
      .collect(Collectors.joining(","));
  }
}
