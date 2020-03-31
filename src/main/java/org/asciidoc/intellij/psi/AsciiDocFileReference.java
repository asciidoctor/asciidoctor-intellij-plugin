package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionUtilCore;
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
import com.intellij.util.IncorrectOperationException;
import icons.AsciiDocIcons;
import org.apache.commons.lang.ArrayUtils;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.completion.AsciiDocCompletionContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_SUPPORTED;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;

public class AsciiDocFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private static final int MAX_DEPTH = 10;
  private static final Pattern URL = Pattern.compile("^\\p{Alpha}[\\p{Alnum}.+-]+:/{0,2}");
  private static final Pattern ATTRIBUTES = Pattern.compile("\\{([a-zA-Z0-9_]+[a-zA-Z0-9_-]*)}");

  private String key;
  private String macroName;
  private String base;
  private boolean isAnchor;

  public boolean isFolder() {
    return isFolder;
  }

  public boolean isAnchor() {
    return isAnchor;
  }

  public boolean isAntora() {
    return isAntora;
  }

  public boolean isFile() {
    return !isAnchor && !isFolder;
  }

  public AsciiDocFileReference withAnchor(boolean isAnchor) {
    this.isAnchor = isAnchor;
    return this;
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
        // check if the name would be a valid path name
        String path = parent.getVirtualFile().getCanonicalPath();
        if (path != null) {
          Paths.get(path, name);
        }
      } catch (IncorrectOperationException | InvalidPathException e) {
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
                               boolean isFolder, boolean isAntora, int suffix) {
    super(element, textRange);
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
    this.isAntora = isAntora;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset() + suffix);
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
    if (isAnchor) {
      return multiResolveAnchor(false);
    }
    resolve(base + key, results, 0);
    return results.toArray(new ResolveResult[0]);
  }

  public ResolveResult[] multiResolveAnchor(boolean ignoreCase) {
    List<ResolveResult> results = new ArrayList<>();
    List<ResolveResult> fileResult = new ArrayList<>();
    if (base.equals("#") || base.length() == 0) {
      fileResult.add(new PsiElementResolveResult(myElement.getContainingFile()));
    } else {
      resolve(base.substring(0, base.length() - 1), fileResult, 0);
    }
    List<LookupElementBuilder> items = new ArrayList<>();
    for (ResolveResult resolveResult : fileResult) {
      PsiElement element = resolveResult.getElement();
      if (element instanceof AsciiDocFile) {
        AsciiDocUtil.findBlockIds(items, element, 0);
      }
    }
    for (LookupElementBuilder item : items) {
      PsiElement element = item.getPsiElement();
      if (ignoreCase) {
        String lowerCaseKey = key.toLowerCase(Locale.US);
        if (element instanceof AsciiDocSection) {
          if (((AsciiDocSection) element).matchesAutogeneratedId(lowerCaseKey) && ((AsciiDocSection) element).getBlockId() == null) {
            results.add(new PsiElementResolveResult(element));
          }
        } else if (element != null) {
          for (String lookupString : item.getAllLookupStrings()) {
            if (lookupString.toLowerCase(Locale.US).equals(lowerCaseKey)) {
              results.add(new PsiElementResolveResult(element));
              break;
            }
          }
        }
      } else {
        if (element instanceof AsciiDocSection) {
          boolean possibleRefText = (base.equals("#") || base.length() == 0) && isPossibleRefText(key);
          if (((AsciiDocSection) element).matchesAutogeneratedId(key) && ((AsciiDocSection) element).getBlockId() == null) {
            results.add(new PsiElementResolveResult(element));
          } else if (possibleRefText && key.equals(((AsciiDocSection) element).getTitle())) {
            results.add(new PsiElementResolveResult(element));
          }
        } else if (element != null && item.getAllLookupStrings().contains(key)) {
          results.add(new PsiElementResolveResult(element));
        }
      }
    }
    if (results.size() == 0) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
      if (antoraModuleDir == null) {
        // try to find section/anchor globally only if this is not Antora
        final List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(myElement.getProject(), key);
        for (AsciiDocBlockId id : ids) {
          results.add(new PsiElementResolveResult(id));
        }
        final List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(myElement.getProject(), key);
        boolean possibleRefText = (base.equals("#") || base.length() == 0) && isPossibleRefText(key);
        for (AsciiDocSection section : sections) {
          // anchors only match with autogenerated IDs, not with the section title
          if (section.matchesAutogeneratedId(key) || possibleRefText) {
            results.add(new PsiElementResolveResult(section));
          }
        }
      }
    }
    return results.toArray(new ResolveResult[0]);
  }

  private boolean isPossibleRefText(String key) {
    return key.contains(" ") || !key.toLowerCase(Locale.US).equals(key);
  }

  public boolean isPossibleRefText() {
    return isPossibleRefText(key);
  }

  private String handleAntora(String key) {
    if (isAntora) {
      key = AsciiDocUtil.replaceAntoraPrefix(myElement, key, null);
    } else if (myElement instanceof AsciiDocLink && macroName.equals("xref") &&
      // as long as no version has been specified
      !key.contains("@")) {
      if (AsciiDocUtil.findAntoraPagesDir(myElement) != null) {
        // if this is a link/xref, default to page family
        key = AsciiDocUtil.replaceAntoraPrefix(myElement, key, "page");
      }
    }
    return key;
  }

  private void resolve(String key, List<ResolveResult> results, int depth) {
    if (ANTORA_SUPPORTED.contains(macroName)) {
      if (depth == 0) {
        Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(key);
        if (!urlMatcher.find()) {
          if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(key).matches()) {
            VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
            if (antoraModuleDir != null) {
              VirtualFile virtualFile = AsciiDocUtil.resolvePrefix(myElement.getProject(), antoraModuleDir, key);
              if (virtualFile != null) {
                PsiElement psiFile = PsiManager.getInstance(myElement.getProject()).findDirectory(virtualFile);
                if (psiFile != null) {
                  results.add(new PsiElementResolveResult(psiFile));
                  return;
                }
              }
            }
          } else {
            key = handleAntora(key);
          }
        }
      }
    }
    if (depth > MAX_DEPTH) {
      return;
    }

    Matcher matcher = ATTRIBUTES.matcher(key);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), attributeName, myElement);
      Set<String> searched = new HashSet<>(declarations.size());
      for (AttributeDeclaration decl : declarations) {
        String value = decl.getAttributeValue();
        if (value == null) {
          continue;
        }
        if (searched.contains(value)) {
          continue;
        }
        searched.add(value);
        resolve(matcher.replaceFirst(Matcher.quoteReplacement(value)), results, depth + 1);
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
      } else if ("link".endsWith(macroName) || "xref".endsWith(macroName) || "<<".equals(macroName)) {
        file = resolve(key + ".adoc");
        if (file != null) {
          results.add(new PsiElementResolveResult(file));
        } else if (key.endsWith(".html")) {
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

  @SuppressWarnings("checkstyle:MethodLength")
  @NotNull
  @Override
  public Object[] getVariants() {
    List<LookupElementBuilder> items = new ArrayList<>();
    if ((isAntora || base.length() == 0)) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
      if (antoraModuleDir != null) {
        if (base.length() == 0) {
          if (!"image".equals(macroName)) {
            toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(myElement), '$');
            toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(myElement), '$');
            toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(myElement), '$');
            toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(myElement), '$');
            toAntoraLookupItem(items, "page", AsciiDocUtil.findAntoraPagesDir(myElement), '$');
          }
          List<AntoraModule> antoraModules = AsciiDocUtil.collectPrefixes(myElement.getProject(), antoraModuleDir);
          for (AntoraModule antoraModule : antoraModules) {
            toAntoraLookupItem(items, antoraModule);
          }
        } else if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(base).matches()) {
          Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(key);
          if (!urlMatcher.find()) {
            if (!"image".equals(macroName)) {
              VirtualFile vf = AsciiDocUtil.resolvePrefix(myElement.getProject(), antoraModuleDir, base);
              if (vf != null) {
                toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(myElement.getProject().getBaseDir(), vf), '$');
                toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(myElement.getProject().getBaseDir(), vf), '$');
                toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(myElement.getProject().getBaseDir(), vf), '$');
                toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(myElement.getProject().getBaseDir(), vf), '$');
                toAntoraLookupItem(items, "page", AsciiDocUtil.findAntoraPagesDir(myElement.getProject().getBaseDir(), vf), '$');
              }
              return items.toArray();
            }
          }
        }
      }
    }

    if (base.endsWith("#") || (macroName.equals("<<") && base.length() == 0)) {
      return getVariantsForAnchor();
    }

    final CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector =
      new CommonProcessors.CollectUniquesProcessor<>();

    if ("image".equals(macroName)) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
      if (antoraModuleDir != null) {
        getVariants(AsciiDocUtil.replaceAntoraPrefix(myElement, base, "image"), collector, 0);
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
      item = handleTrailing(item, '/');
      items.add(item);
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
          lb = handleTrailing(lb, '/');
        } else if (result.getElement() instanceof PsiFile) {
          lb = handleTrailing(lb, '/');
        }
        items.add(lb);
      }
    }

    Object[] variants = new Object[candidates.length + items.size()];
    for (int i = 0; i < candidates.length; i++) {
      PsiElement candidate = candidates[i];
      if (candidate instanceof PsiDirectory) {
        final Icon icon = candidate.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        String name = ((PsiDirectory) candidate).getName();
        LookupElementBuilder lb = FileInfoManager.getFileLookupItem(candidate, name, icon);
        lb = handleTrailing(lb, '/');
        variants[i] = lb;
      } else {
        Object item = FileInfoManager.getFileLookupItem(candidate);
        variants[i] = item;
      }
    }

    for (int i = 0; i < items.size(); i++) {
      variants[i + candidates.length] = items.get(i);
    }

    if (base.length() == 0) {
      variants = ArrayUtils.addAll(variants, getVariantsForAnchor());
    }

    return variants;
  }

  @NotNull
  private Object[] getVariantsForAnchor() {
    List<LookupElementBuilder> items = new ArrayList<>();
    if (base.length() > 1) {
      // if a file has been specified, show anchors from that file
      List<ResolveResult> fileResult = new ArrayList<>();
      resolve(base.substring(0, base.length() - 1), fileResult, 0);
      for (ResolveResult resolveResult : fileResult) {
        PsiElement element = resolveResult.getElement();
        if (element instanceof AsciiDocFile) {
          AsciiDocUtil.findBlockIds(items, element, 0);
        }
      }
    } else {
      // if no file has been specified, show anchors from project
      List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(myElement.getProject());
      for (final AsciiDocBlockId id : ids) {
        String name = id.getName();
        if (name != null && name.length() > 0) {
          items.add(LookupElementBuilder.create(id)
            .withCaseSensitivity(true)
            .withIcon(AsciiDocIcons.ASCIIDOC_ICON)
            .withTypeText(id.getContainingFile().getName())
          );
        }
      }
      // plus sections from current file with ID or full section name
      List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(myElement.getProject());
      PsiFile myFile = getElement().getContainingFile().getOriginalFile();
      for (AsciiDocSection section : sections) {
        // advocate to only use automatic references in current file
        if (!section.getContainingFile().equals(myFile)) {
          continue;
        }
        // if they have a block ID, we've seen them above
        if (section.getBlockId() != null) {
          continue;
        }
        items.add(LookupElementBuilder.create(section, section.getAutogeneratedId())
          .withCaseSensitivity(true)
          .withIcon(AsciiDocIcons.Structure.SECTION)
          .withTypeText(section.getContainingFile().getName())
        );
        if (section.getTitle().matches("^[\\w/.:{#].*")
          && isPossibleRefText(section.getTitle())) {
          items.add(LookupElementBuilder.create(section, section.getTitle())
            .withCaseSensitivity(true)
            .withIcon(AsciiDocIcons.Structure.SECTION)
            .withTypeText(section.getContainingFile().getName())
          );
        }
      }

    }
    boolean foundExisting = false;
    String keyToFind = key.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "").trim();
    items:
    for (LookupElementBuilder item : items) {
      for (String lookup : item.getAllLookupStrings()) {
        if (lookup.startsWith(keyToFind)) {
          foundExisting = true;
          break items;
        }
      }
    }
    if (keyToFind.length() == 0) {
      foundExisting = true;
    }
    if (!foundExisting) {
      items.add(LookupElementBuilder.create(key));
    }
    return items.toArray();
  }

  private void toAntoraLookupItem(List<LookupElementBuilder> items, String placeholder, VirtualFile antoraDir, char trail) {
    if (antoraDir != null) {
      PsiDirectory dir = PsiManager.getInstance(myElement.getProject()).findDirectory(antoraDir);
      if (dir != null) {
        final Icon icon = dir.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        LookupElementBuilder lb;
        lb = FileInfoManager.getFileLookupItem(dir, placeholder, icon);
        lb = lb.withPresentableText(placeholder + trail);
        if (dir.getParent() != null) {
          lb = lb.withTypeText(dir.getParent().getName() + "/" + dir.getName());
        } else {
          lb = lb.withTypeText(dir.getName());
        }
        lb = handleTrailing(lb, trail);
        items.add(lb);
      }
    }
  }

  private void toAntoraLookupItem(List<LookupElementBuilder> items, AntoraModule module) {
    String placeholder = module.getPrefix().substring(0, module.getPrefix().length() - 1);
    PsiDirectory dir = PsiManager.getInstance(myElement.getProject()).findDirectory(module.getFile());
    if (dir != null) {
      final Icon icon = dir.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
      LookupElementBuilder lb;
      lb = FileInfoManager.getFileLookupItem(dir, placeholder, icon);
      lb = lb.withPresentableText(module.getPrefix());
      StringBuilder typeText = new StringBuilder();
      if (module.getTitle() != null) {
        lb = lb.withTailText(" " + module.getTitle(), true);
      }
      typeText.append(module.getComponent()).append(":").append(module.getModule());
      lb = lb.withTypeText(typeText.toString());
      lb = handleTrailing(lb, ':');
      items.add(lb);
    }
  }

  private LookupElementBuilder handleTrailing(LookupElementBuilder lb, char trail) {
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
          || insertionContext.getDocument().getText().charAt(offset) != trail)
          && !isFolder) {
          // the finalizing '/' hasn't been entered yet, autocomplete it here
          insertionContext.getDocument().insertString(offset, String.valueOf(trail));
          offset += 1;
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        } else if (insertionContext.getDocument().getTextLength() > offset &&
          insertionContext.getDocument().getText().charAt(offset) == trail) {
          insertionContext.getEditor().getCaretModel().moveToOffset(offset + 1);
        }
      }
      AutoPopupController.getInstance(insertionContext.getProject())
        .scheduleAutoPopup(insertionContext.getEditor());
    });
  }

  private void getVariants(String base, CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector,
                           int depth) {
    if (depth == 0 && ANTORA_SUPPORTED.contains(macroName)) {
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
    PsiFile myFile = element.getContainingFile();
    if (myFile == null) {
      return null;
    }
    PsiDirectory startDir = myFile.getContainingDirectory();
    if (startDir == null) {
      startDir = myFile.getOriginalFile().getContainingDirectory();
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
        if (split[i].equals(".")) {
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
      if (split[split.length - 1].equals(".")) {
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
