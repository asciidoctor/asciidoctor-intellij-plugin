package org.asciidoc.intellij.settings;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.editor.jeditor.JeditorHtmlPanelProvider;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

  @Property(surroundWithTag = false)
  @MapAnnotation(surroundWithTag = false, entryTagName = "attribute")
  @NotNull
  private Map<String, String> attributes = new HashMap<>();

  @Attribute("VerticalSplit")
  private boolean myIsVerticalSplit = true;

  @Attribute("EditorFirst")
  private boolean myIsEditorFirst = true;

  @Attribute("EnableInjections")
  private boolean myEnableInjections = true;

  @Attribute("DisabledInjectionsByLanguage")
  @Nullable
  private String myDisabledInjectionsByLanguage;

  @Attribute("EnableFormatSource")
  private boolean myEnableFormatSource = false;

  @Attribute("ShowAsciiDocWarningsAndErrorsInEditor")
  private boolean myShowAsciiDocWarningsAndErrorsInEditor = true;

  public AsciiDocPreviewSettings() {
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public AsciiDocPreviewSettings(@NotNull SplitFileEditor.SplitEditorLayout splitEditorLayout,
                                 @NotNull AsciiDocHtmlPanelProvider.ProviderInfo htmlPanelProviderInfo,
                                 @NotNull AsciiDocHtmlPanel.PreviewTheme previewTheme,
                                 @NotNull Map<String, String> attributes, boolean verticalSplit, boolean editorFirst,
                                 boolean enableInjections, @Nullable String disabledInjectionsByLanguage,
                                 boolean enableFormatSource,
                                 boolean showAsciiDocWarningsAndErrorsInEditor) {
    mySplitEditorLayout = splitEditorLayout;
    myHtmlPanelProviderInfo = htmlPanelProviderInfo;
    myPreviewTheme = previewTheme;
    this.attributes = attributes;
    myIsVerticalSplit = verticalSplit;
    myIsEditorFirst = editorFirst;
    myEnableInjections = enableInjections;
    myDisabledInjectionsByLanguage = disabledInjectionsByLanguage;
    myEnableFormatSource = enableFormatSource;
    myShowAsciiDocWarningsAndErrorsInEditor = showAsciiDocWarningsAndErrorsInEditor;
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

  @NotNull
  public Map<String, String> getAttributes() {
    return attributes;
  }

  public boolean isVerticalSplit() {
    return myIsVerticalSplit;
  }

  public boolean isEditorFirst() {
    return myIsEditorFirst;
  }

  public boolean isEnabledInjections() {
    return myEnableInjections;
  }

  public String getDisabledInjectionsByLanguage() {
    return myDisabledInjectionsByLanguage;
  }

  public List<String> getDisabledInjectionsByLanguageAsList() {
    List<String> list = new ArrayList<>();
    if (myDisabledInjectionsByLanguage != null) {
      Arrays.asList(myDisabledInjectionsByLanguage.split(";")).forEach(
        entry -> list.add(entry.trim().toLowerCase(Locale.US))
      );
    }
    return list;
  }

  public boolean isEnabledFormatSource() {
    return myEnableFormatSource;
  }

  public boolean isShowAsciiDocWarningsAndErrorsInEditor() {
    return myShowAsciiDocWarningsAndErrorsInEditor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AsciiDocPreviewSettings that = (AsciiDocPreviewSettings) o;

    if (mySplitEditorLayout != that.mySplitEditorLayout) {
      return false;
    }
    if (!myHtmlPanelProviderInfo.equals(that.myHtmlPanelProviderInfo)) {
      return false;
    }
    if (myPreviewTheme != that.myPreviewTheme) {
      return false;
    }
    if (myIsVerticalSplit != that.myIsVerticalSplit) {
      return false;
    }
    if (myIsEditorFirst != that.myIsEditorFirst) {
      return false;
    }
    if (myEnableInjections != that.myEnableInjections) {
      return false;
    }
    if (!Objects.equals(myDisabledInjectionsByLanguage, that.myDisabledInjectionsByLanguage)) {
      return false;
    }
    if (myEnableFormatSource != that.myEnableFormatSource) {
      return false;
    }
    if (myShowAsciiDocWarningsAndErrorsInEditor != that.myShowAsciiDocWarningsAndErrorsInEditor) {
      return false;
    }
    return attributes.equals(that.attributes);
  }

  @Override
  public int hashCode() {
    int result = mySplitEditorLayout.hashCode();
    result = 31 * result + myHtmlPanelProviderInfo.hashCode();
    result = 31 * result + myPreviewTheme.hashCode();
    result = 31 * result + attributes.hashCode();
    result = 31 * result + (myIsVerticalSplit ? 1 : 0);
    result = 31 * result + (myIsEditorFirst ? 1 : 0);
    result = 31 * result + (myEnableInjections ? 1 : 0);
    result = 31 * result + Objects.hashCode(myDisabledInjectionsByLanguage);
    result = 31 * result + (myEnableFormatSource ? 1 : 0);
    result = 31 * result + (myShowAsciiDocWarningsAndErrorsInEditor ? 1 : 0);
    return result;
  }

  public interface Holder {
    void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings);

    @NotNull
    AsciiDocPreviewSettings getAsciiDocPreviewSettings();
  }
}
