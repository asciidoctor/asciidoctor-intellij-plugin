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
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocBlockId.class);
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

  public static List<AsciiDocBlockId> findIds(Project project, VirtualFile virtualFile, String key) {
    List<AsciiDocBlockId> result = null;
    AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
    if (asciiDocFile != null) {
      Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocBlockId.class);
      for (AsciiDocBlockId blockId : properties) {
        if (key.equals(blockId.getId())) {
          if (result == null) {
            result = new ArrayList<>();
          }
          result.add(blockId);
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
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocBlockId.class);
        result.addAll(properties);
      }
    }
    return result;
  }

  public static List<AsciiDocAttributeDeclaration> findAttributes(Project project, String key) {
    List<AsciiDocAttributeDeclaration> result = null;
    Collection<VirtualFile> virtualFiles =
      FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocAttributeDeclaration> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocAttributeDeclaration.class);
        for (AsciiDocAttributeDeclaration attributeDeclaration : properties) {
          if (key.equals(attributeDeclaration.getAttributeName())) {
            if (result == null) {
              result = new ArrayList<>();
            }
            result.add(attributeDeclaration);
          }
        }
      }
    }
    return result != null ? result : Collections.emptyList();
  }

  public static List<AsciiDocAttributeDeclaration> findAttributes(Project project) {
    List<AsciiDocAttributeDeclaration> result = new ArrayList<>();
    Collection<VirtualFile> virtualFiles =
      FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocAttributeDeclaration> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocAttributeDeclaration.class);
        result.addAll(properties);
      }
    }
    return result;
  }
}
