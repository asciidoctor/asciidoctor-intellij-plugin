package org.asciidoc.intellij.formatting;

import com.intellij.codeInsight.editorActions.AutoHardWrapHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider;
import com.intellij.psi.impl.source.codeStyle.lineIndent.FormatterBasedLineIndentProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * When auto-wrapping of lines in in-progress, don't adjust the line's indent to avoid confusing the logic in
 * {@link AutoHardWrapHandler#wrapLineIfNecessary}.
 *
 * @author Alexander Schwartz
 */
public class AsciiDocLineIndentProvider extends FormatterBasedLineIndentProvider {

  @Override
  public @Nullable String getLineIndent(@NotNull Project project, @NotNull Editor editor, @Nullable Language language, int offset) {
    if (editor.getUserData(AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY) == Boolean.TRUE) {
      return LineIndentProvider.DO_NOT_ADJUST;
    } else {
      return super.getLineIndent(project, editor, language, offset);
    }
  }

  @Override
  public boolean isSuitableFor(@Nullable Language language) {
    return language == AsciiDocLanguage.INSTANCE;
  }
}
