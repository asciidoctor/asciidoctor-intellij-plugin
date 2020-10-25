package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageMacroAttributeServiceTest {

  private ImageMacroAttributeService service;

  @Before
  public void setup() {
    service = new ImageMacroAttributeService(new MacroAttributeService());
  }

  @Test
  public void shouldPassMacroAttributesCorrectlyToTheAttributeService() {
    ImageAttributes imageAttributes = new ImageAttributes() {

      @Override
      public Optional<Integer> getWidth() {
        return Optional.of(250);
      }

      @Override
      public Optional<String> getAlt() {
        return Optional.of("Image description");
      }
    };

    final String result = service.toAttributeString(imageAttributes);

    assertThat(result).isEqualTo("width=250,alt=\"Image description\"");
  }
}
