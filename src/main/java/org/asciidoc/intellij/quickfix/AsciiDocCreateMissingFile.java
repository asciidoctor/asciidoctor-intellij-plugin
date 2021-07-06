package org.asciidoc.intellij.quickfix;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;

import java.util.regex.Pattern;

/**
 * @author Alexander Schwartz 2019
 */
public interface AsciiDocCreateMissingFile {

  default void applyFix(PsiElement element, Project project) {
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
            } else if (e != null) {
              ApplicationManager.getApplication().invokeLater(() -> {
                PsiFile file = ((PsiFile) e);
                VirtualFile vf = file.getVirtualFile();
                if (vf == null) {
                  vf = file.getOriginalFile().getVirtualFile();
                }
                if (vf != null) {
                  OpenFileAction.openFile(vf, project);
                }
              });
              break;
            }
          }
        }
      }
    }
  }

  static boolean isAvailable(PsiElement element) {
    boolean isAvailable = false;
    PsiFile containingFile = element.getContainingFile();
    if (element instanceof AsciiDocBlockMacro) {
      // this will create empty files, doesn't make sense for all images
      if (((AsciiDocBlockMacro) element).getMacroName().equals("image")) {
        // in case of image macro with a draw.io image and the plugin is installed,
        // creating an empty file makes sense
        String fileName = element.getFirstChild().getNextSibling().getText();
        boolean isDrawioFile = Pattern.matches("^.*[.](drawio|dio)[.](svg|png)$", fileName);
        boolean isDrawioPluginInstalled = PluginId.findId("de.docs_as_co.intellij.plugin.drawio", "de.docs_as_co.intellij.plugin.diagramsnet") != null;
        if (!isDrawioPluginInstalled || !isDrawioFile) {
          return false;
        }
      }
    }
    if (containingFile != null) {
      PsiDirectory parent = containingFile.getParent();
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
    }
    return isAvailable;
  }

}
