package org.asciidoc.intellij.inspections;

import com.intellij.grazie.ide.inspection.grammar.GrazieInspection;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.testFramework.PlatformTestUtil;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocGrammarInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(GrazieInspection.class, SpellCheckingInspection.class);
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }

  public void testAttributeParsing() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testSomethingQuoted() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testSectionWithAttributes() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testSeparateBehavior() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/grammar";
  }
}
