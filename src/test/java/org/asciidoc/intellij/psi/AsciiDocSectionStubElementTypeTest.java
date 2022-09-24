package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.CharTableImpl;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.assertj.core.api.Assertions;

/**
 * Tests to ensure that incremental re-parsing works, as this is a important feature to speed up
 * processing when editing a document in the editor.
 */
public class AsciiDocSectionStubElementTypeTest extends BasePlatformTestCase {

  private final AsciiDocSectionStubElementType sut = (AsciiDocSectionStubElementType) AsciiDocElementTypes.SECTION;

  public void testTreatTwoHeadingsWithSameLevelAsReparseable() {
    ASTNode oldAst = sut.parse("== Heading\nText", new CharTableImpl());
    ASTNode newAst = sut.parse("== Other Heading\nOther Text", new CharTableImpl());
    Assertions.assertThat(sut.isValidReparse(oldAst, newAst)).isTrue();
  }

  public void testTreatTwoHeadingsWithDifferentLevelAsNotReparseable() {
    ASTNode oldAst = sut.parse("== Heading\nText", new CharTableImpl());
    ASTNode newAst = sut.parse("=== Other Level\nOther Text", new CharTableImpl());
    Assertions.assertThat(sut.isValidReparse(oldAst, newAst)).isFalse();
  }

  public void testTreatOpeningBlockAsNotReparsable() {
    ASTNode oldAst = sut.parse("== Heading\nText", new CharTableImpl());
    ASTNode newAst = sut.parse("== Heading\nText\n\n====", new CharTableImpl());
    Assertions.assertThat(sut.isValidReparse(oldAst, newAst)).isFalse();
  }

  public void testTreatAddedHeadingAsDifferent() {
    ASTNode oldAst = sut.parse("== Heading\nText", new CharTableImpl());
    ASTNode newAst = sut.parse("== Heading\nText\n\n== Other Heading", new CharTableImpl());
    Assertions.assertThat(sut.isValidReparse(oldAst, newAst)).isFalse();
  }

}
