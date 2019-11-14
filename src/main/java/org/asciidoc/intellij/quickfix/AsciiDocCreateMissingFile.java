package org.asciidoc.intellij.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2019
 */
public class AsciiDocCreateMissingFile implements IntentionAction {

  private final AsciiDocBlockMacro element;

  public AsciiDocCreateMissingFile(AsciiDocBlockMacro element) {
    this.element = element;
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return "Create the missing file";
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return "AsciiDoc";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    boolean isAvailable = false;
    PsiDirectory parent = file.getParent();
    if (parent != null) {
      for (PsiReference r : element.getReferences()) {
        if (r instanceof AsciiDocFileReference) {
          AsciiDocFileReference adr = (AsciiDocFileReference) r;
          PsiElement resolved = r.resolve();
          if (resolved instanceof PsiDirectory) {
            parent = (PsiDirectory) resolved;
          } else if (resolved == null) {
            if (adr.canBeCreated(parent)) {
              isAvailable = true;
              break;
            }
          }
        }
      }
    }
    return isAvailable;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiDirectory parent = element.getContainingFile().getParent();
    if (parent == null) {
      return;
    }
    for (PsiReference r : element.getReferences()) {
      if (r instanceof AsciiDocFileReference) {
        AsciiDocFileReference adr = (AsciiDocFileReference) r;
        PsiElement resolved = r.resolve();
        if (resolved instanceof PsiDirectory) {
          parent = (PsiDirectory) resolved;
        } else if (resolved == null) {
          if (adr.canBeCreated(parent)) {
            PsiElement e = adr.createFileOrFolder(parent);
            if (e instanceof PsiDirectory) {
              parent = (PsiDirectory) e;
            } else {
              OpenFileAction.openFile(((PsiFile) e).getVirtualFile(), project);
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
