package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_SUPPORTED;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ATTRIBUTES;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;

public class AsciiDocFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private static final int MAX_DEPTH = 10;
  /**
   * Detects strings that resemble URIs.
   * <p>
   * Examples:
   * <ul>
   * <li>http://domain</li>
   * <li>https://domain
   * <li>file:///path</li>
   * <li>data:info</li>
   * </ul>
   * <p>
   * not c:/sample.adoc or c:\sample.adoc.
   * <p>
   * taken from Asciidoctor rx.rb, UriSniffRx
   */
  public static final Pattern URL = Pattern.compile("^\\p{Alpha}[\\p{Alnum}.+-]+:/{0,2}", Pattern.UNICODE_CHARACTER_CLASS);
  public static final String FILE_PREFIX = "file:///";

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
    name = AsciiDocUtil.resolveAttributes(myElement, name);
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


  public PsiElement createFileOrFolder(PsiDirectory parent) {
    if (canBeCreated(parent)) {
      String name = getRangeInElement().substring(myElement.getText());
      name = AsciiDocUtil.resolveAttributes(myElement, name);
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
      this(element, macroName, base, textRange, isFolder, isAntora, 0);
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
      this(element, macroName, base, textRange, isFolder, false, 0);
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
    multiResolveAnchor(items, key, results, ignoreCase, new ArrayDeque<>());
    return results.toArray(new ResolveResult[0]);
  }

  private void multiResolveAnchor(List<LookupElementBuilder> items, String key, List<ResolveResult> results, boolean ignoreCase, ArrayDeque<Trinity<String, String, String>> stack) {
    if (stack.size() > 10) {
      return;
    }
    if (stack.stream().anyMatch(p -> p.getThird().equals(key))) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(key);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      Optional<Trinity<String, String, String>> alreadyInStack = stack.stream().filter(p -> p.getFirst().equals(attributeName)).findAny();
      if (alreadyInStack.isPresent()) {
        // ensure that all occurrences in the replacement get the same value
        stack.push(alreadyInStack.get());
        multiResolveAnchor(items, matcher.replaceAll(Matcher.quoteReplacement(alreadyInStack.get().getSecond())), results, ignoreCase, stack);
        stack.pop();
      } else {
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
          stack.add(new Trinity<>(attributeName, value, key));
          multiResolveAnchor(items, matcher.replaceAll(Matcher.quoteReplacement(value)), results, ignoreCase, stack);
          stack.pop();
        }
      }
      if (results.size() > 0) {
        return;
      }
      // if not found, try to match it with a block ID that has the attributes unreplaced
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
        if (element != null && item.getAllLookupStrings().contains(key)) {
          results.add(new PsiElementResolveResult(element));
        } else {
          AsciiDocSection section = null;
          if (element instanceof AsciiDocSection) {
            section = (AsciiDocSection) element;
          } else if (element != null && element.getParent() instanceof AsciiDocSection) {
            // the items would only return the
            section = (AsciiDocSection) element.getParent();
          }
          if (section != null) {
            boolean possibleRefText = (base.equals("#") || base.length() == 0) && isPossibleRefText(key);
            if (section.matchesAutogeneratedId(key) && section.getBlockId() == null) {
              results.add(new PsiElementResolveResult(element));
            } else if (possibleRefText && key.equals(section.getTitle())) {
              results.add(new PsiElementResolveResult(element));
            }
          }
        }
      }
    }
    if (results.size() == 0) {
      boolean findEverywhere = false;
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
      if (antoraModuleDir == null) {
        findEverywhere = true;
      } else if (base.equals("#") || base.length() == 0) {
        VirtualFile antoraPartials = AsciiDocUtil.findAntoraPartials(myElement);
        if (antoraPartials != null) {
          String antoraPartialsCanonicalPath = antoraPartials.getCanonicalPath();
          String myCanonicalPath = myElement.getContainingFile().getVirtualFile().getCanonicalPath();
          if (myCanonicalPath != null && antoraPartialsCanonicalPath != null && myCanonicalPath.startsWith(antoraPartialsCanonicalPath)) {
            findEverywhere = true;
          }
        }
      }
      if (findEverywhere) {
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
  }

  private boolean isPossibleRefText(String key) {
    return key.contains(" ") || !key.toLowerCase(Locale.US).equals(key);
  }

  public boolean isPossibleRefText() {
    return isPossibleRefText(key);
  }

  private List<String> handleAntora(String key) {
    if (isAntora) {
      String resolvedKey = AsciiDocUtil.resolveAttributes(myElement, key);
      if (resolvedKey != null) {
        String defaultFamily = null;
        if (macroName.equals("image")) {
          defaultFamily = "image";
        } else if (macroName.equals("xref") || macroName.equals("xref-attr")) {
          defaultFamily = "page";
        }
        return AsciiDocUtil.replaceAntoraPrefix(myElement, resolvedKey, defaultFamily);
      }
    } else if (macroName.equals("image")) {
      VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(myElement);
      if (antoraImagesDir != null) {
        return Collections.singletonList(antoraImagesDir.getCanonicalPath() + "/" + key);
      }
    } else if ((myElement instanceof AsciiDocLink && macroName.equals("xref")) ||
      (myElement.getParent() instanceof AsciiDocBlockMacro && ((AsciiDocBlockMacro) myElement.getParent()).getMacroName().equals("image") && macroName.equals("xref-attr")) ||
      (myElement.getParent() instanceof AsciiDocInlineMacro && ((AsciiDocInlineMacro) myElement.getParent()).getMacroName().equals("image") && macroName.equals("xref-attr"))
    ) {
      if (AsciiDocUtil.findAntoraPagesDir(myElement) != null) {
        // if this is a link/xref, default to page family
        String resolvedKey = AsciiDocUtil.resolveAttributes(myElement, key);
        if (resolvedKey != null) {
          return AsciiDocUtil.replaceAntoraPrefix(myElement, resolvedKey, "page");
        }
      }
    }
    return Collections.singletonList(key);
  }

  private void resolve(String key, List<ResolveResult> results, int depth) {
    List<String> keys = Collections.singletonList(key);
    if (ANTORA_SUPPORTED.contains(macroName)) {
      if (depth == 0) {
        Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(key);
        if (!urlMatcher.find()) {
          if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(key).matches()) {
            VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
            if (antoraModuleDir != null) {
              String resolvedKey = AsciiDocUtil.resolveAttributes(myElement, key);
              if (resolvedKey != null) {
                List<VirtualFile> virtualFiles = AsciiDocUtil.resolvePrefix(myElement.getProject(), antoraModuleDir, resolvedKey);
                for (VirtualFile virtualFile : virtualFiles) {
                  PsiElement psiFile = PsiManager.getInstance(myElement.getProject()).findDirectory(virtualFile);
                  if (psiFile != null) {
                    results.add(new PsiElementResolveResult(psiFile));
                  }
                }
                if (virtualFiles.size() > 0) {
                  return;
                }
              }
            }
          } else {
            keys = handleAntora(key);
          }
        }
      }
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    for (String k : keys) {
      int c = results.size();
      resolveAttributes(k, results, depth);
      if (results.size() == c && "image".equals(macroName) && k.equals(key) && depth == 0) {
        resolveAttributes("{imagesdir}/" + k, results, depth);
      }
    }
    resolveAntoraPageAlias(key, results, depth);
  }

  @SuppressWarnings("StringSplitter")
  private void resolveAntoraPageAlias(String key, List<ResolveResult> results, int depth) {
    if (ANTORA_SUPPORTED.contains(macroName) && !isAnchor() && !isFolder() && depth == 0) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(myElement);
      if (antoraModuleDir != null) {
        List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(myElement.getProject(), "page-aliases", myElement);
        Map<String, String> myAttributes = AsciiDocUtil.collectAntoraAttributes(myElement);
        parseAntoraPrefix(key, myAttributes);
        for (AttributeDeclaration decl : declarations) {
          String shortKey = normalizeKeyForSearch(key);
          String value = decl.getAttributeValue();
          if (value == null) {
            continue;
          }
          if (!value.contains(shortKey)) {
            continue;
          }
          if (!(decl instanceof AsciiDocAttributeDeclarationImpl)) {
            continue;
          }
          AsciiDocAttributeDeclarationImpl declImpl = (AsciiDocAttributeDeclarationImpl) decl;
          Map<String, String> otherAttributes = AsciiDocUtil.collectAntoraAttributes(declImpl);
          for (String element : value.split("[ ,]+")) {
            Map<String, String> elementAttributes = new HashMap<>(otherAttributes);
            String shortElement = normalizeKeyForSearch(element);
            if (!shortElement.contains(shortKey)) {
              continue;
            }
            parseAntoraPrefix(element, elementAttributes);
            if (!Objects.equals(myAttributes.get("page-component-name"), elementAttributes.get("page-component-name"))) {
              continue;
            }
            if (!Objects.equals(myAttributes.get("page-component-version"), elementAttributes.get("page-component-version"))) {
              continue;
            }
            if (!Objects.equals(myAttributes.get("page-module"), elementAttributes.get("page-module"))) {
              continue;
            }
            if (!shortElement.equals(shortKey)) {
              continue;
            }
            results.add(new PsiElementResolveResult(declImpl.getContainingFile()));
          }
        }
      }
    }
  }

  @NotNull
  private String normalizeKeyForSearch(String key) {
    String shortKey = key.replaceAll(".adoc$", "");
    shortKey = shortKey.replaceAll("^.*\\$", "");
    shortKey = shortKey.replaceAll("^.*:", "");
    return shortKey;
  }

  private void parseAntoraPrefix(String element, Map<String, String> elementAttributes) {
    int start = 0;
    int i = 0;
    Matcher matcher = AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(element);
    if (matcher.find()) {
      i += matcher.end();
      String tmp = element.substring(start, i - 1);
      StringTokenizer tokenizer = new StringTokenizer(tmp, ":", false);
      if (tokenizer.countTokens() == 1) {
        String module = tokenizer.nextToken();
        if (!module.equals(".") && module.length() > 0) {
          elementAttributes.put("page-module", module);
        }
      } else {
        String component = tokenizer.nextToken();
        String module = tokenizer.nextToken();
        if (!component.equals(".") && component.length() > 0) {
          elementAttributes.put("page-component", component);
        }
        if (!module.equals(".") && module.length() > 0) {
          elementAttributes.put("page-module", module);
        }
      }
      start = i;
    }
  }

  private void resolveAttributes(String key, List<ResolveResult> results, int depth) {
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
      PsiElement file = resolve(key);
      if (file != null) {
        results.add(new PsiElementResolveResult(file));
      } else if ("link".endsWith(macroName) || "xref".endsWith(macroName) || macroName.equals("xref-attr") || "<<".equals(macroName)) {
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

  public boolean inspectAntoraXrefWithoutExtension() {
    if (!"xref".endsWith(macroName)) {
      return false;
    }
    if (!isFile()) {
      return false;
    }
    if (key.length() == 0) {
      // just the part before a hash (#)
      return false;
    }
    if (!isAntora && AsciiDocUtil.findAntoraModuleDir(myElement) == null) {
      return false;
    }
    String resolvedKey = AsciiDocUtil.resolveAttributes(myElement, key);
    if (resolvedKey != null) {
      key = resolvedKey;
    }
    // file has an extension if it contains a dot; it might contain an extension if it has an unresolved reference.
    return !key.contains(".") && !key.contains("{");
  }

  @NotNull
  private String removeFileProtocolPrefix(String value) {
    if (value.startsWith(FILE_PREFIX)) {
      if (SystemInfo.isWindows) {
        // file:///c:/... -> c:/...
        value = value.substring(FILE_PREFIX.length());
      } else {
        // file:///etc/... -> /etc/...
        value = value.substring(FILE_PREFIX.length() - 1);
      }
    }
    return value;
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
              List<VirtualFile> vfs = AsciiDocUtil.resolvePrefix(myElement.getProject(), antoraModuleDir, base);
              for (VirtualFile vf : vfs) {
                final VirtualFile projectDir = ProjectUtil.guessProjectDir(myElement.getProject());
                toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(projectDir, vf), '$');
                toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(projectDir, vf), '$');
                toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(projectDir, vf), '$');
                toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(projectDir, vf), '$');
                toAntoraLookupItem(items, "page", AsciiDocUtil.findAntoraPagesDir(projectDir, vf), '$');
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
        getVariants(base, collector, 0);
      } else {
        // this is not antora, therefore try with and without imagesdir
        getVariants(base, collector, 0);
        getVariants("{imagesdir}/" + base, collector, 0);
      }
    } else if ("link".equals(macroName) || "xref".equals(macroName) || "xref-attr".equals(macroName)) {
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
      // TODO: reduce the number of anchors if this is an Antora project if this is a nav page or a page
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

  private void getVariants(String myBase, CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector,
                           int depth) {
    List<String> bases = Collections.singletonList(myBase);
    if (depth == 0 && ANTORA_SUPPORTED.contains(macroName)) {
      bases = handleAntora(myBase);
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    for (String base : bases) {
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
  }

  @Override
  @NotNull
  public TextRange getRangeInElement() {
    return super.getRangeInElement();
  }

  private PsiElement resolve(String fileName) {
    fileName = removeFileProtocolPrefix(fileName);
    if (URL.matcher(fileName).matches()) {
      return null;
    }
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
      if (element.getContainingFile().getVirtualFile() != null &&
        element.getContainingFile().getVirtualFile().getFileSystem().getProtocol().equals("temp")) {
        VirtualFile vf = element.getContainingFile().getVirtualFile().getFileSystem().findFileByPath(fileName);
        if (vf != null) {
          PsiElement result = PsiManager.getInstance(element.getProject()).findFile(vf);
          if (result == null) {
            result = PsiManager.getInstance(element.getProject()).findDirectory(vf);
          }
          return result;
        }
      } else if (SystemInfo.isWindows) {
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

  public boolean matches(PsiElement element) {
    if (element instanceof AsciiDocBlockId) {
      AsciiDocBlockId blockId = (AsciiDocBlockId) element;
      if (isAnchor && key.equals(blockId.getName())) {
        return true;
      }
    }
    return false;
  }
}
