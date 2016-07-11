package org.asciidoc.intellij.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AsciiDocPreviewConfigurable implements SearchableConfigurable {
  @Nullable
  private AsciiDocPreviewSettingsForm myForm = null;
  @NotNull
  private AsciiDocApplicationSettings myAsciiDocApplicationSettings;

  public AsciiDocPreviewConfigurable(@NotNull AsciiDocApplicationSettings asciiDocApplicationSettings) {
    myAsciiDocApplicationSettings = asciiDocApplicationSettings;
  }

  @NotNull
  @Override
  public String getId() {
    return "Settings.AsciiDoc.Preview";
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return AsciiDocBundle.message("settings.asciidoc.preview.name");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return getForm().getComponent();
  }

  @Override
  public boolean isModified() {
    return !getForm().getAsciiDocPreviewSettings().equals(myAsciiDocApplicationSettings.getAsciiDocPreviewSettings());
  }

  @Override
  public void apply() throws ConfigurationException {
    myAsciiDocApplicationSettings.setAsciiDocPreviewSettings(getForm().getAsciiDocPreviewSettings());
  }

  @Override
  public void reset() {
    getForm().setAsciiDocPreviewSettings(myAsciiDocApplicationSettings.getAsciiDocPreviewSettings());
  }

  @Override
  public void disposeUIResources() {
    myForm = null;
  }

  @NotNull
  public AsciiDocPreviewSettingsForm getForm() {
    if (myForm == null) {
      myForm = new AsciiDocPreviewSettingsForm();
    }
    return myForm;
  }
}
