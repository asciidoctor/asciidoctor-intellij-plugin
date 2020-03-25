package org.asciidoc.intellij.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.StubBasedPsiElement;

public interface AsciiDocBlockId extends StubBasedPsiElement<AsciiDocBlockIdStub>, NavigationItem, AsciiDocNamedElement {
  boolean patternIsValid();
}
