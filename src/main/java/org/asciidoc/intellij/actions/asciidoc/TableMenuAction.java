package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.asciidoc.intellij.ui.TableSizer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A kind of hacky class to display a custom component ({@link TableSizer}) in a menu like way. The rest of menu items
 * is based on declared actions with ids starting by SUB_ACTIONS_PREFIX.
 *
 * @author bbrenne
 */
public class TableMenuAction extends AsciiDocAction {

  public static final String SUB_ACTIONS_PREFIX = "asciidoc.tables.";

  private final JPanel panel = new JPanel();

  private JPanel topPanel;
  private JLabel titleLabel;
  private JLabel sizeLabel;

  private final ArrayList<CustomLabel> items = new ArrayList<>();

  private boolean initialized = false;
  private AnActionEvent event;
  private JBPopup popup;

  public TableMenuAction() {

  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent eve) {
    event = eve;
    if (!initialized) {
      initialized = true;
      initialize(eve.getActionManager());
    }

    resetStyles();

    popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null).createPopup();
    popup.showUnderneathOf(eve.getInputEvent().getComponent());
  }

  private void initialize(ActionManager actionManager) {
    //retrieve data from sub-actions (all actions with id that start with the prefix)
    List<String> ids = actionManager.getActionIdList(SUB_ACTIONS_PREFIX);
    for (String id : ids) {
      AnAction action = actionManager.getAction(id);
      CustomLabel item = new CustomLabel(action.getTemplatePresentation().getText(), action.getTemplatePresentation().getIcon(), SwingConstants.LEFT);
      item.setToolTipText(action.getTemplatePresentation().getDescription());
      item.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          action.actionPerformed(AnActionEvent.createEvent(
            DataManager.getInstance().getDataContext(panel),
            action.getTemplatePresentation(),
            ActionPlaces.EDITOR_TOOLBAR,
            ActionUiKind.TOOLBAR,
            e
          ));
          popup.closeOk(null);
        }
      });
      items.add(item);
    }
    TableSizer tableSizer = new TableSizer();
    tableSizer.addActionListener(e -> {
      if (e instanceof TableSizer.CreateTableActionEvent ctae) {
        final Project project = event.getProject();
        if (project == null) {
          return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
          return;
        }
        String table = CreateTableAction.generateTable(ctae.getWidth(), ctae.getHeight(), "");
        final Document document = editor.getDocument();
        final int offset = editor.getCaretModel().getOffset();
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                document.insertString(offset, table);
              }
            });
          }
        }, null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
        popup.closeOk(null);
      }
    });
    titleLabel = new JLabel(getTemplatePresentation().getText());
    sizeLabel = new JLabel("");
    tableSizer.setSizeDisplay(sizeLabel);

    // layout items
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.removeAll();
    topPanel = new JPanel(new BorderLayout());
    topPanel.setOpaque(true);
    topPanel.add(titleLabel, BorderLayout.WEST);
    topPanel.add(sizeLabel, BorderLayout.EAST);

    panel.add(topPanel);
    panel.add(tableSizer);
    JPanel itemsPanel = new JPanel(new GridLayout(items.size(), 1));
    for (CustomLabel item : items) {
      itemsPanel.add(item);
    }
    panel.add(itemsPanel);
  }

  private void resetStyles() {
    //To avoid wrong color when changing theme
    for (CustomLabel item : items) {
      item.setForeground(UIManager.getColor("Menu.foreground"));
      item.setBackground(UIManager.getColor("Menu.background"));
      item.setBorder(new EmptyBorder(UIManager.getInsets("MenuItem.margin")));
    }
    titleLabel.setFont(UIManager.getFont("MenuItem.font"));
    titleLabel.setForeground(UIManager.getColor("Menu.foreground"));
    sizeLabel.setFont(UIManager.getFont("MenuItem.font"));
    sizeLabel.setForeground(UIManager.getColor("Menu.foreground"));
    topPanel.setBorder(new EmptyBorder(UIManager.getInsets("MenuItem.margin")));
    topPanel.setBackground(UIManager.getColor("Menu.background"));
  }


  private static class CustomLabel extends JLabel {
    CustomLabel(@NotNull String text, Icon icon, int horizontalAlignment) {
      super(text, icon, horizontalAlignment);
      setOpaque(true);
      addMouseListener(new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent e) {
          setForeground(UIManager.getColor("Menu.selectionForeground"));
          setBackground(UIManager.getColor("Menu.selectionBackground"));
        }

        @Override
        public void mouseExited(MouseEvent e) {
          setForeground(UIManager.getColor("Menu.foreground"));
          setBackground(UIManager.getColor("Menu.background"));
        }
      });
    }
  }
}
