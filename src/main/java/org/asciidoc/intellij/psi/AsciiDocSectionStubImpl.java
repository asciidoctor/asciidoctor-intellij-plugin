package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocSectionStubImpl extends StubBase<AsciiDocSectionImpl> implements AsciiDocSectionStub {
  private final String titleNoSubstitution;

  public AsciiDocSectionStubImpl(StubElement parent, String titleNoSubstitution) {
    super(parent, AsciiDocElementTypes.SECTION);
    this.titleNoSubstitution = titleNoSubstitution;
  }

  @NotNull
  @Override
  public String getTitleNoSubstitution() {
    return titleNoSubstitution;
  }
}
