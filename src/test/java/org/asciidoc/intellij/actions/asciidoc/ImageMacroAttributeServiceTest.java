package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.components.ServiceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageMacroAttributeServiceTest {

  @Mock
  private ImageAttributes imageAttributes;

  @Mock
  private MacroAttributeService macroAttributeService;

  @Mock
  private MacroAttribute attributeWithLabel;

  @Mock
  private MacroAttribute attributeInQuotesWithLabel;

  private MockedStatic<ServiceManager> mockedServiceManager;
  private MockedStatic<MacroAttribute> mockedMacroAttribute;

  private ImageMacroAttributeService service;

  @Before
  public void setup() {
    mockedServiceManager = Mockito.mockStatic(ServiceManager.class);
    mockedMacroAttribute = Mockito.mockStatic(MacroAttribute.class);

    mockedServiceManager.when(() -> ServiceManager.getService(MacroAttributeService.class))
      .thenReturn(macroAttributeService);

    service = new ImageMacroAttributeService();
  }

  @After
  public void teardown() {
    mockedServiceManager.close();
    mockedMacroAttribute.close();
  }

  @Test
  public void shouldPassMacroAttributesCorrectlyToTheAttributeService() {
    final Optional<Integer> widthOption = Optional.of(250);
    final Optional<String> altOption = Optional.of("Image description");
    final String attributeString = "attributeString";
    when(macroAttributeService.toAttributeString(any())).thenReturn(attributeString);
    when(imageAttributes.getWidth()).thenReturn(widthOption);
    when(imageAttributes.getAlt()).thenReturn(altOption);
    mockedMacroAttribute.when(() -> MacroAttribute.createWithLabel(widthOption, "width"))
      .thenReturn(attributeWithLabel);
    mockedMacroAttribute.when(() -> MacroAttribute.createInQuotesWithLabel(altOption, "alt"))
      .thenReturn(attributeInQuotesWithLabel);

    final String result = service.toAttributeString(imageAttributes);

    assertEquals(result, attributeString);
    verify(macroAttributeService).toAttributeString(attributeWithLabel, attributeInQuotesWithLabel);
  }
}
