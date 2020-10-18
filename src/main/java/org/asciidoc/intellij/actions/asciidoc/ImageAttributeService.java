package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

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
      MacroAttribute.createWithLabel(attributes.getWidth(), WIDTH_LABEL),
      MacroAttribute.createInQuotesWithLabel(attributes.getAlt(), ALT_LABEL)
    );
  }
}
