package org.asciidoc.intellij.psi;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AsciiDocUtil {

  static List<AsciiDocBlockId> findIds(Project project, String key) {
    List<AsciiDocBlockId> result = null;
    Collection<VirtualFile> virtualFiles =
            FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (VirtualFile virtualFile : virtualFiles) {
      if (index.isInLibrary(virtualFile)
        || index.isExcluded(virtualFile)
        || index.isInLibraryClasses(virtualFile)
        || index.isInLibrarySource(virtualFile)) {
        continue;
      }
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

  static List<AsciiDocBlockId> findIds(Project project) {
    List<AsciiDocBlockId> result = new ArrayList<>();
    Collection<VirtualFile> virtualFiles =
            FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (VirtualFile virtualFile : virtualFiles) {
      if (index.isInLibrary(virtualFile)
        || index.isExcluded(virtualFile)
        || index.isInLibraryClasses(virtualFile)
        || index.isInLibrarySource(virtualFile)) {
        continue;
      }
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocBlockId.class);
        result.addAll(properties);
      }
    }
    return result;
  }

  static List<AsciiDocAttributeDeclaration> findAttributes(Project project, String key) {
    List<AsciiDocAttributeDeclaration> result = null;
    Collection<VirtualFile> virtualFiles =
      FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (VirtualFile virtualFile : virtualFiles) {
      if (index.isInLibrary(virtualFile)
        || index.isExcluded(virtualFile)
        || index.isInLibraryClasses(virtualFile)
        || index.isInLibrarySource(virtualFile)) {
        continue;
      }
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocAttributeDeclaration> attributeDeclarations = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocAttributeDeclaration.class);
        for (AsciiDocAttributeDeclaration attributeDeclaration : attributeDeclarations) {
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

  static List<AsciiDocAttributeDeclaration> findAttributes(Project project) {
    List<AsciiDocAttributeDeclaration> result = new ArrayList<>();
    Collection<VirtualFile> virtualFiles =
      FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, GlobalSearchScope.allScope(project));
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (VirtualFile virtualFile : virtualFiles) {
      if (index.isInLibrary(virtualFile)
        || index.isExcluded(virtualFile)
        || index.isInLibraryClasses(virtualFile)
        || index.isInLibrarySource(virtualFile)) {
        continue;
      }
      AsciiDocFile asciiDocFile = (AsciiDocFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (asciiDocFile != null) {
        Collection<AsciiDocAttributeDeclaration> attributeDeclarations = PsiTreeUtil.findChildrenOfType(asciiDocFile, AsciiDocAttributeDeclaration.class);
        result.addAll(attributeDeclarations);
      }
    }
    return result;
  }


  @Nullable
  public static PsiElement getStatementAtCaret(@NotNull Editor editor, @NotNull PsiFile psiFile) {
    int caret = editor.getCaretModel().getOffset();

    final Document doc = editor.getDocument();
    CharSequence chars = doc.getCharsSequence();
    int offset = caret == 0 ? 0 : CharArrayUtil.shiftBackward(chars, caret - 1, " \t");
    if (offset < 0) {
      // happens if spaces and tabs at beginning of file
      offset = 0;
    }
    if (doc.getLineNumber(offset) < doc.getLineNumber(caret)) {
      offset = CharArrayUtil.shiftForward(chars, caret, " \t");
    }

    return psiFile.findElementAt(offset);
  }

  @NotNull
  public static AsciiDocFile createFileFromText(@NotNull Project project, @NotNull String text) {
    return (AsciiDocFile) PsiFileFactory.getInstance(project).createFileFromText("a.adoc", AsciiDocLanguage.INSTANCE, text);
  }

}
