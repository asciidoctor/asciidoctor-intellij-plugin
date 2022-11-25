package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.asciidoc.intellij.editor.jcef.AsciiDocJCEFHtmlPanel;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;

public class OpenDevtoolsAction extends AsciiDocAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    SplitFileEditor<?, ?> fileEditor = AsciiDocActionUtil.findSplitEditor(event);
    if (fileEditor instanceof AsciiDocSplitEditor) {
      AsciiDocHtmlPanel panel = ((AsciiDocSplitEditor) fileEditor).getSecondEditor().getPanel();
      if (panel instanceof AsciiDocJCEFHtmlPanel) {
        AsciiDocJCEFHtmlPanel asciiDocJCEFHtmlPanel = (AsciiDocJCEFHtmlPanel) panel;
        if (!asciiDocJCEFHtmlPanel.isDisposed()) {
          asciiDocJCEFHtmlPanel.openDevtools();
        }
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    boolean visible = false;
    SplitFileEditor<?, ?> fileEditor = AsciiDocActionUtil.findSplitEditor(event);
    if (fileEditor instanceof AsciiDocSplitEditor) {
      AsciiDocHtmlPanel panel = ((AsciiDocSplitEditor) fileEditor).getSecondEditor().getPanel();
      if (panel instanceof AsciiDocJCEFHtmlPanel) {
        visible = true;
      }
    }
    event.getPresentation().setVisible(visible);
  }
}
