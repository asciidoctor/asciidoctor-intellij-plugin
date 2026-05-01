package org.asciidoc.intellij.settings.language;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.execution.ParametersListUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerArbitrary;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForGo;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForJavaScript;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForPowershell;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForPython;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForRuby;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForTypeScript;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocSuggestedParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Configure the script language interpreters.
 */
public class AsciiDocScriptLanguagesForm extends DialogWrapper implements AsciiDocSelectedLanguageSettings {
  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerForJavaScript.class);

  private JTextField additionalParameters;
  private JPanel contentPane;
  private JButton defaultInterpreterPath;
  private JButton defaultUtilPath;
  private JTextPane explanationLabel;
  private JComboBox<LanguageItem> languageSelector;
  private JButton interpreterOpen;
  private JTable parametersTable;
  private JTextField interpreterPath;
  private JLabel utilLabel;
  private JButton utilOpen;
  private JPanel utilPanel;
  private JTextField utilPath;
  private JCheckBox alwaysUseTempFile;
  private JLabel conflictLabel;

  @NotNull
  private final Consumer<AsciiDocScriptLanguageSettings> accepted;
  private final List<LanguageItem> languageItems;
  private int lastSelectionIndex = -1;

  @Nullable
  @Getter
  private AsciiDocLanguageSettingsParam setupLanguageSettingsParam;

  public AsciiDocScriptLanguagesForm(@NotNull Component parentComponent,
                                     @Nullable AsciiDocScriptLanguageSettings scriptLanguageSettings,
                                     @NotNull Consumer<AsciiDocScriptLanguageSettings> accepted) {
    super(parentComponent, true);
    this.accepted = accepted;
    setTitle("Script Language Execution");
    addOpenDialog(interpreterOpen, interpreterPath);
    addOpenDialog(utilOpen, utilPath);

    languageItems = new ArrayList<>(List.of(
      new LanguageItem("Go", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingGo).orElse(null), AsciiDocLanguages.GO),
      new LanguageItem("JavaScript", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingJavaScript).orElse(null), AsciiDocLanguages.JAVA_SCRIPT),
      new LanguageItem("PowerShell", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingPowerShell).orElse(null), AsciiDocLanguages.POWER_SHELL),
      new LanguageItem("Python", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingPython).orElse(null), AsciiDocLanguages.PYTHON),
      new LanguageItem("Ruby", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingRuby).orElse(null), AsciiDocLanguages.RUBY),
      new LanguageItem("TypeScript", Optional.ofNullable(scriptLanguageSettings)
        .map(AsciiDocScriptLanguageSettings::getLanguageSettingTypeScript).orElse(null), AsciiDocLanguages.TYPE_SCRIPT)
    ));
    languageSelector.removeAllItems();
    for (LanguageItem languageItem : languageItems) {
      languageSelector.addItem(languageItem);
    }
    int selectedIndex = Optional.ofNullable(scriptLanguageSettings)
      .map(AsciiDocScriptLanguageSettings::getSelectedLanguage)
      .map(AsciiDocLanguages::getIndex)
      .orElse(0);
    languageSelector.setSelectedIndex(selectedIndex);
    languageSelector.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
        Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof LanguageItem item) {
          setText(item.languageName());
          int iconIndex = index >= 0 ? index : languageSelector.getSelectedIndex();
          Icon icon = getIconForLanguage(iconIndex);
          if (icon != null) {
            setIcon(icon);
          }
        }
        return result;
      }
    });

    TableColumnModel columnModel = buildTableColumnModel();
    parametersTable.setAutoCreateColumnsFromModel(false);
    parametersTable.setColumnModel(columnModel);
    parametersTable.setToolTipText(
      "The \"Version\" is the version the feature was declared completely stable.");

    parametersTable.addMouseListener(
      new TableMouseListener(this, parametersTable, additionalParameters));

    defaultInterpreterPath.addActionListener(
      (ActionEvent actionEvent) -> interpreterPath.setText(setupLanguageSettingsParam.getInterpreter().get()));

    defaultUtilPath.addActionListener(
      (ActionEvent actionEvent) -> {
        if (setupLanguageSettingsParam.getUtil() != null) {
          utilPath.setText(setupLanguageSettingsParam.getUtil().get());
        }
      });

    setupLanguageForIndex();
    languageSelector.addItemListener((ItemEvent itemEvent) -> {
      if (itemEvent.getStateChange() == ItemEvent.DESELECTED) {
        if (lastSelectionIndex < 0) {
          LOG.error("Unexpected state: No language was selected before");
        } else {
          saveSettingsForLanguage(lastSelectionIndex);
        }
      } else if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
        setupLanguageForIndex();
      }
      /* Scrolls automatically to the bottom, if longer than the embedding scrollPane,
      but the user should see of course initially the beginning of the text. */
      SwingUtilities.invokeLater(() -> explanationLabel.scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
      checkIncompatibilities();
    });
    additionalParameters.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        checkIncompatibilities();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        checkIncompatibilities();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        checkIncompatibilities();
      }
    });
    alwaysUseTempFile.addActionListener((ActionEvent actionEvent) -> checkIncompatibilities());
    checkIncompatibilities();

    if (SystemInfo.isWindows) {
      /* Extra different tooltip for Windows, because only that must always use a temporary file for TypeScript.
       * Linux and macOS can use adhoc inline evaluation for TypeScript. */
      alwaysUseTempFile.setToolTipText("Always write the code into a temporary file and then execute that file." +
        " Go & TypeScript must always use temp-files.");
    }

    // DialogWrapper needs explicit initialization to build and attach the center panel.
    init();
  }

  private void checkIncompatibilities() {
    final String text = AsciiDocLanguageCompatibilityCheck.findIncompatibilities(languageSelector.getSelectedIndex(),
      additionalParameters.getText(), alwaysUseTempFile.isSelected());
    conflictLabel.setText(text);
  }

  private static @Nullable AsciiDocScriptLanguageSetting buildScriptSettings(JTextField path, //
                                                                             JTextField utilPath, //
                                                                             JTextField additionalParameters, //
                                                                             JCheckBox alwaysUseTempFile) {
    final AsciiDocScriptLanguageSetting setting;
    if (!StringUtils.isBlank(path.getText())) {
      setting = new AsciiDocScriptLanguageSetting(path.getText(),
        utilPath.getText(),
        getParams(additionalParameters.getText()),
        alwaysUseTempFile.isSelected() ? Boolean.TRUE : null);
    } else {
      setting = null;
    }
    return setting;
  }

  private static @NonNull TableColumnModel buildTableColumnModel() {
    DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
    TableColumn columnParameter = new TableColumn(0, 100);
    columnParameter.setHeaderValue("Parameter");
    columnModel.addColumn(columnParameter);

    TableColumn columnDescription = new TableColumn(1, 430);
    columnDescription.setHeaderValue("Description");
    columnModel.addColumn(columnDescription);

    TableColumn columnMinVersion = new TableColumn(2, 30);
    columnMinVersion.setHeaderValue("Version");
    columnModel.addColumn(columnMinVersion);
    return columnModel;
  }

  private static @Nullable Icon getIconForLanguage(int languageIndex) {
    return switch (languageIndex) {
      case
        AsciiDocLanguages.Indices.INDEX_GO -> // Icon source: https://en.wikipedia.org/wiki/Go_(programming_language)#/media/File:Go_Logo_Blue.svg, it's in public domain.
        IconLoader.getIcon("/icons/languages/Go_Logo_Blue.svg", AsciiDocRunnerArbitrary.class);
      case AsciiDocLanguages.Indices.INDEX_JAVA_SCRIPT -> com.intellij.icons.AllIcons.FileTypes.JavaScript;
      case
        AsciiDocLanguages.Indices.INDEX_POWER_SHELL -> // Source https://commons.wikimedia.org/wiki/File:PowerShell_5.0_icon.png, it's in public domain.
        IconLoader.getIcon("/icons/languages/PowerShell_5.0_icon.png", AsciiDocRunnerArbitrary.class);
      case
        AsciiDocLanguages.Indices.INDEX_PYTHON -> // Source https://www.python.org/community/logos/, its use is generally encouraged.
        IconLoader.getIcon("/icons/languages/python-logo-only.svg", AsciiDocRunnerArbitrary.class);
      case
        AsciiDocLanguages.Indices.INDEX_RUBY -> // Icon source: https://commons.wikimedia.org/wiki/File:Ruby_logo.svg, it's in public domain.
        IconLoader.getIcon("/icons/languages/Ruby_logo.svg", AsciiDocRunnerArbitrary.class);
      case
        AsciiDocLanguages.Indices.INDEX_TYPE_SCRIPT -> // Icon source: https://commons.wikimedia.org/wiki/File:Typescript_logo_2020.svg?uselang=de#Lizenz, it's in public domain.
        IconLoader.getIcon("/icons/languages/Typescript_logo_2020.svg", AsciiDocRunnerArbitrary.class);
      default -> null;
    };
  }

  @Nullable
  private static List<String> getParams(@Nullable String params) {
    return StringUtils.isNotBlank(params) ? ParametersListUtil.parse(params) : null;
  }

  private void addOpenDialog(JButton open, JTextField path) {
    open.addActionListener((ActionEvent actionEvent) -> {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(new File(path.getText()));
      int result = fileChooser.showOpenDialog(contentPane);
      if (result == JFileChooser.APPROVE_OPTION) {
        path.setText(fileChooser.getSelectedFile().getAbsolutePath());
      }
    });
  }

  @Override
  protected void doOKAction() {
    saveSettingsForLanguage(languageSelector.getSelectedIndex());
    accepted.accept(AsciiDocScriptLanguageSettings.builder()
      .languageSettingGo(languageItems.get(AsciiDocLanguages.Indices.INDEX_GO).setting)
      .languageSettingJavaScript(languageItems.get(AsciiDocLanguages.Indices.INDEX_JAVA_SCRIPT).setting)
      .languageSettingPowerShell(languageItems.get(AsciiDocLanguages.Indices.INDEX_POWER_SHELL).setting)
      .languageSettingPython(languageItems.get(AsciiDocLanguages.Indices.INDEX_PYTHON).setting)
      .languageSettingRuby(languageItems.get(AsciiDocLanguages.Indices.INDEX_RUBY).setting)
      .languageSettingTypeScript(languageItems.get(AsciiDocLanguages.Indices.INDEX_TYPE_SCRIPT).setting)
      .selectedLanguage(languageItems.get(languageSelector.getSelectedIndex()).asciiDocLanguage)
      .build());
    super.doOKAction();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  private void saveSettingsForLanguage(int index) {
    LanguageItem deselectedItem = languageSelector.getItemAt(index);
    AsciiDocScriptLanguageSetting setting = buildScriptSettings(interpreterPath, utilPath, additionalParameters,
      alwaysUseTempFile);
    languageItems.set(index,
      new LanguageItem(deselectedItem.languageName, setting, languageItems.get(index).asciiDocLanguage));
  }

  private void setPanelEnabled(JPanel panel, boolean isEnabled) {
    if (panel == null) {
      return;
    }
    panel.setEnabled(isEnabled);
    Component[] components = panel.getComponents();
    for (var component : components) {
      if (component instanceof JPanel componentPanel) {
        setPanelEnabled(componentPanel, isEnabled);
      }
      component.setEnabled(isEnabled);
    }
  }

  private void setUtilPath(boolean enabled, @Nullable String path) {
    utilLabel.setEnabled(enabled);
    setPanelEnabled(utilPanel, enabled);
    utilPath.setText(path);
  }

  private void setupLanguageForIndex() {
    setupLanguageSettings(buildAsciiDocLanguageSettingsParamBuilder().build(), languageSelector.getSelectedIndex());
    lastSelectionIndex = languageSelector.getSelectedIndex();
  }

  private AsciiDocLanguageSettingsParam.AsciiDocLanguageSettingsParamBuilder buildAsciiDocLanguageSettingsParamBuilder() {
    final AsciiDocLanguageSettingsParam.AsciiDocLanguageSettingsParamBuilder builder =
      AsciiDocLanguageSettingsParam.builder();
    return switch (languageSelector.getSelectedIndex()) {
      case AsciiDocLanguages.Indices.INDEX_GO -> builder //
        .getInterpreter(AsciiDocRunnerForGo::findGoInterpreter) //
        .suggestedParameterExtractor(AsciiDocRunnerForGo::suggestedParameters) //
        .explanationText("""
          Execute Go "scripts" with "go run". If the script is run via temp-file, \
          use this to fetch the source-directory:
          import "os" ...
          sourceDir := os.Getenv("adocSourceDir")""")
        .mustUseTemporaryFile(true);
      case AsciiDocLanguages.Indices.INDEX_JAVA_SCRIPT -> builder //
        .getInterpreter(AsciiDocRunnerForJavaScript::findJavaScriptInterpreter) //
        .suggestedParameterExtractor(AsciiDocRunnerForJavaScript::suggestedParameters)
        /* 'file://' prefix is mandatory when run under Windows, irrelevant under Linux.
        * But it's general preferable to have source-code working under every OS. */
        .explanationText("""
          Execute JavaScript with Node.js. If the script is run via temp-file, use this to import JavaScript modules \
          from the source-directory:
          const sourceDir = 'file://' + process.env.adocSourceDir;
          const myJsModule = await import (sourceDir + '/myJsModule.js');""")
        .mustUseTemporaryFile(false);
      case AsciiDocLanguages.Indices.INDEX_POWER_SHELL -> builder //
        .getInterpreter(AsciiDocRunnerForPowershell::findPowerShellInterpreter) //
        .suggestedParameterExtractor(AsciiDocRunnerForPowershell::suggestedParameters) //
        .explanationText("Execute PowerShell scripts. If the script is run via temp-file,"
          + " use \"$env:adocSourceDir\" to get the source-directory.")
        .mustUseTemporaryFile(false);
      case AsciiDocLanguages.Indices.INDEX_PYTHON -> builder //
        .getInterpreter(AsciiDocRunnerForPython::findPythonInterpreter) //
        .suggestedParameterExtractor(AsciiDocRunnerForPython::suggestedParameters) //
        .explanationText("""
          Execute Python scripts. If the script is run via temp-file, \
          use this to import Python modules from the source-directory:
          import os
          sourceDir = os.environ['adocSourceDir']
          import importlib.util
          spec = importlib.util.spec_from_file_location("myModule", sourceDir + '/myModule.py')
          myModule = importlib.util.module_from_spec(spec)
          spec.loader.exec_module(myModule)""")
        .mustUseTemporaryFile(false);
      case AsciiDocLanguages.Indices.INDEX_RUBY -> builder //
        .getInterpreter(AsciiDocRunnerForRuby::findRubyInterpreter) //
        .suggestedParameterExtractor(AsciiDocRunnerForRuby::suggestedParameters) //
        .explanationText("""
          Execute Ruby scripts. If the script is run via temp-file, \
          use this to import Ruby modules from the source-directory:
          sourceDir = ENV['adocSourceDir']
          require sourceDir + '/myModule.rb'""")
        .mustUseTemporaryFile(false);
      case AsciiDocLanguages.Indices.INDEX_TYPE_SCRIPT -> builder //
        .getInterpreter(AsciiDocRunnerForTypeScript::findNodePackageExecutor) //
        .getUtil(SystemInfo.isWindows ? AsciiDocRunnerForTypeScript::findNpxScript : null) //
        .suggestedParameterExtractor(AsciiDocRunnerForTypeScript::suggestedParameters) //
        /* 'file://' prefix is mandatory when run under Windows, irrelevant under Linux.
         * But it's general preferable to have source-code working under every OS. */
        .explanationText(
          """
            Execute TypeScript with Node.js npx tsx. Launching the npx bash script directly under Linux, \
            and using PowerShell with Node.js npx.ps1 tsx under Windows. If the script is run via temp-file, use \
            this to import any files from the source-directory:
            const sourceDir = 'file://' + process.env.adocSourceDir;
            const myModule = await import (sourceDir + '/myModule.ts');""")
        .mustUseTemporaryFile(SystemInfo.isWindows);
      default -> throw new IllegalStateException("Unexpected value: " + languageSelector.getSelectedIndex());
    };
  }

  private void setupLanguageSettings(AsciiDocLanguageSettingsParam setupLanguageSettingsParam, int index) {
    AsciiDocScriptLanguageSetting scriptSetting = languageItems.get(index).setting;
    if (scriptSetting != null) {
      if (StringUtils.isNotBlank(scriptSetting.getInterpreterPath())) {
        interpreterPath.setText(scriptSetting.getInterpreterPath());
        additionalParameters.setText(scriptSetting.expandParameters());
      } else {
        interpreterPath.setText(setupLanguageSettingsParam.getInterpreter().get());
        additionalParameters.setText(null);
      }
      if (StringUtils.isNotBlank(scriptSetting.getUtilPath())) {
        setUtilPath(true, scriptSetting.getUtilPath());
      } else if (setupLanguageSettingsParam.getUtil() != null) {
        setUtilPath(true, setupLanguageSettingsParam.getUtil().get());
      } else {
        setUtilPath(false, null);
      }
      setAlwaysUseTempFile(setupLanguageSettingsParam, Boolean.TRUE.equals(scriptSetting.getAlwaysUseTempFile()));
    } else {
      interpreterPath.setText(setupLanguageSettingsParam.getInterpreter().get());

      if (setupLanguageSettingsParam.getUtil() != null) {
        setUtilPath(true, setupLanguageSettingsParam.getUtil().get());
      } else {
        setUtilPath(false, null);
      }

      setAlwaysUseTempFile(setupLanguageSettingsParam, false);
    }
    /* Format all text without serifs, and after the first linebreak as fixed-width-font
    (because that's where the source-code starts). */
    String explanation = setupLanguageSettingsParam.explanationText();
    String[] explanationParts = explanation.split("\\R", 2);
    String formattedExplanation = explanationParts.length == 2
      ? "<font face=\"sans-serif\">%s<pre>%s</pre></font>".formatted(explanationParts[0], explanationParts[1])
      : "<font face=\"sans-serif\">%s</font>".formatted(explanation);
    explanationLabel.setText("<html>%s</html>".formatted(formattedExplanation));

    List<AsciiDocSuggestedParameter> params = setupLanguageSettingsParam.suggestedParameterExtractor().get();
    parametersTable.setModel(new AsciiDocSuggestedParametersTableModel(params));

    this.setupLanguageSettingsParam = setupLanguageSettingsParam;
  }

  private void setAlwaysUseTempFile(AsciiDocLanguageSettingsParam setupLanguageSettingsParam, boolean selected) {
    if (setupLanguageSettingsParam.mustUseTemporaryFile()) {
      alwaysUseTempFile.setSelected(true);
      alwaysUseTempFile.setEnabled(false);
    } else {
      alwaysUseTempFile.setSelected(selected);
      alwaysUseTempFile.setEnabled(true);
    }
  }

  private static class AsciiDocSuggestedParametersTableModel extends AbstractTableModel {
    @NotNull
    private final List<AsciiDocSuggestedParameter> params;

    AsciiDocSuggestedParametersTableModel(@NotNull List<AsciiDocSuggestedParameter> params) {
      Objects.requireNonNull(params, "params must not be null");
      this.params = params;
    }

    @Override
    public int getRowCount() {
      return params.size();
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      AsciiDocSuggestedParameter param = params.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> param.parameter();
        case 1 -> param.description();
        case 2 -> param.minVersionStable();
        default -> null;
      };
    }
  }

  private record LanguageItem(@NotNull String languageName, @Nullable AsciiDocScriptLanguageSetting setting,
                              @NotNull AsciiDocLanguages asciiDocLanguage)
    implements Serializable {
    @Override
    public @NonNull String toString() {
      return languageName;
    }
  }

  private record TableMouseListener(AsciiDocSelectedLanguageSettings selectedLanguageSettings,
                                    JTable scriptParametersTable,
                                    JTextField additionalParameters) implements MouseListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      int row = scriptParametersTable.rowAtPoint(e.getPoint());
      if (row >= 0 && row < selectedLanguageSettings.getSetupLanguageSettingsParam().suggestedParameterExtractor().get().size()) {
        AsciiDocSuggestedParameter param = selectedLanguageSettings.getSetupLanguageSettingsParam().suggestedParameterExtractor().get().get(
          row);
        String additionalParams = additionalParameters.getText();
        if (StringUtils.isBlank(additionalParams)) {
          additionalParameters.setText(param.parameter());
        } else if (!additionalParameters.getText().contains(param.parameter())) {
          additionalParameters.setText(additionalParams + " " + param.parameter());
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) { // No action needed
    }

    @Override
    public void mouseReleased(MouseEvent e) { // No action needed
    }

    @Override
    public void mouseEntered(MouseEvent e) { // No action needed
    }

    @Override
    public void mouseExited(MouseEvent e) { // No action needed
    }
  }
}
