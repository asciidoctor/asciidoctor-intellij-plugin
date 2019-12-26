package org.asciidoc.intellij.parser;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationImpl;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationKeyIndex;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationStub;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationStubImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class AsciiDocAttributeDeclarationStubElementType extends IStubElementType<AsciiDocAttributeDeclarationStub, AsciiDocAttributeDeclaration> {
  AsciiDocAttributeDeclarationStubElementType() {
    super("ASCIIDOC_ATTRIBUTE_DECLARATION", AsciiDocLanguage.INSTANCE);
  }

  @Override
  public AsciiDocAttributeDeclaration createPsi(@NotNull AsciiDocAttributeDeclarationStub stub) {
    return new AsciiDocAttributeDeclarationImpl(stub, this);
  }

  @NotNull
  @Override
  public AsciiDocAttributeDeclarationStub createStub(@NotNull AsciiDocAttributeDeclaration psi, StubElement parentStub) {
    return new AsciiDocAttributeDeclarationStubImpl(parentStub, psi.getAttributeName(), psi.getAttributeValue());
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "attribute.declaration";
  }

  @Override
  public void serialize(@NotNull AsciiDocAttributeDeclarationStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getAttributeName());
    dataStream.writeName(stub.getAttributeValue());
  }

  @NotNull
  @Override
  public AsciiDocAttributeDeclarationStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    final StringRef attributeNameRef = dataStream.readName();
    final StringRef attributeValueRef = dataStream.readName();
    Objects.requireNonNull(attributeNameRef);
    return new AsciiDocAttributeDeclarationStubImpl(parentStub,
      attributeNameRef.getString(),
      attributeValueRef == null ? null : attributeValueRef.getString());
  }

  @Override
  public void indexStub(@NotNull AsciiDocAttributeDeclarationStub stub, @NotNull IndexSink sink) {
    if (stub.getAttributeName() != null) {
      sink.occurrence(AsciiDocAttributeDeclarationKeyIndex.KEY, stub.getAttributeName());
    }
  }
}
