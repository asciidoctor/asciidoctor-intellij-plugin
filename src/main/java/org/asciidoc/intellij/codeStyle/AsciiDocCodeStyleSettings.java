package org.asciidoc.intellij.codeStyle;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class AsciiDocCodeStyleSettings extends CustomCodeStyleSettings {

  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public boolean ONE_SENTENCE_PER_LINE = true;
  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public boolean FORMATTING_ENABLED = true;
  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public int BLANK_LINES_AFTER_HEADER = 1;
  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public int BLANK_LINES_KEEP_AFTER_HEADER = 1;

  protected AsciiDocCodeStyleSettings(CodeStyleSettings container) {
    super("AsciiDocCodeStyleSettings", container);
  }
}
