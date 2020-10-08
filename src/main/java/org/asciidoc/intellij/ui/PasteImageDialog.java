package org.asciidoc.intellij.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.asciidoc.intellij.ui.OptionsPanelFactory.createOptionPanelWithButtonGroup;

public final class PasteImageDialog extends DialogWrapper {

  private final ImageWidthField widthInputField = new ImageWidthField();
  private final IncludeWidthCheckBox includeWidthCheckbox = new IncludeWidthCheckBox();
  private final String dialogActionDescription;
  private final ButtonGroup buttonGroup;
  private final JPanel optionsPanel;

  public static PasteImageDialog createPasteImageFileDialog(final List<Action> options,
                                                            final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    return create(
      "Import Image File from Clipboard",
      "Would you like to copy the image file or only import a reference?",
      options,
      initialWidthFuture
    );
  }

  public static PasteImageDialog createPasteImageDataDialog(final List<Action> options,
                                                            final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    return create(
      "Import Image Data from Clipboard",
      "Which format do you want the image to be saved to?",
      options,
      initialWidthFuture
    );
  }

  private static PasteImageDialog create(final String dialogTitle,
                                         final String dialogActionDescription,
                                         final List<Action> options,
                                         final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    final Pair<JPanel, ButtonGroup> optionPanelWithButtonGroup = createOptionPanelWithButtonGroup(options);

    return new PasteImageDialog(
      dialogTitle,
      dialogActionDescription,
      initialWidthFuture,
      optionPanelWithButtonGroup.getSecond(),
      optionPanelWithButtonGroup.getFirst()
    );
  }

  private PasteImageDialog(final String dialogTitle,
                           final String dialogActionDescription,
                           final CompletableFuture<Optional<Integer>> initialWidthFuture,
                           final ButtonGroup buttonGroup,
                           final JPanel optionsPanel) {
    super(false);
    this.dialogActionDescription = dialogActionDescription;
    this.buttonGroup = buttonGroup;
    this.optionsPanel = optionsPanel;
    setupDialog(dialogTitle, initialWidthFuture);
  }

  private void setupDialog(final String dialogTitle, final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    setTitle(dialogTitle);
    setResizable(false);
    includeWidthCheckbox.onStateChanged(widthInputField::setEnabled);
    widthInputField.initializeWith(initialWidthFuture, this::enableCheckboxAndRemoveTooltip);
    init();
  }

  private void enableCheckboxAndRemoveTooltip() {
    includeWidthCheckbox.setEnabled(true);
    includeWidthCheckbox.setToolTipText(null);
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new GridLayout(3, 0));

    panel.add(new JLabel(dialogActionDescription));
    panel.add(optionsPanel);
    panel.add(createWidthPanel());

    return panel;
  }

  @NotNull
  private JPanel createWidthPanel() {
    final JPanel widthPanel = new JPanel(new GridLayout(1, 2));

    widthPanel.add(includeWidthCheckbox);
    widthPanel.add(widthInputField);

    return widthPanel;
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
      } catch (final ConfigurationException exception) {
        final String errorMessageWithoutFieldNamePrefix = exception
          .getMessage()
          .substring(ImageWidthField.FIELD_NAME.length() + 1);
        return new ValidationInfo(errorMessageWithoutFieldNamePrefix, widthInputField);
      }
    }
    return super.doValidate();
  }

  public String getSelectedActionCommand() {
    return buttonGroup.getSelection().getActionCommand();
  }

  public Optional<Integer> getWidth() {
    return includeWidthCheckbox.isSelected()
      ? widthInputField.getValueOption()
      : Optional.empty();
  }
}
