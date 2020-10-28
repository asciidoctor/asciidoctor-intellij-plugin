package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public final class MacroAttributeService {

  /**
   * Takes a list of {@link MacroAttribute}s and transforms them into an attribute string.
   * {@link MacroAttribute}s without a value will be ignored.
   *
   * @param attributes list of {@link MacroAttribute}s
   * @return list of attribute strings concatenated by comma.
   */
  public @NotNull String toAttributeString(@NotNull final MacroAttribute... attributes) {
    return Stream.of(attributes)
      .flatMap(attribute -> attribute.asAttributeStringOption().map(Stream::of).orElseGet(Stream::empty))
      .collect(Collectors.joining(","));
  }
}
