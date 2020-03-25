package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubElement;

public interface AsciiDocBlockIdStub extends StubElement<AsciiDocBlockIdImpl> {
  String getName();
}
