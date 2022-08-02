package org.asciidoc.intellij.graziepro;

import com.intellij.grazie.pro.yaml.YamlScopeProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntaxTraverser;
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

    // For those elements inside a root element, Grazie will call again if it has found a match to check the scope.
    // This way it will recognize the scope of, for example, bold within a paragraph.
    PsiElement parent = SyntaxTraverser.psiApi().parents(element).find(
      (e) -> e instanceof AsciiDocLink || e instanceof AsciiDocTextQuoted
    );

    if (parent instanceof AsciiDocLink) {
      result.add("link");
    } else if (parent instanceof AsciiDocTextQuoted) {
      if (((AsciiDocTextQuoted) parent).isBold()) {
        result.add("strong");
      }
      if (((AsciiDocTextQuoted) parent).isItalic()) {
        result.add("emphasis");
      }
    }

    return result;
  }
}
