package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MacroAttributeTest {

  @Test
  public void shouldHandleEmptyValues() {
    MacroAttribute attribute = MacroAttribute.createWithLabel(null, null);

    assertFalse(attribute.asAttributeStringOption().isPresent());
  }

  @Test
  public void shouldHandleEmptyQuotedValues() {
    MacroAttribute attribute = MacroAttribute.createInQuotesWithLabel(null, null);

    assertFalse(attribute.asAttributeStringOption().isPresent());
  }

  @Test
  public void shouldStringifyValueForAttributeString() {
    MacroAttribute attribute = MacroAttribute.createWithLabel(250, null);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(stringOptional.isPresent());
    assertEquals("250", stringOptional.get());
  }


  @Test
  public void shouldEscapeDoubleQuotesInAttributeString() {
    MacroAttribute attribute = MacroAttribute.createWithLabel("Message \"with\" quotes", null);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(stringOptional.isPresent());
    assertEquals("Message \\\"with\\\" quotes", stringOptional.get());
  }

  @Test
  public void shouldQuoteValueInAttributeString() {
    MacroAttribute attribute = MacroAttribute.createInQuotesWithLabel(true, null);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(stringOptional.isPresent());
    assertEquals("\"true\"", stringOptional.get());
  }

  @Test
  public void shouldIncludeLabelCorrectlyInAttributeString() {
    MacroAttribute attribute = MacroAttribute.createWithLabel(75, "width");

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(stringOptional.isPresent());
    assertEquals("width=75", stringOptional.get());
  }

  @Test
  public void shouldIncludeLabelWithQuotedValueAndEscapedDoubleQuotesCorrectlyInAttributeString() {
    MacroAttribute attribute = MacroAttribute.createInQuotesWithLabel("Image of \"the\" person", "alt");

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(stringOptional.isPresent());
    assertEquals("alt=\"Image of \\\"the\\\" person\"", stringOptional.get());
  }
}
