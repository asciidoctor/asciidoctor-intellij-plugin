package org.asciidoc.intellij.builder;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ModuleRootManager;
// import com.intellij.platform.backend.workspace.WorkspaceModelTopics;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.asciidoc.intellij.annotator.AsciiDocExternalAnnotatorProcessor;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto;

import java.util.List;

/**
 * This is a dummy provider to clear all errors in asciidoctor files and re-trigger a new code analysis for the
 * currently selected/open files using {@link DaemonCodeAnalyzer}.
 */
public class AsciiDocTargetScopeProvider extends BuildTargetScopeProvider {
  private static final Logger LOG = Logger.getInstance(AsciiDocTargetScopeProvider.class);

  @NotNull
  @Override
  public List<CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope> getBuildTargetScopes(@NotNull CompileScope baseScope, @NotNull Project project, boolean forceBuild) {
    if (forceBuild) {
      ReadAction.run(() -> {
        // avoid exception "Directory index can only be queried after project initialization" in RootIndex.java
        // TODO: the following works only in 2023.2 ... therefore commented out for now.
        // if (WorkspaceModelTopics.Companion.getInstance(project).getModulesAreLoaded()) {
          for (Module module : ModuleManager.getInstance(project).getModules()) {
            try {
              clearProblemsForAsciidocFiles(module, project);
            } catch (ProcessCanceledException e) {
              throw e;
            } catch (RuntimeException e) {
              // clearing problems is optional; a damaged virtual file system or an invalid file name must not prevent the build
              LOG.warn("unable to clear problems for module " + module.getName(), e);
            }
          }
        // }
      });
    }
    return super.getBuildTargetScopes(baseScope, project, forceBuild);
  }

  private static void clearProblemsForAsciidocFiles(Module module, Project project) {
    if (project.isDisposed() || module.isDisposed()) {
      return;
    }
    WolfTheProblemSolver theProblemSolver = WolfTheProblemSolver.getInstance(project);
    FileIndexFacade myFileIndexFacade = FileIndexFacade.getInstance(project);
    ModuleRootManager.getInstance(module).getFileIndex().iterateContent(file -> {
      if (!file.isDirectory() && AsciiDocFileType.INSTANCE == file.getFileType()
        && !myFileIndexFacade.isInLibraryClasses(file)
        && !myFileIndexFacade.isInLibrarySource(file)) {
        theProblemSolver.clearProblemsFromExternalSource(file, AsciiDocExternalAnnotatorProcessor.class);
        PsiFile psiFile = null;
        if (file.isValid()) {
          psiFile = PsiManager.getInstance(project).findFile(file);
        }
        if (psiFile != null) {
          // the DaemonCodeAnalyzer will only trigger for any open file (independent of the file we pass here)
          DaemonCodeAnalyzer.getInstance(project).restart(psiFile, "AsciiDoc preview updated");
        }
      }
      return true;
    });
  }
}
