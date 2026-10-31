package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Replaces the page alias in an xref with the resource ID of the page that declares the alias.
 * The resource ID contains module, component and version only when they differ from the page containing the xref.
 */
public class AsciiDocChangeXrefFromPageAliasToPage implements LocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.changeXrefFromPageAliasToPage");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    if (descriptor.getPsiElement() instanceof AsciiDocLink link) {
      LeafPsiElement child = getLinkFile(link);
      String target = getTarget(link);
      if (child != null && target != null) {
        child.replaceWithText(target);
      }
    }
  }

  public boolean canFix(AsciiDocLink link) {
    return getLinkFile(link) != null && getTarget(link) != null;
  }

  /**
   * The leaf containing the complete resource ID of the page, including prefixes for component, version and module.
   */
  @Nullable
  public static LeafPsiElement getLinkFile(AsciiDocLink link) {
    PsiElement child = link.getFirstChild();
    while (child != null) {
      if (child instanceof LeafPsiElement leaf && leaf.getElementType() == AsciiDocTokenTypes.LINKFILE) {
        return leaf;
      }
      child = child.getNextSibling();
    }
    return null;
  }

  @Nullable
  private static String getTarget(AsciiDocLink link) {
    LeafPsiElement linkFile = getLinkFile(link);
    AsciiDocFileReference fileReference = link.getFileReference();
    // keep attribute references as they are, as they can't be replaced safely
    if (linkFile == null || fileReference == null || linkFile.getText().contains("{")) {
      return null;
    }
    ResolveResult[] results = fileReference.multiResolve(false);
    if (results.length != 1 || !(results[0].getElement() instanceof AsciiDocFile targetPage)) {
      return null;
    }
    VirtualFile targetFile = targetPage.getVirtualFile();
    VirtualFile targetPagesDir = AsciiDocUtil.findAntoraPagesDir(targetPage);
    if (targetFile == null || targetPagesDir == null) {
      return null;
    }
    String path = VfsUtilCore.getRelativePath(targetFile, targetPagesDir, '/');
    String targetModule = AsciiDocUtil.findAttribute("page-module", AsciiDocUtil.collectAntoraAttributes(targetPage));
    String targetComponent = AsciiDocUtil.findAttribute("page-component-name", AsciiDocUtil.collectAntoraAttributes(targetPage));
    String targetVersion = AsciiDocUtil.findAttribute("page-component-version", AsciiDocUtil.collectAntoraAttributes(targetPage));
    String ownModule = AsciiDocUtil.findAttribute("page-module", AsciiDocUtil.collectAntoraAttributes(link));
    String ownComponent = AsciiDocUtil.findAttribute("page-component-name", AsciiDocUtil.collectAntoraAttributes(link));
    String ownVersion = AsciiDocUtil.findAttribute("page-component-version", AsciiDocUtil.collectAntoraAttributes(link));
    if (path == null || targetModule == null || targetComponent == null || targetVersion == null) {
      return null;
    }
    boolean sameComponent = Objects.equals(targetComponent, ownComponent);
    boolean sameVersion = sameComponent && Objects.equals(targetVersion, ownVersion);
    boolean sameModule = sameComponent && Objects.equals(targetModule, ownModule);
    StringBuilder sb = new StringBuilder();
    if (!sameVersion) {
      // an empty version is written as an underscore
      sb.append(targetVersion.isEmpty() ? "_" : targetVersion).append('@');
    }
    if (!sameComponent) {
      sb.append(targetComponent).append(':');
    }
    if (!sameComponent || !sameModule) {
      sb.append(targetModule).append(':');
    }
    return sb.append(path).toString();
  }

}
