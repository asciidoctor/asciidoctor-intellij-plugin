package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ObjectUtils;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.AttributeDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

  public static final Key<CachedValue<Boolean>> KEY_ASCIIDOC_EXCLUDED_BY_IFDEF = new Key<>("asciidoc-excluded-by-ifdef");

  // this is static to avoid "Incorrect CachedValue use: same CV with different captured context, this can cause unstable results and invalid PSI access." exception
  protected static boolean isExcludedByIfdef(PsiElement element, boolean goUp) {
    if (goUp && element != null && element.getPrevSibling() == null) {
      element = element.getParent();
    }
    if (goUp && element != null) {
      element = element.getPrevSibling();
    }
    ProgressManager.checkCanceled();
    while (!(element instanceof PsiFile) && element != null) {
      PsiElement localElement = element;
      Boolean r = CachedValuesManager.getCachedValue(element, KEY_ASCIIDOC_EXCLUDED_BY_IFDEF,
        () -> calculateResult(localElement));
      if (r) {
        return r;
      }
      if (goUp) {
        element = element.getParent();
        if (element != null && element.getPrevSibling() == null) {
          element = element.getParent();
        }
        if (element != null) {
          element = element.getPrevSibling();
        }
      } else {
        break;
      }
    }
    return false;
  }

  @NotNull
  private static CachedValueProvider.Result<Boolean> calculateResult(PsiElement localElement) {
    PsiElement o = localElement;
    boolean result = false;
    while (o != null) {
      if (isExcludedByIfDef(o)) {
        result = true;
        break;
      }
      if (isEndIf(o)) {
        break;
      }
      if (isExcludedByIfdef(o.getLastChild(), false)) {
        result = true;
        break;
      }
      o = o.getPrevSibling();
    }
    return CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT);
  }

  private static boolean isExcludedByIfDef(PsiElement element) {
    if (element instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) element;
      if (blockMacro.getMacroName().equals("ifdef") || blockMacro.getMacroName().equals("ifndef")) {
        String attributeName = blockMacro.getResolvedBody();
        List<AttributeDeclaration> attributes = AsciiDocUtil.findAttributes(element.getProject(), attributeName, element);
        boolean defined = false;
        for (AttributeDeclaration attribute : attributes) {
          if (attribute.getAttributeValue() == null) {
            continue;
          }
          defined = true;
          break;
        }
        if (blockMacro.getMacroName().equals("ifndef")) {
          defined = !defined;
        }
        if (!defined) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isEndIf(PsiElement element) {
    if (element instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) element;
      if (blockMacro.getMacroName().equals("endif")) {
        return true;
      }
    }
    return false;
  }

}
