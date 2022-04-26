package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AsciiDocMoveFileHandler extends MoveFileHandler {
  @Override
  public boolean canProcessElement(PsiFile element) {
    return element.getLanguage() == AsciiDocLanguage.INSTANCE && AsciiDocUtil.isAntoraPage(element);
  }

  @Override
  public void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {
    PsiElementVisitor visitor = new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        for (PsiReference reference : element.getReferences()) {
          if (reference instanceof AsciiDocFileReference) {
            PsiElement psiElement = reference.resolve();
            if (psiElement != null) {
              oldToNewMap.put(element, psiElement);
            }
          }
        }
        element.acceptChildren(this);
      }
    };
    file.accept(visitor);
  }

  public static class MyUsageInfo extends UsageInfo {
    private final PsiElement element;
    private final PsiReference reference;
    public MyUsageInfo(@NotNull PsiReference reference, @NotNull PsiElement element) {
      super(reference);
      this.element = element;
      this.reference = reference;
    }
  }

  @Override
  public @Nullable List<UsageInfo> findUsages(PsiFile file, PsiDirectory newParent, boolean searchInComments, boolean searchInNonJavaFiles) {
    List<UsageInfo> result = new ArrayList<>();
    PsiElementVisitor visitor = new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        for (PsiReference reference : element.getReferences()) {
          if (reference instanceof AsciiDocFileReference) {
            PsiElement resolved = reference.resolve();
            if (resolved != null && resolved.getContainingFile() != file && !(resolved instanceof PsiDirectory)) {
              result.add(new MyUsageInfo(reference, resolved));
            }
          }
        }
        element.acceptChildren(this);
      }
    };
    file.accept(visitor);
    return result;
  }

  @Override
  public void retargetUsages(List<UsageInfo> usageInfos, Map<PsiElement, PsiElement> oldToNewMap) {
    for (UsageInfo usageInfo : usageInfos) {
      if (usageInfo instanceof MyUsageInfo) {
        ((MyUsageInfo) usageInfo).reference.bindToElement(((MyUsageInfo) usageInfo).element);
      }
    }
  }

  @Override
  public void updateMovedFile(PsiFile file) throws IncorrectOperationException {
  }
}
