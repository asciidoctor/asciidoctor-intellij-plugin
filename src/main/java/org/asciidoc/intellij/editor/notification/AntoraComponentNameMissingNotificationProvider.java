package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.function.Function;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_YML;

/**
 * Notify a user that an Antora component name is missing, which is a required field.
 */
public class AntoraComponentNameMissingNotificationProvider implements EditorNotificationProvider, DumbAware {

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    // only in AsciiDoc or Antora files
    Map<String, Object> antora;
    VirtualFile antoraFile;
    if (file.getFileType() == AsciiDocFileType.INSTANCE) {
      VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(project, file);
      if (antoraModuleDir == null || antoraModuleDir.getParent() == null || antoraModuleDir.getParent().getParent() == null) {
        return null;
      }
      antoraFile = antoraModuleDir.getParent().getParent().findChild(ANTORA_YML);
      if (antoraFile == null) {
        return null;
      }
      antora = AsciiDocWrapper.readAntoraYaml(project, antoraFile);
    } else if (file.getName().equals(ANTORA_YML)) {
      antoraFile = file;
      antora = AsciiDocWrapper.readAntoraYaml(project, file);
    } else {
      return null;
    }

    if (antora.get("name") != null) {
      return null;
    }

    return fileEditor -> {
      final EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("The Antora component descriptor 'antora.yml' is missing the attribute 'name'.");
      if (!file.equals(antoraFile)) {
        panel.createActionLabel("Open 'antora.yml'", ()
          -> ApplicationManager.getApplication().invokeLater(()
          -> new OpenFileDescriptor(project, antoraFile).navigate(true)));
      }
      panel.createActionLabel("Read more...", ()
        -> BrowserUtil.browse("https://docs.antora.org/antora/latest/component-version-descriptor/"));
      return panel;
    };
  }
}
