package org.asciidoc.intellij.inspections;

import com.intellij.grazie.ide.inspection.grammar.GrazieInspection;
import com.intellij.ide.plugins.PluginEnabler;
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
    Set<PluginId> enabled = new HashSet<>();

    // to improve performance, remove plugins used for debugging in interactive mode
    disabled.add(PluginId.getId("PsiViewer"));
    disabled.add(PluginId.getId("PlantUML integration"));
    disabled.add(PluginId.getId("com.intellij.platform.images"));
    disabled.add(PluginId.getId("com.intellij.javafx"));

    PluginEnabler.HEADLESS.disableById(disabled);
    PluginEnabler.HEADLESS.enableById(enabled);
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
    doTestNoFix(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/grammar";
  }
}
