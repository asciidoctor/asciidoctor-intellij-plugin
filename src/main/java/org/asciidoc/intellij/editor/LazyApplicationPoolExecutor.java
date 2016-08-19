/*
 * Copyright 2012 Eugene Steinberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.asciidoc.intellij.editor;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * This Executor executes Runnables sequentially and is so lazy that it executes only last Runnable submitted while
 * previously scheduled Runnable is running. Useful when you want to submit a lot of cumulative Runnables without
 * performance impact.
 *
 * @author Eugene Steinberg - plantuml4idea plugin
 */
public class LazyApplicationPoolExecutor implements Executor {

  public static final int DEFAULT_DELAY = 100;

  private Runnable next;

  private Future<?> future;

  private int delay = DEFAULT_DELAY; // delay between command executions

  public LazyApplicationPoolExecutor(int delay) {
    this.delay = delay;
  }

  public LazyApplicationPoolExecutor() {
    this(DEFAULT_DELAY);
  }

  /**
   * Lazily executes the Runnable. Command will be queued for execution, but can be swallowed by another command if it
   * will be submitted before this command will be scheduled for execution
   *
   * @param command command to be executed.
   */
  public synchronized void execute(@NotNull final Runnable command) {
    next = new Runnable() {
      public void run() {
        try {
          command.run();
          Thread.sleep(delay);
        }
        catch (InterruptedException e) {
          System.out.println(System.currentTimeMillis() + ": was interrupted");
          e.printStackTrace();
          Thread.currentThread().interrupt();
        }
        finally {
          if (!Thread.currentThread().isInterrupted())
            scheduleNext(); //needed to execute the very last command
        }
      }
    };
    if (future == null || future.isDone()) {
      scheduleNext();
    }
  }

  private synchronized Future<?> scheduleNext() {
    final Runnable toSchedule = next;
    next = null;
    if (toSchedule != null) {
      future = ApplicationManager.getApplication().executeOnPooledThread(toSchedule);
      return future;
    }
    return null;
  }
}
