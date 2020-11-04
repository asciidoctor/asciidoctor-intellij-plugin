package org.asciidoc.intellij.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.function.Consumer;

public interface AsciiDocHtmlPanel extends Disposable {
  @NotNull
  JComponent getComponent();

  void setHtml(@NotNull String html, @NotNull Map<String, String> attributes);

  void render();

  @NotNull
  static String getCssLines(@Nullable String inlineCss) {
    StringBuilder result = new StringBuilder();

    if (inlineCss != null) {
      result.append("<style>\n").append(inlineCss).append("\n</style>\n");
    }
    return result.toString();
  }

  void scrollToLine(int line, int lineCount);

  Editor getEditor();

  void setEditor(Editor editor);

  default void printToPdf(String canonicalPath, Consumer<Boolean> success) {
    throw new IllegalArgumentException();
  }

  default boolean isPrintingSupported() {
    return false;
  }

  enum PreviewTheme {
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
