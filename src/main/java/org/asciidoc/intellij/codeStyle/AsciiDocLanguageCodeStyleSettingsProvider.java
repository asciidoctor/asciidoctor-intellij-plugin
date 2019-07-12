package org.asciidoc.intellij.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.intellij.openapi.util.io.StreamUtil.readText;
import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AsciiDocLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  private static final Logger LOG =
    Logger.getInstance(AsciiDocLanguageCodeStyleSettingsProvider.class);

  @NotNull
  @Override
  public Language getLanguage() {
    return AsciiDocLanguage.INSTANCE;
  }

  @Override
  public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
    if (settingsType == SPACING_SETTINGS) {
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "FORMATTING_ENABLED", "Enable Formatting (disabling this will override all options)", CodeStyleSettingsCustomizable.SPACES_OTHER);
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "ONE_SENTENCE_PER_LINE", "One sentence per line", CodeStyleSettingsCustomizable.SPACES_OTHER);
    }
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    if (settingsType == SPACING_SETTINGS) {
      return loadSample(settingsType);
    }
    return null;
  }

  private static String loadSample(@NotNull SettingsType settingsType) {
    String name = "/samples/" + settingsType.name() + ".adoc";
    try {
      return readText(
        AsciiDocLanguageCodeStyleSettingsProvider.class.getResourceAsStream(name), UTF_8
      );
    } catch (IOException e) {
      LOG.warn("unable to load sample", e);
    }
    return "";
  }


}
