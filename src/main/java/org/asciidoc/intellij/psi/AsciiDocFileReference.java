package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
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
import org.asciidoc.intellij.completion.AsciiDocCompletionContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiDocFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private static final int MAX_DEPTH = 10;
  private static final Pattern URL = Pattern.compile("^\\p{Alpha}[\\p{Alnum}.+-]+:/{0,2}");
  private static final Pattern ATTRIBUTES = Pattern.compile("\\{([a-zA-Z0-9_]+[a-zA-Z0-9_-]*)}");

  private String key;
  private String macroName;
  private String base;
  private final boolean isFolder;

  /**
   * Create a new file reference.
   * @param isFolder if the argument is a folder, tab will not add a '/' automatically
   */
  public AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, TextRange textRange, boolean isFolder) {
    super(element, textRange);
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
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
        resolve(matcher.replaceFirst(Matcher.quoteReplacement(decl.getAttributeValue())), results, depth + 1);
      }
      if (attributeName.equals("snippets")) {
        VirtualFile springRestDocSnippets = AsciiDocUtil.findSpringRestDocSnippets(this.getElement());
        if (springRestDocSnippets != null) {
          resolve(matcher.replaceFirst(Matcher.quoteReplacement(springRestDocSnippets.getPath())), results, depth + 1);
        }
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
    List<LookupElementBuilder> additionalItems = ContainerUtil.newArrayList();

    List<ResolveResult> results = new ArrayList<>();
    if (base.endsWith("/") || base.length() == 0) {
      resolve(base + "..", results, 0);
    } else {
      resolve(base + "/..", results, 0);
    }
    for (ResolveResult result : results) {
      if (result.getElement() == null) {
        continue;
      }
      final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
      LookupElementBuilder item = FileInfoManager.getFileLookupItem(result.getElement(), ".." /* + '/' */, icon);
      item = handleTrailingSlash(item);
      additionalItems.add(item);
    }

    List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject());
    for (AsciiDocAttributeDeclaration decl : declarations) {
      if (decl.getAttributeValue() == null || decl.getAttributeValue().trim().length() == 0) {
        continue;
      }
      List<ResolveResult> res = new ArrayList<>();
      String val = base;
      if (!val.endsWith("/") && val.length() > 0) {
        val = val + "/";
      }
      resolve(val + decl.getAttributeValue(), res, 0);
      for (ResolveResult result : res) {
        if (result.getElement() == null) {
          continue;
        }
        final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        LookupElementBuilder lb = FileInfoManager.getFileLookupItem(result.getElement(), "{" + decl.getAttributeName() + "}", icon)
          .withTailText(" (" + decl.getAttributeValue() + ")", true)
          .withTypeText(decl.getContainingFile().getName());
        if (result.getElement() instanceof PsiDirectory) {
          lb = handleTrailingSlash(lb);
        }
        additionalItems.add(lb);
      }
    }

    final Object[] variants = new Object[candidates.length + additionalItems.size()];
    for (int i = 0; i < candidates.length; i++) {
      PsiElement candidate = candidates[i];
      if (candidate instanceof PsiDirectory) {
        final Icon icon = candidate.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        String name = ((PsiDirectory) candidate).getName();
        LookupElementBuilder lb = FileInfoManager.getFileLookupItem(candidate, name, icon);
        lb = handleTrailingSlash(lb);
        variants[i] = lb;
      } else {
        variants[i] = FileInfoManager.getFileLookupItem(candidate);
      }
    }

    for (int i = 0; i < additionalItems.size(); i++) {
      variants[i + candidates.length] = additionalItems.get(i);
    }

    return variants;
  }

  private LookupElementBuilder handleTrailingSlash(LookupElementBuilder lb) {
    return lb.withInsertHandler((insertionContext, item) -> {
      int offset = insertionContext.getTailOffset();
      if (insertionContext.getOffsetMap().containsOffset(AsciiDocCompletionContributor.IDENTIFIER_FILE_REFERENCE)) {
        // AsciiDocCompletionContributor left a hint for us to do the replacement
        // (happens if a path elements is a variable)
        insertionContext.getDocument().deleteString(offset, insertionContext.getOffsetMap().getOffset(AsciiDocCompletionContributor.IDENTIFIER_FILE_REFERENCE));
      }
      // when selecting with the mouse IntelliJ will send '\n' as well
      if (insertionContext.getCompletionChar() == '\t'
        || insertionContext.getCompletionChar() == '\n') {
        if ((insertionContext.getDocument().getTextLength() <= offset
          || insertionContext.getDocument().getText().charAt(offset) != '/')
          && !isFolder) {
          // the finalizing '/' hasn't been entered yet, autocomplete it here
          insertionContext.getDocument().insertString(offset, "/");
          offset += 1;
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        } else if (insertionContext.getDocument().getTextLength() > offset &&
          insertionContext.getDocument().getText().charAt(offset) == '/') {
          insertionContext.getEditor().getCaretModel().moveToOffset(offset + 1);
        }
      }
      AutoPopupController.getInstance(insertionContext.getProject())
        .scheduleAutoPopup(insertionContext.getEditor());
    });
  }

  private void getVariants(String base, CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector,
                           int depth) {
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
        getVariants(matcher.replaceFirst(Matcher.quoteReplacement(decl.getAttributeValue())), collector, depth + 1);
      }
      if (attributeName.equals("snippets")) {
        VirtualFile springRestDocSnippets = AsciiDocUtil.findSpringRestDocSnippets(this.getElement());
        if (springRestDocSnippets != null) {
          getVariants(matcher.replaceFirst(Matcher.quoteReplacement(springRestDocSnippets.getPath())), collector, depth + 1);
        }
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
    if (!fileName.startsWith("/") && !fileName.startsWith("\\")) {
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
          return resolveAbsolutePath(element, fileName);
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
    }
    // check if file name is absolute path
    return resolveAbsolutePath(element, fileName);
  }

  private PsiElement resolveAbsolutePath(PsiElement element, String fileName) {
    // check if file name is absolute path
    VirtualFile fileByPath;
    try {
      if (SystemInfo.isWindows) {
        if (fileName.startsWith("/")) {
          fileName = fileName.replace('/', '\\');
        }
      }
      fileByPath = LocalFileSystem.getInstance().findFileByPath(fileName);
    } catch (IllegalArgumentException e) {
      // can happen with exceptions like "path must be canonical" for "/.."
      fileByPath = null;
    }
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
