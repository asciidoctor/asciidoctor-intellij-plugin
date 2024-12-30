package org.asciidoc.intellij.threading;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;

/**
 * Handling common pattern around not blocking the main thread.
 *
 * @author Alexander Schwartz 2021
 */
public class AsciiDocProcessUtil {

  /**
   * Starts a runnable that is interruptable by a write action, but not if this is called from the event dispatcher
   * thread for example when doing refactorings and fixes.
   * Use this as an alternative for
   * ApplicationManager.getApplication().runReadAction() to make tasks interruptable.
   * When running in a background thread with a progress indicator and in a read action, use that one and proceed.
   *
   * @throws ProcessCanceledException when a write action needs priority
   */
  public static void runInReadActionWithWriteActionPriority(Runnable runnable) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      ApplicationManager.getApplication().runReadAction(runnable);
    } else if (ApplicationManager.getApplication().isReadAccessAllowed() && ProgressManager.getInstance().getProgressIndicator() != null) {
      runnable.run();
    } else {
      retryable(() -> ReadAction.computeCancellable(() -> {
        runnable.run();
        return null;
      }));
    }
  }

  /**
   * Starts a computable that is interruptable by a write action, but not if this is called from the event dispatcher
   * thread for example when doing refactorings and fixes.
   * Use this as an alternative for
   * ApplicationManager.getApplication().runReadAction() to make tasks interruptable.
   * When running in a background thread with a progress indicator and in a read action, use that one and proceed.
   *
   * @throws ProcessCanceledException when a write action needs priority
   */
  public static <T> T runInReadActionWithWriteActionPriority(Computable<T> computable) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      return ApplicationManager.getApplication().runReadAction(computable);
    } else if (ApplicationManager.getApplication().isReadAccessAllowed() && ProgressManager.getInstance().getProgressIndicator() != null) {
      return computable.compute();
    } else {
      return retryable(() -> ReadAction.computeCancellable(computable::compute));
    }
  }

  /**
   * Retry if we receive a ProcessCanceledException to avoid throwing away previous computations and to continue
   * to make progress.
   */
  private static <T> T retryable(Computable<T> computable) {
    int retries = 2;
    while (true) {
      try {
        return computable.compute();
      } catch (ProcessCanceledException e) {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
          // this is nested within another read action, don't retry
          throw e;
        }
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator != null && progressIndicator.isCanceled()) {
          // we're nested into another progress indicator that has been cancelled, no need to retry
          throw e;
        }
        if (retries > 0) {
          --retries;
          // wait a tick until the current write action completed
          ApplicationManager.getApplication().runReadAction(() -> {
          });
        } else {
          throw e;
        }
      }
    }
  }

}
