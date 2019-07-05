package org.asciidoc.intellij.actions.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;

public abstract class Intention implements IntentionAction {

  private String getPrefix() {
    final Class<? extends Intention> aClass = getClass();
    final String name = aClass.getSimpleName();
    final StringBuilder buffer = new StringBuilder(name.length() + 10);
    buffer.append("asciidoc");
    for (int i = buffer.length(); i < name.length(); i++) {
      final char c = name.charAt(i);
      if (Character.isUpperCase(c)) {
        buffer.append('.');
        buffer.append(Character.toLowerCase(c));
      } else {
        buffer.append(c);
      }
    }
    return buffer.toString();
  }

  @Override
  @NotNull
  public String getText() {
    return AsciiDocBundle.message(getPrefix() + ".name");
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return AsciiDocBundle.message(getPrefix() + ".family.name");
  }

}
