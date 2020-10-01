package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import groovy.lang.Tuple2;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.asciidoc.intellij.ui.FormatterFactory.createIntegerFormatter;
import static org.asciidoc.intellij.ui.OptionsPanelFactory.createOptionPanelWithButtonGroup;

public final class PasteImageDialog extends DialogWrapper {

  private static final String DIALOG_TITLE = "Import Image File from Clipboard";
  private static final String DIALOG_ACTION_DESCRIPTION =
    "Would you like to copy the image file or only import a reference?";

  private final ButtonGroup buttonGroup;
  private final JPanel optionsPanel;
  private final JFormattedTextField widthInputField;
  private final JCheckBox includeWidthCheckbox;

  public static PasteImageDialog create(final List<Action> options,
                                        final CompletableFuture<Integer> initialWidthFuture) {
    Tuple2<JPanel, ButtonGroup> optionPanelWithButtonGroup = createOptionPanelWithButtonGroup(options);

    return new PasteImageDialog(
      optionPanelWithButtonGroup.getSecond(),
      optionPanelWithButtonGroup.getFirst(),
      new JFormattedTextField(createIntegerFormatter()),
      new JCheckBox("Include width", false),
      initialWidthFuture
    );
  }

  private PasteImageDialog(final ButtonGroup buttonGroup,
                           final JPanel optionsPanel,
                           final JFormattedTextField widthInputField,
                           final JCheckBox includeWidthCheckbox,
                           final CompletableFuture<Integer> initialWidthFuture) {
    super(false);
    this.buttonGroup = buttonGroup;
    this.optionsPanel = optionsPanel;
    this.widthInputField = widthInputField;
    this.includeWidthCheckbox = includeWidthCheckbox;
    setupDialog(initialWidthFuture);
  }

  private void setupDialog(final CompletableFuture<Integer> initialWidthFuture) {
    setTitle(DIALOG_TITLE);
    setResizable(false);
    setupIncludeWidthCheckbox();
    setupWidthInputField(initialWidthFuture);
    init();
  }

  private void setupIncludeWidthCheckbox() {
    includeWidthCheckbox.addItemListener(this::enableWidthFieldOnSelect);
    includeWidthCheckbox.setToolTipText("Enabled once width has been loaded");
    includeWidthCheckbox.setEnabled(false);
  }

  private void enableWidthFieldOnSelect(ItemEvent event) {
    widthInputField.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void setupWidthInputField(final CompletableFuture<Integer> initialWidthFuture) {
    widthInputField.setEnabled(false);
    widthInputField.setToolTipText("Set image width in pixel");

    initialWidthFuture.thenAccept(initialWidth -> {
      widthInputField.setValue(initialWidth);
      includeWidthCheckbox.setEnabled(true);
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

  public Optional<Integer> getWidth() {
    return includeWidthCheckbox.isSelected()
      ? Optional.of((int) widthInputField.getValue())
      : Optional.empty();
  }
}
