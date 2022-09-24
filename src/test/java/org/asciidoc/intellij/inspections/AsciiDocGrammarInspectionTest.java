package org.asciidoc.intellij.inspections;

import com.intellij.grazie.ide.inspection.grammar.GrazieInspection;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.testFramework.PlatformTestUtil;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocGrammarInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeCaseForAnchor().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(GrazieInspection.class, SpellCheckingInspection.class);
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }

  public void testAttributeParsing() {
    doTestNoFix(NAME, true);
  }

  public void testSomethingQuoted() {
    doTestNoFix(NAME, true);
  }

  public void testSectionWithAttributes() {
    doTestNoFix(NAME, true);
  }

  public void testSeparateBehavior() {
    doTestNoFix(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/grammar";
  }
}
