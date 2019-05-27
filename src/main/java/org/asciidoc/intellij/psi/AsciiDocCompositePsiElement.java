package org.asciidoc.intellij.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AsciiDocCompositePsiElement extends AsciiDocPsiElement {
  String getPresentableTagName();

  @NotNull
  List<AsciiDocPsiElement> getCompositeChildren();
}
