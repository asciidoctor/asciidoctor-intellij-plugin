package org.asciidoc.intellij.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.components.fields.IntegerField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.awt.event.ItemEvent.SELECTED;
import static java.lang.Integer.MAX_VALUE;
import static org.asciidoc.intellij.ui.OptionsPanelFactory.createOptionPanelWithButtonGroup;

public final class PasteImageDialog extends DialogWrapper {

  private static final String DIALOG_TITLE = "Import Image File from Clipboard";
  private static final String DIALOG_ACTION_DESCRIPTION =
    "Would you like to copy the image file or only import a reference?";
  private static final int MINIMUM_IMAGE_WIDTH = 5;

  private final ButtonGroup buttonGroup;
  private final JPanel optionsPanel;
  private final IntegerField widthInputField;
  private final JCheckBox includeWidthCheckbox;

  public static PasteImageDialog create(final List<Action> options,
                                        final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    Pair<JPanel, ButtonGroup> optionPanelWithButtonGroup = createOptionPanelWithButtonGroup(options);

    return new PasteImageDialog(
      optionPanelWithButtonGroup.getSecond(),
      optionPanelWithButtonGroup.getFirst(),
      new IntegerField("imageWidth", MINIMUM_IMAGE_WIDTH, MAX_VALUE),
      new JCheckBox("Include width", false),
      initialWidthFuture
    );
  }

  private PasteImageDialog(final ButtonGroup buttonGroup,
                           final JPanel optionsPanel,
                           final IntegerField widthInputField,
                           final JCheckBox includeWidthCheckbox,
                           final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    super(false);
    this.buttonGroup = buttonGroup;
    this.optionsPanel = optionsPanel;
    this.widthInputField = widthInputField;
    this.includeWidthCheckbox = includeWidthCheckbox;
    setupDialog(initialWidthFuture);
  }

  private void setupDialog(final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    setTitle(DIALOG_TITLE);
    setResizable(false);
    setupIncludeWidthCheckbox();
    setupWidthInputField(initialWidthFuture);
    init();
  }

  private void setupIncludeWidthCheckbox() {
    includeWidthCheckbox.setEnabled(false);
    includeWidthCheckbox.setToolTipText("Enabled once width has been loaded");
    includeWidthCheckbox.addItemListener(this::enableWidthFieldOnSelect);
  }

  private void enableWidthFieldOnSelect(ItemEvent event) {
    widthInputField.setEnabled(event.getStateChange() == SELECTED);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void setupWidthInputField(final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    widthInputField.setEnabled(false);
    widthInputField.setToolTipText("Set image width in pixel (minimum value 5)");

    initialWidthFuture.thenAccept(initialWidth -> {
      initialWidth.ifPresent(widthInputField::setValue);
      includeWidthCheckbox.setEnabled(true);
      includeWidthCheckbox.setToolTipText(null);
    });
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(3, 0));

    panel.add(new JLabel(DIALOG_ACTION_DESCRIPTION));
    panel.add(optionsPanel);
    panel.add(createWidthPanel());

    return panel;
  }

  @NotNull
  private JPanel createWidthPanel() {
    JPanel widthPanel = new JPanel(new GridLayout(1, 2));

    widthPanel.add(includeWidthCheckbox);
    widthPanel.add(widthInputField);

    return widthPanel;
  }

  public String getSelectedActionCommand() {
    return buttonGroup.getSelection().getActionCommand();
  }

  @Override
  protected boolean postponeValidation() {
    return false;
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    if (widthInputField.isEnabled()) {
      try {
        widthInputField.validateContent();
      } catch (ConfigurationException e) {
        return new ValidationInfo(e.getMessage().substring(widthInputField.getValueName() != null ? widthInputField.getValueName().length() + 1 : 0), widthInputField);
      }
    }
    return super.doValidate();
  }

  public Optional<Integer> getWidth() {
    return includeWidthCheckbox.isSelected()
      ? Optional.of(widthInputField.getValue())
      : Optional.empty();
  }
}
