package org.asciidoc.intellij.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InlineIncludeProcessor extends BaseRefactoringProcessor {
  private PsiElement myElement;
  private PsiFile myResolved;
  private boolean myInlineThisOnly;
  private boolean myDeleteDeclaration;

  public InlineIncludeProcessor(PsiElement element, Project project, PsiFile resolved, boolean inlineThisOnly, boolean isDeleteDeclaration) {
    super(project);
    myElement = element;
    myResolved = resolved;
    myInlineThisOnly = inlineThisOnly;
    myDeleteDeclaration = isDeleteDeclaration;
  }

  @NotNull
  @Override
  protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
    return new UsageViewDescriptor() {
      @NotNull
      @Override
      public PsiElement[] getElements() {
        return new PsiElement[]{myResolved};
      }

      @Override
      public String getProcessedElementsHeader() {
        return "AsciiDoc include to inline";
      }

      @NotNull
      @Override
      public String getCodeReferencesText(int usagesCount, int filesCount) {
        return AsciiDocBundle.message("invocations.to.be.inlined", UsageViewBundle.getReferencesString(usagesCount, filesCount));
      }

      @Nullable
      @Override
      public String getCommentReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("comments.elements.header",
          UsageViewBundle.getOccurencesString(usagesCount, filesCount));
      }
    };
  }

  @NotNull
  @Override
  protected UsageInfo[] findUsages() {
    if (myInlineThisOnly) {
      return new UsageInfo[]{new UsageInfo(myElement)};
    }

    List<UsageInfo> usages = new ArrayList<>();
    for (PsiReference ref : ReferencesSearch.search(myResolved, ProjectScope.getProjectScope(myProject), false)) {
      PsiElement element = ref.getElement();
      if (element instanceof AsciiDocBlockMacro) {
        if ("include".equals(((AsciiDocBlockMacro) element).getMacroName())) {
          UsageInfo info = new UsageInfo(element);
          usages.add(info);
        }
      }
    }
    return usages.toArray(UsageInfo.EMPTY_ARRAY);
  }

  @Override
  protected void performRefactoring(@NotNull UsageInfo[] usages) {
    // sort by largest offset first to replace at end of file first
    Arrays.sort(usages, (o1, o2) -> -o1.compareToByStartOffset(o2));

    boolean replaced = false;
    for (UsageInfo usage : usages) {
      if (usage.getFile() == null) {
        continue;
      }
      Document document = PsiDocumentManager.getInstance(myProject).getDocument(usage.getFile());
      PsiElement psi = usage.getElement();
      if (psi == null) {
        continue;
      }
      if (document != null) {
        document.replaceString(psi.getTextOffset(), psi.getTextOffset() + psi.getTextLength(), myResolved.getText());
        replaced = true;
      }
    }
    if (replaced && !myInlineThisOnly && myDeleteDeclaration) {
      // only delete file if it was replaced at least once
      myResolved.delete();
    }
  }

  @NotNull
  @Override
  protected String getCommandName() {
    return "inline includes";
  }
}
