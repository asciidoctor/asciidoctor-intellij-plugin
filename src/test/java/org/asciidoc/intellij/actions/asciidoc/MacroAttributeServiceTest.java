package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class MacroAttributeServiceTest {

  private MacroAttributeService service;

  @Before
  public void setup() {
    service = new MacroAttributeService();
  }

  @Test
  public void shouldMapMacroAttributesToLabelString() {
    final MacroAttribute attribute1 = new MacroAttribute(Optional.of("source"), Optional.empty(), false);
    final MacroAttribute attribute2 = new MacroAttribute(Optional.of("java"), Optional.empty(), false);
    final MacroAttribute attribute3 = new MacroAttribute(Optional.of("Example 1"), Optional.of("title"), true);

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals("source,java,title=\"Example 1\"", attributes);
  }

  @Test
  public void shouldFilterEmptyMacroAttributes() {
    final MacroAttribute attribute = new MacroAttribute(Optional.of("Some alt text"), Optional.of("alt"), true);
    final MacroAttribute empty = new MacroAttribute(Optional.empty(), Optional.empty(), false);

    final String attributes = service.toAttributeString(attribute, empty);

    assertEquals("alt=\"Some alt text\"", attributes);
  }

  @Test
  public void shouldReturnAnEmptyStringIfInvokedWithoutArgs() {
    assertEquals("", service.toAttributeString());
  }

}
