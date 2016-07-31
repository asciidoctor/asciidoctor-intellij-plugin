package org.asciidoc.intellij.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.EnumComboBoxModel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class AsciiDocPreviewSettingsForm implements AsciiDocPreviewSettings.Holder {
  private Object myLastItem;
  private ComboBox myPreviewProvider;
  private ComboBox myDefaultSplitLayout;
  private JPanel myMainPanel;
  private EnumComboBoxModel<SplitFileEditor.SplitEditorLayout> mySplitLayoutModel;
  private CollectionComboBoxModel<AsciiDocHtmlPanelProvider.ProviderInfo> myPreviewPanelModel;

  public JComponent getComponent() {
    return myMainPanel;
  }

  private void createUIComponents() {
    //noinspection unchecked
    final List<AsciiDocHtmlPanelProvider.ProviderInfo> providerInfos =
        ContainerUtil.mapNotNull(AsciiDocHtmlPanelProvider.getProviders(),
            new Function<AsciiDocHtmlPanelProvider, AsciiDocHtmlPanelProvider.ProviderInfo>() {
              @Override
              public AsciiDocHtmlPanelProvider.ProviderInfo fun(AsciiDocHtmlPanelProvider provider) {
                if (provider.isAvailable() == AsciiDocHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE) {
                  return null;
                }
                return provider.getProviderInfo();
              }
            });
    myPreviewPanelModel = new CollectionComboBoxModel<AsciiDocHtmlPanelProvider.ProviderInfo>(providerInfos, providerInfos.get(0));
    myPreviewProvider = new ComboBox(myPreviewPanelModel);

    mySplitLayoutModel = new EnumComboBoxModel<SplitFileEditor.SplitEditorLayout>(SplitFileEditor.SplitEditorLayout.class);
    myDefaultSplitLayout = new ComboBox(mySplitLayoutModel);

    myLastItem = myPreviewProvider.getSelectedItem();
    myPreviewProvider.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        final Object item = e.getItem();
        if (e.getStateChange() != ItemEvent.SELECTED || !(item instanceof AsciiDocHtmlPanelProvider.ProviderInfo)) {
          return;
        }

        final AsciiDocHtmlPanelProvider provider = AsciiDocHtmlPanelProvider.createFromInfo((AsciiDocHtmlPanelProvider.ProviderInfo)item);
        final AsciiDocHtmlPanelProvider.AvailabilityInfo availability = provider.isAvailable();

        if (!availability.checkAvailability(myMainPanel)) {
          myPreviewProvider.setSelectedItem(myLastItem);
        }
        else {
          myLastItem = item;
        }
      }
    });
  }

  @Override
  public void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings) {
    if (myPreviewPanelModel.contains(settings.getHtmlPanelProviderInfo())) {
      myPreviewPanelModel.setSelectedItem(settings.getHtmlPanelProviderInfo());
    }
    mySplitLayoutModel.setSelectedItem(settings.getSplitEditorLayout());
  }

  @NotNull
  @Override
  public AsciiDocPreviewSettings getAsciiDocPreviewSettings() {
    if (myPreviewPanelModel.getSelected() == null) {
      throw new IllegalStateException("Should be selected always");
    }
    return new AsciiDocPreviewSettings(mySplitLayoutModel.getSelectedItem(),
        myPreviewPanelModel.getSelected());
  }
}
