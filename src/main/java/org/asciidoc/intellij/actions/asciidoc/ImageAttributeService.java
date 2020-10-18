package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Service
public final class ImageAttributeService {

  private static final String WIDTH_LABEL = "width";
  private static final String ALT_LABEL = "alt";

  private final AttributeService attributeService;

  public ImageAttributeService() {
    attributeService = ServiceManager.getService(AttributeService.class);
  }

  public @NotNull String toAttributeString(@NotNull final ImageAttributes attributes) {
    return attributeService.toAttributeString(
      new Pair<>(attributes.getWidth(), Optional.of(WIDTH_LABEL)),
      new Pair<>(attributes.getAlt().map(alt -> "\"" + alt.replaceAll("\"", "\\\\\"") + "\""), Optional.of(ALT_LABEL))
    );
  }
}
