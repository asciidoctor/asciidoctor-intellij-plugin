package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.file.AsciiDocFileType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AsciiDocUtil {

  public static List<AsciiDocBlockId> findIds(Project project, String key) {
    List<AsciiDocBlockId> result = null;
    Collection<VirtualFile> virtualFiles =
            FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile AsciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (AsciiDocFile != null) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(AsciiDocFile, AsciiDocBlockId.class);
        for (AsciiDocBlockId blockId : properties) {
          if (key.equals(blockId.getId())) {
            if (result == null) {
              result = new ArrayList<>();
            }
            result.add(blockId);
          }
        }
      }
    }
    return result != null ? result : Collections.emptyList();
  }

  public static List<AsciiDocBlockId> findIds(Project project) {
    List<AsciiDocBlockId> result = new ArrayList<>();
    Collection<VirtualFile> virtualFiles =
            FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile AsciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (AsciiDocFile != null) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(AsciiDocFile, AsciiDocBlockId.class);
        result.addAll(properties);
      }
    }
    return result;
  }
}
