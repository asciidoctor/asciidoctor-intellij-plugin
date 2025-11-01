package org.asciidoc.intellij.inspections;

import com.intellij.grazie.ide.inspection.grammar.GrazieInspection;
import com.intellij.grazie.spellcheck.GrazieSpellCheckingInspection;
import com.intellij.testFramework.PlatformTestUtil;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.gradle.internal.impldep.org.junit.Ignore;

/**
 * @author Alexander Schwartz
 */
@Ignore("Doesn't work in 2025.3 due to Cannot invoke \"com.intellij.codeInspection.ex.LocalInspectionToolWrapper.getTool()\" because \"toolWrapper\" is null")
public class AsciiDocGrammarInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeCaseForAnchor().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(GrazieInspection.class, GrazieSpellCheckingInspection.class);
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
    // ignore highlighting, as it will show several Grammar errors which depend on the Grazie version
    doTestNoFix(NAME, false);
  }

  @Override
  protected String getBasePath() {
    return "inspections/grammar";
  }
}
