package org.asciidoc.intellij.editor;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AsciiDocHtmlPanel implements Disposable {
  @NotNull
  public abstract JComponent getComponent();

  public abstract void setHtml(@NotNull String html);

  public abstract void render();

  @NotNull
  protected static String getCssLines(@Nullable String inlineCss) {
    StringBuilder result = new StringBuilder();

    if (inlineCss != null) {
      result.append("<style>\n").append(inlineCss).append("\n</style>\n");
    }
    return result.toString();
  }
}
