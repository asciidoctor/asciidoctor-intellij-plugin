package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
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
import org.asciidoc.intellij.commandRunner.arbitrary.action.AsciiDocAbortRunnerAction;
import org.asciidoc.intellij.commandRunner.arbitrary.action.AsciiDocCopyRunnerAction;
import org.asciidoc.intellij.commandRunner.arbitrary.action.AsciiDocRerunRunnerAction;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSetting;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class to run code in arbitrary language.
 */
public abstract class AsciiDocRunnerArbitrary implements AsciiDocRunner {
  /**
   * Returned by {@link AsciiDocRunnerArbitrary#getTempFileInfo()} with information on how to write a temporary file.
   *
   * @param extension Script file extension, e.g. ".go" for Go or ".ts" for TypeScript.
   */
  record TempFileInfo(@NotNull String extension) {
  }

  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerArbitrary.class);

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
      LOG.error("Missing code to run for " + getTitle() + ".");
      return false;
    }

    final AsciiDocScriptLanguageSetting languageSetting = getScriptLanguageSetting();
    final String interpreter;
    if (languageSetting == null) {
      interpreter = findInterpreter();
    } else {
      interpreter = languageSetting.getInterpreterPath();
    }

    final List<String> resultParams = codeRunParameters(languageSetting);
    boolean useTempFile = useTemporaryFile(languageSetting);
    if (resultParams == null || (!useTempFile && resultParams.isEmpty())) {
      // Implementation error, inline-code w/o temp-file must have at least the inline-code parameter.
      throw new IllegalStateException("No parameters to run inline code for " + getTitle() + ".");
    }

    if (interpreter == null) {
      showError(project, "No " + getTitle() + " interpreter found.");
      return false;
    }
    // Add the code, what this is actually all about, at last.
    final File tempFile;
    if (useTempFile) {
      // Use no inline evaluation, run a code-file.
      TempFileInfo tempFileInfo = getTempFileInfo();
      try {
        tempFile = File.createTempFile(virtualFile.getName(), tempFileInfo.extension());
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), code);
        resultParams.add(tempFile.getAbsolutePath());
      } catch (Exception e) {
        LOG.error("Failed to write code to temporary file for script execution.", e);
        return false;
      }
    } else {
      tempFile = null;
      resultParams.add(code);
    }

    GeneralCommandLine commandLine = new GeneralCommandLine() //
      .withExePath(interpreter) //
      .withParameters(resultParams) //
      .withCharset(StandardCharsets.UTF_8);

    String parentPath = getParentPath(virtualFile);
    // This being null should never happen, and seems more a theoretical case.
    if (parentPath != null) {
      commandLine.withEnvironment(Map.of(AsciiDocLanguageConstants.SCRIPT_PATH_NAME, parentPath));
    }

    Map.Entry<String, String> environment = specialEnvironment();
    if (environment != null) {
      commandLine.withEnvironment(environment.getKey(), environment.getValue());
    }

    String workingDirectory = determineWorkingDirectory(project, virtualFile);
    if (workingDirectory != null) {
      commandLine.withWorkDirectory(workingDirectory);
    }

    // InvokeLater to get the Log-Console in the UI-Task.
    ApplicationManager.getApplication().invokeLater(() -> {
      ConsoleData consoleData = openLogConsole(project, virtualFile, tempFile);
      runCommand(project, commandLine, consoleData);
    });
    return true;
  }

  @Nullable
  Map.Entry<String, String> specialEnvironment() {
    // Override if needed.
    return null;
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
  @Nullable List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    List<String> resultParams = new ArrayList<>();
    if (languageSetting != null && languageSetting.getParameters() != null) {
      resultParams.addAll(languageSetting.getParameters());
    }
    return resultParams;
  }

  /**
   * Run potentially long-running, blocking <code>commandLine</code> in a separate background process.
   *
   * @param project     Project.
   * @param commandLine Command-Line.
   * @param consoleData Console data.
   */
  private void runCommand(Project project, GeneralCommandLine commandLine, ConsoleData consoleData) {
    // ProgressManager to run long-running tasks in the background.
    AsciiDocBackgroundCommand backgroundCommand = new AsciiDocBackgroundCommand(this, project, commandLine,
      consoleData);
    // Must be set before "run" call.
    consoleData.abortAction.setAsciiDocBackgroundCommand(backgroundCommand);
    consoleData.repeatAction.setAsciiDocBackgroundCommand(backgroundCommand);
    ProgressManager.getInstance().run(backgroundCommand);
  }

  @Nullable
  private static String determineWorkingDirectory(Project project, VirtualFile virtualFile) {
    String parent = getParentPath(virtualFile);
    if (parent != null) {
      return parent;
    }
    return project.getBasePath();
  }

  @Nullable
  private static String getParentPath(@Nullable VirtualFile virtualFile) {
    VirtualFile parent = virtualFile == null ? null : virtualFile.getParent();
    if (parent != null) {
      return parent.getPath();
    }
    return null;
  }

  /**
   * Implementing class must implement this to get the actual runner, to run the command / script with it.
   *
   * @return Interpreter.
   */
  @Nullable
  abstract String findInterpreter();

  @Nullable
  abstract AsciiDocScriptLanguageSetting extractScriptLanguageSetting(AsciiDocScriptLanguageSettings languageSettings);

  @Nullable
  private AsciiDocScriptLanguageSetting getScriptLanguageSetting() {
    AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    return Optional.ofNullable(settings.getAsciiDocPreviewSettings().getScriptLanguageSettings()) //
      .map(this::extractScriptLanguageSetting)
      .filter(AsciiDocScriptLanguageSetting::isValid)
      .orElse(null);
  }

  /**
   * Create console to log and view output of script / command.<br>
   * It's a standard console, which is configured over the general UI settings.
   *
   * @param project     Project.
   * @param virtualFile Virtual file.
   * @param tempFile    Temporary file. Add a "Copy path of temporary file into clipboard" to toolbar if set.
   * @return Console-Data.
   */
  private ConsoleData openLogConsole(Project project, @Nullable VirtualFile virtualFile, @Nullable File tempFile) {
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

    if (tempFile != null) {
      AsciiDocCopyRunnerAction copyAction = new AsciiDocCopyRunnerAction(tempFile);
      actionGroup.add(copyAction);
    }

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("AsciiDocConsoleToolbar", actionGroup,
      false);
    toolbar.setTargetComponent(consoleView.getComponent());

    mainPanel.add(toolbar.getComponent(), BorderLayout.WEST);
    mainPanel.add(consoleView.getComponent(), BorderLayout.CENTER);

    String contentTitle = virtualFile == null
      ? getTitle()
      : MessageFormat.format("{0} ({1})", virtualFile.getName(), getTitle());
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

  /**
   * Has this runner a valid interpreter?
   *
   * <p>First runner is tried to be loaded from the settings,
   * then, if not found, tried to be found via {@link #findInterpreter()}.
   *
   * @return Has this runner a valid interpreter?
   */
  boolean hasInterpreter() {
    AsciiDocScriptLanguageSetting languageSetting = getScriptLanguageSetting();
    //noinspection ConstantValue
    return (languageSetting != null && languageSetting.getInterpreterPath() != null)
      || (findInterpreter() != null && codeRunParameters(languageSetting) != null);
  }

  static @Nullable String findNativeInterpreter(String interpreterName) {
    //  ProjectJdkTable is for all SDKs, not only JDKs.
    ProjectJdkTable projectJdkTable = ProjectJdkTable.getInstance();
    for (Sdk sdk : projectJdkTable.getAllJdks()) {
      String sdkName = sdk.getSdkType().getName().toLowerCase();
      LOG.info("Sdk: " + sdk + ", type: " + sdk.getSdkType() + ", sdk type name: " + sdkName);
      if (sdkName.startsWith(interpreterName)) {
        String sdkHomePath = sdk.getHomePath();
        LOG.info("SDK home path: " + sdkHomePath);
        if (sdkHomePath != null) {
          File homePath = new File(sdkHomePath);
          if (homePath.canExecute()) {
            LOG.info("Found " + interpreterName + " interpreter at: " + sdkHomePath);
            return sdkHomePath;
          }
        }
      }
    }
    return null;
  }

  /**
   * Override with <code>return true</code> to keep stdin open for interactive input.
   * This allows the command to accept user input like Y/N prompts.
   *
   * <p>Currently PowerShell with the <code>npx.ps1</code> script (for {@link AsciiDocRunnerForTypeScript}),
   * needs this to return <code>false</code> (under Windows), otherwise it freezes, waiting forever for an input.
   *
   * @return True if this command should accept interactive input, false otherwise.
   */
  boolean isInteractiveCommand() {
    return true;
  }

  /**
   * If this language must always execute its adhoc scripts through a temporary file.
   *
   * @return If this language must always execute its adhoc scripts through a temporary file.
   */
  boolean mustUseTemporaryFile() {
    return false;
  }

  /**
   * Checks whether the setting to always use temporary files is active,
   * or the language requires to always use temporary files.
   *
   * @param languageSetting Language setting.
   * @return True if a temporary file should be used, false otherwise.
   */
  boolean useTemporaryFile(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    return mustUseTemporaryFile()
      || (languageSetting != null && Boolean.TRUE.equals(languageSetting.getAlwaysUseTempFile()));
  }

  /**
   * Returns information how to write a temporary file.
   *
   * @return {@link TempFileInfo} with information on how to write a temporary file.
   */
  @NotNull
  abstract TempFileInfo getTempFileInfo();
}
