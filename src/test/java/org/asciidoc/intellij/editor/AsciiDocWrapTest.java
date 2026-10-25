package org.asciidoc.intellij.editor;

import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * Testing auto-wrapping when typing.
 *
 * @author Alexander Schwartz
 */
public class AsciiDocWrapTest extends AbstractWrapTest {

  public void testWrapAfterSpaceOnMargin() {
    mySettings.setDefaultRightMargin(10);
    // Since 2026.3 (IJPL-245538) no columns are reserved for languages without string literals, so the word that exceeds the margin is wrapped.
    // Before, three additional characters were reserved, and the word before was wrapped as well.
    checkWrapOnTyping(AsciiDocFileType.INSTANCE, "a", "text text <caret>", "text text \na<caret>");
  }

  public void testWrapWithNextLine() {
    mySettings.setDefaultRightMargin(20);
    // this will wrap three additional characters as it is not a plain text editor. This
    checkWrapOnTyping(AsciiDocFileType.INSTANCE, "1234567890", "else <caret>if else if else\nold text", "else 1234567890if \nelse if else\nold text");
  }

}
