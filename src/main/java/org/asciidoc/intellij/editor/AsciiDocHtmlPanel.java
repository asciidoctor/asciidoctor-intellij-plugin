package org.asciidoc.intellij.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AsciiDocHtmlPanel implements Disposable {
  @NotNull
  public abstract JComponent getComponent();

  public abstract void setHtml(@NotNull String html, String imagesdir);

  public abstract void render();

  private Editor editor;

  @NotNull
  protected static String getCssLines(@Nullable String inlineCss) {
    StringBuilder result = new StringBuilder();

    if (inlineCss != null) {
      result.append("<style>\n").append(inlineCss).append("\n</style>\n");
    }
    return result.toString();
  }

  public abstract void scrollToLine(int line, int lineCount);

  public Editor getEditor() {
    return editor;
  }

  public void setEditor(Editor editor) {
    this.editor = editor;
  }

  public enum PreviewTheme {
    INTELLIJ(AsciiDocBundle.message("asciidoc.preview.intellij")),
    ASCIIDOC(AsciiDocBundle.message("asciidoc.preview.asciidoc")),
    DARCULA(AsciiDocBundle.message("asciidoc.preview.darcula"));

    private final String presentationName;

    PreviewTheme(String presentationName) {
      this.presentationName = presentationName;
    }

    @Override
    public String toString() {
      return presentationName;
    }
  }

}
