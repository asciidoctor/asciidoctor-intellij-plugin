package org.asciidoc.intellij.settings.language;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

@Builder
@Data
// No-arg constructor required by IntelliJ XMLB for deserialization.
@NoArgsConstructor
@AllArgsConstructor
/* @Transient on generated getters/setters prevents XMLB from using the accessor path,
 avoiding double serialization alongside the field-level @Tag annotations. */
@Getter(onMethod_ = {@Transient})
@Setter(onMethod_ = {@Transient})
public class AsciiDocScriptLanguageSettings implements Serializable {
  @Tag("languageSettingGo")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingGo;

  @Tag("languageSettingJavaScript")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingJavaScript;

  @Tag("languageSettingPowerShell")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingPowerShell;

  @Tag("languageSettingPython")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingPython;

  @Tag("languageSettingRuby")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingRuby;

  @Tag("languageSettingTypeScript")
  @Property(surroundWithTag = false)
  @Nullable
  private AsciiDocScriptLanguageSetting languageSettingTypeScript;

  /**
   * Last selected language in the GUI. So, if a user tests again and again different interpreter parameters, the user
   * doesn't have to re-select the used language over and over.
   */
  @Attribute("selectedLanguage")
  @Nullable
  private AsciiDocLanguages selectedLanguage;
}
