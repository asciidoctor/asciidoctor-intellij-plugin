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
import org.asciidoctor.SafeMode;
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
  @Nullable // can be returned as null when upgrading from an old release
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
  @Nullable // can be returned as null when upgrading from an old release
  private AsciiDocHtmlPanel.PreviewTheme myPreviewTheme = AsciiDocHtmlPanel.PreviewTheme.INTELLIJ;

  @Attribute("SafeMode")
  @Nullable // can be returned as null when upgrading from an old release
  private SafeMode mySafeMode = SafeMode.UNSAFE;

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

  // can be disabled if it causes problems for a user. Option to disable it will be removed once the feature is stable
  @Attribute("InplacePreviewRefresh")
  private boolean myInplacePreviewRefresh = true;

  @Attribute("DisabledInjectionsByLanguage")
  @Nullable // nullable when read from an old plugin versionn
  private String myDisabledInjectionsByLanguage = "";

  @Attribute("DefaultLanguageForPassthrough")
  @Nullable
  private String myLanguageForPassthrough = "html";

  @Attribute("ShowAsciiDocWarningsAndErrorsInEditor")
  private boolean myShowAsciiDocWarningsAndErrorsInEditor = true;

  @Attribute("EnabledKroki")
  private boolean myEnableKroki = false;

  @Attribute("KrokiUrl")
  private String myKrokiUrl;

  @Attribute("EnabledFoldedAttributeReferencedExperimental")
  private boolean myEnableAttributeFolding = false;

  @Attribute("Zoom")
  private Integer myZoom = 100;

  @Attribute("HideErrorsInSourceBlocks")
  private boolean myHideErrorsInSourceBlocks = false;

  @Attribute("HideErrorsByLanguage")
  @Nullable
  private String myHideErrorsByLanguage;

  public AsciiDocPreviewSettings() {
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  public AsciiDocPreviewSettings(@NotNull SplitFileEditor.SplitEditorLayout splitEditorLayout,
                                 @NotNull AsciiDocHtmlPanelProvider.ProviderInfo htmlPanelProviderInfo,
                                 @NotNull AsciiDocHtmlPanel.PreviewTheme previewTheme,
                                 @NotNull SafeMode safeMode, @NotNull Map<String, String> attributes,
                                 boolean verticalSplit, boolean editorFirst,
                                 boolean enableInjections,  @Nullable String languageForPassthrough,
                                 @Nullable String disabledInjectionsByLanguage,
                                 boolean showAsciiDocWarningsAndErrorsInEditor,
                                 boolean inplacePreviewRefresh,
                                 boolean enableKroki,
                                 String krokiUrl,
                                 boolean enableAttributeFolding,
                                 int zoom,
                                 boolean hideErrorsInSourceBlocks,
                                 @Nullable String hideErrorsByLanguage) {
    mySplitEditorLayout = splitEditorLayout;
    myHtmlPanelProviderInfo = htmlPanelProviderInfo;
    myPreviewTheme = previewTheme;
    mySafeMode = safeMode;
    this.attributes = attributes;
    myIsVerticalSplit = verticalSplit;
    myIsEditorFirst = editorFirst;
    myEnableInjections = enableInjections;
    myLanguageForPassthrough = languageForPassthrough;
    myDisabledInjectionsByLanguage = disabledInjectionsByLanguage;
    myShowAsciiDocWarningsAndErrorsInEditor = showAsciiDocWarningsAndErrorsInEditor;
    myInplacePreviewRefresh = inplacePreviewRefresh;
    myEnableKroki = enableKroki;
    myKrokiUrl = krokiUrl;
    myEnableAttributeFolding = enableAttributeFolding;
    myZoom = zoom;
    myHideErrorsInSourceBlocks = hideErrorsInSourceBlocks;
    myHideErrorsByLanguage = hideErrorsByLanguage;
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

  public SafeMode getSafeMode() {
    if (mySafeMode == null) {
      return SafeMode.UNSAFE;
    }
    return mySafeMode;
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

  public boolean isInplacePreviewRefresh() {
    return myInplacePreviewRefresh;
  }

  public String getDisabledInjectionsByLanguage() {
    return myDisabledInjectionsByLanguage;
  }

  public boolean isKrokiEnabled() {
    return myEnableKroki;
  }

  public String getKrokiUrl() {
    return myKrokiUrl;
  }

  public boolean isAttributeFoldingEnabled() {
    return myEnableAttributeFolding;
  }

  public int getZoom() {
    return myZoom != null ? myZoom : 100;
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
    if (mySafeMode != that.mySafeMode) {
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
    if (!Objects.equals(myLanguageForPassthrough, that.myLanguageForPassthrough)) {
      return false;
    }
    if (!Objects.equals(myDisabledInjectionsByLanguage, that.myDisabledInjectionsByLanguage)) {
      return false;
    }
    if (myShowAsciiDocWarningsAndErrorsInEditor != that.myShowAsciiDocWarningsAndErrorsInEditor) {
      return false;
    }
    if (myInplacePreviewRefresh != that.myInplacePreviewRefresh) {
      return false;
    }
    if (myEnableKroki != that.myEnableKroki) {
      return false;
    }
    if (!Objects.equals(myKrokiUrl, that.myKrokiUrl)) {
      return false;
    }
    if (myEnableAttributeFolding != that.myEnableAttributeFolding) {
      return false;
    }
    if (!Objects.equals(myZoom, that.myZoom)) {
      return false;
    }
    if (myHideErrorsInSourceBlocks != that.myHideErrorsInSourceBlocks) {
      return false;
    }
    if (!Objects.equals(myHideErrorsByLanguage, that.myHideErrorsByLanguage)) {
      return false;
    }
    return attributes.equals(that.attributes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(mySplitEditorLayout);
    result = 31 * result + Objects.hashCode(myHtmlPanelProviderInfo);
    result = 31 * result + Objects.hashCode(myPreviewTheme);
    result = 31 * result + Objects.hashCode(mySafeMode);
    result = 31 * result + attributes.hashCode();
    result = 31 * result + (myIsVerticalSplit ? 1 : 0);
    result = 31 * result + (myIsEditorFirst ? 1 : 0);
    result = 31 * result + (myEnableInjections ? 1 : 0);
    result = 31 * result + Objects.hashCode(myLanguageForPassthrough);
    result = 31 * result + Objects.hashCode(myDisabledInjectionsByLanguage);
    result = 31 * result + (myShowAsciiDocWarningsAndErrorsInEditor ? 1 : 0);
    result = 31 * result + (myInplacePreviewRefresh ? 1 : 0);
    result = 31 * result + (myEnableKroki ? 1 : 0);
    result = 31 * result + Objects.hashCode(myKrokiUrl);
    result = 31 * result + (myEnableAttributeFolding ? 1 : 0);
    result = 31 * result + Objects.hashCode(myZoom);
    result = 31 * result + (myHideErrorsInSourceBlocks ? 1 : 0);
    result = 31 * result + Objects.hashCode(myHideErrorsByLanguage);
    return result;
  }

  public String getLanguageForPassthrough() {
    // any old setting of null (not set) will default to 'html'
    return myLanguageForPassthrough == null ? "html" : myLanguageForPassthrough;
  }

  public boolean isHideErrorsInSourceBlocks() {
    return myHideErrorsInSourceBlocks;
  }

  public void setHideErrorsInSourceBlocks(boolean hideErrorsInSourceBlocks) {
    this.myHideErrorsInSourceBlocks = hideErrorsInSourceBlocks;
  }

  public String getHideErrorsByLanguage() {
    return myHideErrorsByLanguage;
  }

  public void setHideErrorsByLanguage(String hideErrorsByLanguage) {
    this.myHideErrorsByLanguage = hideErrorsByLanguage;
  }

  public List<String> getHiddenErrorsByLanguageAsList() {
    List<String> list = new ArrayList<>();
    if (myHideErrorsByLanguage != null) {
      Arrays.asList(myHideErrorsByLanguage.split(";")).forEach(
        entry -> list.add(entry.trim().toLowerCase(Locale.US))
      );
    }
    return list;
  }

  public interface Holder {
    void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings);

    @NotNull
    AsciiDocPreviewSettings getAsciiDocPreviewSettings();
  }
}
