package org.asciidoc.intellij.psi;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AntoraVersionDescriptorTest {

  @Test
  public void shouldSortSemanticVersionsAlphabetically() {
    AntoraVersionDescriptor alpha = new AntoraVersionDescriptor("alpha", null);
    AntoraVersionDescriptor beta = new AntoraVersionDescriptor("beta", null);
    assertOrder(alpha, beta);
  }

  @Test
  public void shouldSortSemanticVersionsAlphabeticallyCaseInsensitive() {
    AntoraVersionDescriptor alpha = new AntoraVersionDescriptor("ALPHA", null);
    AntoraVersionDescriptor beta = new AntoraVersionDescriptor("beta", null);
    assertOrder(alpha, beta);
  }

  @Test
  public void shouldSortSemanticVersionsAlphabeticallyCaseInsensitive2() {
    AntoraVersionDescriptor alpha = new AntoraVersionDescriptor("alpha", null);
    AntoraVersionDescriptor beta = new AntoraVersionDescriptor("BETA", null);
    assertOrder(alpha, beta);
  }

  @Test
  public void shouldSortNamedBeforeSemantic() {
    AntoraVersionDescriptor semantic = new AntoraVersionDescriptor("v1.0", null);
    AntoraVersionDescriptor named = new AntoraVersionDescriptor("beta", null);
    assertOrder(semantic, named);
  }

  @Test
  public void shouldSortSemantic() {
    AntoraVersionDescriptor first = new AntoraVersionDescriptor("1.0", null);
    AntoraVersionDescriptor second = new AntoraVersionDescriptor("1.1", null);
    assertOrder(first, second);
  }

  @Test
  public void shouldSortSemanticDifferentDigits() {
    AntoraVersionDescriptor first = new AntoraVersionDescriptor("1.0", null);
    AntoraVersionDescriptor second = new AntoraVersionDescriptor("1.0.1", null);
    assertOrder(first, second);
  }

  @Test
  public void shouldSortPrereleaseLast() {
    AntoraVersionDescriptor regular = new AntoraVersionDescriptor("1.0", null);
    AntoraVersionDescriptor prerelease = new AntoraVersionDescriptor("1.1", "alpha");
    assertOrder(prerelease, regular);
  }

  @Test
  public void shouldSortPrereleaseAlphabeticaly() {
    AntoraVersionDescriptor alpha = new AntoraVersionDescriptor("1.1", "alpha");
    AntoraVersionDescriptor beta = new AntoraVersionDescriptor("1.1", "beta");
    assertOrder(alpha, beta);
  }

  private void assertOrder(AntoraVersionDescriptor first, AntoraVersionDescriptor second) {
    List<AntoraVersionDescriptor> list = Arrays.asList(first, second);
    Collections.sort(list);
    Assertions.assertThat(list).containsExactly(first, second);
  }

}
