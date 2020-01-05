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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.completion.AsciiDocCompletionContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AsciiDocFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private static final int MAX_DEPTH = 10;
  private static final Pattern URL = Pattern.compile("^\\p{Alpha}[\\p{Alnum}.+-]+:/{0,2}");
  private static final Pattern ATTRIBUTES = Pattern.compile("\\{([a-zA-Z0-9_]+[a-zA-Z0-9_-]*)}");

  private String key;
  private String macroName;
  private String base;

  public boolean isFolder() {
    return isFolder;
  }

  public boolean canBeCreated(PsiDirectory parent) {
    if (resolve() != null) {
      return false;
    }
    String name = getRangeInElement().substring(myElement.getText());
    name = resolveAttributes(name);
    if (name != null) {
      try {
        if (isFolder) {
          parent.checkCreateFile(name);
        } else {
          parent.checkCreateSubdirectory(name);
        }
      } catch (IncorrectOperationException e) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private String resolveAttributes(String val) {
    Matcher matcher = ATTRIBUTES.matcher(val);
    while (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName, myElement);
      if (declarations.size() == 1) {
        String attrVal = declarations.get(0).getAttributeValue();
        if (attrVal != null) {
          val = matcher.replaceFirst(Matcher.quoteReplacement(attrVal));
          matcher = ATTRIBUTES.matcher(val);
        }
      } else if (declarations.size() > 1) {
        return null;
      }
    }
    return val;
  }


  public PsiElement createFileOrFolder(PsiDirectory parent) {
    if (canBeCreated(parent)) {
      String name = getRangeInElement().substring(myElement.getText());
      name = resolveAttributes(name);
      if (name != null) {
        if (isFolder) {
          return parent.createSubdirectory(name);
        } else {
          return parent.createFile(name);
        }
      }
    }
    return null;
  }

  private final boolean isFolder;
  private final boolean isAntora;

  /**
   * Create a new file reference.
   *
   * @param isFolder if the argument is a folder, tab will not add a '/' automatically
   */
  public AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, TextRange textRange,
                               boolean isFolder, boolean isAntora) {
    super(element, textRange);
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
    this.isAntora = isAntora;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  public AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, TextRange textRange,
                               boolean isFolder) {
    super(element, textRange);
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
    this.isAntora = false;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    if (base.endsWith("#")) {
      List<ResolveResult> fileResult = new ArrayList<>();
      if (base.length() > 1) {
        resolve(base.substring(0, base.length() - 1), fileResult, 0);
      } else {
        fileResult.add(new PsiElementResolveResult(myElement.getContainingFile()));
      }
      for (ResolveResult resolveResult : fileResult) {
        PsiElement element = resolveResult.getElement();
        if (element instanceof AsciiDocFile) {
          Collection<AsciiDocBlockId> blockIds = PsiTreeUtil.findChildrenOfType(element, AsciiDocBlockId.class);
          for (AsciiDocBlockId blockId : blockIds) {
            if (key.equals(blockId.getId())) {
              results.add(new PsiElementResolveResult(blockId));
            }
          }
          Collection<AsciiDocSection> sections = PsiTreeUtil.findChildrenOfType(element, AsciiDocSection.class);
          for (AsciiDocSection section : sections) {
            if (PsiTreeUtil.findChildOfType(section, AsciiDocBlockId.class) != null) {
              // element has an ID specified, therefore skip checking the autogenerated ID
              continue;
            }
            if (section.matchesAutogeneratedId(key)) {
              results.add(new PsiElementResolveResult(section));
            }
          }
        }
      }
      if (results.size() == 0) {
        final List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(myElement.getProject(), key);
        final List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(myElement.getProject(), key);
        for (AsciiDocBlockId id : ids) {
          results.add(new PsiElementResolveResult(id));
        }
        for (AsciiDocSection section : sections) {
          results.add(new PsiElementResolveResult(section));
        }
      }
      return results.toArray(new ResolveResult[0]);
    }
    resolve(base + key, results, 0);
    return results.toArray(new ResolveResult[0]);
  }

  private String handleAntora(String key) {
    if (isAntora) {
      key = AsciiDocUtil.replaceAntoraPrefix(myElement, key);
    } else if (myElement instanceof AsciiDocLink &&
      // as long as nothing else has been specified
      !key.contains(":") && !key.contains("$") && !key.contains("@")) {
      if (AsciiDocUtil.findAntoraPagesDir(myElement) != null) {
        // if this is a link/xref, default to page family
        key = "page$" + key;
        key = AsciiDocUtil.replaceAntoraPrefix(myElement, key);
      }
    }
    return key;
  }

  private void resolve(String key, List<ResolveResult> results, int depth) {
    if (depth == 0) {
      key = handleAntora(key);
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(key);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName, myElement);
      for (AttributeDeclaration decl : declarations) {
        if (decl.getAttributeValue() == null) {
          continue;
        }
        resolve(matcher.replaceFirst(Matcher.quoteReplacement(decl.getAttributeValue())), results, depth + 1);
      }
    } else {
      // if this is an image, and we are inside an Antora module, just look in the Antora path
      VirtualFile antoraImagesDir = null;
      if ("image".equals(macroName)) {
        antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(myElement);
        if (antoraImagesDir != null) {
          key = antoraImagesDir.getCanonicalPath() + "/" + key;
        }
      }
      PsiElement file = resolve(key);
      if (file != null) {
        results.add(new PsiElementResolveResult(file));
      } else if ("image".equals(macroName) && antoraImagesDir == null) {
        // if it is an image, iterate over all available imagesdir declarations
        if (!URL.matcher(key).matches()) {
          List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), "imagesdir", myElement);
          for (AttributeDeclaration decl : declarations) {
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
      } else if ("link".endsWith(macroName) || "xref".endsWith(macroName)) {
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
    if (isAntora && base.length() == 0) {
      List<LookupElementBuilder> items = new ArrayList<>();
      toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(myElement));
      toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(myElement));
      toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(myElement));
      toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(myElement));
      return items.toArray();
    }

    if (base.endsWith("#")) {
      return getVariantsForAnchor();
    }

    final CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector =
      new CommonProcessors.CollectUniquesProcessor<>();

    if ("image".equals(macroName)) {
      VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(myElement);
      if (antoraImagesDir != null) {
        getVariants(antoraImagesDir.getCanonicalPath() + "/" + base, collector, 0);
      } else {
        // this is not antora, therefore try with and without imagesdir
        getVariants(base, collector, 0);
        getVariants("{imagesdir}/" + base, collector, 0);
      }
    } else if ("link".equals(macroName) || "xref".equals(macroName)) {
      getVariants(base, collector, 0);
      getVariants(base + ".adoc", collector, 0);
      if (base.endsWith(".html")) {
        getVariants(base.replaceAll("\\.html$", ".adoc"), collector, 0);
      }
    } else {
      getVariants(base, collector, 0);
    }

    Set<PsiElement> set = new HashSet<>(collector.getResults());
    if ("image".equals(macroName)) {
      // image macro should not suggest or resolve asciidoc files
      set = set.stream().filter(psiElement -> psiElement.getLanguage() != AsciiDocLanguage.INSTANCE).collect(Collectors.toSet());
    }
    final PsiElement[] candidates = PsiUtilCore.toPsiElementArray(set);
    List<LookupElementBuilder> additionalItems = new ArrayList<>();

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

    List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), myElement);
    Set<String> searched = new HashSet<>(declarations.size());
    for (AttributeDeclaration decl : declarations) {
      if (decl.getAttributeValue() == null || decl.getAttributeValue().trim().length() == 0) {
        continue;
      }
      if ("imagesdir".equals(decl.getAttributeName())) {
        // unlikely, won't have that as an attribute in an image path
        continue;
      }
      List<ResolveResult> res = new ArrayList<>();
      String val = base;
      if (!val.endsWith("/") && val.length() > 0) {
        val = val + "/";
      }
      if ("image".equals(macroName)) {
        VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(myElement);
        if (antoraImagesDir != null) {
          val = antoraImagesDir.getCanonicalPath() + "/" + val;
        }
      }
      // an attribute might be declared with the same value in multiple files, try only once for each combination
      String key = decl.getAttributeName() + ":" + decl.getAttributeValue();
      if (searched.contains(key)) {
        continue;
      }
      searched.add(key);
      resolve(val + decl.getAttributeValue(), res, 0);
      for (ResolveResult result : res) {
        if (result.getElement() == null) {
          continue;
        }
        final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        LookupElementBuilder lb;
        lb = FileInfoManager.getFileLookupItem(result.getElement(), "{" + decl.getAttributeName() + "}", icon)
          .withTailText(" (" + decl.getAttributeValue() + ")", true);
        if (decl instanceof AsciiDocAttributeDeclaration) {
          lb = lb.withTypeText(((AsciiDocAttributeDeclaration) decl).getContainingFile().getName());
        }
        if (result.getElement() instanceof PsiDirectory) {
          lb = handleTrailingSlash(lb);
        } else if (result.getElement() instanceof PsiFile) {
          lb = handleTrailingHash(lb);
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
        Object item = FileInfoManager.getFileLookupItem(candidate);
        if (candidate instanceof PsiFile && item instanceof LookupElementBuilder) {
          item = handleTrailingHash((LookupElementBuilder) item);
        }
        variants[i] = item;
      }
    }

    for (int i = 0; i < additionalItems.size(); i++) {
      variants[i + candidates.length] = additionalItems.get(i);
    }

    return variants;
  }

  @NotNull
  private Object[] getVariantsForAnchor() {
    List<ResolveResult> fileResult = new ArrayList<>();
    if (base.length() > 1) {
      resolve(base.substring(0, base.length() - 1), fileResult, 0);
    } else {
      fileResult.add(new PsiElementResolveResult(myElement.getContainingFile()));
    }
    List<LookupElementBuilder> items = new ArrayList<>();
    boolean foundExisting = false;
    for (ResolveResult resolveResult : fileResult) {
      PsiElement element = resolveResult.getElement();
      if (element instanceof AsciiDocFile) {
        Collection<AsciiDocBlockId> properties = PsiTreeUtil.findChildrenOfType(element, AsciiDocBlockId.class);
        for (AsciiDocBlockId blockId : properties) {
          final Icon icon = blockId.getParent().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
          items.add(FileInfoManager.getFileLookupItem(blockId, blockId.getName(), icon));
          if (key.equals(blockId.getId())) {
            foundExisting = true;
          }
        }
        Collection<AsciiDocSection> sections = PsiTreeUtil.findChildrenOfType(element, AsciiDocSection.class);
        for (AsciiDocSection section : sections) {
          // element has an ID specified, therefore skip checking the autogenerated ID
          if (PsiTreeUtil.findChildOfType(section, AsciiDocBlockId.class) != null) {
            continue;
          }
          final Icon icon = section.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
          items.add(FileInfoManager.getFileLookupItem(section, section.getAutogeneratedId(), icon));
          if (section.matchesAutogeneratedId(key)) {
            foundExisting = true;
          }
        }
      }
    }
    if (!foundExisting) {
      items.add(LookupElementBuilder.create(key));
    }
    return items.toArray();
  }

  private void toAntoraLookupItem(List<LookupElementBuilder> items, String placeholder, VirtualFile antoraDir) {
    if (antoraDir != null) {
      PsiDirectory dir = PsiManager.getInstance(myElement.getProject()).findDirectory(antoraDir);
      if (dir != null) {
        final Icon icon = dir.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        LookupElementBuilder lb;
        lb = FileInfoManager.getFileLookupItem(dir, placeholder, icon);
        if (dir.getParent() != null) {
          lb = lb.withTypeText(dir.getParent().getName() + "/" + dir.getName());
        } else {
          lb = lb.withTypeText(dir.getName());
        }
        lb = handleTrailingDollar(lb);
        items.add(lb);
      }
    }
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

  private LookupElementBuilder handleTrailingHash(LookupElementBuilder lb) {
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
          || insertionContext.getDocument().getText().charAt(offset) != '#')
          && !isFolder) {
          // the finalizing '/' hasn't been entered yet, autocomplete it here
          insertionContext.getDocument().insertString(offset, "#");
          offset += 1;
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        } else if (insertionContext.getDocument().getTextLength() > offset &&
          insertionContext.getDocument().getText().charAt(offset) == '#') {
          insertionContext.getEditor().getCaretModel().moveToOffset(offset + 1);
        }
      }
      AutoPopupController.getInstance(insertionContext.getProject())
        .scheduleAutoPopup(insertionContext.getEditor());
    });
  }

  private LookupElementBuilder handleTrailingDollar(LookupElementBuilder lb) {
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
          || insertionContext.getDocument().getText().charAt(offset) != '$')
          && !isFolder) {
          // the finalizing '$' hasn't been entered yet, autocomplete it here
          insertionContext.getDocument().insertString(offset, "$");
          offset += 1;
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        } else if (insertionContext.getDocument().getTextLength() > offset &&
          insertionContext.getDocument().getText().charAt(offset) == '$') {
          insertionContext.getEditor().getCaretModel().moveToOffset(offset + 1);
        }
      }
      AutoPopupController.getInstance(insertionContext.getProject())
        .scheduleAutoPopup(insertionContext.getEditor());
    });
  }

  private void getVariants(String base, CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector,
                           int depth) {
    if (depth == 0) {
      base = handleAntora(base);
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(base);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName, myElement);
      for (AttributeDeclaration decl : declarations) {
        if (decl.getAttributeValue() == null) {
          continue;
        }
        getVariants(matcher.replaceFirst(Matcher.quoteReplacement(decl.getAttributeValue())), collector, depth + 1);
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
  @NotNull
  public TextRange getRangeInElement() {
    return super.getRangeInElement();
  }

  private PsiElement resolve(String fileName) {
    PsiElement element = getElement();
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
      String[] split = StringUtil.trimEnd(fileName, "/").split("/", -1);
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
