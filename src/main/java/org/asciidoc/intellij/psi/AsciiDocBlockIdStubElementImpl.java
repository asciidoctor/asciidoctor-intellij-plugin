package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

public class AsciiDocBlockIdStubElementImpl<T extends StubElement> extends StubBasedPsiElementBase<T> {
  public AsciiDocBlockIdStubElementImpl(@NotNull T stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public AsciiDocBlockIdStubElementImpl(final ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public Language getLanguage() {
    return AsciiDocLanguage.INSTANCE;
  }

}
