package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.ICustomParsingType;
import com.intellij.psi.tree.IReparseableElementTypeBase;
import com.intellij.util.CharTable;
import com.intellij.util.io.StringRef;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.parser.AsciiDocParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class AsciiDocSectionStubElementType extends IStubElementType<AsciiDocSectionStub, AsciiDocSection> implements IReparseableElementTypeBase, ICustomParsingType {

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

  @Override
  public boolean isParsable(@Nullable ASTNode parent, @NotNull CharSequence buffer, @NotNull Language fileLanguage, @NotNull Project project) {
    char lastChar = buffer.charAt(buffer.length() - 1);
    if (lastChar == ' ' || lastChar == '\n' || lastChar == '\t') {
      return false;
    }
    return true;
  }

  @Override
  public boolean isValidReparse(@NotNull ASTNode oldNode, @NotNull ASTNode newNode) {
    return newNode.getElementType() == AsciiDocElementTypes.SECTION && newNode.getTreeNext() == null;
  }

  @Override
  public ASTNode parseContents(@NotNull ASTNode chameleon) {
    final PsiElement psi = chameleon.getTreeParent() != null ? chameleon.getTreeParent().getPsi() : chameleon.getPsi();
    assert psi != null : chameleon;
    final Project project = psi.getProject();

    final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
    final Lexer lexer = new AsciiDocLexer();
    final PsiBuilder builder = factory.createBuilder(project, chameleon, lexer, chameleon.getElementType().getLanguage(), chameleon.getChars());

    final PsiBuilder.Marker root = builder.mark();
    new AsciiDocParser().parse(AsciiDocElementTypes.SECTION, builder);
    if (!builder.eof()) {
      throw new AssertionError("Unexpected token: '" + builder.getTokenText() + "'");
    }
    root.done(chameleon.getElementType());

    return builder.getTreeBuilt().getFirstChildNode();
  }

  @NotNull
  @Override
  public ASTNode parse(@NotNull CharSequence text, @NotNull CharTable table) {
    final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
    final Lexer lexer = new AsciiDocLexer();
    final PsiBuilder builder = factory.createBuilder(LanguageParserDefinitions.INSTANCE.forLanguage(AsciiDocLanguage.INSTANCE), lexer, text);
    new AsciiDocParser().parse(AsciiDocElementTypes.SECTION, builder);
    if (!builder.eof()) {
      throw new AssertionError("Unexpected token: '" + builder.getTokenText() + "'");
    }
    return builder.getTreeBuilt().getFirstChildNode();
  }
}
