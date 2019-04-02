package org.asciidoc.intellij.builder;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto;

import java.util.List;

/**
 * This is a dummy provider to clear all error in asciidoctor files and re-trigger a code analyze for the
 * currently selected/open files using {@link DaemonCodeAnalyzer}.
 */
public class AsciidocTargetScopeProvider extends BuildTargetScopeProvider {
  @NotNull
  @Override
  public List<CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope> getBuildTargetScopes(@NotNull CompileScope baseScope, @NotNull CompilerFilter filter, @NotNull Project project, boolean forceBuild) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      clearProblemsForAsciidocFiles(module, project);
    }
    return super.getBuildTargetScopes(baseScope, filter, project, forceBuild);
  }

  private static void clearProblemsForAsciidocFiles(Module module, Project project) {
    WolfTheProblemSolver theProblemSolver = WolfTheProblemSolver.getInstance(project);
    ModuleRootManager.getInstance(module).getFileIndex().iterateContent(file -> {
      if (!file.isDirectory() && AsciiDocFileType.INSTANCE == file.getFileType()) {
        // this will clear the problems for all Asciidoc files in the modules
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
