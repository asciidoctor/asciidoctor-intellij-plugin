package org.asciidoc.intellij.ui;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Integer.MAX_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    final CompletableFuture<Optional<Integer>> initialWidthFuture = CompletableFuture.completedFuture(Optional.of(newValue));
    // TODO replace with proper mocking once available
    final AtomicBoolean runnableWasExecuted = new AtomicBoolean(false);

    widthField.initializeWith(initialWidthFuture, () -> runnableWasExecuted.set(true));

    assertEquals(widthField.getValue(), newValue);
    assertTrue(runnableWasExecuted.get());
  }

  @Test
  public void ignoresFutureResultWhenCompletesWithAnEmptyOptionalButStillInvokesRunnable() {
    final CompletableFuture<Optional<Integer>> initialWidthFuture = CompletableFuture.completedFuture(Optional.empty());
    // TODO replace with proper mocking once available
    final AtomicBoolean runnableWasExecuted = new AtomicBoolean(false);
    final Integer previousValue = 320;
    widthField.setValue(previousValue);

    widthField.initializeWith(initialWidthFuture, () -> runnableWasExecuted.set(true));

    assertEquals(widthField.getValue(), previousValue);
    assertTrue(runnableWasExecuted.get());
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
}
