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
import java.util.regex.Pattern;

public class AsciiDocSectionStubElementType extends IStubElementType<AsciiDocSectionStub, AsciiDocSection> implements IReparseableElementTypeBase, ICustomParsingType {

  public static final String SECTION_WITH_VAR = "#VAR#";

  // this contains "_" as this is a character often prefixed to IDs, sometimes not
  public static final Pattern NORMALIZED_CHARS_IN_INDEX = Pattern.compile("[ ._-]");

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
    return new AsciiDocSectionStubImpl(parentStub, ((AsciiDocSectionImpl) psi).getTitleNoSubstitution());
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "section";
  }

  @Override
  public void serialize(@NotNull AsciiDocSectionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getTitleNoSubstitution());
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
    if (stub.getTitleNoSubstitution() != null) {
      String normalizedKey = AsciiDocSectionImpl.INVALID_SECTION_ID_CHARS.matcher(stub.getTitleNoSubstitution().toLowerCase(Locale.US)).replaceAll("");
      normalizedKey = AsciiDocSectionStubElementType.NORMALIZED_CHARS_IN_INDEX.matcher(normalizedKey).replaceAll("");
      if (AsciiDocUtil.ATTRIBUTES.matcher(stub.getTitleNoSubstitution()).find()) {
        // add an additional entry to find all block IDs with an attribute more easily
        sink.occurrence(AsciiDocSectionKeyIndex.KEY, SECTION_WITH_VAR);
      }
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
