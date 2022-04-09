package org.asciidoc.intellij.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.containers.ContainerUtil;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// these methods and attributes are necessary for this form as it is augmented at compile and run time
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
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
  private JFormattedTextField myZoom;
  private JPanel myZoomSettings;
  private JBCheckBox myHideErrorsInSourceBlocks;
  private JBTextField myHideErrorsByLanguage;
  private JBLabel myHideErrorsByLanguageLabel;
  private LinkLabel<?> myDownloadDependenciesLink;
  private ContextHelpLabel myDownloadDependenciesHelp;
  private JPanel myDownloadDependenciesPanel;
  private JBLabel myDownloadDependenciesComplete;
  private JPanel myDownloadDependenciesFailedDiagram;
  private LinkLabel<?> myDownloadDependenciesFailedDiagramBrowser;
  private LinkLabel<?> myDownloadDependenciesFailedDiagramPickFile;
  private LinkLabel<?> myDownloadDependenciesFailedPdfBrowser;
  private LinkLabel<?> myDownloadDependenciesFailedPdfPickFile;
  private JPanel myDownloadDependenciesFailedPdf;
  private JPanel myDownloadDependenciesFailedPlantuml;
  private LinkLabel<?> myDownloadDependenciesFailedPlantumlBrowser;
  private LinkLabel<?> myDownloadDependenciesFailedPlantumlPickFile;
  private JPanel myDownloadDependenciesFailedDitaamini;
  private LinkLabel<?> myDownloadDependenciesFailedDitaaminiPickFile;
  private LinkLabel<?> myDownloadDependenciesFailedDitaaminiBrowser;

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
      adjustZoomOptions();
    });

    attributeTable = new AttributeTable();
    attributesPanel = new JPanel(new BorderLayout());
    attributesPanel.add(attributeTable.getComponent(), BorderLayout.CENTER);
    // from 2020.1 onwards the attributes panel requires a width, otherwise it will have a zero width
    attributesPanel.setMinimumSize(new Dimension(400, 100));
    myDownloadDependenciesHelp = ContextHelpLabel.create(
      AsciiDocBundle.message(
        "asciidoc.download.folderandcontents",
        (AsciiDocDownloaderUtil.getAsciidoctorJDiagramFile().getName() + ", " + AsciiDocDownloaderUtil.getAsciidoctorJDiagramPlantumlFile().getName() + ", " + AsciiDocDownloaderUtil.getAsciidoctorJDiagramDitaaminiFile().getName() + " and " + AsciiDocDownloaderUtil.getAsciidoctorJPdfFile().getName()),
        AsciiDocDownloaderUtil.DOWNLOAD_PATH)
    );
  }

  private void adjustKrokiOptions() {
    if (myEnableKroki.isSelected()) {
      myKrokiUrlPanel.setVisible(true);
    } else {
      myKrokiUrlPanel.setVisible(false);
    }
  }

  private void adjustZoomOptions() {
    if (myPreviewPanelModel.getSelected() != null &&
      (myPreviewPanelModel.getSelected().getName().contains("JavaFX") ||
        myPreviewPanelModel.getSelected().getName().contains("JCEF"))) {
      myZoomSettings.setVisible(true);
    } else {
      myZoomSettings.setVisible(false);
    }
  }

  private void adjustDownloadDependenciesOptions() {
    if (AsciiDocDownloaderUtil.downloadComplete()) {
      myDownloadDependenciesComplete.setVisible(true);
      myDownloadDependenciesLink.setVisible(false);
      myDownloadDependenciesHelp.setVisible(false);
      myDownloadDependenciesFailedDiagram.setVisible(false);
      myDownloadDependenciesFailedPlantuml.setVisible(false);
      myDownloadDependenciesFailedDitaamini.setVisible(false);
      myDownloadDependenciesFailedPdf.setVisible(false);
    } else {
      myDownloadDependenciesComplete.setVisible(false);
      myDownloadDependenciesLink.setVisible(true);
      myDownloadDependenciesHelp.setVisible(true);
      myDownloadDependenciesFailedDiagram.setVisible(!AsciiDocDownloaderUtil.getAsciidoctorJDiagramFile().exists());
      myDownloadDependenciesFailedPlantuml.setVisible(!AsciiDocDownloaderUtil.getAsciidoctorJDiagramPlantumlFile().exists());
      myDownloadDependenciesFailedDitaamini.setVisible(!AsciiDocDownloaderUtil.getAsciidoctorJDiagramDitaaminiFile().exists());
      myDownloadDependenciesFailedPdf.setVisible(!AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJPdf());
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
      myHideErrorsInSourceBlocks.setVisible(myEnableInjections.isSelected());
    });
    myDisableLanguageInjection.setVisible(myEnableInjections.isSelected());
    myHideErrorsInSourceBlocks.setVisible(myEnableInjections.isSelected());

    myLanguageForPassthrough.setText(settings.getLanguageForPassthrough());

    myDisabledInjectionsByLanguage.setText(settings.getDisabledInjectionsByLanguage());

    myShowAsciiDocWarningsAndErrorsInEditor.setSelected(settings.isShowAsciiDocWarningsAndErrorsInEditor());

    myInplacePreviewRefresh.setSelected(settings.isInplacePreviewRefresh());

    myEnableKroki.setSelected(settings.isKrokiEnabled());

    myKrokiUrl.setText(settings.getKrokiUrl());

    myEnableKroki.addItemListener(e -> adjustKrokiOptions());

    adjustKrokiOptions();

    myEnabledAttributeFolding.setSelected(settings.isAttributeFoldingEnabled());

    myKrokiUrl.setTextToTriggerEmptyTextStatus("https://kroki.io");

    NumberFormat rateFormat = NumberFormat.getNumberInstance();
    rateFormat.setMinimumFractionDigits(0);
    ((DecimalFormat) rateFormat).setParseBigDecimal(true);
    rateFormat.setGroupingUsed(false);
    myZoom.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(rateFormat)));
    myZoom.setValue(BigDecimal.valueOf(settings.getZoom(), 0));
    myHideErrorsInSourceBlocks.setSelected(settings.isHideErrorsInSourceBlocks());
    myHideErrorsInSourceBlocks.addItemListener(e -> {
      myHideErrorsByLanguage.setVisible(!myHideErrorsInSourceBlocks.isSelected());
      myHideErrorsByLanguageLabel.setVisible(!myHideErrorsInSourceBlocks.isSelected());
    });
    myHideErrorsByLanguage.setVisible(!myHideErrorsInSourceBlocks.isSelected());
    myHideErrorsByLanguageLabel.setVisible(!myHideErrorsInSourceBlocks.isSelected());

    myHideErrorsByLanguage.setText(settings.getHideErrorsByLanguage());

    myDownloadDependenciesLink.setListener((source, data) -> {
      AsciiDocDownloaderUtil.download(null, this::adjustDownloadDependenciesOptions, throwable -> {
        if (!AsciiDocDownloaderUtil.getAsciidoctorJDiagramFile().exists()) {
          myDownloadDependenciesFailedDiagram.setVisible(true);
        }
        if (!AsciiDocDownloaderUtil.getAsciidoctorJDiagramPlantumlFile().exists()) {
          myDownloadDependenciesFailedPlantuml.setVisible(true);
        }
        if (!AsciiDocDownloaderUtil.getAsciidoctorJDiagramDitaaminiFile().exists()) {
          myDownloadDependenciesFailedDitaamini.setVisible(true);
        }
        if (!AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJPdf()) {
          myDownloadDependenciesFailedPdf.setVisible(true);
        }
      });
    }, null);

    myDownloadDependenciesFailedDiagramBrowser.setListener((source, data) -> {
      BrowserUtil.browse(AsciiDocDownloaderUtil.getAsciidoctorJDiagramUrl());
    }, null);

    myDownloadDependenciesFailedDiagramPickFile.setListener((source, data) -> {
      AsciiDocDownloaderUtil.pickAsciidoctorJDiagram(null, this::adjustDownloadDependenciesOptions, throwable ->
        myDownloadDependenciesFailedDiagramPickFile.setText("Pick failed: " + throwable.getMessage()));
    }, null);

    myDownloadDependenciesFailedPlantumlBrowser.setListener((source, data) -> {
      BrowserUtil.browse(AsciiDocDownloaderUtil.getAsciidoctorJDiagramPlantumlUrl());
    }, null);

    myDownloadDependenciesFailedPlantumlPickFile.setListener((source, data) -> {
      AsciiDocDownloaderUtil.pickAsciidoctorJDiagramPlantuml(null, this::adjustDownloadDependenciesOptions, throwable ->
        myDownloadDependenciesFailedPlantumlPickFile.setText("Pick failed: " + throwable.getMessage()));
    }, null);

    myDownloadDependenciesFailedDitaaminiBrowser.setListener((source, data) -> {
      BrowserUtil.browse(AsciiDocDownloaderUtil.getAsciidoctorJDiagramDitaaminiUrl());
    }, null);

    myDownloadDependenciesFailedDitaaminiPickFile.setListener((source, data) -> {
      AsciiDocDownloaderUtil.pickAsciidoctorJDiagramDitaamini(null, this::adjustDownloadDependenciesOptions, throwable ->
        myDownloadDependenciesFailedDitaaminiPickFile.setText("Pick failed: " + throwable.getMessage()));
    }, null);

    myDownloadDependenciesFailedPdfBrowser.setListener((source, data) -> {
      BrowserUtil.browse(AsciiDocDownloaderUtil.getAsciidoctorJPdfUrl());
    }, null);

    myDownloadDependenciesFailedPdfPickFile.setListener((source, data) -> {
      AsciiDocDownloaderUtil.pickAsciidoctorJPdf(null, this::adjustDownloadDependenciesOptions, throwable ->
        myDownloadDependenciesFailedPdfPickFile.setText("Pick failed: " + throwable.getMessage()));
    }, null);

    adjustDownloadDependenciesOptions();
    myDownloadDependenciesFailedDiagram.setVisible(false);
    myDownloadDependenciesFailedPlantuml.setVisible(false);
    myDownloadDependenciesFailedDitaamini.setVisible(false);
    myDownloadDependenciesFailedPdf.setVisible(false);
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
      myPreviewPanelModel.getSelected(), myPreviewThemeModel.getSelectedItem(), mySafeModeModel.getSelectedItem(), attributes,
      myVerticalLayout.isSelected(), myEditorTop.isSelected() || myEditorLeft.isSelected(), myEnableInjections.isSelected(),
      myLanguageForPassthrough.getText(), myDisabledInjectionsByLanguage.getText(),
      myShowAsciiDocWarningsAndErrorsInEditor.isSelected(), myInplacePreviewRefresh.isSelected(),
      myEnableKroki.isSelected(), krokiUrl, myEnabledAttributeFolding.isSelected(),
      getZoom(),
      myHideErrorsInSourceBlocks.isSelected(), myHideErrorsByLanguage.getText());
  }

  private int getZoom() {
    Object value = myZoom.getValue();
    // if a user enters infinity as a value, a Double is returned.
    // a user also managed to have a null value returned.
    // Therefore default to 100 in these cases.
    if (!(value instanceof BigDecimal)) {
      return 100;
    }
    return ((BigDecimal) value).setScale(0, RoundingMode.UP).unscaledValue().intValue();
  }
}
