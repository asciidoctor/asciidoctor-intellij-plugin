package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

@Service
public final class ImageMacroAttributeService {

  private static final String WIDTH_LABEL = "width";
  private static final String ALT_LABEL = "alt";

  private final MacroAttributeService macroAttributeService;

  // is created by ServiceManager
  @SuppressWarnings("unused")
  public ImageMacroAttributeService() {
    macroAttributeService = ServiceManager.getService(MacroAttributeService.class);
  }

  @TestOnly
  public ImageMacroAttributeService(MacroAttributeService macroAttributeService) {
    this.macroAttributeService = macroAttributeService;
  }

  public @NotNull String toAttributeString(@NotNull final ImageAttributes attributes) {
    return macroAttributeService.toAttributeString(
      MacroAttribute.createWithLabel(attributes.getWidth().orElse(null), WIDTH_LABEL),
      MacroAttribute.createInQuotesWithLabel(attributes.getAlt().orElse(null), ALT_LABEL)
    );
  }
}
