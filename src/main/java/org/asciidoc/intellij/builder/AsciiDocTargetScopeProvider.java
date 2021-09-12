package org.asciidoc.intellij.builder;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.workspaceModel.ide.WorkspaceModelTopics;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto;

import java.util.List;

/**
 * This is a dummy provider to clear all errors in asciidoctor files and re-trigger a new code analysis for the
 * currently selected/open files using {@link DaemonCodeAnalyzer}.
 */
public class AsciiDocTargetScopeProvider extends BuildTargetScopeProvider {
  @NotNull
  @Override
  public List<CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope> getBuildTargetScopes(@NotNull CompileScope baseScope, @NotNull Project project, boolean forceBuild) {
    if (forceBuild) {
      ReadAction.run(() -> {
        // avoid exception "Directory index can only be queried after project initialization" in RootIndex.java
        //noinspection UnstableApiUsage
        if (WorkspaceModelTopics.Companion.getInstance(project).getModulesAreLoaded()) {
          for (Module module : ModuleManager.getInstance(project).getModules()) {
            clearProblemsForAsciidocFiles(module, project);
          }
        }
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
        // this will clear the problems for all Asciidoc files in the modules
        // consider using clearProblemsFromExternalSource available from 2019.x?
        theProblemSolver.clearProblems(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
          // the DaemonCodeAnalyzer will only trigger for any open file (independent of the file we pass here)
          DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
        }
      }
      return true;
    });
  }
}
