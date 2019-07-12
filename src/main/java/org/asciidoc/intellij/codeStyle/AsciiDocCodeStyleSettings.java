package org.asciidoc.intellij.codeStyle;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class AsciiDocCodeStyleSettings extends CustomCodeStyleSettings {

  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public boolean ONE_SENTENCE_PER_LINE = true;
  @SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName"})
  public boolean FORMATTING_ENABLED = true;

  protected AsciiDocCodeStyleSettings(CodeStyleSettings container) {
    super("AsciiDocCodeStyleSettings", container);
  }
}
