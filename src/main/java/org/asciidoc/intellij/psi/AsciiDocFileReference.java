package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiDocFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  /*
  TODO:
    GOAL: insert trailing "/" if a directory has been selected
    ISSUE: when extending the range by one character, "tab" doesn't replace the the original value any more
    IDEAS:
    * be more specific in getRangeInElement()? Maybe implement MultiRangeReference?
    * use an insert handler to do the magic?
   */
  private static final int MAX_DEPTH = 10;
  private static final Pattern URL = Pattern.compile("^\\p{Alpha}[\\p{Alnum}.+-]+:/{0,2}");
  private static final Pattern ATTRIBUTES = Pattern.compile("\\{([a-zA-Z0-9_]+[a-zA-Z0-9_-]*)}");

  private String key;
  private String macroName;
  private String base;

  public AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, TextRange textRange) {
    super(element, textRange);
    this.macroName = macroName;
    this.base = base;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    resolve(base + key, results, 0);
    return results.toArray(new ResolveResult[0]);
  }

  private void resolve(String key, List<ResolveResult> results, int depth) {
    if (depth > MAX_DEPTH) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(key);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName);
      for (AsciiDocAttributeDeclaration decl : declarations) {
        if (decl.getAttributeValue() == null) {
          continue;
        }
        String val = decl.getAttributeValue();
        if (val.contains("{asciidoctorconfigdir}") &&
          (decl.getContainingFile().getName().equals(".asciidoctorconfig") || decl.getContainingFile().getName().equals(".asciidoctorconfig.adoc"))
          && decl.getContainingFile().getVirtualFile().getParent().getCanonicalPath() != null) {
          val = val.replaceAll("\\{asciidoctorconfigdir}", decl.getContainingFile().getVirtualFile().getParent().getCanonicalPath());
        }
        resolve(matcher.replaceFirst(val), results, depth + 1);
      }
    } else {
      PsiElement file = resolve(key);
      if (file != null) {
        results.add(new PsiElementResolveResult(file));
      } else if ("image".equals(macroName)) {
        if (!URL.matcher(key).matches()) {
          List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), "imagesdir");
          for (AsciiDocAttributeDeclaration decl : declarations) {
            if (decl.getAttributeValue() == null) {
              continue;
            }
            if (URL.matcher(decl.getAttributeValue()).matches()) {
              continue;
            }
            file = resolve(decl.getAttributeValue() + "/" + key);
            if (file != null) {
              results.add(new PsiElementResolveResult(file));
            }
          }
        }
      } else if ("link".endsWith(macroName)) {
        file = resolve(key + ".adoc");
        if (file != null) {
          results.add(new PsiElementResolveResult(file));
        } else if (base.endsWith(".html")) {
          file = resolve(key.replaceAll("\\.html$", ".adoc"));
          if (file != null) {
            results.add(new PsiElementResolveResult(file));
          }
        }
      }
    }
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector =
      new CommonProcessors.CollectUniquesProcessor<>();

    getVariants(base, collector, 0);
    if ("image".equals(macroName)) {
      getVariants("{imagesdir}/" + base, collector, 0);
    } else if ("link".equals(macroName)) {
      getVariants(base + ".adoc", collector, 0);
      if (base.endsWith(".html")) {
        getVariants(base.replaceAll("\\.html$", ".adoc"), collector, 0);
      }
    }

    final THashSet<PsiElement> set = new THashSet<>(collector.getResults());
    final PsiElement[] candidates = PsiUtilCore.toPsiElementArray(set);
    List<Object> additionalItems = ContainerUtil.newArrayList();

    List<ResolveResult> results = new ArrayList<>();
    resolve(base + "/..", results, 0);
    for (ResolveResult result : results) {
      if (result.getElement() == null) {
        continue;
      }
      final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
      additionalItems.add(FileInfoManager.getFileLookupItem(result.getElement(), ".." /* + '/' */, icon));
    }

    List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject());
    for (AsciiDocAttributeDeclaration decl : declarations) {
      if (decl.getAttributeValue() == null || decl.getAttributeValue().trim().length() == 0) {
        continue;
      }
      List<ResolveResult> res = new ArrayList<>();
      resolve(base + "/" + decl.getAttributeValue(), res, 0);
      for (ResolveResult result : res) {
        if (result.getElement() == null) {
          continue;
        }
        final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        additionalItems.add(
          FileInfoManager.getFileLookupItem(result.getElement(), "{" + decl.getAttributeName() + "}", icon)
            .withTailText(" (" + decl.getAttributeValue() + ")", true)
            .withTypeText(decl.getContainingFile().getName())
        );
      }
    }


    final Object[] variants = new Object[candidates.length + additionalItems.size()];
    for (int i = 0; i < candidates.length; i++) {
      PsiElement candidate = candidates[i];
      if (candidate instanceof PsiDirectory) {
        final Icon icon = candidate.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        String name = ((PsiDirectory) candidate).getName(); // + "/";
        variants[i] = FileInfoManager.getFileLookupItem(candidate, name, icon);
      }
      variants[i] = FileInfoManager.getFileLookupItem(candidate);
    }

    for (int i = 0; i < additionalItems.size(); i++) {
      variants[i + candidates.length] = additionalItems.get(i);
    }

    return variants;
  }

  private void getVariants(String base, CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector, int depth) {
    if (depth > MAX_DEPTH) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(base);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName);
      for (AsciiDocAttributeDeclaration decl : declarations) {
        if (decl.getAttributeValue() == null) {
          continue;
        }
        String val = decl.getAttributeValue();
        if (val.contains("{asciidoctorconfigdir}") &&
          (decl.getContainingFile().getName().equals(".asciidoctorconfig") || decl.getContainingFile().getName().equals(".asciidoctorconfig.adoc"))
          && decl.getContainingFile().getVirtualFile().getCanonicalPath() != null) {
          val = val.replaceAll("\\{asciidoctorconfigdir}", decl.getContainingFile().getVirtualFile().getCanonicalPath());
        }
        getVariants(matcher.replaceFirst(val), collector, depth + 1);
      }
    } else {
      PsiElement resolve = resolve(base);
      if (resolve != null) {
        for (final PsiElement child : resolve.getChildren()) {
          if (child instanceof PsiFileSystemItem) {
            collector.process((PsiFileSystemItem) child);
          }
        }
      }
    }
  }

  @Override
  public TextRange getRangeInElement() {
    return super.getRangeInElement();
  }

  private PsiElement resolve(String fileName) {
    PsiElement element = getElement();
    if (element == null) {
      return null;
    }
    PsiDirectory startDir = element.getContainingFile().getContainingDirectory();
    if (startDir == null) {
      startDir = element.getContainingFile().getOriginalFile().getContainingDirectory();
    }
    if (startDir == null) {
      return null;
    }
    if (fileName.length() == 0) {
      return startDir;
    }
    String[] split = StringUtil.trimEnd(fileName, "/").split("/");
    PsiDirectory dir = startDir;
    for (int i = 0; i < split.length - 1; ++i) {
      if (split[i].length() == 0) {
        continue;
      }
      if (split[i].equals("..")) {
        dir = dir.getParent();
        if (dir == null) {
          return null;
        }
        continue;
      }
      dir = dir.findSubdirectory(split[i]);
      if (dir == null) {
        // check if file name is absolute path
        VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(fileName);
        if (fileByPath != null) {
          PsiFile file = PsiManager.getInstance(element.getProject()).findFile(fileByPath);
          if (file != null) {
            return file;
          }
          return PsiManager.getInstance(element.getProject()).findDirectory(fileByPath);
        }
        return null;
      }
    }
    if (split[split.length - 1].equals("..")) {
      dir = dir.getParent();
      return dir;
    }
    PsiFile file = dir.findFile(split[split.length - 1]);
    if (file != null) {
      return file;
    }
    dir = dir.findSubdirectory(split[split.length - 1]);
    if (dir != null) {
      return dir;
    }
    // check if file name is absolute path
    VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(fileName);
    if (fileByPath != null) {
      file = PsiManager.getInstance(element.getProject()).findFile(fileByPath);
      if (file != null) {
        return file;
      }
      return PsiManager.getInstance(element.getProject()).findDirectory(fileByPath);
    }
    return null;
  }

}
