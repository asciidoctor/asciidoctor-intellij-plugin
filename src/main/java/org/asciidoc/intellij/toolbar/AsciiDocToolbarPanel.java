package org.asciidoc.intellij.toolbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ui.JBEmptyBorder;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.actions.asciidoc.TableMenuAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static org.asciidoc.intellij.actions.asciidoc.TableMenuAction.SUB_ACTIONS_PREFIX;

/**
 * inspired by {@link com.intellij.ui.EditorNotificationPanel}.
 */
public class AsciiDocToolbarPanel extends JPanel implements Disposable {

  private static final String ASCII_DOC_ACTION_GROUP_ID = "AsciiDoc.TextFormatting";

  private Editor myEditor;

  public AsciiDocToolbarPanel(Editor editor, @NotNull final JComponent targetComponentForActions) {
    super(new BorderLayout());
    myEditor = editor;

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

    final DefaultActionGroup group = (DefaultActionGroup) actionManager.getAction(groupId);
    AnAction[] children = group.getChildren(null);

    //Create new group of actions without actions starting by SUB_ACTIONS_PREFIX.
    String[] actionIds = actionManager.getActionIds(SUB_ACTIONS_PREFIX);
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
    final ActionToolbarImpl editorToolbar =
      ((ActionToolbarImpl) actionManager.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true));
    editorToolbar.setOpaque(false);
    editorToolbar.setBorder(new JBEmptyBorder(0, 2, 0, 2));

    return editorToolbar;
  }


  @Override
  public void dispose() {
    myEditor = null;
  }
}
