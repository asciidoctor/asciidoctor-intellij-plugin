package org.asciidoc.intellij.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.net.URL;
import java.net.URLConnection;

public class AsciiDocPreviewSettingsForm implements AsciiDocPreviewSettings.Holder {
  private Object myLastItem;
  private ComboBox myPreviewProvider;
  private ComboBox myDefaultSplitLayout;
  private ComboBox myPreviewThemeLayout;
  private JPanel myMainPanel;
  private EnumComboBoxModel<SplitFileEditor.SplitEditorLayout> mySplitLayoutModel;
  private EnumComboBoxModel<AsciiDocHtmlPanel.PreviewTheme> myPreviewThemeModel;
  private CollectionComboBoxModel<AsciiDocHtmlPanelProvider.ProviderInfo> myPreviewPanelModel;
  private JTextField myMathJaxUrl;
  private JTextArea myMathJaxHubConfig;

  public JComponent getComponent() {
    return myMainPanel;
  }

  private void createUIComponents() {
    //noinspection unchecked
    final List<AsciiDocHtmlPanelProvider.ProviderInfo> providerInfos =
      ContainerUtil.mapNotNull(AsciiDocHtmlPanelProvider.getProviders(),
        provider -> {
          if (provider.isAvailable() == AsciiDocHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE) {
            return null;
          }
          return provider.getProviderInfo();
        });
    myPreviewPanelModel = new CollectionComboBoxModel<>(providerInfos, providerInfos.get(0));
    myPreviewProvider = new ComboBox(myPreviewPanelModel);

    mySplitLayoutModel = new EnumComboBoxModel<>(SplitFileEditor.SplitEditorLayout.class);
    myDefaultSplitLayout = new ComboBox(mySplitLayoutModel);

    myPreviewThemeModel = new EnumComboBoxModel<>(AsciiDocHtmlPanel.PreviewTheme.class);
    myPreviewThemeLayout = new ComboBox(myPreviewThemeModel);

    myLastItem = myPreviewProvider.getSelectedItem();
    myPreviewProvider.addItemListener(e -> {
      final Object item = e.getItem();
      if (e.getStateChange() != ItemEvent.SELECTED || !(item instanceof AsciiDocHtmlPanelProvider.ProviderInfo)) {
        return;
      }

      final AsciiDocHtmlPanelProvider provider = AsciiDocHtmlPanelProvider.createFromInfo((AsciiDocHtmlPanelProvider.ProviderInfo) item);
      final AsciiDocHtmlPanelProvider.AvailabilityInfo availability = provider.isAvailable();

      if (!availability.checkAvailability(myMainPanel)) {
        myPreviewProvider.setSelectedItem(myLastItem);
      } else {
        myLastItem = item;
      }
    });

    myMathJaxUrl = new JTextField();
    myMathJaxUrl.setInputVerifier(new  MathJaxUrlInputVerifier());

  }

  @Override
  public void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings) {
    if (myPreviewPanelModel.contains(settings.getHtmlPanelProviderInfo())) {
      myPreviewPanelModel.setSelectedItem(settings.getHtmlPanelProviderInfo());
    }
    mySplitLayoutModel.setSelectedItem(settings.getSplitEditorLayout());
    myPreviewThemeModel.setSelectedItem(settings.getPreviewTheme());
    myMathJaxUrl.setText(settings.getMyMathJaxUrl());
    myMathJaxHubConfig.setText(settings.getMyMathJaxHubConfig());
  }

  @NotNull
  @Override
  public AsciiDocPreviewSettings getAsciiDocPreviewSettings() {
    if (myPreviewPanelModel.getSelected() == null) {
      throw new IllegalStateException("Should be selected always");
    }
    return new AsciiDocPreviewSettings(mySplitLayoutModel.getSelectedItem(),
      myPreviewPanelModel.getSelected(), myPreviewThemeModel.getSelectedItem(), myMathJaxUrl.getText(), myMathJaxHubConfig.getText());
  }
}


class MathJaxUrlInputVerifier extends InputVerifier {
  @Override
  public boolean verify(JComponent input) {
    String text = ((JTextField) input).getText();
    Logger logger = Logger.getInstance(MathJaxUrlInputVerifier.class);
    try {
      logger.warn(text);
      URLConnection urlconnection = new URL(text).openConnection();
      urlconnection.getContent();
      ((JTextField) input).setBackground(Color.GREEN);
      return true;
    } catch (Exception e)
    {
      ((JTextField) input).setBackground(Color.RED);
      return false;
    }
  }
}

