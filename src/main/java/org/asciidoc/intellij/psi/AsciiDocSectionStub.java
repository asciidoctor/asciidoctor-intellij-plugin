package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubElement;

public interface AsciiDocSectionStub extends StubElement<AsciiDocSectionImpl> {
  String getTitleNoSubstitution();
}
