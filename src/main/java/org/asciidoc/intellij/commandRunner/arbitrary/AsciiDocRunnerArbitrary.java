package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import kotlin.Unit;
import org.asciidoc.intellij.commandRunner.AsciiDocRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class to run code in arbitrary language.
 */
public abstract class AsciiDocRunnerArbitrary implements AsciiDocRunner {
  /**
   * Console-View, Abort- and Repeat-Action belonging to it.
   *
   * @param consoleView  Console-View.
   * @param abortAction  Abort-Action.
   * @param repeatAction Repeat-Action.
   */
  record ConsoleData(@NotNull ConsoleView consoleView,
                     @NotNull AsciiDocAbortRunnerAction abortAction,
                     @NotNull AsciiDocRerunRunnerAction repeatAction) {
  }

  private static final String TOOL_WINDOW_ID = "AsciiDoc Runner";

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
      ConsoleData consoleData = openLogConsole(project, virtualFile);
      runCommand(project, commandLine, consoleData);
    });
    return true;
  }

  /**
   * Rerun the current command in the same console as before, appending output to the existing output.
   *
   * @param project     Project.
   * @param commandLine Command line.
   * @param consoleData ConsoleData.
   */
  void rerun(Project project, GeneralCommandLine commandLine, ConsoleData consoleData) {
    ApplicationManager.getApplication().invokeLater(() -> runCommand(project, commandLine, consoleData));
  }

  /**
   * The parameter name to pass the code to execute, e.g. "-c" for Python or "-e" for Ruby.
   *
   * @return The parameter name to pass the code to execute, e.g. "-c" for Python or "-e" for Ruby.
   */
  @NonNull
  abstract String codeRunParameter();

  /**
   * Run potentially long-running, blocking <code>commandLine</code> in a separate background process.
   *
   * @param project     Project.
   * @param commandLine Command-Line.
   * @param consoleData Console data.
   */
  private void runCommand(Project project, GeneralCommandLine commandLine, ConsoleData consoleData) {
    // ProgressManager to run long-running tasks in the background.
    AsciiDocBackgroundCommand backgroundCommand =
      new AsciiDocBackgroundCommand(this, project, commandLine, consoleData);
    // Must be set before "run" call.
    consoleData.abortAction.setAsciiDocBackgroundCommand(backgroundCommand);
    consoleData.repeatAction.setAsciiDocBackgroundCommand(backgroundCommand);
    ProgressManager.getInstance().run(backgroundCommand);
  }

  @Nullable
  private static String determineWorkingDirectory(Project project, VirtualFile virtualFile) {
    VirtualFile parent = virtualFile == null ? null : virtualFile.getParent();
    if (parent != null) {
      return parent.getPath();
    }
    return project.getBasePath();
  }

  /**
   * Implementing class must implement this to get the actual runner, to run the command / script with it.
   *
   * @param project Project.
   * @return Interpreter.
   */
  abstract String findInterpreter(Project project);

  /**
   * Create console to log and view output of script / command.<br>
   * It's a standard console, which is configured over the general UI settings.
   *
   * @param project     Project.
   * @param virtualFile Virtual file.
   * @return Console-Data.
   */
  private ConsoleData openLogConsole(Project project, @Nullable VirtualFile virtualFile) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = getOrCreateConsoleToolWindow(toolWindowManager);

    ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

    // Create a panel with toolbar and console
    JBPanel<JBPanel<?>> mainPanel = new JBPanel<>(new BorderLayout());

    // Create the toolbar with the abort action
    DefaultActionGroup actionGroup = new DefaultActionGroup();

    AsciiDocAbortRunnerAction abortAction = new AsciiDocAbortRunnerAction();
    actionGroup.add(abortAction);

    AsciiDocRerunRunnerAction rerunAction = new AsciiDocRerunRunnerAction();
    actionGroup.add(rerunAction);

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AsciiDocConsoleToolbar", actionGroup,
      false);
    toolbar.setTargetComponent(consoleView.getComponent());

    mainPanel.add(toolbar.getComponent(), BorderLayout.WEST);
    mainPanel.add(consoleView.getComponent(), BorderLayout.CENTER);

    String contentTitle = virtualFile == null ? getTitle() : virtualFile.getName() + " (" + TOOL_WINDOW_ID + ")";
    final Content content = ContentFactory.getInstance().createContent(mainPanel, contentTitle, false);

    toolWindow.getContentManager().addContent(content);
    toolWindow.getContentManager().setSelectedContent(content);
    toolWindow.show();

    return new ConsoleData(consoleView, abortAction, rerunAction);
  }

  /**
   * Create console tool window on demand.
   *
   * @param toolWindowManager Tool window manager.
   * @return Console tool window.
   */
  private ToolWindow getOrCreateConsoleToolWindow(ToolWindowManager toolWindowManager) {
    ToolWindow existing = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    if (existing != null) {
      return existing;
    }

    try {
      return toolWindowManager.registerToolWindow(TOOL_WINDOW_ID, builder -> {
        builder.anchor = ToolWindowAnchor.BOTTOM;
        builder.canCloseContent = true;
        builder.stripeTitle = () -> TOOL_WINDOW_ID;
        builder.icon = IconLoader.getIcon("/icons/runScript.svg", AsciiDocRunnerArbitrary.class);
        return Unit.INSTANCE;
      });
    } catch (IllegalArgumentException ex) {
      // Another caller has registered this tool window concurrently.
      ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
      if (toolWindow != null) {
        return toolWindow;
      }
      throw ex;
    }
  }

  /**
   * Show error popup on UI thread.
   *
   * @param project Project.
   * @param message Message.
   */
  void showError(@Nullable Project project, @NotNull String message) {
    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project, message, getTitle()));
  }
}
