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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.injection.LanguageGuesser;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocRunLineMarkersProvider extends RunLineMarkerContributor implements DumbAware {

  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (!TrustedProjects.isTrusted(element.getProject())) {
      return null;
    }
    // Line markers need to be placed at leaf elements only
    // https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html#register-the-line-marker-provider
    if (!(element instanceof LeafPsiElement)) {
      return null;
    }
    while (element != null) {
      // for higher order elements, search the tree upwards from the leaf element
      if ((element instanceof AsciiDocListing)) {
        return handleListing((AsciiDocListing) element);
      }
      if ((element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.LISTING_TEXT)) {
        return handleListingLine(element);
      }
      if ((element instanceof AsciiDocTextQuoted)) {
        return handleQuotedText((AsciiDocTextQuoted) element);
      }
      if (element.getPrevSibling() != null) {
        break;
      }
      if (element instanceof AsciiDocBlock || element instanceof AsciiDocSection) {
        break;
      }
      if (element instanceof PsiFile) {
        break;
      }
      element = element.getParent();
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
    VirtualFile virtualFile = getVirtualFile(element);
    if (virtualFile == null) {
      return null;
    }

    if (!matches(element.getProject(), virtualFile, text, true)) {
      return null;
    }

    DumbAwareAction runAction = new DumbAwareAction(() -> AsciiDocBundle.message("asciidoc.runner.launch.command", text), AllIcons.RunConfigurations.TestState.Run) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        execute(element.getProject(), virtualFile, text, DefaultRunExecutor.getRunExecutorInstance());
      }
    };

    return new Info(AllIcons.RunConfigurations.TestState.Run, new DumbAwareAction[]{runAction}, (e) -> AsciiDocBundle.message("asciidoc.runner.launch.command", text));
  }

  private VirtualFile getVirtualFile(PsiElement element) {
    VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
    if (virtualFile == null) {
      virtualFile = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    return virtualFile;
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
    if (virtualFile.getParent() == null) {
      return false;
    }
    DataContext dataContext = createDataContext(project, virtualFile, null);

    return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> RunAnythingProvider.EP_NAME.getExtensionList().stream()
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
    AsciiDocRunner runner = AsciiDocRunner.EP_NAME.getExtensionList().stream().filter(r -> {
      try {
        return r.isApplicable(language);
      } catch (NoClassDefFoundError ex) {
        // this might fail due to the optional dependency being loaded after the AsciiDoc plugin in the wrong classloader
        // https://youtrack.jetbrains.com/issue/IDEA-287090/
        return false;
      }
    }).findFirst().orElse(null);
    if (runner == null) {
      return null;
    }
    VirtualFile virtualFile = listing.getContainingFile().getVirtualFile();
    DumbAwareAction runAction = new DumbAwareAction(runner::getTitle, AllIcons.RunConfigurations.TestState.Run_run) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        runner.run(listing.getContentTextRange().substring(listing.getText()), project, virtualFile, DefaultRunExecutor.getRunExecutorInstance());
      }
    };

    return new Info(AllIcons.RunConfigurations.TestState.Run_run, new DumbAwareAction[]{runAction}, e -> runner.getTitle());
  }
}
