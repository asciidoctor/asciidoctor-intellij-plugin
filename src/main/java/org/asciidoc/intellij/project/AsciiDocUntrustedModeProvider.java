package org.asciidoc.intellij.project;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.UntrustedProjectModeProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;

public class AsciiDocUntrustedModeProvider implements UntrustedProjectModeProvider {
  @NotNull
  @Override
  public ProjectSystemId getSystemId() {
    return new ProjectSystemId("AsciiDoc", "AsciiDoc");
  }

  @Override
  public void loadAllLinkedProjects(@NotNull Project project) {
    // nop
  }

  @Override
  public boolean shouldShowEditorNotification(@NotNull Project project) {
    if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getSafeMode().equals(SafeMode.SECURE)) {
      // as the IDE has been configured to be SECURE on AsciiDoc anyway, there is no need to show the notification.
      return false;
    }
    FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors();
    if (editors.length > 0) {
      // when project is open, show the notification only if at least one AsciiDoc file is open in an editor
      for (FileEditor editor : editors) {
        if (editor instanceof AsciiDocSplitEditor) {
          return true;
        }
      }
    }
    return false;
  }
}
