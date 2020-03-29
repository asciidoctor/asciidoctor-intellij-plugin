package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class AsciiDocSectionStubElementType extends IStubElementType<AsciiDocSectionStub, AsciiDocSection> {

  // this contains "_" as this is a character often prefixed to IDs, sometimes not
  public static final String NORMALIZED_CHARS_IN_INDEX = "[ ._-]";

  public AsciiDocSectionStubElementType() {
    super("ASCIIDOC_SECTION", AsciiDocLanguage.INSTANCE);
  }

  @Override
  public AsciiDocSection createPsi(@NotNull AsciiDocSectionStub stub) {
    return new AsciiDocSectionImpl(stub, this);
  }

  @NotNull
  @Override
  public AsciiDocSectionStub createStub(@NotNull AsciiDocSection psi, StubElement parentStub) {
    return new AsciiDocSectionStubImpl(parentStub, psi.getTitle());
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "section";
  }

  @Override
  public void serialize(@NotNull AsciiDocSectionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getTitle());
  }

  @NotNull
  @Override
  public AsciiDocSectionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    final StringRef titleRef = dataStream.readName();
    Objects.requireNonNull(titleRef);
    return new AsciiDocSectionStubImpl(parentStub,
      titleRef.getString()
    );
  }

  @Override
  public void indexStub(@NotNull AsciiDocSectionStub stub, @NotNull IndexSink sink) {
    if (stub.getTitle() != null) {
      String normalizedKey = AsciiDocSectionImpl.INVALID_SECTION_ID_CHARS.matcher(stub.getTitle().toLowerCase(Locale.US)).replaceAll("");
      normalizedKey = normalizedKey.replaceAll(NORMALIZED_CHARS_IN_INDEX, "");
      sink.occurrence(AsciiDocSectionKeyIndex.KEY, normalizedKey);
    }
  }
}
