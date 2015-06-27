package org.asciidoc.intellij.toolbar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/** inspired by {@link com.intellij.ui.EditorNotificationPanel} */
public class AsciiDocToolbarPanel extends JPanel implements Disposable {

  protected static final String ASCII_DOC_ACTION_GROUP_ID = "AsciiDoc.TextFormatting";

  private Editor myEditor;
  private final JPanel myLinksPanel;

  public AsciiDocToolbarPanel(Editor editor) {
    super(new BorderLayout());
    myEditor = editor;
    myEditor.putUserData(AsciiDocToolbarLoaderComponent.ASCII_DOC_TOOLBAR, this);

    myLinksPanel = new JPanel(new FlowLayout());
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
    panel.add("West", this.myLinksPanel);
    panel.setMinimumSize(new Dimension(0, 0));
    this.add("Center", panel);

    ActionGroup anAction = (ActionGroup)ActionManager.getInstance().getAction(ASCII_DOC_ACTION_GROUP_ID);
    addChildren(anAction);
  }

  private void addChildren(@NotNull ActionGroup anAction) {
    AnAction[] children = anAction.getChildren(null);
    for (AnAction child : children) {
      if (child instanceof ActionGroup) {
        addChildren(((ActionGroup)child));
      }
      else if (child instanceof Separator) {
      }
      else {
        createActionLabel(child);
      }
    }
  }

  public HyperlinkLabel createActionLabel(@NotNull final AnAction anAction) {
    Icon icon = anAction.getTemplatePresentation().getIcon();
    HyperlinkLabel label = new HyperlinkLabel(icon == null ? anAction.getTemplatePresentation().getText() : "");
    if (icon != null) {
      label.setIcon(icon);
      label.setUseIconAsLink(true);
    }
    label.addHyperlinkListener(new HyperlinkAdapter() {
      protected void hyperlinkActivated(HyperlinkEvent e) {
        executeAction(anAction);
      }
    });
    this.myLinksPanel.add(label);
    return label;
  }

  protected void executeAction(AnAction action) {
    AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(this), "AsciiDocToolbarPanel", action.getTemplatePresentation(), ActionManager.getInstance(), 0);
    action.beforeActionPerformedUpdate(event);
    action.update(event);
    if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
      action.actionPerformed(event);
    }
  }

  @Override
  public void dispose() {
    myEditor.putUserData(AsciiDocToolbarLoaderComponent.ASCII_DOC_TOOLBAR, null);
    myEditor = null;
  }
}
