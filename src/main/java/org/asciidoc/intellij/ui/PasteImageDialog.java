package org.asciidoc.intellij.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.components.JBTextField;
import org.asciidoc.intellij.actions.asciidoc.ImageAttributes;
import org.asciidoc.intellij.ui.components.ImageAttributesPanel;
import org.asciidoc.intellij.ui.components.ImageWidthField;
import org.asciidoc.intellij.ui.components.PasteOptionPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.asciidoc.intellij.ui.OptionsPanelFactory.createOptionPanelWithButtonGroup;

public final class PasteImageDialog extends DialogWrapper implements ImageAttributes {

  private final ButtonGroup buttonGroup;
  private final PasteOptionPanel pasteOptionPanel;
  private final ImageAttributesPanel attributesPanel;

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
      new PasteOptionPanel(dialogActionDescription, optionPanelWithButtonGroup.getFirst()),
      new ImageAttributesPanel(initialWidthFuture),
      optionPanelWithButtonGroup.getSecond()
    );
  }

  private PasteImageDialog(final String dialogTitle,
                           final PasteOptionPanel pasteOptionPanel,
                           final ImageAttributesPanel attributesPanel,
                           final ButtonGroup buttonGroup) {
    super(false);
    this.pasteOptionPanel = pasteOptionPanel;
    this.attributesPanel = attributesPanel;
    this.buttonGroup = buttonGroup;
    setupDialog(dialogTitle);
  }

  private void setupDialog(final String dialogTitle) {
    setTitle(dialogTitle);
    setResizable(false);
    init();
  }

  @Override
  protected @NotNull JComponent createCenterPanel() {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

    panel.add(pasteOptionPanel);
    panel.add(Box.createVerticalStrut(10));
    panel.add(attributesPanel);

    return panel;
  }

  @Override
  protected boolean postponeValidation() {
    return false;
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    final ImageWidthField widthField = attributesPanel.getWidthField();

    if (widthField.isEnabled()) {
      try {
        widthField.validateContent();
      } catch (final ConfigurationException exception) {
        final String errorMessageWithoutFieldNamePrefix = exception
          .getMessage()
          .substring(ImageWidthField.FIELD_NAME.length() + 1);
        return new ValidationInfo(errorMessageWithoutFieldNamePrefix, widthField);
      }
    }
    return super.doValidate();
  }

  public String getSelectedActionCommand() {
    return buttonGroup.getSelection().getActionCommand();
  }

  @Override
  public Optional<Integer> getWidth() {
    final ImageWidthField widthField = attributesPanel.getWidthField();

    return widthField.isEnabled()
      ? widthField.getValueOption()
      : Optional.empty();
  }

  @Override
  public Optional<String> getAlt() {
    final JBTextField altField = attributesPanel.getAltField();

    return altField.isEnabled()
      ? Optional.ofNullable(altField.getText()).filter(alt -> !alt.isEmpty())
      : Optional.empty();
  }
}
