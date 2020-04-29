package org.asciidoc.intellij.lexer;

import com.intellij.psi.tree.ILazyParseableElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocLazyElementType extends ILazyParseableElementType {
  public AsciiDocLazyElementType(@NotNull @NonNls String debugName) {
    super(debugName, AsciiDocLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "AsciiDoc:" + super.toString();
  }
}
