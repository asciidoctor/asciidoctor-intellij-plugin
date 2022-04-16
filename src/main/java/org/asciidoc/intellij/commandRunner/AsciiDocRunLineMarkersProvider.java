package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.runAnything.RunAnythingAction;
import com.intellij.ide.actions.runAnything.RunAnythingContext;
import com.intellij.ide.actions.runAnything.RunAnythingRunConfigurationProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingRecentProjectProvider;
import com.intellij.ide.impl.TrustedProjects;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.injection.LanguageGuesser;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocRunLineMarkersProvider extends RunLineMarkerContributor {
  public static final Key<CachedValue<Boolean>> KEY_ASCIIDOC_RUNNABLE = new Key<>("asciidoc-runnable");

  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (!TrustedProjects.isTrusted(element.getProject())) {
      return null;
    }
    if ((element instanceof AsciiDocListing)) {
      return handleListing((AsciiDocListing) element);
    }
    if ((element.getNode().getElementType() == AsciiDocTokenTypes.LISTING_TEXT)) {
      return handleListingLine(element);
    }
    if ((element instanceof AsciiDocTextQuoted)) {
      return handleQuotedText((AsciiDocTextQuoted) element);
    }
    return null;
  }

  private Info handleListingLine(PsiElement line) {
    String text = line.getText();
    return handleCommand(line, text);
  }

  private Info handleQuotedText(AsciiDocTextQuoted quotedText) {
    if (!quotedText.isMono()) {
      return null;
    }
    String text = AsciiDocTextQuoted.getBodyRange(quotedText).shiftLeft(quotedText.getTextOffset()).substring(quotedText.getText());
    return handleCommand(quotedText, text);
  }
  @Nullable
  private Info handleCommand(PsiElement element, String text) {
    VirtualFile virtualFile = element.getContainingFile().getVirtualFile();

    boolean matches = CachedValuesManager.getCachedValue(element, KEY_ASCIIDOC_RUNNABLE,
      () -> {
        boolean result = matches(element.getProject(), virtualFile, text, true);
        return CachedValueProvider.Result.create(result, element);
      }
    );

    if (!matches) {
      return null;
    }

    AnAction runAction = new AnAction(() -> AsciiDocBundle.message("asciidoc.runner.snippet"), AllIcons.RunConfigurations.TestState.Run) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        execute(element.getProject(), virtualFile, text, DefaultRunExecutor.getRunExecutorInstance());
      }
    };

    return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{runAction}, (e) -> AsciiDocBundle.message("asciidoc.runner.launch.command", text));
  }

  private void execute(Project project, VirtualFile virtualFile, String command, Executor executor) {
    DataContext dataContext = createDataContext(project, virtualFile, executor);
    String trimmedCmd = command.trim();
    ApplicationManager.getApplication().runReadAction(() -> {
      //noinspection unchecked
      for (RunAnythingProvider<Object> provider : RunAnythingProvider.EP_NAME.getExtensions()) {
        Object value = provider.findMatchingValue(dataContext, trimmedCmd);
        if (value != null) {
          provider.execute(dataContext, value);
          return;
        }
      }
    });
  }

  @SuppressWarnings("SameParameterValue")
  private boolean matches(Project project, VirtualFile virtualFile, String command, boolean allowRunConfigurations) {
    String trimmedCmd = command.trim();
    if (trimmedCmd.isEmpty()) {
      return false;
    }
    DataContext dataContext = createDataContext(project, virtualFile, null);

    return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> RunAnythingProvider.EP_NAME.extensions()
      .filter(it -> checkForCLI(it, allowRunConfigurations))
        .anyMatch(provider -> provider.findMatchingValue(dataContext, trimmedCmd) != null));
  }

  private DataContext createDataContext(Project project, VirtualFile virtualFile, Executor executor) {
    SimpleDataContext.Builder builder = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(CommonDataKeys.VIRTUAL_FILE, virtualFile.getParent())
      .add(RunAnythingProvider.EXECUTING_CONTEXT, new RunAnythingContext.RecentDirectoryContext(virtualFile.getParent().getPath()));
    if (executor != null) {
      builder.add(RunAnythingAction.EXECUTOR_KEY, executor);
    }
    return builder.build();
  }

  private boolean checkForCLI(RunAnythingProvider<?> it, boolean allowRunConfigurations) {
    return (!(it instanceof RunAnythingCommandProvider)
      && !(it instanceof RunAnythingRecentProjectProvider)
      && (!(it instanceof RunAnythingRunConfigurationProvider) || allowRunConfigurations));
  }

  private Info handleListing(AsciiDocListing listing) {
    String fenceLanguage = listing.getFenceLanguage();
    if (fenceLanguage == null) {
      return null;
    }
    Language language = LanguageGuesser.guessLanguage(fenceLanguage);
    if (language == null) {
      return null;
    }
    AsciiDocRunner runner = AsciiDocRunner.EP_NAME.extensions().filter(r -> r.isApplicable(language)).findFirst().orElse(null);
    if (runner == null) {
      return null;
    }
    VirtualFile virtualFile = listing.getContainingFile().getVirtualFile();
    AnAction runAction = new AnAction(runner::getTitle, AllIcons.RunConfigurations.TestState.Run_run) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        runner.run(listing.getContentTextRange().substring(listing.getText()), project, virtualFile, DefaultRunExecutor.getRunExecutorInstance());
      }
    };

    return new Info(AllIcons.RunConfigurations.TestState.Run_run, new AnAction[] {runAction}, e -> runner.getTitle());
  }
}
