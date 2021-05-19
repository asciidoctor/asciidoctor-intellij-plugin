package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocParser implements PsiParser {

  @NotNull
  @Override
  public ASTNode parse(@NotNull IElementType rootElementType, @NotNull PsiBuilder builder) {
    try {
      PsiBuilder.Marker root = builder.mark();
      AsciiDocParserImpl parserImpl = new AsciiDocParserImpl(builder);
      parserImpl.parse();

      root.done(rootElementType);
      return builder.getTreeBuilt();
    } catch (RuntimeException e) {
      if (e instanceof RuntimeExceptionWithAttachments || e instanceof ControlFlowException) {
        throw e;
      }
      throw AsciiDocPsiImplUtil.getRuntimeException("Problem parsing lexer result", builder.getOriginalText().toString(), e);
    }
  }
}
