package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class AttributeServiceTest {

  private AttributeService service;

  @Before
  public void setup() {
    service = new AttributeService();
  }

  @Test
  public void shouldMapAttributesToCommaSeparatedLabelValueString() {
    final Pair<Optional<?>, Optional<String>> attribute1 = new Pair<>(Optional.of("1"), Optional.of("a1"));
    final Pair<Optional<?>, Optional<String>> attribute2 = new Pair<>(Optional.of(2), Optional.of("a2"));
    final Pair<Optional<?>, Optional<String>> attribute3 = new Pair<>(Optional.of(true), Optional.of("a3"));

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals("a1=1,a2=2,a3=true", attributes);
  }

  @Test
  public void shouldMapAttributesWithEmptyAndNonEmptyOptionalLabelCorrectly() {
    final Pair<Optional<?>, Optional<String>> attribute1 = new Pair<>(Optional.of("source"), Optional.empty());
    final Pair<Optional<?>, Optional<String>> attribute2 = new Pair<>(Optional.of("java"), Optional.empty());
    final Pair<Optional<?>, Optional<String>> attribute3 = new Pair<>(Optional.of("\"Example 1\""), Optional.of("title"));

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals("source,java,title=\"Example 1\"", attributes);
  }

  @Test
  public void shouldFiltersEmptyOptionalValues() {
    final Pair<Optional<?>, Optional<String>> attribute = new Pair<>(Optional.of(true), Optional.of("attribute"));
    final Pair<Optional<?>, Optional<String>> empty = new Pair<>(Optional.empty(), Optional.of("empty"));

    final String attributes = service.toAttributeString(attribute, empty);

    assertEquals("attribute=true", attributes);
  }

  @Test
  public void shouldReturnAnEmptyStringIfInvokedWithoutArgs() {
    assertEquals("", service.toAttributeString());
  }
}
