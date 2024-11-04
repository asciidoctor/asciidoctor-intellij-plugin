package org.asciidoc.intellij.toolbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ui.JBEmptyBorder;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.actions.asciidoc.TableMenuAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.asciidoc.intellij.actions.asciidoc.TableMenuAction.SUB_ACTIONS_PREFIX;

/**
 * inspired by {@link com.intellij.ui.EditorNotificationPanel}.
 */
public class AsciiDocToolbarPanel extends JPanel {

  private static final String ASCII_DOC_ACTION_GROUP_ID = "AsciiDoc.TextFormatting";

  public AsciiDocToolbarPanel(Editor editor, @NotNull final JComponent targetComponentForActions) {
    super(new BorderLayout());

    JPanel myLinksPanel = new JPanel(new FlowLayout());
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
    panel.add("West", myLinksPanel);
    panel.setMinimumSize(new Dimension(0, 0));
    this.add("Center", panel);

    final ActionToolbar toolbar = createToolbarFromGroupId(ASCII_DOC_ACTION_GROUP_ID);
    toolbar.setTargetComponent(targetComponentForActions);
    panel.add(toolbar.getComponent());

  }

  @NotNull
  private static ActionToolbar createToolbarFromGroupId(@NotNull String groupId) {
    final ActionManager actionManager = ActionManager.getInstance();

    if (!actionManager.isGroup(groupId)) {
      throw new IllegalStateException(groupId + " should have been a group");
    }

    DefaultActionGroup toolbarGroup = new DefaultActionGroup();

    final ActionGroup group = (ActionGroup) actionManager.getAction(groupId);
    @SuppressWarnings("RedundantCast") // needed for 2024.2
    AnAction[] children = group.getChildren((AnActionEvent) null);

    //Create new group of actions without actions starting by SUB_ACTIONS_PREFIX.
    List<String> actionIds = actionManager.getActionIdList(SUB_ACTIONS_PREFIX);
    ArrayList<AnAction> exclusions = new ArrayList<>();
    for (String actionId : actionIds) {
      exclusions.add(actionManager.getAction(actionId));
    }
    for (AnAction child : children) {
      if (!exclusions.contains(child)) {
        toolbarGroup.addAction(child);
      }
    }

    TableMenuAction tableMenuAction = new TableMenuAction();
    tableMenuAction.getTemplatePresentation().setDescription("Create table");
    tableMenuAction.getTemplatePresentation().setText("Create table");
    tableMenuAction.getTemplatePresentation().setIcon(AsciiDocIcons.EditorActions.TABLE);
    toolbarGroup.addAction(tableMenuAction);
    final ActionToolbar editorToolbar =
      actionManager.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true);
    JComponent component = editorToolbar.getComponent();
    component.setOpaque(false);
    component.setBorder(new JBEmptyBorder(0, 2, 0, 2));

    return editorToolbar;
  }

}
