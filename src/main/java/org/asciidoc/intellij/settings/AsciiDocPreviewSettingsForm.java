package org.asciidoc.intellij.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.containers.ContainerUtil;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AsciiDocPreviewSettingsForm implements AsciiDocPreviewSettings.Holder {
  private Object myLastItem;
  private ComboBox myPreviewProvider;
  private ComboBox myDefaultSplitLayout;
  private ComboBox myPreviewThemeLayout;
  private ComboBox mySafeModeSetting;
  private JPanel myMainPanel;
  private EnumComboBoxModel<SplitFileEditor.SplitEditorLayout> mySplitLayoutModel;
  private EnumComboBoxModel<AsciiDocHtmlPanel.PreviewTheme> myPreviewThemeModel;
  private EnumComboBoxModel<SafeMode> mySafeModeModel;
  private CollectionComboBoxModel<AsciiDocHtmlPanelProvider.ProviderInfo> myPreviewPanelModel;
  private AttributeTable attributeTable;
  private JPanel attributesPanel;
  private JBRadioButton myVerticalLayout;
  private JBRadioButton myHorizontalLayout;
  private JBLabel myVerticalSplitLabel;
  private JBRadioButton myEditorLeft;
  private JBRadioButton myEditorBottom;
  private JBRadioButton myEditorRight;
  private JBRadioButton myEditorTop;
  private JBCheckBox myEnableInjections;
  private JBTextField myDisabledInjectionsByLanguage;
  private JPanel myDisableLanguageInjection;
  private JBCheckBox myShowAsciiDocWarningsAndErrorsInEditor;
  private JBCheckBox myInplacePreviewRefresh;
  private JBCheckBox myEnableKroki;
  private JPanel myKrokiUrlPanel;
  private JBTextField myKrokiUrl;
  private JBTextField myLanguageForPassthrough;
  private JBCheckBox myEnabledAttributeFolding;

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

    mySafeModeModel = new EnumComboBoxModel<>(SafeMode.class);
    mySafeModeSetting = new ComboBox(mySafeModeModel);

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

    attributeTable = new AttributeTable();
    attributesPanel = new JPanel(new BorderLayout());
    attributesPanel.add(attributeTable.getComponent(), BorderLayout.CENTER);
    // from 2020.1 onwards the attributes panel requires a width, otherwise it will have a zero width
    attributesPanel.setMinimumSize(new Dimension(400, 100));
  }

  private void adjustKrokiOptions() {
    if (myEnableKroki.isSelected()) {
      myKrokiUrlPanel.setVisible(true);
    } else {
      myKrokiUrlPanel.setVisible(false);
    }
  }

  private void adjustSplitOption() {
    boolean isEditorFirst = myEditorTop.isSelected() || myEditorLeft.isSelected();
    boolean isVerticalSplit = myVerticalLayout.isSelected();
    myEditorBottom.setVisible(!isVerticalSplit);
    myEditorTop.setVisible(!isVerticalSplit);
    myEditorLeft.setVisible(isVerticalSplit);
    myEditorRight.setVisible(isVerticalSplit);
    myEditorLeft.setSelected(isVerticalSplit && isEditorFirst);
    myEditorRight.setSelected(isVerticalSplit && !isEditorFirst);
    myEditorTop.setSelected(!isVerticalSplit && isEditorFirst);
    myEditorBottom.setSelected(!isVerticalSplit && !isEditorFirst);
  }

  @Override
  public void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings) {
    if (myPreviewPanelModel.contains(settings.getHtmlPanelProviderInfo())) {
      myPreviewPanelModel.setSelectedItem(settings.getHtmlPanelProviderInfo());
    }
    mySplitLayoutModel.setSelectedItem(settings.getSplitEditorLayout());
    myPreviewThemeModel.setSelectedItem(settings.getPreviewTheme());
    mySafeModeSetting.setSelectedItem(settings.getSafeMode());

    List<AttributeTableItem> attributes = settings.getAttributes().entrySet().stream()
      .filter(a -> a.getKey() != null)
      .map(a -> new AttributeTableItem(a.getKey(), a.getValue()))
      .sorted(Comparator.comparing(AttributeTableItem::getKey))
      .collect(Collectors.toList());

    attributeTable.setValues(attributes);

    myVerticalLayout.setSelected(settings.isVerticalSplit());
    myHorizontalLayout.setSelected(!settings.isVerticalSplit());
    myEditorLeft.setSelected(settings.isVerticalSplit() && settings.isEditorFirst());
    myEditorRight.setSelected(settings.isVerticalSplit() && !settings.isEditorFirst());
    myEditorTop.setSelected(!settings.isVerticalSplit() && settings.isEditorFirst());
    myEditorBottom.setSelected(!settings.isVerticalSplit() && !settings.isEditorFirst());

    myVerticalLayout.addActionListener(e -> adjustSplitOption());
    myHorizontalLayout.addActionListener(e -> adjustSplitOption());

    adjustSplitOption();

    myEnableInjections.setSelected(settings.isEnabledInjections());

    myEnableInjections.addItemListener(e -> {
      myDisableLanguageInjection.setVisible(myEnableInjections.isSelected());
    });
    myDisableLanguageInjection.setVisible(myEnableInjections.isSelected());

    myLanguageForPassthrough.setText(settings.getLanguageForPassthrough());

    myDisabledInjectionsByLanguage.setText(settings.getDisabledInjectionsByLanguage());

    myShowAsciiDocWarningsAndErrorsInEditor.setSelected(settings.isShowAsciiDocWarningsAndErrorsInEditor());

    myInplacePreviewRefresh.setSelected(settings.isInplacePreviewRefresh());

    myEnableKroki.setSelected(settings.isKrokiEnabled());

    myKrokiUrl.setText(settings.getKrokiUrl());

    myEnableKroki.addItemListener(e -> {
      adjustKrokiOptions();
    });

    adjustKrokiOptions();

    myEnabledAttributeFolding.setSelected(settings.isAttributeFoldingEnabled());

    myKrokiUrl.setTextToTriggerEmptyTextStatus("https://kroki.io");

  }

  @NotNull
  @Override
  public AsciiDocPreviewSettings getAsciiDocPreviewSettings() {
    if (myPreviewPanelModel.getSelected() == null) {
      throw new IllegalStateException("Should be selected always");
    }

    Map<String, String> attributes = attributeTable.getTableView().getItems().stream()
      .filter(a -> a.getKey() != null && a.getValue() != null)
      .collect(Collectors.toMap(AttributeTableItem::getKey, AttributeTableItem::getValue, (a, b) -> b));

    String krokiUrl = myKrokiUrl.getText();
    if ("https://kroki.io".equals(krokiUrl)) {
      krokiUrl = "";
    }

    return new AsciiDocPreviewSettings(mySplitLayoutModel.getSelectedItem(),
      myPreviewPanelModel.getSelected(), myPreviewThemeModel.getSelectedItem(),  mySafeModeModel.getSelectedItem(), attributes,
      myVerticalLayout.isSelected(), myEditorTop.isSelected() || myEditorLeft.isSelected(), myEnableInjections.isSelected(),
      myLanguageForPassthrough.getText(), myDisabledInjectionsByLanguage.getText(),
      myShowAsciiDocWarningsAndErrorsInEditor.isSelected(), myInplacePreviewRefresh.isSelected(),
      myEnableKroki.isSelected(), krokiUrl, myEnabledAttributeFolding.isSelected());
  }
}
