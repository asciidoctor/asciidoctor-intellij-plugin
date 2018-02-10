package org.asciidoc.intellij.settings;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
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

  @Attribute("PreviewTheme")
  @NotNull
  private AsciiDocHtmlPanel.PreviewTheme myPreviewTheme = AsciiDocHtmlPanel.PreviewTheme.INTELLIJ;

  @Attribute("MathJaxUrl")
  private String myMathJaxUrl="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.6.0/MathJax.js?config=TeX-MML-AM_HTMLorMML";

  @Attribute("MathJaxHubConfig")
  private String myMathJaxHubConfig="<script type=\"text/x-mathjax-config\">\n" +
    "MathJax.Hub.Config({\n" +
    "  messageStyle: \"none\",\n" +
    "  menuSettings: {\n" +
    "  zoom: \"Click\"\n" +
    "  },\n" +
    "  tex2jax: {\n" +
    "    inlineMath: [[\"\\\\(\", \"\\\\)\"]],\n" +
    "    displayMath: [[\"\\\\[\", \"\\\\]\"]],\n" +
    "    ignoreClass: \"nostem|nolatexmath\"\n" +
    "  },\n" +
    "  asciimath2jax: {\n" +
    "    delimiters: [[\"\\\\$\", \"\\\\$\"]],\n" +
    "    ignoreClass: \"nostem|noasciimath\"\n" +
    "  },\n" +
    "  TeX: { equationNumbers: { autoNumber: \"none\" } }\n" +
    "});\n" +
    "</script> ";

  public AsciiDocPreviewSettings() {
  }

  public AsciiDocPreviewSettings(@NotNull SplitFileEditor.SplitEditorLayout splitEditorLayout,
                                 @NotNull AsciiDocHtmlPanelProvider.ProviderInfo htmlPanelProviderInfo,
                                 @NotNull AsciiDocHtmlPanel.PreviewTheme previewTheme,
                                 String mathJaxUrl,
                                 String mathJaxHubConfig) {
    mySplitEditorLayout = splitEditorLayout;
    myHtmlPanelProviderInfo = htmlPanelProviderInfo;
    myPreviewTheme = previewTheme;
    myMathJaxUrl = mathJaxUrl;
    myMathJaxHubConfig = mathJaxHubConfig;
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

  public String getMyMathJaxUrl()
  {
    if (myMathJaxUrl != null)
      return myMathJaxUrl;
    else
      return "";
  }

  public String getMyMathJaxHubConfig()
  {
    if (myMathJaxHubConfig != null)
      return myMathJaxHubConfig;
    else
      return "";
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
    if (!myMathJaxUrl.equals(settings.myMathJaxUrl)) return false;
    if (!myMathJaxHubConfig.equals(settings.myMathJaxHubConfig)) return false;



    return true;
  }

  @Override
  public int hashCode() {
    int result = mySplitEditorLayout.hashCode();
    result = 31 * result + myHtmlPanelProviderInfo.hashCode();
    result = 31 * result + myPreviewTheme.hashCode();
    result = 31 * result + myMathJaxUrl.hashCode();
    result = 31 * result + myMathJaxHubConfig.hashCode();
    return result;
  }

  public interface Holder {
    void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings);

    @NotNull
    AsciiDocPreviewSettings getAsciiDocPreviewSettings();
  }
}
