package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.impl.TrustedProjects;
import com.intellij.ide.impl.UntrustedProjectEditorNotificationPanel;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.UntrustedProjectNotificationProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.ThreeState;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Show a notification to the user that the project is not trusted, therefore some functionality is limited.
 * Starting with 2021.3.1 this should no longer be necessary, as the notification will show on all editors once the project is not trusted,
 * independent of the previously needed UntrustedProjectModeProvider that would show this only in Maven or Gradle projects.
 */
public class AsciiDocUntrustedNotificationProvider extends com.intellij.ui.EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {

  private static final Key<EditorNotificationPanel> KEY = Key.create("Untrusted AsciiDoc project");

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Override
  public @Nullable EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
    if (!(fileEditor instanceof AsciiDocSplitEditor)) {
      return null;
    }
    if (TrustedProjects.getTrustedState(project) == ThreeState.YES) {
      return null;
    }
    if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getSafeMode().equals(SafeMode.SECURE)) {
      // as the IDE has been configured to be SECURE on AsciiDoc anyway, there is no need to show the notification.
      return null;
    }
    if (new UntrustedProjectNotificationProvider().createNotificationPanel(file, fileEditor, project) != null) {
      // someone else is already showing that panel
      return null;
    }
    return new UntrustedProjectEditorNotificationPanel(project, fileEditor, () -> {
      ExternalSystemUtil.confirmLoadingUntrustedProject(project, getSystemId());
      return null;
    });
  }

  @NotNull
  public ProjectSystemId getSystemId() {
    return new ProjectSystemId("AsciiDoc", "AsciiDoc");
  }

}
