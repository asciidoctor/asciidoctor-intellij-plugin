package org.asciidoc.intellij.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizableOptions;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static com.intellij.openapi.util.io.StreamUtil.readText;
import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.BLANK_LINES_SETTINGS;
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
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "FORMATTING_ENABLED", "Enable Formatting (disabling this will override all options)", CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER);
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "ONE_SENTENCE_PER_LINE", "One sentence per line", CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER);
    } else if (settingsType == BLANK_LINES_SETTINGS) {
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "BLANK_LINES_AFTER_HEADER", "After header", CodeStyleSettingsCustomizableOptions.getInstance().BLANK_LINES);
      consumer.showCustomOption(AsciiDocCodeStyleSettings.class, "BLANK_LINES_KEEP_AFTER_HEADER", "After header", CodeStyleSettingsCustomizableOptions.getInstance().BLANK_LINES_KEEP);
    }
  }

  @Override
  public String getCodeSample(@NotNull SettingsType settingsType) {
    if (settingsType == SPACING_SETTINGS || settingsType == BLANK_LINES_SETTINGS) {
      return loadSample(settingsType);
    }
    return null;
  }

  private static String loadSample(@NotNull SettingsType settingsType) {
    String name = "/samples/" + settingsType.name() + ".adoc";
    try (InputStream is = AsciiDocLanguageCodeStyleSettingsProvider.class.getResourceAsStream(name)) {
      if (is == null) {
        LOG.warn("unable to load sample");
        return "";
      }
      try (Reader r = new InputStreamReader(is, UTF_8)) {
        return readText(r).replaceAll("\r", "");
      }
    } catch (IOException e) {
      LOG.warn("unable to load sample", e);
    }
    return "";
  }


}
