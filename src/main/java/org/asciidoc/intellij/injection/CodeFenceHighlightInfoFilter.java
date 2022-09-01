package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocElementWithLanguage;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class CodeFenceHighlightInfoFilter implements HighlightInfoFilter {

  private static final Set<HighlightSeverity> SEVERITIES = Set.of(HighlightInfoType.SYMBOL_TYPE_SEVERITY,
    HighlightInfoType.INJECTED_FRAGMENT_SYNTAX_SEVERITY,
    HighlightInfoType.INJECTED_FRAGMENT_SEVERITY,
    HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY,
    HighlightInfoType.HIGHLIGHTED_REFERENCE_SEVERITY);

  public static final String SOURCE_PREFIX = "source-";

  @Override
  public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
    if (file == null) {
      return true;
    }
    Project project = file.getProject();
    InjectedLanguageManager manager = InjectedLanguageManager.getInstance(project);
    if (manager == null) {
      return true;
    }
    PsiLanguageInjectionHost injectionHost = manager.getInjectionHost(file);
    PsiFile topLevelFile = manager.getTopLevelFile(file);
    if (topLevelFile.getFileType() == AsciiDocFileType.INSTANCE
      && injectionHost instanceof AsciiDocElementWithLanguage) {
      if (!SEVERITIES.contains(highlightInfo.getSeverity())) {
        if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isHideErrorsInSourceBlocks()) {
          return false;
        }
        String language = ((AsciiDocElementWithLanguage) injectionHost).getFenceLanguage();
        if (language != null && language.startsWith(SOURCE_PREFIX)) {
          if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getHiddenErrorsByLanguageAsList().contains(language.substring(SOURCE_PREFIX.length()))) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
