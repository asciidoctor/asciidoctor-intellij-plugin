package org.asciidoc.intellij.ui;

import com.intellij.ui.components.fields.IntegerField;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.Integer.MAX_VALUE;

public final class ImageWidthField extends IntegerField {

  public static final @NotNull String FIELD_NAME = "imageWidth";

  public ImageWidthField() {
    super(FIELD_NAME, 0, MAX_VALUE);
    setEnabled(false);
    setCanBeEmpty(true);
    setToolTipText("Set image width in pixel");
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public void initializeWith(@NotNull final CompletableFuture<Optional<Integer>> initialWidthFuture,
                             @NotNull final Runnable onInitializationCompleted) {
    initialWidthFuture.thenAccept(initialWidth -> {
      initialWidth.ifPresent(this::setValue);
      onInitializationCompleted.run();
    });
  }

  public @NotNull Optional<Integer> getValueOption() {
    return Optional.of(super.getValue()).filter(this::excludeZero);
  }

  private boolean excludeZero(@NotNull final Integer width) {
    return !width.equals(0);
  }
}
