package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AsciiDocInspectionBase extends LocalInspectionTool {
  protected static final AsciiDocVisitor DUMMY_VISITOR = new AsciiDocVisitor() {
  };

  @NotNull
  @Override
  public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
    AsciiDocFile file = ObjectUtils.tryCast(session.getFile(), AsciiDocFile.class);
    return file != null
      ? buildAsciiDocVisitor(holder, session)
      : DUMMY_VISITOR;
  }

  @NotNull
  @Override
  public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    throw new IllegalStateException();
  }

  @Nullable
  @Override
  public final ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    throw new IllegalStateException();
  }

  @NotNull
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitFile(PsiFile file) {
        checkFile((AsciiDocFile) file, holder);
      }
    };
  }

  protected void checkFile(@NotNull AsciiDocFile file, @NotNull ProblemsHolder problemsHolder) {
  }
}
