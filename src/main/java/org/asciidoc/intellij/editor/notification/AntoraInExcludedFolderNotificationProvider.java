package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Notify a user that an Antora component is in an excluded folder, as the IDE doesn't index it.
 */
public class AntoraInExcludedFolderNotificationProvider implements EditorNotificationProvider, DumbAware {

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    if (!ProjectRootManager.getInstance(project).getFileIndex().isExcluded(file)) {
      return null;
    }
    if (AsciiDocUtil.findAntoraModuleDir(project, file) == null) {
      return null;
    }
    return fileEditor -> {
      final EditorNotificationPanel panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning);
      panel.setText("This Antora component is in an excluded folder and is not indexed. "
        + "References to other Antora components, search and refactoring might not work.");
      panel.createActionLabel("Read more...", ()
        -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/features/advanced/antora.html#antora-excluded-folders"));
      return panel;
    };
  }
}
