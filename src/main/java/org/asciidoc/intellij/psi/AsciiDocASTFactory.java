package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocASTFactory extends ASTFactory {
  @Nullable
  @Override
  public CompositeElement createComposite(@NotNull IElementType type) {
    if (type == AsciiDocElementTypes.LISTING) {
      return new AsciiDocListing(type);
    }

    return super.createComposite(type);
  }

}
