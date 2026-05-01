package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;

public abstract class AsciiDocRunnerArbitrary implements AsciiDocRunner {
  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerArbitrary.class);
  private static final int PROCESS_TIMEOUT_MILLIS = 30_000;
  private static final String TOOL_WINDOW_ID = "AsciiDoc Runner";
  private static final String OUTPUT_PREFIX = "[stdout] ";
  private static final String ERROR_PREFIX = "[stderr] ";
  private static final String SYSTEM_PREFIX = "[system] ";

  @Override
  public boolean run(String command, Project project, VirtualFile virtualFile, Executor executor) {
    String code = command == null ? "" : command.trim();
    if (code.isEmpty()) {
      return false;
    }

    String interpreter = findInterpreter(project);
    if (interpreter == null) {
      showError(project, "No " + getTitle() + " interpreter found.");
      return false;
    }

    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(interpreter)
      .withParameters(codeRunParameter(), code)
      .withCharset(StandardCharsets.UTF_8);

    String workingDirectory = determineWorkingDirectory(project, virtualFile);

    if (workingDirectory != null) {
      commandLine.withWorkDirectory(workingDirectory);
    }

    // InvokeLater to get the Log-Console in the UI-Task.
    ApplicationManager.getApplication().invokeLater(() -> {
      ConsoleView consoleView = openLogConsole(project, virtualFile);
      runCommand(project, commandLine, consoleView);
    });
    return true;
  }

  /**
   * The parameter name to pass the code to execute, e.g. "-c" for Python or "-e" for Ruby.
   *
   * @return The parameter name to pass the code to execute, e.g. "-c" for Python or "-e" for Ruby.
   */
  @NonNull abstract String codeRunParameter();

  private void runCommand(Project project, GeneralCommandLine commandLine, ConsoleView consoleView) {
    // ProgressManager to run long-running tasks in the background.
    ProgressManager.getInstance().run(new BackgroundCommand(project, commandLine, consoleView));
  }

  @Nullable
  private static String determineWorkingDirectory(Project project, VirtualFile virtualFile) {
    VirtualFile parent = virtualFile == null ? null : virtualFile.getParent();
    if (parent != null) {
      return parent.getPath();
    }
    return project.getBasePath();
  }

  abstract String findInterpreter(Project project);

  private ConsoleView openLogConsole(Project project, @Nullable VirtualFile virtualFile) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = getOrCreateLogToolWindow(toolWindowManager);

    ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    String contentTitle = virtualFile == null ? getTitle() : virtualFile.getName() + " (" + TOOL_WINDOW_ID + ")";
    Content content = ContentFactory.getInstance().createContent(consoleView.getComponent(), contentTitle, false);
    toolWindow.getContentManager().addContent(content);
    toolWindow.getContentManager().setSelectedContent(content);
    toolWindow.show();

    return consoleView;
  }

  private static ToolWindow getOrCreateLogToolWindow(ToolWindowManager toolWindowManager) {
    ToolWindow existing = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    if (existing != null) {
      return existing;
    }

    try {
      return toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, builder -> {
        builder.anchor = ToolWindowAnchor.BOTTOM;
        builder.canCloseContent = true;
        builder.stripeTitle = () -> TOOL_WINDOW_ID;
        return Unit.INSTANCE;
      });
    } catch (IllegalArgumentException ex) {
      // Another caller might have registered it concurrently.
      ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
      if (toolWindow != null) {
        return toolWindow;
      }
      throw ex;
    }
  }

  private static void appendSystemLine(ConsoleView consoleView, String message) {
    ApplicationManager.getApplication().invokeLater(() ->
      consoleView.print(SYSTEM_PREFIX + message + "\n", ConsoleViewContentType.SYSTEM_OUTPUT));
  }

  private void showError(Project project, String message) {
    Messages.showErrorDialog(project, message, getTitle());
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
          return trimmed;
        }
      }
    }
    return "";
  }

  private static class CommandListener implements ProcessListener {
    private final StringBuilder stdoutBuffer = new StringBuilder();
    private final StringBuilder stderrBuffer = new StringBuilder();
    private final ConsoleView consoleView;

    CommandListener(ConsoleView consoleView) {
      this.consoleView = consoleView;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      String text = event.getText();
      if (text == null || text.isEmpty()) {
        return;
      }

      ConsoleViewContentType contentType;
      String prefix;
      if (ProcessOutputTypes.STDERR.equals(outputType)) {
        stderrBuffer.append(text);
        contentType = ConsoleViewContentType.ERROR_OUTPUT;
        prefix = ERROR_PREFIX;
      } else if (ProcessOutputTypes.STDOUT.equals(outputType)) {
        stdoutBuffer.append(text);
        contentType = ConsoleViewContentType.NORMAL_OUTPUT;
        prefix = OUTPUT_PREFIX;
      } else {
        contentType = ConsoleViewContentType.SYSTEM_OUTPUT;
        prefix = SYSTEM_PREFIX;
      }

      String line = prefix + text;
      ApplicationManager.getApplication().invokeLater(() -> consoleView.print(line, contentType));
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      // No-op: we append final state after waitFor() to keep ordering deterministic.
    }

    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
      // No-op.
    }

    @Override
    public void startNotified(@NotNull ProcessEvent event) {
      // No-op.
    }
  }

  private class BackgroundCommand extends Task.Backgroundable {
    private final Project project;
    private final GeneralCommandLine commandLine;
    private final ConsoleView consoleView;

    BackgroundCommand(Project project, GeneralCommandLine commandLine, ConsoleView consoleView) {
      super(project, AsciiDocRunnerArbitrary.this.getTitle());
      this.project = project;
      this.commandLine = commandLine;
      this.consoleView = consoleView;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      try {
        OSProcessHandler processHandler = new OSProcessHandler(commandLine);

        CommandListener commandListener = new CommandListener(consoleView);
        processHandler.addProcessListener(commandListener);

        processHandler.startNotify();
        if (!processHandler.waitFor(PROCESS_TIMEOUT_MILLIS)) {
          processHandler.destroyProcess();
          appendSystemLine(consoleView, getTitle() + " execution timed out after " + PROCESS_TIMEOUT_MILLIS + " ms.");
          showError(project, getTitle() + " execution timed out after " + PROCESS_TIMEOUT_MILLIS + " ms.");
          return;
        }

        Integer exitCode = processHandler.getExitCode();
        if (exitCode == null || exitCode != 0) {
          String errorText = firstNonBlank(
            commandListener.stderrBuffer.toString(),
            commandListener.stdoutBuffer.toString(),
            getTitle() + " execution failed.");
          appendSystemLine(consoleView, "Process exited with code: " + (exitCode == null ? "unknown" : exitCode));
          showError(project, errorText);
          return;
        }

        appendSystemLine(consoleView, getTitle() + " executed successfully.");
      } catch (ExecutionException ex) {
        String errorMessage = "Unable to execute " + AsciiDocRunnerArbitrary.this.getTitle() + " code";
        LOG.warn(errorMessage, ex);
        showError(project, firstNonBlank(ex.getMessage(), errorMessage));
      }
    }
  }
}
