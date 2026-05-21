package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Runs long-running script / command in background.
 */
class AsciiDocBackgroundCommand extends Task.Backgroundable {
  private static final Logger LOG = Logger.getInstance(AsciiDocBackgroundCommand.class);

  private final AsciiDocRunnerArbitrary asciiDocRunnerArbitrary;
  private final Project project;
  private final GeneralCommandLine commandLine;
  private final AsciiDocRunnerArbitrary.ConsoleData consoleData;

  @Nullable
  private ProcessHandler processHandler;
  private boolean abort = false;

  AsciiDocBackgroundCommand(@NotNull AsciiDocRunnerArbitrary asciiDocRunnerArbitrary, @NotNull Project project,
                            @NotNull GeneralCommandLine commandLine,
                            @NotNull AsciiDocRunnerArbitrary.ConsoleData consoleData) {
    super(project, asciiDocRunnerArbitrary.getTitle());
    this.asciiDocRunnerArbitrary = asciiDocRunnerArbitrary;
    this.project = project;
    this.commandLine = commandLine;
    this.consoleData = consoleData;
  }

  /**
   * Returns first non-blank string.
   *
   * @param values Strings.
   * @return First non-blank string.
   */
  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    try {
      Instant start = Instant.now();
      processHandler = new KillableColoredProcessHandler(commandLine);

      consoleData.consoleView().attachToProcess(processHandler);

      // Start notifying Listeners.
      processHandler.startNotify();
      /*
       Wait indefinitely for process termination. The user can always stop it with the "Abort" button.
       Very useful, e.g., if the user starts an editor from a manual, like "editor -a -h -x -trx:264 ...",
       and works on it for a few days, not shutting the pc down between work days.
       */
      if (!processHandler.waitFor() && !abort && isRunning()) {
        processHandler.destroyProcess();
        String timeoutMessage = "Forcefully killed %s after error.".formatted(getTitle());
        appendSystemLine(timeoutMessage);
        asciiDocRunnerArbitrary.showError(project, timeoutMessage);
        return;
      }

      Duration duration = Duration.between(start, Instant.now());
      if (abort) {
        appendSystemLine("%s aborted after %s.".formatted(getTitle(), duration));
        return;
      }
      Integer exitCode = processHandler.getExitCode();
      if (exitCode == null || exitCode != 0) {
        String errorText = getTitle() + " execution failed.";
        appendSystemLine("Process exited with code: " + (exitCode == null ? "unknown" : exitCode));
        asciiDocRunnerArbitrary.showError(project, errorText);
        return;
      }

      appendSystemLine("%s executed successfully in %s.".formatted(getTitle(), duration));
    } catch (ExecutionException ex) {
      String errorMessage = "Unable to execute " + asciiDocRunnerArbitrary.getTitle() + " code";
      LOG.warn(errorMessage, ex);
      asciiDocRunnerArbitrary.showError(project, firstNonBlank(ex.getMessage(), errorMessage));
    }
  }

  /**
   * Append system message line.
   *
   * @param message Message.
   */
  private void appendSystemLine(String message) {
    ApplicationManager.getApplication().invokeLater(
      () -> consoleData.consoleView().print("[system] " + message + "\n", ConsoleViewContentType.SYSTEM_OUTPUT));
  }

  /**
   * Process is either terminating or already terminated.
   *
   * @return Process is either terminating or already terminated.
   */
  private boolean isBasicallyTerminated() {
    return processHandler != null && (processHandler.isProcessTerminating() || processHandler.isProcessTerminated());
  }

  /**
   * Terminate process.
   */
  public void abort() {
    if (isRunning() && processHandler != null) {
      abort = true;
      processHandler.destroyProcess();
    }
  }

  /**
   * Process already started and is neither terminating nor terminated.
   *
   * @return Process already started and is neither terminating nor terminated.
   */
  public boolean isRunning() {
    return !isBasicallyTerminated();
  }

  /**
   * Rerun the command.<br>
   * If a command is currently running it gets aborted.
   */
  public void rerun() {
    abort();
    asciiDocRunnerArbitrary.rerun(project, commandLine, consoleData);
  }
}
