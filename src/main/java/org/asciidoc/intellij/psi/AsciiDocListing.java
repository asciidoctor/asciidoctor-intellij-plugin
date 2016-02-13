package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocListing extends AsciiDocBlock {
  public AsciiDocListing(@NotNull ASTNode node) {
    super(node);
  }
}
