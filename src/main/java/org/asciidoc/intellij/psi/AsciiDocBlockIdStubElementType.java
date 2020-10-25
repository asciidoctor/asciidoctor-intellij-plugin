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
import java.util.Objects;

public class AsciiDocBlockIdStubElementType extends IStubElementType<AsciiDocBlockIdStub, AsciiDocBlockId> {

  public static final String BLOCK_ID_WITH_VAR = "#VAR#";

  public AsciiDocBlockIdStubElementType() {
    super("ASCIIDOC_BLOCKID", AsciiDocLanguage.INSTANCE);
  }

  @Override
  public AsciiDocBlockId createPsi(@NotNull AsciiDocBlockIdStub stub) {
    return new AsciiDocBlockIdImpl(stub, this);
  }

  @NotNull
  @Override
  public AsciiDocBlockIdStub createStub(@NotNull AsciiDocBlockId psi, StubElement parentStub) {
    return new AsciiDocBlockIdStubImpl(parentStub, psi.getName());
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "blockId";
  }

  @Override
  public void serialize(@NotNull AsciiDocBlockIdStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @NotNull
  @Override
  public AsciiDocBlockIdStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    final StringRef titleRef = dataStream.readName();
    Objects.requireNonNull(titleRef);
    return new AsciiDocBlockIdStubImpl(parentStub,
      titleRef.getString()
    );
  }

  @Override
  public void indexStub(@NotNull AsciiDocBlockIdStub stub, @NotNull IndexSink sink) {
    if (stub.getName() != null) {
      if (AsciiDocUtil.ATTRIBUTES.matcher(stub.getName()).matches()) {
        // add an additional entry to find all block IDs with an attribute more easily
        sink.occurrence(AsciiDocBlockIdKeyIndex.KEY, BLOCK_ID_WITH_VAR);
      }
      sink.occurrence(AsciiDocBlockIdKeyIndex.KEY, stub.getName());
    }
  }
}
