package org.asciidoc.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtenionsAvailableNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Asciidoctor extensions could be enabled");

  private static final String DONT_ASK_TO_CHANGE_PROVIDER_TYPE_KEY = "asciidoc.do.not.ask.to.enable.extensions";

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    String bp = null;
    for(Project p : ProjectManager.getInstance().getOpenProjects()) {
      if(p.getBasePath() != null && file.getPath().startsWith(p.getBasePath())) {
        bp = p.getBasePath();
      }
    }
    if (bp == null) {
      return null;
    }
    final String projectBasePath = bp;
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    if(asciiDocApplicationSettings.getExtensionsEnabled(projectBasePath) != null
      || !Boolean.TRUE.equals(asciiDocApplicationSettings.getExtensionsPresent(projectBasePath))) {
      return null;
    }
    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("This project contains Asciidoctor Extensions in .asciidoctor/lib that can be executed to render the preview. As this Ruby code runs as your local user, don't run this code unchecked.");
    panel.createActionLabel("I've checked the code and trust it, enable them!", () -> {
      asciiDocApplicationSettings.setExtensionsEnabled(projectBasePath, true);
      EditorNotifications.updateAll();
    });
    panel.createActionLabel("Don't enable them!", () -> {
      asciiDocApplicationSettings.setExtensionsEnabled(projectBasePath, false);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}
