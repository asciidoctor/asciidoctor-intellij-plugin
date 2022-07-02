package org.asciidoc.intellij.graziepro;

import com.intellij.grazie.pro.yaml.YamlScopeProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.asciidoc.intellij.grazie.AsciiDocLanguageSupport;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocCell;
import org.asciidoc.intellij.psi.AsciiDocHeading;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocListItem;
import org.asciidoc.intellij.psi.AsciiDocStandardBlock;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.asciidoc.intellij.psi.AsciiDocTitle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This maps the scoping of different AsciiDoc elements as defined for Vale.
 * See <a href="https://vale.sh/docs/topics/scoping/">Vale Scoping</a> for more information.
 */
public class AsciiDocGrazieProScopeProvider implements YamlScopeProvider {

  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  @Override
  public @NotNull List<String> getApplicableScopes(@NotNull PsiElement element) {
    List<String> result = new ArrayList<>();
    result.add("markup");

    // Rules for the root elements, and the whole block will have that scope.
    if (element instanceof AsciiDocHeading) {
      result.add("heading");
    }
    if (element instanceof AsciiDocCell) {
      result.add("table");
      result.add("cell");
    }
    if (element instanceof AsciiDocTitle &&
      element.getParent() instanceof AsciiDocStandardBlock &&
      ((AsciiDocStandardBlock) element.getParent()).getType() == AsciiDocBlock.Type.TABLE) {
      result.add("table");
      result.add("caption");
    }
    if (element instanceof AsciiDocListItem) {
      result.add("list");
    }

    // Rules where we look at the children. Only one of the children will have that role, still the whole block
    // will have that scope.
    new ScopeExtractingVisitor(element, result).visitElement(element);

    return result;
  }

  private class ScopeExtractingVisitor extends PsiElementVisitor {
    private final PsiElement root;
    private final List<String> result;

    ScopeExtractingVisitor(PsiElement root, List<String> result) {
      this.root = root;
      this.result = result;
    }

    @Override
    public void visitElement(@NotNull PsiElement child) {
      AsciiDocLanguageSupport.Behavior elementBehavior = languageSupport.getElementBehavior(root, child);
      switch (elementBehavior) {
        case STEALTH:
        case UNKNOWN:
        case ABSORB:
          break;
        case SEPARATE:
        case TEXT:
          if (child instanceof AsciiDocLink) {
            result.add("link");
          }
          if (child instanceof AsciiDocTextQuoted
            && ((AsciiDocTextQuoted) child).isBold()) {
            result.add("strong");
          }
          if (child instanceof AsciiDocTextQuoted
            && ((AsciiDocTextQuoted) child).isItalic()) {
            result.add("emphasis");
          }
          if (child.getFirstChild() != null) {
            child.acceptChildren(this);
          }
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + elementBehavior);
      }
    }
  }

}
