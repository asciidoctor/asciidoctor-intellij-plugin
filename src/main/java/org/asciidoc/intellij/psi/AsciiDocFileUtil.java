package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiFilter;
import com.intellij.util.indexing.FileBasedIndex;
import org.asciidoc.intellij.file.AsciiDocFileType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AsciiDocFileUtil {

  public static List<AsciiDocSection> findSections(Project project, String key) {
    List<AsciiDocSection> result = null;
    Collection<VirtualFile> virtualFiles =
      FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, AsciiDocFileType.INSTANCE,
        GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile asciiDocFile = (AsciiDocFile)PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        ArrayList<AsciiDocSection> properties = new ArrayList<>();
        new PsiFilter(AsciiDocSection.class).createVisitor(properties).visitFile(asciiDocFile);
        for (AsciiDocSection property : properties) {
          if (key.equals(property.getTitle())) {
            if (result == null) {
              result = new ArrayList<>();
            }
            result.add(property);
          }
        }
      }
    }
    return result != null ? result : Collections.emptyList();
  }

  public static List<AsciiDocSection> findSections(Project project) {
    List<AsciiDocSection> result = new ArrayList<>();
    Collection<VirtualFile> virtualFiles =
      FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, AsciiDocFileType.INSTANCE,
        GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile simpleFile = (AsciiDocFile)PsiManager.getInstance(project).findFile(virtualFile);
      if (simpleFile != null) {
        new PsiFilter(AsciiDocSection.class).createVisitor(result).visitFile(simpleFile);
      }
    }
    return result;
  }
}
