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
  public void mapsAttributesToCommaSeparatedLabelValueString() {
    final Pair<String, Optional<?>> attribute1 = new Pair<>("a1", Optional.of("1"));
    final Pair<String, Optional<?>> attribute2 = new Pair<>("a2", Optional.of(2));
    final Pair<String, Optional<?>> attribute3 = new Pair<>("a3", Optional.of(true));

    final String attributes = service.toAttributeString(attribute1, attribute2, attribute3);

    assertEquals(attributes, "a1=1,a2=2,a3=true");
  }

  @Test
  public void filtersEmptyOptionalValues() {
    final Pair<String, Optional<?>> attribute = new Pair<>("attribute", Optional.of(true));
    final Pair<String, Optional<?>> empty = new Pair<>("empty", Optional.empty());

    final String attributes = service.toAttributeString(attribute, empty);

    assertEquals(attributes, "attribute=true");
  }

  @Test
  public void returnsAnEmptyStringIfInvokedWithoutArgs() {
    assertEquals(service.toAttributeString(), "");
  }
}
