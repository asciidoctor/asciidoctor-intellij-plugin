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
    // this will wrap three additional characters as it is not a plain text editor.
    checkWrapOnTyping(AsciiDocFileType.INSTANCE, "a", "text text <caret>", "text \ntext a<caret>");
  }

  public void testWrapWithNextLine() {
    mySettings.setDefaultRightMargin(20);
    // this will wrap three additional characters as it is not a plain text editor. This
    checkWrapOnTyping(AsciiDocFileType.INSTANCE, "1234567890", "else <caret>if else if else\nold text", "else 1234567890if \nelse if else\nold text");
  }

}
