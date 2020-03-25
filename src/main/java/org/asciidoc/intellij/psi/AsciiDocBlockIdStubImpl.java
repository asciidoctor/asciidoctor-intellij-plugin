package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocBlockIdStubImpl extends StubBase<AsciiDocBlockIdImpl> implements AsciiDocBlockIdStub {
  private final String name;

  public AsciiDocBlockIdStubImpl(StubElement parent, String name) {
    super(parent, AsciiDocElementTypes.BLOCKID);
    this.name = name;
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }
}
