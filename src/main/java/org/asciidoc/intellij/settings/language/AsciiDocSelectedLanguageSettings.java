package org.asciidoc.intellij.settings.language;

/**
 * Get information about currently selected language settings.
 */
interface AsciiDocSelectedLanguageSettings {
  /**
   * Returns currently selected language settings.
   *
   * @return Currently selected language settings.
   */
  AsciiDocLanguageSettingsParam getSetupLanguageSettingsParam();
}
