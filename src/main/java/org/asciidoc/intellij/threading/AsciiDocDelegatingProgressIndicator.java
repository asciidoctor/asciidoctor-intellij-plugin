package org.asciidoc.intellij.threading;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.StandardProgressIndicator;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import org.jetbrains.annotations.Nullable;

public class AsciiDocDelegatingProgressIndicator extends AbstractProgressIndicatorExBase implements StandardProgressIndicator {

  private final ProgressIndicator delegate;

  private AsciiDocDelegatingProgressIndicator(ProgressIndicator delegate) {
    this.delegate = delegate;
    initStateFrom(this.delegate);
  }

  @Nullable
  public static ProgressIndicator build() {
    ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (progressIndicator != null) {
      progressIndicator = new AsciiDocDelegatingProgressIndicator(progressIndicator);
    }
    return progressIndicator;
  }

  @Override
  public void setText(String text) {
    super.setText(text);
    delegate.setText(text);
  }

  @Override
  public void setText2(String text) {
    super.setText2(text);
    delegate.setText2(text);
  }

  @Override
  public void cancel() {
    super.cancel();
    delegate.cancel();
  }

  @Override
  public boolean isCanceled() {
    return super.isCanceled() || delegate.isCanceled();
  }

  @Override
  public void setFraction(double fraction) {
    super.setFraction(fraction);
    delegate.setFraction(fraction);
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
    super.setIndeterminate(indeterminate);
    delegate.setIndeterminate(indeterminate);
  }
}
