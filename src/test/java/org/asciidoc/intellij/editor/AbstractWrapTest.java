package org.asciidoc.intellij.editor;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.editor.impl.view.FontLayoutService;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.MockFontLayoutService;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

/**
 * This copied parts from by com.intellij.codeInsight.wrap.AbstractWrapTest and its base classes.
 *
 * @author Alexander Schwartz
 */
public abstract class AbstractWrapTest extends LightPlatformCodeInsightTestCase {

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected CodeStyleSettings mySettings;

  protected void checkWrapOnTyping(@NotNull FileType fileType,
                                   @NotNull String textToType,
                                   @NotNull String initial,
                                   @NotNull String expected) {
    String name = "test." + fileType.getDefaultExtension();
    configureFromFileText(name, initial);
    assertFileTypeResolved(fileType, name);
    for (char c : textToType.toCharArray()) {
      type(c);
    }
    checkResultByText(expected);
  }

  public static final int TEST_CHAR_WIDTH = 10; // char width matches the one in EditorTestUtil.configureSoftWraps
  public static final int TEST_LINE_HEIGHT = 10;
  public static final int TEST_DESCENT = 2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mySettings = CodeStyle.createTestSettings(CodeStyle.getSettings(getProject()));
    mySettings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN = true;

    CodeStyleSettingsManager.getInstance(getProject()).setTemporarySettings(mySettings);
    FontLayoutService.setInstance(new MockFontLayoutService(TEST_CHAR_WIDTH, TEST_LINE_HEIGHT, TEST_DESCENT));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      FontLayoutService.setInstance(null);
    } catch (Throwable e) {
      addSuppressedException(e);
    } finally {
      super.tearDown();
    }
  }

  protected static void assertFileTypeResolved(@NotNull FileType type, @NotNull String path) {
    String name = PathUtil.getFileName(path);
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(name);
    assertEquals(type + " file type must be in this test classpath, but only " + fileType + " was found by '" +
      name + "' file name (with default extension '" + fileType.getDefaultExtension() + "')", type, fileType);
  }

}
