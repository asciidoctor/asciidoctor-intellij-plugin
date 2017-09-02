package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiElementVisitor;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

public class AsciiDocVisitor extends PsiElementVisitor {

  public void visitSections(@NotNull AsciiDocSection o) {
    visitElement(o);
  }

}
