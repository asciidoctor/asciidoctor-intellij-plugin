package org.asciidoc.intellij.actions.asciidoc;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttributeServiceTest {

  private AttributeService service;

  @Before
  public void setup() {
    service = new AttributeService();
  }

  @Test
  public void shouldMapMacroAttributesToLabelString() {
    final MacroAttribute attribute1 = mockedMacroAttribute(Optional.of("source"));
    final MacroAttribute attribute2 = mockedMacroAttribute(Optional.of("java"));
    final MacroAttribute attribute3 = mockedMacroAttribute(Optional.of("title=\"Example 1\""));

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals("source,java,title=\"Example 1\"", attributes);
  }

  @Test
  public void shouldFilterEmptyMacroAttributes() {
    final MacroAttribute attribute = mockedMacroAttribute(Optional.of("alt=\"Some alt text\""));
    final MacroAttribute empty = mockedMacroAttribute(Optional.empty());

    final String attributes = service.toAttributeString(attribute, empty);

    assertEquals("alt=\"Some alt text\"", attributes);
    verify(empty, never()).asAttributeStringOption();
  }

  @Test
  public void shouldReturnAnEmptyStringIfInvokedWithoutArgs() {
    assertEquals("", service.toAttributeString());
  }

  private @NotNull MacroAttribute mockedMacroAttribute(final @NotNull Optional<String> stringValueOption) {
    final MacroAttribute attribute = mock(MacroAttribute.class);

    when(attribute.hasValue()).thenReturn(stringValueOption.isPresent());
    when(attribute.asAttributeStringOption()).thenReturn(stringValueOption);

    return attribute;
  }
}
