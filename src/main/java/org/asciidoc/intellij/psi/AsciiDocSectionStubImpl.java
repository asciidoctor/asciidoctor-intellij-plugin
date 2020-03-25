package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocSectionStubImpl extends StubBase<AsciiDocSectionImpl> implements AsciiDocSectionStub {
  private final String title;

  public AsciiDocSectionStubImpl(StubElement parent, String title) {
    super(parent, AsciiDocElementTypes.SECTION);
    this.title = title;
  }

  @NotNull
  @Override
  public String getTitle() {
    return title;
  }
}
