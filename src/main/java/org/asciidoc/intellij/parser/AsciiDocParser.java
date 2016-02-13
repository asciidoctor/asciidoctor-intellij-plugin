package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocParser implements PsiParser {

  @NotNull
  @Override
  public ASTNode parse(@NotNull IElementType rootElementType, @NotNull PsiBuilder builder) {

    PsiBuilder.Marker root = builder.mark();
    AsciiDocParserImpl parserImpl = new AsciiDocParserImpl(builder);
    parserImpl.parse();

    root.done(rootElementType);
    return builder.getTreeBuilt();
  }
}
