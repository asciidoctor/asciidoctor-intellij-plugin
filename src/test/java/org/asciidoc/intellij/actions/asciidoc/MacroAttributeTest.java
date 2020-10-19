package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MacroAttributeTest {

  @Test
  public void shouldHandleEmptyValues() {
    MacroAttribute attribute = new MacroAttribute(Optional.empty(), Optional.empty(), false);

    assertFalse(attribute.hasValue());
    assertFalse(attribute.asAttributeStringOption().isPresent());

  }

  @Test
  public void shouldHandleEmptyQuotedValues() {
    MacroAttribute attribute = new MacroAttribute(Optional.empty(), Optional.empty(), true);

    assertFalse(attribute.hasValue());
    assertFalse(attribute.asAttributeStringOption().isPresent());
  }

  @Test
  public void shouldStringifyValueForAttributeString() {
    MacroAttribute attribute = new MacroAttribute(Optional.of(250), Optional.empty(), false);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(attribute.hasValue());
    assertTrue(stringOptional.isPresent());
    assertEquals("250", stringOptional.get());
  }


  @Test
  public void shouldEscapeDoubleQuotesInAttributeString() {
    MacroAttribute attribute = new MacroAttribute(Optional.of("Message \"with\" quotes"), Optional.empty(), false);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(attribute.hasValue());
    assertTrue(stringOptional.isPresent());
    assertEquals("Message \\\"with\\\" quotes", stringOptional.get());
  }

  @Test
  public void shouldQuoteValueInAttributeString() {
    MacroAttribute attribute = new MacroAttribute(Optional.of(true), Optional.empty(), true);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(attribute.hasValue());
    assertTrue(stringOptional.isPresent());
    assertEquals("\"true\"", stringOptional.get());
  }

  @Test
  public void shouldIncludeLabelCorrectlyInAttributeString() {
    MacroAttribute attribute = new MacroAttribute(Optional.of(75), Optional.of("width"), false);

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(attribute.hasValue());
    assertTrue(stringOptional.isPresent());
    assertEquals("width=75", stringOptional.get());
  }

  @Test
  public void shouldIncludeLabelWithQuotedValueAndEscapedDoubleQuotesCorrectlyInAttributeString() {
    MacroAttribute attribute = new MacroAttribute(
      Optional.of("Image of \"the\" person"),
      Optional.of("alt"),
      true
    );

    Optional<String> stringOptional = attribute.asAttributeStringOption();

    assertTrue(attribute.hasValue());
    assertTrue(stringOptional.isPresent());
    assertEquals("alt=\"Image of \\\"the\\\" person\"", stringOptional.get());
  }
}
