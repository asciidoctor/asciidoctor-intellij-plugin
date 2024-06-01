package org.asciidoc.intellij.inspections;

import com.intellij.grazie.ide.inspection.grammar.GrazieInspection;
import com.intellij.ide.plugins.DisabledPluginsState;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.intellij.testFramework.PlatformTestUtil;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocGrammarInspectionTest extends AsciiDocQuickFixTestBase {

  static {
    Set<PluginId> disabled = new HashSet<>();

    // to avoid: com.intellij.openapi.progress.ProcessCanceledException: java.lang.IncompatibleClassChangeError
    disabled.add(PluginId.getId("com.intellij.grazie.pro"));

    DisabledPluginsState.Companion.saveDisabledPluginsAndInvalidate(disabled);
  }

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
    // ignore highlighting, as it will show several Grammar errors which depend on the Grazie version
    doTestNoFix(NAME, false);
  }

  @Override
  protected String getBasePath() {
    return "inspections/grammar";
  }
}
