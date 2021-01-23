package org.asciidoc.intellij.threading;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;

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
   *
   * @throws ProcessCanceledException when a write action needs priority
   */
  public static void runInReadActionWithWriteActionPriority(Runnable runnable) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      ApplicationManager.getApplication().runReadAction(runnable);
    } else {
      retryable(() -> {
        if (!ProgressManager.getInstance().runInReadActionWithWriteActionPriority(runnable, null)) {
          throw new ProcessCanceledException();
        }
      });
    }
  }

  /**
   * Starts a computable that is interruptable by a write action, but not if this is called from the event dispatcher
   * thread for example when doing refactorings and fixes.
   * Use this as an alternative for
   * ApplicationManager.getApplication().runReadAction() to make tasks interruptable.
   *
   * @throws ProcessCanceledException when a write action needs priority
   */
  public static <T> T runInReadActionWithWriteActionPriority(Computable<T> computable) {
    Ref<T> ref = new Ref<>();
    if (ApplicationManager.getApplication().isDispatchThread()) {
      ref.set(ApplicationManager.getApplication().runReadAction(computable));
    } else {
      retryable(() -> {
        if (!ProgressManager.getInstance().runInReadActionWithWriteActionPriority(() -> ref.set(computable.compute()), null)) {
          throw new ProcessCanceledException();
        }
      });
    }
    return ref.get();
  }

  /**
   * Retry if we receive a ProcessCanceledException to avoid throwing away previous computations and to continue
   * to make progress.
   */
  private static void retryable(Runnable runnable) {
    int retries = 2;
    while (true) {
      try {
        runnable.run();
        /*
        if (retries != 2) {
          System.out.println("retry succeeded");
        }
        */
        return;
      } catch (ProcessCanceledException e) {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
          // this is nested, don't retry
          throw e;
        }
        if (retries > 0) {
          --retries;
          // wait a tick until the current write action completed
          ApplicationManager.getApplication().runReadAction(() -> {
          });
        } else {
          // System.out.println("retry failed");
          throw e;
        }
      }
    }
  }

}
