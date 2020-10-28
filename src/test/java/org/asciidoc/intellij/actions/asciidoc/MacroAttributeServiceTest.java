package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MacroAttributeServiceTest {

  private MacroAttributeService service;

  @Before
  public void setup() {
    service = new MacroAttributeService();
  }

  @Test
  public void shouldMapMacroAttributesToLabelString() {
    final MacroAttribute attribute1 = MacroAttribute.createWithLabel("source", null);
    final MacroAttribute attribute2 = MacroAttribute.createWithLabel("java", null);
    final MacroAttribute attribute3 = MacroAttribute.createInQuotesWithLabel("Example 1", "title");

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals("source,java,title=\"Example 1\"", attributes);
  }

  @Test
  public void shouldFilterEmptyMacroAttributes() {
    final MacroAttribute attribute = MacroAttribute.createInQuotesWithLabel("Some alt text", "alt");
    final MacroAttribute empty = MacroAttribute.createWithLabel(null, null);

    final String attributes = service.toAttributeString(attribute, empty);

    assertEquals("alt=\"Some alt text\"", attributes);
  }

  @Test
  public void shouldReturnAnEmptyStringIfInvokedWithoutArgs() {
    assertEquals("", service.toAttributeString());
  }

}
