package org.asciidoc.intellij.ui.components;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class ImageWidthFieldTest {

  private ImageWidthField widthField;

  @Before
  public void setup() {
    widthField = new ImageWidthField();
  }

  @Test
  public void isSetupCorrectly() {
    assertEquals(widthField.getMinValue(), 0);
    assertEquals(widthField.getMaxValue(), MAX_VALUE);
    assertEquals(widthField.getValueName(), "imageWidth");
    assertEquals(widthField.getToolTipText(), "Set image width in pixel");
    assertTrue(widthField.isCanBeEmpty());
    assertFalse(widthField.isEnabled());
  }

  @Test
  public void setsValueWhenInitializeWidthFutureCompletesAndInvokesRunnable() {
    final Integer newValue = 500;
    final CompletableFuture<Optional<Integer>> initialWidthFuture = completedFuture(Optional.of(newValue));
    final Runnable onInitializationCompleted = Mockito.spy(Runnable.class);

    widthField.initializeWith(initialWidthFuture, onInitializationCompleted);

    assertEquals(widthField.getValue(), newValue);
    verify(onInitializationCompleted).run();
  }

  @Test
  public void ignoresFutureResultWhenCompletesWithAnEmptyOptionalButStillInvokesRunnable() {
    final CompletableFuture<Optional<Integer>> initialWidthFuture = completedFuture(Optional.empty());
    final Runnable onInitializationCompleted = Mockito.spy(Runnable.class);
    final Integer previousValue = 320;
    widthField.setValue(previousValue);

    widthField.initializeWith(initialWidthFuture, onInitializationCompleted);

    assertEquals(widthField.getValue(), previousValue);
    verify(onInitializationCompleted).run();
  }

  @Test
  public void returnsEmptyOptionWhenValueIsZero() {
    widthField.setValue(0);

    final Optional<Integer> valueOption = widthField.getValueOption();

    assertFalse(valueOption.isPresent());
  }

  @Test
  public void returnsOptionWhenValueIsAPositiveInteger() {
    final Integer imageWidth = 234;
    widthField.setValue(imageWidth);

    final Optional<Integer> valueOption = widthField.getValueOption();

    assertTrue(valueOption.isPresent());
    assertEquals(valueOption.get(), imageWidth);
  }

  @Test
  public void setsANewPreferredWidth() {
    final int expectedWidth = 70;

    int preferredWidth = widthField.getPreferredSize().width;

    assertEquals(preferredWidth, expectedWidth);
  }
}
