package org.asciidoc.intellij.settings;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.editor.jeditor.JeditorHtmlPanelProvider;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;

public final class AsciiDocPreviewSettings {
  public static final AsciiDocPreviewSettings DEFAULT = new AsciiDocPreviewSettings();

  @Attribute("DefaultSplitLayout")
  @NotNull
  private SplitFileEditor.SplitEditorLayout mySplitEditorLayout = SplitFileEditor.SplitEditorLayout.SPLIT;

  @Tag("HtmlPanelProviderInfo")
  @Property(surroundWithTag = false)
  @NotNull
  private AsciiDocHtmlPanelProvider.ProviderInfo myHtmlPanelProviderInfo = JeditorHtmlPanelProvider.INFO;

  {
    final AsciiDocHtmlPanelProvider.AvailabilityInfo availabilityInfo = new JavaFxHtmlPanelProvider().isAvailable();
    if (availabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      myHtmlPanelProviderInfo = JavaFxHtmlPanelProvider.INFO;
    }
  }

  @Attribute("PreviewTheme")
  @NotNull
  private AsciiDocHtmlPanel.PreviewTheme myPreviewTheme = AsciiDocHtmlPanel.PreviewTheme.INTELLIJ;

  public AsciiDocPreviewSettings() {
  }

  public AsciiDocPreviewSettings(@NotNull SplitFileEditor.SplitEditorLayout splitEditorLayout,
                                 @NotNull AsciiDocHtmlPanelProvider.ProviderInfo htmlPanelProviderInfo,
                                 @NotNull AsciiDocHtmlPanel.PreviewTheme previewTheme) {
    mySplitEditorLayout = splitEditorLayout;
    myHtmlPanelProviderInfo = htmlPanelProviderInfo;
    myPreviewTheme = previewTheme;
  }

  @NotNull
  public SplitFileEditor.SplitEditorLayout getSplitEditorLayout() {
    if (mySplitEditorLayout == null) {
      return SplitFileEditor.SplitEditorLayout.SPLIT;
    }
    return mySplitEditorLayout;
  }

  @NotNull
  public AsciiDocHtmlPanel.PreviewTheme getPreviewTheme() {
    if (myPreviewTheme == null) {
      return AsciiDocHtmlPanel.PreviewTheme.INTELLIJ;
    }
    return myPreviewTheme;
  }

  @NotNull
  public AsciiDocHtmlPanelProvider.ProviderInfo getHtmlPanelProviderInfo() {
    return myHtmlPanelProviderInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AsciiDocPreviewSettings settings = (AsciiDocPreviewSettings)o;

    if (mySplitEditorLayout != settings.mySplitEditorLayout) return false;
    if (myPreviewTheme != settings.myPreviewTheme) return false;
    if (!myHtmlPanelProviderInfo.equals(settings.myHtmlPanelProviderInfo)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mySplitEditorLayout.hashCode();
    result = 31 * result + myHtmlPanelProviderInfo.hashCode();
    result = 31 * result + myPreviewTheme.hashCode();
    return result;
  }

  public interface Holder {
    void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings);

    @NotNull
    AsciiDocPreviewSettings getAsciiDocPreviewSettings();
  }
}
