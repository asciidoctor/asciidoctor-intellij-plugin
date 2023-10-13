package org.asciidoc.intellij.psi;

import com.google.errorprone.annotations.CheckReturnValue;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import icons.AsciiDocIcons;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.PercentCodec;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.completion.AsciiDocCompletionContributor;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_AND_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_SUPPORTED;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_YML;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ATTRIBUTES;
import static org.asciidoc.intellij.psi.AsciiDocUtil.COMPONENT_MODULE;
import static org.asciidoc.intellij.psi.AsciiDocUtil.FAMILY;
import static org.asciidoc.intellij.psi.AsciiDocUtil.MODULE;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN_WITHOUT_FILE;
import static org.asciidoc.intellij.psi.AsciiDocUtil.VERSION;
import static org.asciidoc.intellij.psi.AsciiDocUtil.isAntoraPartial;

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



  private final String key;
  private final String macroName;
  private final String base;
  private boolean isAnchor;
  private final PsiElement root;

  public boolean isFolder() {
    return isFolder;
  }

  public boolean isAnchor() {
    if (isAnchor && ATTRIBUTES.matcher(key).find()) {
      List<ResolveResult> resolveResults = List.of(multiResolve(true));
      for (ResolveResult resolveResult : resolveResults) {
        if (resolveResult.getElement() instanceof PsiFile) {
          return false;
        } else if (resolveResult.getElement() instanceof AsciiDocSection ||
          resolveResult.getElement() instanceof AsciiDocBlockId) {
          return true;
        }
      }
    }
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

  public boolean canBeCreated() {
    if (resolve() != null) {
      return false;
    }
    List<ResolveResult> resolved = new ArrayList<>();
    resolve(base, resolved, 0, new HashSet<>());
    if (resolved.size() == 1) {
      PsiElement baseElement = resolved.get(0).getElement();
      if (baseElement instanceof PsiDirectory) {
        PsiDirectory parent = (PsiDirectory) baseElement;
        String name = AsciiDocUtil.resolveAttributes(root, key);
        if (name != null) {
          if (!VALID_FILENAME.matcher(name).matches() || name.contains("/") || name.contains("\\")) {
            return false;
          }
          try {
            if (isFolder) {
              parent.checkCreateSubdirectory(name);
            } else {
              parent.checkCreateFile(name);
            }
            // check if the name would be a valid path name
            String path = parent.getVirtualFile().getCanonicalPath();
            if (path != null) {
              Paths.get(path, name);
            }
          } catch (IncorrectOperationException | InvalidPathException e) {
            return false;
          }
          return true;
        }
      }
    }
    return false;
  }

  public @Nullable PsiElement createFileOrFolder() {
    if (canBeCreated()) {
      List<ResolveResult> resolved = new ArrayList<>();
      resolve(base, resolved, 0, new HashSet<>());
      if (resolved.size() == 1) {
        PsiElement baseElement = resolved.get(0).getElement();
        if (baseElement instanceof PsiDirectory) {
          PsiDirectory parent = (PsiDirectory) baseElement;
          String name = AsciiDocUtil.resolveAttributes(root, key);
          if (name != null) {
            if (isFolder) {
              return parent.createSubdirectory(name);
            } else {
              return parent.createFile(name);
            }
          }
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
    this.root = element;
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
    this.isAntora = isAntora;
    this.key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset() + suffix);
  }

  private AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, String key,
                                boolean isFolder, boolean isAntora) {
    super(element);
    this.root = element;
    this.macroName = macroName;
    this.base = base;
    this.isFolder = isFolder;
    this.isAntora = isAntora;
    this.key = key;
  }

  /**
   * Teleport the reference to a new file, thereby cloning the reference.
   * Use this, for example, if a reference is included in another file, and should be resolved in the other
   * file's context.
   */
  public AsciiDocFileReference(AsciiDocFileReference original, PsiElement root) {
    super(original.getElement(), original.getRangeInElement());
    this.root = root;
    this.macroName = original.macroName;
    this.base = original.base;
    this.isFolder = original.isFolder;
    this.isAntora = original.isAntora;
    this.key = original.key;
  }

  public AsciiDocFileReference includedInParent(PsiElement parent) {
    return new AsciiDocFileReference(parent, macroName, base, key, isFolder, isAntora);
  }

  public AsciiDocFileReference(@NotNull PsiElement element, @NotNull String macroName, String base, TextRange textRange,
                               boolean isFolder) {
    this(element, macroName, base, textRange, isFolder, false, 0);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    if (isAnchor) {
      results.addAll(multiResolveAnchor(false));
    }
    // an anchor might resolve to a file instead if it has attributes
    if (!isAnchor || ATTRIBUTES.matcher(key).find()) {
      if ("link-attr".equals(macroName) && "self".equals(base + key)) {
        VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
        if (antoraModuleDir != null) {
          PsiElement parent = root.getParent();
          while (parent != null && !(parent instanceof HasFileReference)) {
            parent = parent.getParent();
          }
          if (parent != null) {
            AsciiDocFileReference fileReference = ((HasFileReference) parent).getFileReference();
            if (fileReference != null) {
              return fileReference.multiResolve(incompleteCode);
            }
          }
        }
      }
      resolve(base + key, results, 0, new HashSet<>());
    }
    return results.toArray(new ResolveResult[0]);
  }

  public List<ResolveResult> multiResolveAnchor(boolean ignoreCase) {
    List<ResolveResult> results = new ArrayList<>();
    List<ResolveResult> fileResult = new ArrayList<>();
    if (base.equals("#") || base.isEmpty()) {
      fileResult.add(new PsiElementResolveResult(root.getContainingFile()));
    } else {
      resolve(base.substring(0, base.length() - 1), fileResult, 0, new HashSet<>());
    }
    Set<LookupElementBuilder> items = new HashSet<>();
    for (ResolveResult resolveResult : fileResult) {
      PsiElement element = resolveResult.getElement();
      if (element instanceof AsciiDocFile) {
        AsciiDocUtil.findBlockIds(items, (AsciiDocFile) element);
      }
    }
    multiResolveAnchor(items, key, results, ignoreCase, new ArrayDeque<>());
    return results;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement otherElement) {
    VirtualFile otherAntoraModuleDir = AsciiDocUtil.findAntoraModuleDir(otherElement);
    VirtualFile myAntoraModuleDir = AsciiDocUtil.findAntoraModuleDir(this.getElement());
    if (otherAntoraModuleDir != null && myAntoraModuleDir != null) {
      ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(this.getElement());
      if (manipulator != null) {
        VirtualFile otherAntoraPagesDir = AsciiDocUtil.findAntoraPagesDir(otherElement);
        VirtualFile otherVf;
        if (otherElement instanceof PsiDirectory) {
          otherVf = ((PsiDirectory) otherElement).getVirtualFile();
        } else {
          otherVf = otherElement.getContainingFile().getVirtualFile();
        }
        if (otherVf.getCanonicalPath() != null && otherAntoraPagesDir != null &&
          otherAntoraPagesDir.getCanonicalPath() != null &&
          otherVf.getCanonicalPath().startsWith(otherAntoraPagesDir.getCanonicalPath())) {
          if (this.macroName.equals("antora-nav")) {
            String relativePath = FileUtil.getRelativePath(otherVf.getParent().getPath(),
              otherVf.getPath(), '/');
            if (relativePath != null) {
              manipulator.handleContentChange(this.getElement(), getRangeInElement().shiftLeft(base.length()).grown(base.length()), relativePath);
            }
          } else {
            Map<String, Object> otherAntoraComponent;
            try {
              otherAntoraComponent = AsciiDocWrapper.readAntoraYaml(otherElement.getProject(), otherAntoraModuleDir.getParent().getParent().findChild("antora.yml"));
            } catch (YAMLException | NullPointerException ex) {
              return otherElement;
            }

            Map<String, Object> myAntoraComponent;
            try {
              if (this.macroName.equals("antora-startpage")) {
                myAntoraComponent = AsciiDocWrapper.readAntoraYaml(otherElement.getProject(), this.myElement.getContainingFile().getVirtualFile());
              } else {
                myAntoraComponent = AsciiDocWrapper.readAntoraYaml(otherElement.getProject(), AsciiDocUtil.findAntoraModuleDir(this.myElement).getParent().getParent().findChild("antora.yml"));
              }
            } catch (YAMLException | NullPointerException ex) {
              return otherElement;
            }

            StringBuilder sb = new StringBuilder();

            boolean addModule = false;

            if (!Objects.equals(AsciiDocUtil.getAttributeAsString(myAntoraComponent, "name"), AsciiDocUtil.getAttributeAsString(otherAntoraComponent, "name"))) {
              sb.append(AsciiDocUtil.getAttributeAsString(otherAntoraComponent, "name"));
              sb.append(":");
              addModule = true;
            }
            if (this.macroName.equals("antora-startpage")) {
              addModule = true;
            }
            if (!addModule && !otherAntoraModuleDir.getName().equals(AsciiDocUtil.findAntoraModuleDir(this.myElement).getName())) {
              addModule = true;
            }
            if (addModule) {
              sb.append(otherAntoraModuleDir.getName());
              sb.append(":");
            }
            String relativePath;
            if (macroName.equals("include") && Objects.equals(myAntoraModuleDir, otherAntoraModuleDir)) {
              // includes within the same module will always be relative to the file
              relativePath = FileUtil.getRelativePath(myElement.getContainingFile().getVirtualFile().getParent().getPath(), otherVf.getPath(), '/');
            } else {
              relativePath = FileUtil.getRelativePath(otherAntoraPagesDir.getPath(), otherVf.getPath(), '/');
            }
            if (relativePath != null) {
              sb.append(relativePath);
              manipulator.handleContentChange(this.getElement(), getRangeInElement().shiftLeft(base.length()).grown(base.length()), sb.toString());
            }
          }
        }
      }
    }
    return otherElement;
  }

  private void multiResolveAnchor(Set<LookupElementBuilder> items, String key, List<ResolveResult> results, boolean ignoreCase, ArrayDeque<Trinity<String, String, String>> stack) {
    if (stack.size() > 10) {
      return;
    }
    if (stack.stream().anyMatch(p -> p.getThird().equals(key))) {
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(key);
    int start = 0;
    while (matcher.find(start)) {
      String attributeName = matcher.group(1);
      Optional<Trinity<String, String, String>> alreadyInStack = stack.stream().filter(p -> p.getFirst().equals(attributeName)).findAny();
      if (alreadyInStack.isPresent()) {
        // ensure that all occurrences in the replacement get the same value
        stack.push(alreadyInStack.get());
        String newName = new StringBuilder(key).replace(matcher.start(), matcher.end(), alreadyInStack.get().getSecond()).toString();
        multiResolveAnchor(items, newName, results, ignoreCase, stack);
        stack.pop();
      } else {
        List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(root.getProject(), attributeName, root);
        Set<String> searched = new HashSet<>(declarations.size());
        for (AttributeDeclaration decl : declarations) {
          String value = decl.getAttributeValue();
          if (value == null) {
            continue;
          }
          if (searched.contains(value)) {
            continue;
          }
          // avoid replacements where new value contains the old attribute as placeholder
          if (value.contains("{" + decl.getAttributeName() + "}")) {
            continue;
          }
          searched.add(value);
          stack.add(new Trinity<>(attributeName, value, key));
          String newName = new StringBuilder(key).replace(matcher.start(), matcher.end(), value).toString();
          multiResolveAnchor(items, newName, results, ignoreCase, stack);
          stack.pop();
        }
      }
      if (!results.isEmpty()) {
        return;
      }
      start = matcher.end();
      // if not found, try to match it with a block ID that has the attributes unreplaced
    }
    PsiElement incomplete = null;
    for (LookupElementBuilder item : items) {
      if (item.getAllLookupStrings().contains("???") && incomplete == null) {
        incomplete = item.getPsiElement();
      }
      checkIfItemMatches(key, results, ignoreCase, item);
    }
    if (results.isEmpty()) {
      boolean findEverywhere = false;
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
      if (antoraModuleDir == null) {
        findEverywhere = true;
      } else if (base.endsWith("#") || base.isEmpty()) {
        VirtualFile antoraPartials = AsciiDocUtil.findAntoraPartials(root);
        if (antoraPartials != null) {
          String antoraCanonicalPath = antoraPartials.getCanonicalPath();
          findEverywhere = checkIfElementIsInPath(antoraCanonicalPath);
        }
        if (!findEverywhere) {
          // example can be used as includes, although this is a bad practice.
          VirtualFile antoraExample = AsciiDocUtil.findAntoraExamplesDir(root);
          if (antoraExample != null) {
            String antoraCanonicalPath = antoraExample.getCanonicalPath();
            findEverywhere = checkIfElementIsInPath(antoraCanonicalPath);
          }
        }
      }
      if (findEverywhere) {
        // try to find section/anchor globally only if this is not Antora
        final List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(root.getProject(), key);
        for (AsciiDocBlockId id : ids) {
          results.add(new PsiElementResolveResult(id));
        }
        final List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(root.getProject(), key);
        boolean possibleRefText = (base.equals("#") || base.isEmpty()) && isPossibleRefText(key);
        for (AsciiDocSection section : sections) {
          // anchors only match with autogenerated IDs, not with the section title
          if (section.matchesAutogeneratedId(key) || possibleRefText) {
            results.add(new PsiElementResolveResult(section));
          }
        }
      }
    }
    if (results.isEmpty() && incomplete != null) {
      // there might have been an incompletely resolved include, that might point to the right starting point
      results.add(new PsiElementResolveResult(incomplete, false));
    }
  }

  private void checkIfItemMatches(String key, List<ResolveResult> results, boolean ignoreCase, LookupElementBuilder item) {
    PsiElement element = item.getPsiElement();
    if (element == null) {
      return;
    }
    if (ignoreCase) {
      String lowerCaseKey = key.toLowerCase(Locale.US);
      if (element instanceof AsciiDocSection) {
        if (((AsciiDocSection) element).matchesAutogeneratedId(lowerCaseKey) && ((AsciiDocSection) element).getBlockId() == null) {
          results.add(new PsiElementResolveResult(element));
        }
      } else {
        for (String lookupString : item.getAllLookupStrings()) {
          if (lookupString.toLowerCase(Locale.US).equals(lowerCaseKey)) {
            results.add(new PsiElementResolveResult(element));
            break;
          }
        }
      }
    } else {
      if (item.getAllLookupStrings().contains(key)) {
        results.add(new PsiElementResolveResult(element));
        return;
      }
      for (String lookupString : item.getAllLookupStrings()) {
        try {
          if (!key.matches(ATTRIBUTES.matcher(lookupString).replaceAll(".*"))) {
            continue;
          }
        } catch (PatternSyntaxException ex) {
          // ignore
        }
        boolean found = checkItemInKey(key, lookupString, element, new ArrayDeque<>());
        if (found) {
          results.add(new PsiElementResolveResult(element));
          return;
        }
      }
      AsciiDocSection section = null;
      if (element instanceof AsciiDocSection) {
        section = (AsciiDocSection) element;
      } else if (element.getParent() instanceof AsciiDocSection) {
        // the items would only return the heading
        section = (AsciiDocSection) element.getParent();
      }
      if (section != null) {
        boolean possibleRefText = (base.equals("#") || base.isEmpty()) && isPossibleRefText(key);
        if (section.matchesAutogeneratedId(key) && section.getBlockId() == null) {
          results.add(new PsiElementResolveResult(element));
        } else if (possibleRefText && section.matchesTitle(key)) {
          results.add(new PsiElementResolveResult(element));
        }
      }
    }
  }

  private boolean checkItemInKey(String key, String lookupString, PsiElement element, ArrayDeque<Trinity<String, String, String>> stack) {
    if (stack.size() > 10) {
      return false;
    }
    Matcher matcher = ATTRIBUTES.matcher(lookupString);
    int start = 0;
    if (matcher.find(start)) {
      String attributeName = matcher.group(1);
      Optional<Trinity<String, String, String>> alreadyInStack = stack.stream().filter(p -> p.getFirst().equals(attributeName)).findAny();
      if (alreadyInStack.isPresent()) {
        // ensure that all occurrences in the replacement get the same value
        stack.push(alreadyInStack.get());
        String newName = new StringBuilder(lookupString).replace(matcher.start(), matcher.end(), alreadyInStack.get().getSecond()).toString();
        boolean found = checkItemInKey(key, newName, element, stack);
        if (found) {
          return true;
        }
        stack.pop();
      } else {
        List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(element.getProject(), attributeName, element);
        Set<String> searched = new HashSet<>(declarations.size());
        for (AttributeDeclaration decl : declarations) {
          String value = decl.getAttributeValue();
          if (value == null) {
            continue;
          }
          if (searched.contains(value)) {
            continue;
          }
          // avoid replacements where new value contains the old attribute as placeholder
          if (value.contains("{" + decl.getAttributeName() + "}")) {
            continue;
          }
          searched.add(value);
          stack.add(new Trinity<>(attributeName, value, lookupString));
          String newName = new StringBuilder(lookupString).replace(matcher.start(), matcher.end(), value).toString();
          boolean found = checkItemInKey(key, newName, element, stack);
          stack.pop();
          if (found) {
            return true;
          }
        }
      }
    } else {
      return key.equals(lookupString);
    }
    return false;
  }

  private boolean checkIfElementIsInPath(String antoraCanonicalPath) {
    String myCanonicalPath = root.getContainingFile().getOriginalFile().getVirtualFile().getCanonicalPath();
    return myCanonicalPath != null && antoraCanonicalPath != null && myCanonicalPath.startsWith(antoraCanonicalPath);
  }

  private boolean isPossibleRefText(String key) {
    return key.contains(" ") || !key.toLowerCase(Locale.US).equals(key);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isPossibleRefText() {
    return isPossibleRefText(key);
  }

  private List<String> handleAntora(String key) {
    if (macroName.equals("antora-startpage")) {
      return AsciiDocUtil.replaceAntoraPrefixForStartPage(root, key);
    } else if (isAntora || key.startsWith("./")) {
      String resolvedKey = AsciiDocUtil.resolveAttributes(root, key);
      if (resolvedKey != null) {
        String defaultFamily = null;
        if (macroName.equals("image") || macroName.equals("video") || macroName.equals("audio")) {
          defaultFamily = "image";
        } else if (macroName.equals("xref") || macroName.equals("xref-attr") || macroName.equals("include")) {
          defaultFamily = "page";
        }
        return AsciiDocUtil.replaceAntoraPrefix(root, resolvedKey, defaultFamily);
      }
    } else if (macroName.equals("image") || macroName.equals("video") || macroName.equals("audio")) {
      VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(root);
      if (antoraImagesDir != null) {
        return Collections.singletonList(antoraImagesDir.getCanonicalPath() + "/" + key);
      }
    } else if ((getElement() instanceof AsciiDocLink && macroName.equals("xref")) || macroName.equals("include") ||
      (getElement().getParent() instanceof AsciiDocBlockMacro && ((AsciiDocBlockMacro) getElement().getParent()).getMacroName().equals("image") && macroName.equals("xref-attr")) ||
      (getElement().getParent() instanceof AsciiDocInlineMacro && ((AsciiDocInlineMacro) getElement().getParent()).getMacroName().equals("image") && macroName.equals("xref-attr"))
    ) {
      String resolvedKey = AsciiDocUtil.resolveAttributes(root, key);
      if (resolvedKey != null) {
        return AsciiDocUtil.replaceAntoraPrefix(root, resolvedKey, "page");
      }
    }
    return Collections.singletonList(key);
  }

  private static final Pattern VALID_FILENAME = Pattern.compile("^([\\p{Alnum}_\\-{}./\\\\() ,$:@]|(%[a-zA-Z0-9]))*$", Pattern.UNICODE_CHARACTER_CLASS);

  private void resolve(String key, List<ResolveResult> results, int depth, Collection<String> searchedKeys) {
    if (searchedKeys.contains(key) || !VALID_FILENAME.matcher(key).matches() || URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(key).find()) {
      // skip all non-valid filenames, also URLs
      return;
    }
    searchedKeys.add(key);
    List<String> keys = Collections.singletonList(key);
    if (ANTORA_SUPPORTED.contains(macroName) && !ATTRIBUTES.matcher(key).find()) {
      Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(key);
      if (!urlMatcher.find() && !AsciiDocUtil.isAntoraPartial(root)) {
        if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(key).matches() || AsciiDocUtil.ANTORA_FAMILY_PATTERN.matcher(key).matches()) {
          VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
          if (antoraModuleDir != null) {
            List<VirtualFile> virtualFiles = AsciiDocUtil.resolvePrefix(root, antoraModuleDir, key);
            for (VirtualFile virtualFile : virtualFiles) {
              PsiElement psiFile = PsiManager.getInstance(root.getProject()).findDirectory(virtualFile);
              if (psiFile != null) {
                results.add(new PsiElementResolveResult(psiFile));
              }
            }
            if (!virtualFiles.isEmpty()) {
              return;
            }
          }
        } else if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(key).find() || AsciiDocUtil.ANTORA_FAMILY_PATTERN.matcher(key).find() || key.startsWith("./")) {
          keys = handleAntora(key);
        }
      }
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    for (String k : keys) {
      if (!VALID_FILENAME.matcher(k).matches() || URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(k).find()) {
        // skip all non-valid filenames, also URLs
        return;
      }
      int c = results.size();
      resolveAttributes(k, results, depth, searchedKeys);
      if (results.size() == c && ("image".equals(macroName) || "video".equals(macroName) || "audio".equals(macroName)) && k.equals(key) && depth == 0) {
        resolveAttributes("{imagesdir}/" + k, results, depth, searchedKeys);
        if (results.size() == c && k.startsWith("/")) {
          VirtualFile hugoStaticFile = AsciiDocUtil.findHugoStaticFolder(myElement);
          if (hugoStaticFile != null) {
            resolveAttributes(hugoStaticFile.getCanonicalPath() + k, results, depth, searchedKeys);
          }
        }
      }
    }
    resolveAntoraPageAlias(key, results, depth);
  }

  @SuppressWarnings("StringSplitter")
  private void resolveAntoraPageAlias(String key, List<ResolveResult> results, int depth) {
    if (ANTORA_SUPPORTED.contains(macroName) && !isFolder() && depth == 0 && results.isEmpty() && !macroName.equals("include")) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
      if (antoraModuleDir != null) {
        List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(root.getProject(), "page-aliases", root);
        Collection<AttributeDeclaration> myAttributes = AsciiDocUtil.collectAntoraAttributes(root);
        myAttributes = parseAntoraPrefix(key, myAttributes);
        String pageComponentName = AsciiDocUtil.findAttribute("page-component-name", myAttributes);
        String pageComponentVersion = AsciiDocUtil.findAttribute("page-component-version", myAttributes);
        String pageModule = AsciiDocUtil.findAttribute("page-module", myAttributes);
        String shortKey = normalizeKeyForSearch(key);
        for (AttributeDeclaration decl : declarations) {
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
          Collection<AttributeDeclaration> otherAttributes = AsciiDocUtil.collectAntoraAttributes(declImpl);
          for (String element : value.split(",")) {
            Collection<AttributeDeclaration> elementAttributes = new ArrayList<>(otherAttributes);
            String shortElement = normalizeKeyForSearch(element.trim());
            if (!shortElement.contains(shortKey)) {
              continue;
            }
            elementAttributes = parseAntoraPrefix(element.trim(), elementAttributes);
            if (!Objects.equals(pageComponentName, AsciiDocUtil.findAttribute("page-component-name", elementAttributes))) {
              continue;
            }
            if (!Objects.equals(pageComponentVersion, AsciiDocUtil.findAttribute("page-component-version", elementAttributes))) {
              continue;
            }
            if (!Objects.equals(pageModule, AsciiDocUtil.findAttribute("page-module", elementAttributes))) {
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

  private static final Pattern STRIP_FILE_EXTENSION = Pattern.compile(".adoc$");
  private static final Pattern STRIP_FAMILY = Pattern.compile("^.*\\$");
  private static final Pattern STRIP_MODULE = Pattern.compile("^.*:");

  @NotNull
  public static String normalizeKeyForSearch(String key) {
    key = STRIP_FILE_EXTENSION.matcher(key).replaceAll("");
    key = STRIP_FAMILY.matcher(key).replaceAll("");
    key = STRIP_MODULE.matcher(key).replaceAll("");
    return key;
  }

  @CheckReturnValue
  public static Collection<AttributeDeclaration> parseAntoraPrefix(String element, Collection<AttributeDeclaration> elementAttributes) {
    // avoid modifying the collection
    elementAttributes = new ArrayList<>(elementAttributes);
    Matcher matcher = AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(element);
    if (matcher.find()) {
      Matcher version = AsciiDocUtil.VERSION.matcher(element);
      if (version.find()) {
        String v = version.group("version");
        if (v.equals("_")) {
          // starting from Antora 3.0.0.alpha-3 a version can be empty. It will be treated internally as an empty string.
          // in xrefs, this is represented by a single underscope ("_")
          v = "";
        }
        elementAttributes.removeIf(ad -> ad.getAttributeName().equals("page-component-version"));
        elementAttributes.add(new AsciiDocAttributeDeclarationDummy("page-component-version", v));
        element = version.replaceFirst("");
      }
      Matcher componentModuleMatcher = AsciiDocUtil.COMPONENT_MODULE.matcher(element);
      if (componentModuleMatcher.find()) {
        String component = componentModuleMatcher.group("component");
        if (!component.isEmpty() && !component.equals(".")) {
          elementAttributes.removeIf(ad -> ad.getAttributeName().equals("page-component"));
          elementAttributes.add(new AsciiDocAttributeDeclarationDummy("page-component", component));
        }
        String module = componentModuleMatcher.group("module");
        if (!module.isEmpty() && !module.equals(".")) {
          elementAttributes.removeIf(ad -> ad.getAttributeName().equals("page-module"));
          elementAttributes.add(new AsciiDocAttributeDeclarationDummy("page-module", module));
        }
      } else {
        Matcher moduleMatcher = AsciiDocUtil.MODULE.matcher(element);
        if (moduleMatcher.find()) {
          String module = moduleMatcher.group("module");
          if (!module.isEmpty() && !module.equals(".")) {
            elementAttributes.removeIf(ad -> ad.getAttributeName().equals("page-module"));
            elementAttributes.add(new AsciiDocAttributeDeclarationDummy("page-module", module));
          }
        }
      }
    }
    return elementAttributes;
  }

  private void resolveAttributes(String key, List<ResolveResult> results, int depth, Collection<String> searchedKeys) {
    if (!VALID_FILENAME.matcher(key).matches() || URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(key).find()) {
      // skip all non-valid filenames, also URLs
      return;
    }
    Matcher matcher = ATTRIBUTES.matcher(key);
    if (matcher.find()) {
      String attributeName = matcher.group(1);
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(root.getProject(), attributeName, root);
      Set<String> searched = new HashSet<>(declarations.size());
      for (AttributeDeclaration decl : declarations) {
        String value = decl.getAttributeValue();
        if (value == null) {
          continue;
        }
        // avoid replacements where new value contains the old attribute as placeholder
        if (value.contains("{" + decl.getAttributeName() + "}")) {
          continue;
        }
        if (searched.contains(value)) {
          continue;
        }
        searched.add(value);
        if (URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(value).find()) {
          continue;
        }
        String newKey = new StringBuilder(key).replace(matcher.start(), matcher.end(), value).toString();
        resolve(newKey, results, depth + 1, searchedKeys);
      }
    } else {
      if (URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(key).find()) {
        PsiElementResolveResult result = new PsiElementResolveResult(new BrowsableUrl(key));
        if (!results.contains(result)) {
          results.add(result);
        }
        return;
      }
      List<PsiElement> file = resolve(key);
      if (file != null && !file.isEmpty()) {
        results.addAll(file.stream().map(PsiElementResolveResult::new).collect(Collectors.toList()));
      } else if (("link".endsWith(macroName) || "xref".endsWith(macroName) || macroName.equals("xref-attr") || "<<".equals(macroName)) && !key.endsWith(".")) {
        String extension = AsciiDocFileType.getExtensionOrDefault(root);
        file = resolve(key + "." + extension);
        if (file != null && !file.isEmpty()) {
          results.addAll(file.stream().map(PsiElementResolveResult::new).collect(Collectors.toList()));
        } else if (key.endsWith(".html")) {
          file = resolve(key.replaceAll("\\.html$", "." + extension));
          if (file != null && !file.isEmpty()) {
            results.addAll(file.stream().map(PsiElementResolveResult::new).collect(Collectors.toList()));
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
    if (key.isEmpty()) {
      // just the part before a hash (#)
      return false;
    }
    if (!isAntora && AsciiDocUtil.findAntoraModuleDir(root) == null) {
      return false;
    }
    String resolvedKey = AsciiDocUtil.resolveAttributes(root, key);
    if (resolvedKey == null) {
      resolvedKey = key;
    }
    // file has an extension if it contains a dot; it might contain an extension if it has an unresolved reference.
    return !resolvedKey.contains(".") && !resolvedKey.contains("{");
  }

  public boolean inspectAntoraXrefWithoutNaturalReference() {
    if (!"xref".endsWith(macroName)) {
      return false;
    }
    if (isFile()) {
      return false;
    }
    if (key.isEmpty()) {
      // just the part before a hash (#)
      return false;
    }
    String resolvedKey = AsciiDocUtil.resolveAttributes(root, key);
    if (resolvedKey == null) {
      resolvedKey = key;
    }
    // unsure if it has an unresolved reference
    if (resolvedKey.contains("{")) {
      return false;
    }
    // a dot indicates a file name
    if (resolvedKey.contains(".")) {
      return false;
    }
    // natural reference: needs a space an capitalization
    //noinspection RedundantIfStatement
    if (!resolvedKey.contains(" ") || resolvedKey.toLowerCase(Locale.ROOT).equals(key)) {
      return false;
    }
    return true;
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
  @Override
  public Object @NotNull [] getVariants() {
    List<LookupElementBuilder> items = new ArrayList<>();
    if ((isAntora || base.isEmpty())) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
      if (antoraModuleDir != null) {
        if (base.isEmpty()) {
          if (!"image".equals(macroName)) {
            toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(root), '$');
            toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(root), '$');
            toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(root), '$');
            toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(root), '$');
            toAntoraLookupItem(items, "page", AsciiDocUtil.findAntoraPagesDir(root), '$');
          }
          List<AntoraModule> antoraModules = AsciiDocUtil.collectAntoraPrefixes(root.getProject(), antoraModuleDir, false);
          for (AntoraModule antoraModule : antoraModules) {
            toAntoraLookupItem(items, antoraModule);
          }
        } else if (AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(base).matches()) {
          Matcher urlMatcher = URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(key);
          if (!urlMatcher.find()) {
            if (!"image".equals(macroName)) {
              List<VirtualFile> vfs = AsciiDocUtil.resolvePrefix(root, antoraModuleDir, base);
              for (VirtualFile vf : vfs) {
                toAntoraLookupItem(items, "example", AsciiDocUtil.findAntoraExamplesDir(root.getProject(), vf), '$');
                toAntoraLookupItem(items, "partial", AsciiDocUtil.findAntoraPartials(root.getProject(), vf), '$');
                toAntoraLookupItem(items, "attachment", AsciiDocUtil.findAntoraAttachmentsDir(root.getProject(), vf), '$');
                toAntoraLookupItem(items, "image", AsciiDocUtil.findAntoraImagesDir(root.getProject(), vf), '$');
                toAntoraLookupItem(items, "page", AsciiDocUtil.findAntoraPagesDir(root.getProject(), vf), '$');
              }
              if (!("include".equals(macroName) && AsciiDocUtil.isAntoraPage(myElement))) {
                return items.toArray();
              }
            }
          }
        }
      } else if (macroName.equals("antora-startpage") && base.isEmpty()) {
        List<AntoraModule> antoraModules = AsciiDocUtil.collectAntoraPrefixes(root.getProject(), root.getContainingFile().getOriginalFile().getVirtualFile(), true);
        for (AntoraModule antoraModule : antoraModules) {
          toAntoraLookupItem(items, antoraModule);
        }
      }
    }

    if (base.endsWith("#") || (macroName.equals("<<") && base.isEmpty())) {
      return getVariantsForAnchor();
    }

    final CommonProcessors.CollectUniquesProcessor<PsiFileSystemItem> collector =
      new CommonProcessors.CollectUniquesProcessor<>();

    if ("image".equals(macroName) || "audio".equals(macroName) || "video".equals(macroName)) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
      if (antoraModuleDir != null) {
        getVariants(base, collector, 0);
      } else {
        // this is not antora, therefore try with and without imagesdir
        getVariants(base, collector, 0);
        getVariants("{imagesdir}/" + base, collector, 0);
        if (base.startsWith("/")) {
          VirtualFile hugoStaticFile = AsciiDocUtil.findHugoStaticFolder(myElement);
          if (hugoStaticFile != null) {
            getVariants(hugoStaticFile.getCanonicalPath() + base, collector, 0);
          }
        }
      }
    } else if ("link".equals(macroName) || "xref".equals(macroName) || "xref-attr".equals(macroName)) {
      getVariants(base, collector, 0);
      String extension = AsciiDocFileType.getExtensionOrDefault(root);
      getVariants(base + "." + extension, collector, 0);
      if (base.endsWith(".html")) {
        getVariants(base.replaceAll("\\.html$", "." + extension), collector, 0);
      }
    } else if (!(macroName.equals("antora-startpage") && base.isEmpty())) {
      getVariants(base, collector, 0);
    }

    Set<PsiElement> set = new HashSet<>(collector.getResults());
    if ("image".equals(macroName)) {
      // image macro should not suggest or resolve asciidoc files
      set = set.stream().filter(psiElement -> psiElement.getLanguage() != AsciiDocLanguage.INSTANCE).collect(Collectors.toSet());
    }
    final PsiElement[] candidates = PsiUtilCore.toPsiElementArray(set);

    if (!isAntora || (!base.isEmpty() && !ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(base).matches() && !ANTORA_PREFIX_PATTERN.matcher(base).matches() && !ANTORA_FAMILY_PATTERN.matcher(base).matches())) {
      LookupElementBuilder item = LookupElementBuilder.create("..")
        .withTypeText(" (parent folder of current file)", true)
        .withIcon(PlatformIcons.FOLDER_ICON);
      item = handleTrailing(item, '/');
      items.add(item);
    }

    if (ANTORA_SUPPORTED.contains(macroName) && AsciiDocUtil.findAntoraModuleDir(root) != null) {
      // Antora 3 support relative resource names starting with a dot
      LookupElementBuilder item = LookupElementBuilder.create(".")
        .withTypeText(" (relative to current file)", true)
        .withIcon(PlatformIcons.FOLDER_ICON);
      item = handleTrailing(item, '/');
      items.add(item);
    }

    if (!macroName.equals("antora-nav") && !macroName.equals("antora-startpage")) {
      List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(root.getProject(), root);
      Set<String> searched = new HashSet<>(declarations.size());
      for (AttributeDeclaration decl : declarations) {
        String attributeValue = decl.getAttributeValue();
        if (attributeValue == null || attributeValue.trim().isEmpty() || ATTRIBUTES.matcher(attributeValue).matches()) {
          continue;
        }
        String attributeName = decl.getAttributeName();
        if ("imagesdir".equals(attributeName) || "page-aliases".equals(attributeName) || "keywords".equals(attributeName) || "description".equals(attributeName) || attributeName.startsWith("page-") || attributeName.startsWith("url-")) {
          // unlikely, won't have that as an attribute in an image path
          continue;
        }
        String val = base;
        if (!val.endsWith("/") && !val.isEmpty()) {
          val = val + "/";
        }
        if ("image".equals(macroName)) {
          VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(root);
          if (antoraImagesDir != null) {
            val = antoraImagesDir.getCanonicalPath() + "/" + val;
          }
        }
        // an attribute might be declared with the same value in multiple files, try only once for each combination
        String key = attributeName + ":" + attributeValue;
        if (searched.contains(key)) {
          continue;
        }
        searched.add(key);
        List<ResolveResult> res = new ArrayList<>();
        resolve(val + attributeValue, res, 0, new HashSet<>());
        for (ResolveResult result : res) {
          if (result.getElement() == null) {
            continue;
          }
          final Icon icon = result.getElement().getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
          LookupElementBuilder lb;
          lb = FileInfoManager.getFileLookupItem(result.getElement(), "{" + attributeName + "}", icon)
            .withTailText(" (" + attributeValue + ")", true);
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
    }

    if ("link-attr".equals(macroName)) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(root);
      if (antoraModuleDir != null) {
        LookupElementBuilder lb = LookupElementBuilder.create("self")
          .withTailText(" (self link of this element)", true)
          .withIcon(PlatformIcons.FILE_ICON);
        items.add(lb);
      }
    }

    Object[] variants = new Object[candidates.length + items.size()];
    for (int i = 0; i < candidates.length; i++) {
      PsiElement candidate = candidates[i];
      if (!candidate.isValid()) {
        continue;
      }
      if (candidate instanceof PsiDirectory) {
        final Icon icon = candidate.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY);
        String name = ((PsiDirectory) candidate).getName();
        LookupElementBuilder lb = FileInfoManager.getFileLookupItem(candidate, name, icon);
        lb = handleTrailing(lb, '/');
        variants[i] = lb;
      } else {
        // handle special volume information on MacOS X
        // see for example: https://youtrack.jetbrains.com/issue/R-1028
        Object item;
        if (SystemInfo.isMac && candidate instanceof PsiFile && ((PsiFile) candidate).getName().equals(".VolumeIcon.icns")) {
          item = candidate;
        } else {
          item = FileInfoManager.getFileLookupItem(candidate);
        }
        variants[i] = item;
      }
    }

    for (int i = 0; i < items.size(); i++) {
      variants[i + candidates.length] = items.get(i);
    }

    if (base.isEmpty() && (macroName.equals("xref") || macroName.equals("<<") || macroName.equals("xref-attr"))) {
      variants = ArrayUtils.addAll(variants, getVariantsForAnchor());
    }

    return variants;
  }

  @NotNull
  private Object[] getVariantsForAnchor() {
    Set<LookupElementBuilder> items = new HashSet<>();
    if (base.length() > 1) {
      // if a file has been specified, show anchors from that file
      List<ResolveResult> fileResult = new ArrayList<>();
      resolve(base.substring(0, base.length() - 1), fileResult, 0, new HashSet<>());
      for (ResolveResult resolveResult : fileResult) {
        PsiElement element = resolveResult.getElement();
        if (element instanceof AsciiDocFile) {
          AsciiDocUtil.findBlockIds(items, (AsciiDocFile) element);
        }
      }
    } else {
      if (AsciiDocUtil.isAntoraPage(root)) {
        // if it is an Antora page, all IDs must be in this file or in its includes
        AsciiDocUtil.findBlockIds(items, (AsciiDocFile) root.getContainingFile());
      } else {
        // if no file has been specified, show anchors from the current project
        List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(root.getProject());
        for (final AsciiDocBlockId id : ids) {
          String name = id.getName();
          if (name != null && !name.isEmpty()) {
            items.add(LookupElementBuilder.create(id)
              .withCaseSensitivity(true)
              .withIcon(AsciiDocIcons.ASCIIDOC_ICON)
              .withTypeText(id.getContainingFile().getName())
            );
          }
        }
        // plus sections from current file with ID or full section name
        // advocate to only use automatic references in the current file
        List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(root.getContainingFile().getOriginalFile());
        for (AsciiDocSection section : sections) {
          // if they have a block ID, we've seen them above
          if (section.getBlockId() != null) {
            continue;
          }
          items.add(LookupElementBuilder.create(section, section.getAutogeneratedId())
            .withCaseSensitivity(true)
            .withIcon(AsciiDocIcons.Structure.SECTION)
            .withTypeText(section.getContainingFile().getName())
          );
          if (section.getTitle().matches("(?U)^[\\w/.:{#].*")
            && isPossibleRefText(section.getTitle())) {
            items.add(LookupElementBuilder.create(section, section.getTitle())
              .withCaseSensitivity(true)
              .withIcon(AsciiDocIcons.Structure.SECTION)
              .withTypeText(section.getContainingFile().getName())
            );
          }
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
    if (keyToFind.isEmpty()) {
      foundExisting = true;
    }
    if (!foundExisting) {
      items.add(LookupElementBuilder.create(key));
    }
    return items.toArray();
  }

  private void toAntoraLookupItem(List<LookupElementBuilder> items, String placeholder, VirtualFile antoraDir, char trail) {
    if (antoraDir != null) {
      PsiDirectory dir = PsiManager.getInstance(root.getProject()).findDirectory(antoraDir);
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
    PsiDirectory dir = PsiManager.getInstance(root.getProject()).findDirectory(module.getFile());
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
    if (depth == 0 && ANTORA_SUPPORTED.contains(macroName) && !AsciiDocUtil.isAntoraPartial(root)) {
      bases = handleAntora(myBase);
    }
    if (depth > MAX_DEPTH) {
      return;
    }
    for (String base : bases) {
      Matcher matcher = ATTRIBUTES.matcher(base);
      if (matcher.find()) {
        String attributeName = matcher.group(1);
        List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(root.getProject(), attributeName, root);
        for (AttributeDeclaration decl : declarations) {
          if (decl.getAttributeValue() == null) {
            continue;
          }
          getVariants(matcher.replaceFirst(Matcher.quoteReplacement(decl.getAttributeValue())), collector, depth + 1);
        }
      } else {
        List<PsiElement> resolves = resolve(base);
        if (resolves != null) {
          for (PsiElement resolve : resolves) {
            VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(resolve);
            if (resolve instanceof PsiDirectory) {
              if (((PsiDirectory) resolve).getVirtualFile().equals(antoraModuleDir)) {
                if (macroName.equals("xref") || macroName.equals("xref-attr")) {
                  VirtualFile antoraPagesDir = AsciiDocUtil.findAntoraPagesDir(resolve);
                  if (antoraPagesDir == null) {
                    continue;
                  }
                  resolve = PsiManager.getInstance(root.getProject()).findDirectory(antoraPagesDir);
                  if (resolve == null) {
                    continue;
                  }
                }
              }
            }
            for (final PsiElement child : resolve.getChildren()) {
              if (child instanceof PsiFileSystemItem) {
                collector.process((PsiFileSystemItem) child);
              }
            }
            if (resolve instanceof PsiDirectory) {
              for (final PsiDirectory child : ((PsiDirectory) resolve).getSubdirectories()) {
                collector.process(child);
              }
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

  private List<@NotNull PsiElement> resolve(String fileName) {
    fileName = removeFileProtocolPrefix(fileName);
    if (URL_PREFIX_PATTERN_WITHOUT_FILE.matcher(fileName).matches()) {
      return null;
    }
    if (fileName.contains("%")) {
      try {
        fileName = new String(new PercentCodec().decode(fileName.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      } catch (DecoderException e) {
        // noop
      }
    }
    // resolving of files will take place relative to the original element, not the transposed one.
    PsiElement element = myElement;
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

    if (macroName.equals("xref") || macroName.equals("xref-attr") || macroName.equals("include") || macroName.equals("image")) {
      if (isAntoraPartial(root)) {
        return resolveReferenceInPartial(fileName, element, startDir);
      } else if (!base.startsWith(".") && (macroName.equals("xref") || macroName.equals("xref-attr"))) {
        VirtualFile antoraPagesDir = AsciiDocUtil.findAntoraPagesDir(root);
        if (antoraPagesDir != null) {
          PsiDirectory directory = PsiManager.getInstance(root.getProject()).findDirectory(antoraPagesDir);
          if (directory != null) {
            startDir = directory;
          }
        }
      }
      List<@NotNull PsiElement> collectorFiles = getCollectorFiles(element, startDir, fileName);
      if (!collectorFiles.isEmpty()) {
        return collectorFiles;
      }
    }

    if (fileName.isEmpty()) {
      return Collections.singletonList(startDir);
    }
    if (!fileName.startsWith("/") && !fileName.startsWith("\\")) {
      String[] split = StringUtil.trimEnd(fileName, "/").split("/", -1);
      PsiDirectory dir = startDir;
      for (int i = 0; i < split.length - 1; ++i) {
        if (split[i].isEmpty()) {
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
        if (dir != null) {
          return Collections.singletonList(dir);
        } else {
          return Collections.emptyList();
        }
      }
      if (split[split.length - 1].equals(".")) {
        return Collections.singletonList(dir);
      }
      PsiFile file = dir.findFile(split[split.length - 1]);
      if (file != null) {
        return Collections.singletonList(file);
      }
      dir = dir.findSubdirectory(split[split.length - 1]);
      if (dir != null) {
        return Collections.singletonList(dir);
      }
    }
    // check if file name is absolute path
    return resolveAbsolutePath(element, fileName);
  }

  private List<@NotNull PsiElement> getCollectorFiles(PsiElement element, PsiDirectory startDir, String fileName) {
    VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(startDir);
    if (antoraModuleDir != null) {
      VirtualFile antoraFile = antoraModuleDir.getParent().getParent().findChild(ANTORA_YML);
      if (antoraFile == null) {
        return Collections.emptyList();
      }
      Map<String, Object> antora;
      try {
        antora = AsciiDocWrapper.readAntoraYaml(myElement.getProject(), antoraFile);
      } catch (YAMLException ex) {
        return Collections.emptyList();
      }
      Object ext = antora.get("ext");
      if (!(ext instanceof Map)) {
        return Collections.emptyList();
      }
      Object collector = ((Map<?, ?>) ext).get("collector");
      if (collector instanceof List) {
        for (Object item : ((List<?>) collector)) {
          if (item instanceof Map) {
            List<@NotNull PsiElement> result = matchCollector(element, startDir, fileName, antoraFile.getParent(), (Map<?, ?>) item);
            if (!result.isEmpty()) {
              return result;
            }
          }
        }
      } else if (collector instanceof Map) {
        List<@NotNull PsiElement> result = matchCollector(element, startDir, fileName, antoraFile.getParent(), (Map<?, ?>) collector);
        if (!result.isEmpty()) {
          return result;
        }
      }
    }
    return Collections.emptyList();
  }

  private List<@NotNull PsiElement> matchCollector(PsiElement element, PsiDirectory startDir, String fileName, VirtualFile antoraComponentDir, Map<?, ?> item) {
    if (!(item.get("scan") instanceof Map)) {
      return Collections.emptyList();
    }
    item = (Map<?, ?>) item.get("scan");
    String fullFile = fileName;
    if (!SystemInfo.isWindows && fullFile.length() > 1 && !fullFile.startsWith("/")) {
      fullFile = startDir.getVirtualFile().getCanonicalPath() + "/" + fullFile;
    } else if (SystemInfo.isWindows && !fullFile.matches("/[A-Z]:/.*")) {
      fullFile = startDir.getVirtualFile().getCanonicalPath() + "/" + fullFile;
    }
    String base = antoraComponentDir.getCanonicalPath();
    if (base == null) {
      return Collections.emptyList();
    }
    if (item.get("base") != null) {
      base = base + "/" + item.get("base");
    }
    if (!(item.get("dir") instanceof String)) {
      return Collections.emptyList();
    }
    if (fullFile.startsWith(base)) {
      fullFile = antoraComponentDir.getCanonicalPath() + "/" + item.get("dir") + "/" + fullFile.substring(base.length());
    }
    List<@NotNull PsiElement> psiElements = resolveAbsolutePath(element, fullFile);
    if (psiElements == null) {
      return Collections.emptyList();
    }
    return psiElements;
  }

  @SuppressWarnings("checkstyle:MethodLength")
  @NotNull
  private List<@NotNull PsiElement> resolveReferenceInPartial(String fileName, PsiElement element, PsiDirectory startDir) {
    List<@NotNull PsiElement> resolveAbsolutePath = resolveAbsolutePath(root, fileName);
    if (resolveAbsolutePath != null && !resolveAbsolutePath.isEmpty()) {
      return resolveAbsolutePath;
    }
    Matcher antoraVersionMatcher = VERSION.matcher(fileName);
    String antoraVersion = null;
    if (antoraVersionMatcher.find()) {
      antoraVersion = antoraVersionMatcher.group(1);
      fileName = antoraVersionMatcher.replaceFirst("");
    }
    List<@NotNull PsiElement> result = new ArrayList<>();
    String antoraComponent = null;
    String antoraModule = null;
    Matcher antoraComponentModuleMatcher = COMPONENT_MODULE.matcher(fileName);
    if (antoraComponentModuleMatcher.find()) {
      antoraComponent = antoraComponentModuleMatcher.group(1);
      antoraModule = antoraComponentModuleMatcher.group(2);
      fileName = antoraComponentModuleMatcher.replaceFirst("");
    } else {
      Matcher antoraModuleMatcher = MODULE.matcher(fileName);
      if (antoraModuleMatcher.find()) {
        antoraModule = antoraModuleMatcher.group(1);
        fileName = antoraModuleMatcher.replaceFirst("");
      }
    }
    String antoraFamily = "page";
    if (macroName.equals("video") || macroName.equals("image") || macroName.equals("audio")) {
      antoraFamily = "image";
    }
    Matcher antoraFamilyMatcher = FAMILY.matcher(fileName);
    boolean familySet = false;
    if (antoraFamilyMatcher.find()) {
      familySet = true;
      antoraFamily = antoraFamilyMatcher.group(1);
      fileName = antoraFamilyMatcher.replaceFirst("");
    }
    if (antoraModule != null && antoraModule.equals("")) {
      antoraModule = "ROOT";
    }
    // we're in the partials folder
    if (fileName.startsWith("./")) {
      VirtualFile vf = startDir.getVirtualFile();
      String cp1 = vf.getCanonicalPath();
      VirtualFile antoraPartials = AsciiDocUtil.findAntoraPartials(root);
      if (antoraPartials == null) {
        return result;
      }
      String antoraPartialsPath = antoraPartials.getCanonicalPath();
      if (antoraPartialsPath == null) {
        return result;
      }
      fileName = FileUtil.getRelativePath(antoraPartialsPath,
        cp1 + "/" + fileName.substring(2), '/');
      if (fileName == null) {
        return result;
      }
    }
    if (fileName.equals("/.") || fileName.equals("/..") || fileName.equals(".") || fileName.equals("..")) {
      return result;
    }
    String lastPart;
    try {
      Path fn = Path.of(fileName).getFileName();
      if (fn != null) {
        lastPart = fn.toString();
      } else {
        lastPart = "?";
      }
    } catch (InvalidPathException ex) {
      lastPart = "?";
    }
    if (lastPart.equals(".")) {
      return result;
    }
    if (!fileName.isEmpty() && !fileName.contains("..")) {
      Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(lastPart, new AsciiDocSearchScope(element.getProject()));
      fileName = StringUtils.stripEnd(fileName, "/");
      for (VirtualFile vf2 : virtualFiles) {
        VirtualFile antoraFamilyDir;
        switch (antoraFamily) {
          case "page":
            antoraFamilyDir = AsciiDocUtil.findAntoraPagesDir(element.getProject(), vf2);
            break;
          case "partial":
            antoraFamilyDir = AsciiDocUtil.findAntoraPartials(element.getProject(), vf2);
            break;
          case "image":
            antoraFamilyDir = AsciiDocUtil.findAntoraImagesDir(element.getProject(), vf2);
            break;
          case "attachment":
            antoraFamilyDir = AsciiDocUtil.findAntoraAttachmentsDir(element.getProject(), vf2);
            break;
          case "example":
            antoraFamilyDir = AsciiDocUtil.findAntoraExamplesDir(element.getProject(), vf2);
            break;
          default:
            antoraFamilyDir = null;
        }
        if (antoraFamilyDir == null || antoraFamilyDir.getCanonicalPath() == null || vf2.getCanonicalPath() == null) {
          continue;
        }
        String relativePath = FileUtil.getRelativePath(FileUtil.normalize(antoraFamilyDir.getCanonicalPath() + "/" + fileName), vf2.getCanonicalPath(), '/');
        if (relativePath == null || !relativePath.equals(".")) {
          continue;
        }
        PsiElement fileOrDirectory;
        if (!vf2.isDirectory()) {
          fileOrDirectory = PsiManager.getInstance(element.getProject()).findFile(vf2);
        } else {
          fileOrDirectory = PsiManager.getInstance(element.getProject()).findDirectory(vf2);
        }
        if (fileOrDirectory == null) {
          continue;
        }
        Collection<AttributeDeclaration> attributes = AsciiDocUtil.collectAntoraAttributes(fileOrDirectory);
        if (antoraModule != null && !Objects.equals(findAttributeValue(attributes, "page-module"), antoraModule)) {
          continue;
        }
        if (antoraComponent != null && !Objects.equals(findAttributeValue(attributes, "page-component-name"), antoraComponent)) {
          continue;
        }
        if (antoraVersion != null && !Objects.equals(findAttributeValue(attributes, "page-component-version"), antoraVersion)) {
          continue;
        }
        result.add(fileOrDirectory);
      }
    } else {
      List<AntoraModule> antoraModules = AsciiDocUtil.collectAntoraPrefixes(root.getProject(), antoraComponent, antoraVersion, antoraModule);
      modules: for (AntoraModule module : antoraModules) {
        VirtualFile antoraDir = module.getFile();
        if (familySet || !fileName.isEmpty()) {
          switch (antoraFamily) {
            case "page":
              antoraDir = AsciiDocUtil.findAntoraPagesDir(element.getProject(), antoraDir);
              break;
            case "partial":
              antoraDir = AsciiDocUtil.findAntoraPartials(element.getProject(), antoraDir);
              break;
            case "image":
              antoraDir = AsciiDocUtil.findAntoraImagesDir(element.getProject(), antoraDir);
              break;
            case "attachment":
              antoraDir = AsciiDocUtil.findAntoraAttachmentsDir(element.getProject(), antoraDir);
              break;
            case "example":
              antoraDir = AsciiDocUtil.findAntoraExamplesDir(element.getProject(), antoraDir);
              break;
            default:
              // noop
          }
        }
        if (antoraDir == null) {
          continue;
        }
        PsiDirectory directory = PsiManager.getInstance(element.getProject()).findDirectory(antoraDir);
        if (directory == null) {
          continue;
        }
        if (fileName.isEmpty()) {
          result.add(directory);
        } else {
          String[] split = StringUtil.trimEnd(fileName, "/").split("/", -1);
          PsiDirectory dir = directory;
          for (int i = 0; i < split.length - 1; ++i) {
            if (split[i].isEmpty()) {
              continue;
            }
            if (split[i].equals("..")) {
              dir = dir.getParent();
              if (dir == null) {
                continue modules;
              }
              continue;
            }
            if (split[i].equals(".")) {
              continue;
            }
            dir = dir.findSubdirectory(split[i]);
            if (dir == null) {
              continue modules;
            }
          }
          if (split[split.length - 1].equals("..")) {
            if (dir.getParent() != null) {
              result.add(dir.getParent());
            }
            continue;
          }
          if (split[split.length - 1].equals(".")) {
            result.add(dir);
            continue;
          }
          PsiFile file = dir.findFile(split[split.length - 1]);
          if (file != null) {
            result.add(file);
            continue;
          }
          dir = dir.findSubdirectory(split[split.length - 1]);
          if (dir != null) {
            result.add(dir);
            continue;
          }
        }
      }
    }
    return result;
  }

  private String findAttributeValue(Collection<AttributeDeclaration> attributes, String name) {
    for (AttributeDeclaration attribute : attributes) {
      if (attribute.getAttributeName().equals(name)) {
        return attribute.getAttributeValue();
      }
    }
    return null;
  }

  private List<@NotNull PsiElement> resolveAbsolutePath(PsiElement element, String fileName) {
    // check if file name is absolute path
    if (!fileName.contains("/") && !fileName.contains("\\")) {
      return null;
    }
    VirtualFile fileByPath;
    try {
      if (element.getContainingFile().getVirtualFile() != null &&
        element.getContainingFile().getVirtualFile().getFileSystem().getProtocol().equals("temp")) {
        VirtualFile vf = element.getContainingFile().getVirtualFile().getFileSystem().findFileByPath(fileName);
        if (vf != null && vf.isValid()) {
          PsiElement result = PsiManager.getInstance(element.getProject()).findFile(vf);
          if (result == null) {
            result = PsiManager.getInstance(element.getProject()).findDirectory(vf);
          }
          if (result == null) {
            return Collections.emptyList();
          }
          return Collections.singletonList(result);
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
    if (fileByPath != null && fileByPath.isValid()) {
      PsiFile file = PsiManager.getInstance(element.getProject()).findFile(fileByPath);
      if (file != null) {
        return Collections.singletonList(file);
      }
      PsiDirectory directory = PsiManager.getInstance(element.getProject()).findDirectory(fileByPath);
      if (directory == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(directory);
    }
    return null;
  }

  public boolean matches(PsiElement element) {
    if (element instanceof AsciiDocBlockId) {
      AsciiDocBlockId blockId = (AsciiDocBlockId) element;
      //noinspection RedundantIfStatement
      if (isAnchor && key.equals(blockId.getName())) {
        return true;
      }
    }
    return false;
  }

  private class BrowsableUrl extends FakePsiElement implements SyntheticElement {
    private final String url;

    @Override
    public boolean isEquivalentTo(PsiElement another) {
      return this.equals(another);
    }

    @Override
    public int hashCode() {
      return -1;
    }

    @Override
    public boolean equals(Object another) {
      return another instanceof BrowsableUrl && url.equals(((BrowsableUrl) another).url);
    }

    private BrowsableUrl(String url) {
      this.url = url;
    }

    @Override
    public PsiElement getParent() {
      return myElement;
    }

    @Override
    public void navigate(boolean requestFocus) {
      BrowserUtil.browse(url);
    }

    @Override
    public String getPresentableText() {
      return url;
    }

    @Override
    public String getName() {
      return url;
    }

    @Override
    public TextRange getTextRange() {
      final TextRange rangeInElement = getRangeInElement();
      final TextRange elementRange = getElement().getTextRange();
      return elementRange != null ? rangeInElement.shiftRight(elementRange.getStartOffset()) : rangeInElement;
    }
  }

}
