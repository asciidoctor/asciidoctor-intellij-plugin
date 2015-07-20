package org.asciidoc.intellij.lexer;

import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocElementType extends IElementType {
  public AsciiDocElementType(@NotNull @NonNls String debugName) {
    super(debugName, AsciiDocLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "AsciiDoc:" + super.toString();
  }
}
