package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageAttributeServiceTest {

  @Mock
  private ImageAttributes imageAttributes;

  @Mock
  private AttributeService attributeService;

  private ImageAttributeService service;

  @Before
  public void setup() {
    Mockito.mockStatic(ServiceManager.class)
      .when(() -> ServiceManager.getService(AttributeService.class))
      .thenReturn(attributeService);

    service = new ImageAttributeService();
  }

  @Test
  public void shouldPassAttributeLabelPairsCorrectlyToTheAttributeService() {
    final Optional<Integer> widthOption = Optional.of(250);
    final String alt = "Image description";
    final String attributeString = "attributeString";
    when(attributeService.toAttributeString(any())).thenReturn(attributeString);
    when(imageAttributes.getWidth()).thenReturn(widthOption);
    when(imageAttributes.getAlt()).thenReturn(Optional.of(alt));

    final String result = service.toAttributeString(imageAttributes);

    assertEquals(result, attributeString);
    verify(attributeService).toAttributeString(
      new Pair<>(widthOption, Optional.of("width")),
      new Pair<>(Optional.of("\"" + alt + "\""), Optional.of("alt"))
    );
  }

  @Test
  public void shouldEscapeQuotesInAltText() {
    final String alt = "a quote: \"";
    final String attributeString = "attributeString";
    when(attributeService.toAttributeString(any())).thenReturn(attributeString);
    when(imageAttributes.getAlt()).thenReturn(Optional.of(alt));

    final String result = service.toAttributeString(imageAttributes);

    assertEquals(result, attributeString);
    verify(attributeService).toAttributeString(
      new Pair<>(Optional.empty(), Optional.of("width")),
      new Pair<>(Optional.of("\"a quote: \\\"\""), Optional.of("alt"))
    );
  }

}
