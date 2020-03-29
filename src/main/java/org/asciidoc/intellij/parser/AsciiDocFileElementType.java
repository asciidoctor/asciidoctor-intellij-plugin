package org.asciidoc.intellij.parser;

import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.tree.IStubFileElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocFile;

public class AsciiDocFileElementType extends IStubFileElementType<PsiFileStub<AsciiDocFile>> {

  public AsciiDocFileElementType() {
    super(AsciiDocLanguage.INSTANCE);
  }

  @Override
  public int getStubVersion() {
    return 7;
  }
}
