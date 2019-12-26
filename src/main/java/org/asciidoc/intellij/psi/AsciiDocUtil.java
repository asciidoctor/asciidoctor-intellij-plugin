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
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
    Collection<AsciiDocAttributeDeclaration> asciiDocAttributeDeclarations = AsciiDocAttributeDeclarationKeyIndex.getInstance().get(key, project, scope);
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (AsciiDocAttributeDeclaration asciiDocAttributeDeclaration : asciiDocAttributeDeclarations) {
      VirtualFile virtualFile = asciiDocAttributeDeclaration.getContainingFile().getVirtualFile();
      if (index.isInLibrary(virtualFile)
        || index.isExcluded(virtualFile)
        || index.isInLibraryClasses(virtualFile)
        || index.isInLibrarySource(virtualFile)) {
        continue;
      }
      if (result == null) {
        result = new ArrayList<>();
      }
      result.add(asciiDocAttributeDeclaration);
    }
    return result != null ? result : Collections.emptyList();
  }

  static List<AsciiDocAttributeDeclaration> findAttributes(Project project) {
    List<AsciiDocAttributeDeclaration> result = new ArrayList<>();
    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    Collection<String> keys = AsciiDocAttributeDeclarationKeyIndex.getInstance().getAllKeys(project);
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
    for (String key : keys) {
      Collection<AsciiDocAttributeDeclaration> asciiDocAttributeDeclarations = AsciiDocAttributeDeclarationKeyIndex.getInstance().get(key, project, scope);
      for (AsciiDocAttributeDeclaration asciiDocAttributeDeclaration : asciiDocAttributeDeclarations) {
        VirtualFile virtualFile = asciiDocAttributeDeclaration.getContainingFile().getVirtualFile();
        if (index.isInLibrary(virtualFile)
          || index.isExcluded(virtualFile)
          || index.isInLibraryClasses(virtualFile)
          || index.isInLibrarySource(virtualFile)) {
          continue;
        }
        result.add(asciiDocAttributeDeclaration);
      }
    }
    return result;
  }

  static List<AttributeDeclaration> findAttributes(Project project, String key, PsiElement current) {
    List<AttributeDeclaration> result = new ArrayList<>(findAttributes(project, key));

    if (key.equals("snippets")) {
      augmentList(result, AsciiDocUtil.findSpringRestDocSnippets(current), "snippets");
    }

    if (key.equals("partialsdir")) {
      augmentList(result, AsciiDocUtil.findAntoraPartials(current), "partialsdir");
    }
    if (key.equals("imagesdir")) {
      augmentList(result, AsciiDocUtil.findAntoraImagesDir(current), "imagesdir");
    }
    if (key.equals("attachmentsdir")) {
      augmentList(result, AsciiDocUtil.findAntoraAttachmentsDir(current), "attachmentsdir");
    }
    if (key.equals("examplesdir")) {
      augmentList(result, AsciiDocUtil.findAntoraExamplesDir(current), "examplesdir");
    }
    return result;
  }


  static List<AttributeDeclaration> findAttributes(Project project, PsiElement current) {
    List<AttributeDeclaration> result = new ArrayList<>(findAttributes(project));

    augmentList(result, AsciiDocUtil.findSpringRestDocSnippets(current), "snippets");

    augmentList(result, AsciiDocUtil.findAntoraPartials(current), "partialsdir");
    augmentList(result, AsciiDocUtil.findAntoraImagesDir(current), "imagesdir");
    augmentList(result, AsciiDocUtil.findAntoraAttachmentsDir(current), "attachmentsdir");
    augmentList(result, AsciiDocUtil.findAntoraExamplesDir(current), "examplesdir");

    return result;
  }

  static void augmentList(List<AttributeDeclaration> list, VirtualFile file, String attributeName) {
    if (file != null) {
      String value = file.getPath();
      value = value.replaceAll("\\\\", "/");
      list.add(new AsciiDocAttributeDeclarationDummy(attributeName, value));
    }
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

  public static VirtualFile findAntoraPartials(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile antoraPartials = dir.findChild("partials");
        if (antoraPartials != null) {
          return antoraPartials;
        }
        VirtualFile antoraPages = dir.findChild("pages");
        if (antoraPages != null) {
          VirtualFile antoraPagePartials = antoraPages.findChild("_partials");
          if (antoraPagePartials != null) {
            return antoraPagePartials;
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
    }
    return null;
  }

  public static VirtualFile findAntoraAttachmentsDir(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile assets = dir.findChild("assets");
        if (assets != null) {
          VirtualFile attachments = assets.findChild("attachments");
          if (attachments != null) {
            return attachments;
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
    }
    return null;
  }

  public static String findAntoraImagesDirRelative(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    String imagesDir = "";
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile assets = dir.findChild("assets");
        if (assets != null) {
          VirtualFile images = assets.findChild("images");
          if (images != null) {
            return imagesDir + "assets/images";
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
      imagesDir = "../" + imagesDir;
    }
    return null;
  }

  public static String findAntoraAttachmentsDirRelative(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    String attachmentsDir = "";
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile assets = dir.findChild("assets");
        if (assets != null) {
          VirtualFile attachments = assets.findChild("attachments");
          if (attachments != null) {
            return attachmentsDir + "assets/attachments";
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
      attachmentsDir = "../" + attachmentsDir;
    }
    return null;
  }

  public static VirtualFile findAntoraImagesDir(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile assets = dir.findChild("assets");
        if (assets != null) {
          VirtualFile images = assets.findChild("images");
          if (images != null) {
            return images;
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
    }
    return null;
  }

  public static VirtualFile findAntoraExamplesDir(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    while (dir != null) {
      if (dir.getParent() != null && dir.getParent().getName().equals("modules") &&
        dir.getParent().getParent().findChild("antora.yml") != null) {
        VirtualFile examples = dir.findChild("examples");
        if (examples != null) {
          return examples;
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
    }
    return null;
  }

  public static VirtualFile findSpringRestDocSnippets(VirtualFile projectBasePath, VirtualFile fileBaseDir) {
    VirtualFile dir = fileBaseDir;
    while (dir != null) {
      VirtualFile pom = dir.findChild("pom.xml");
      if (pom != null) {
        VirtualFile targetDir = dir.findChild("target");
        if (targetDir != null) {
          VirtualFile snippets = targetDir.findChild("generated-snippets");
          if (snippets != null) {
            return snippets;
          }
        }
      }
      VirtualFile buildGradle = dir.findChild("build.gradle");
      if (buildGradle != null) {
        VirtualFile buildDir = dir.findChild("build");
        if (buildDir != null) {
          VirtualFile snippets = buildDir.findChild("generated-snippets");
          if (snippets != null) {
            return snippets;
          }
        }
      }
      VirtualFile buildGradleKts = dir.findChild("build.gradle.kts");
      if (buildGradleKts != null) {
        VirtualFile buildDir = dir.findChild("build");
        if (buildDir != null) {
          VirtualFile snippets = buildDir.findChild("generated-snippets");
          if (snippets != null) {
            return snippets;
          }
        }
      }
      if (projectBasePath.equals(dir)) {
        break;
      }
      dir = dir.getParent();
    }

    return null;
  }

  public static VirtualFile findSpringRestDocSnippets(PsiElement element) {
    VirtualFile springRestDocSnippets = null;
    VirtualFile vf;
    vf = element.getContainingFile().getVirtualFile();
    if (vf == null) {
      // when running autocomplete, there is only an original file
      vf = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    if (vf != null) {
      springRestDocSnippets = findSpringRestDocSnippets(element.getProject().getBaseDir(), vf);
    }
    return springRestDocSnippets;
  }

  public static VirtualFile findAntoraPartials(PsiElement element) {
    VirtualFile antoraPartials = null;
    VirtualFile vf;
    vf = element.getContainingFile().getVirtualFile();
    if (vf == null) {
      // when running autocomplete, there is only an original file
      vf = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    if (vf != null) {
      antoraPartials = findAntoraPartials(element.getProject().getBaseDir(), vf);
    }
    return antoraPartials;
  }

  public static VirtualFile findAntoraImagesDir(PsiElement element) {
    VirtualFile antoraImagesDir = null;
    VirtualFile vf;
    vf = element.getContainingFile().getVirtualFile();
    if (vf == null) {
      // when running autocomplete, there is only an original file
      vf = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    if (vf != null) {
      antoraImagesDir = findAntoraImagesDir(element.getProject().getBaseDir(), vf);
    }
    return antoraImagesDir;
  }

  public static VirtualFile findAntoraExamplesDir(PsiElement element) {
    VirtualFile antoraExamplesDir = null;
    VirtualFile vf;
    vf = element.getContainingFile().getVirtualFile();
    if (vf == null) {
      // when running autocomplete, there is only an original file
      vf = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    if (vf != null) {
      antoraExamplesDir = findAntoraExamplesDir(element.getProject().getBaseDir(), vf);
    }
    return antoraExamplesDir;
  }

  public static VirtualFile findAntoraAttachmentsDir(PsiElement element) {
    VirtualFile antoraAttachmentsDir = null;
    VirtualFile vf;
    vf = element.getContainingFile().getVirtualFile();
    if (vf == null) {
      // when running autocomplete, there is only an original file
      vf = element.getContainingFile().getOriginalFile().getVirtualFile();
    }
    if (vf != null) {
      antoraAttachmentsDir = findAntoraAttachmentsDir(element.getProject().getBaseDir(), vf);
    }
    return antoraAttachmentsDir;
  }

}
