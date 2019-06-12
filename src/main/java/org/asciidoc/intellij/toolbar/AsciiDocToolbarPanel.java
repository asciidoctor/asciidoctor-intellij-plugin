package org.asciidoc.intellij.toolbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ui.JBEmptyBorder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/** inspired by {@link com.intellij.ui.EditorNotificationPanel}. */
public class AsciiDocToolbarPanel extends JPanel implements Disposable {

  private static final String ASCII_DOC_ACTION_GROUP_ID = "AsciiDoc.TextFormatting";

  private Editor myEditor;

  public AsciiDocToolbarPanel(Editor editor, @NotNull final JComponent targetComponentForActions) {
    super(new BorderLayout());
    myEditor = editor;
    myEditor.putUserData(AsciiDocToolbarLoaderComponent.ASCII_DOC_TOOLBAR, this);

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
    final ActionGroup group = ((ActionGroup)actionManager.getAction(groupId));
    final ActionToolbarImpl editorToolbar =
      ((ActionToolbarImpl)actionManager.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true));
    editorToolbar.setOpaque(false);
    editorToolbar.setBorder(new JBEmptyBorder(0, 2, 0, 2));

    return editorToolbar;
  }


  @Override
  public void dispose() {
    myEditor.putUserData(AsciiDocToolbarLoaderComponent.ASCII_DOC_TOOLBAR, null);
    myEditor = null;
  }
}
