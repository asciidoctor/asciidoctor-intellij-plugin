package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.scanner.ScannerException;

import javax.swing.*;
import java.util.List;
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
      try {
        antora = AsciiDocWrapper.readAntoraYaml(project, antoraFile);
      } catch (YAMLException ex) {
        return prepareMessageFromException("Can't read Antora component descriptor 'antora.yml': ",
          ex, file, antoraFile, project);
      }
    } else if (file.getName().equals(ANTORA_YML)) {
      antoraFile = file;
      try {
        antora = AsciiDocWrapper.readAntoraYaml(project, file);
      } catch (YAMLException ex) {
        return prepareMessageFromException("Can't read Antora component descriptor 'antora.yml': ",
          ex, file, antoraFile, project);
      }
    } else {
      return null;
    }

    if (antora.get("name") != null) {
      return null;
    }

    return prepareMessageFromException("The Antora component descriptor 'antora.yml' is missing the attribute 'name'" +
      ".", null, file, antoraFile, project);
  }

  private static @NotNull Function<@NotNull FileEditor, @Nullable JComponent> prepareMessageFromException(String message, RuntimeException ex, @NotNull VirtualFile file, VirtualFile antoraFile, @NotNull Project project) {
    return fileEditor -> {
      final EditorNotificationPanel panel = new EditorNotificationPanel();
      String m = message;
      if (ex instanceof ScannerException scannerException) {
        m = m + scannerException.getProblem();
      } else if (ex != null) {
        m = m + ex.getMessage();
      }
      if (!file.equals(antoraFile)) {
        if (ex instanceof ScannerException scannerException) {
          panel.createActionLabel("Open 'antora.yml'", ()
            -> ApplicationManager.getApplication().invokeLater(()
            -> new OpenFileDescriptor(project, antoraFile, scannerException.getProblemMark().getLine(),
            scannerException.getProblemMark().getColumn()).navigate(true)));
        } else {
          panel.createActionLabel("Open 'antora.yml'", ()
          -> ApplicationManager.getApplication().invokeLater(()
          -> new OpenFileDescriptor(project, antoraFile).navigate(true)));
          }
      }
      panel.setText(m);
      panel.createActionLabel("Read more...", ()
        -> BrowserUtil.browse("https://docs.antora.org/antora/latest/component-version-descriptor/"));

      MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(fileEditor);
      connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
        @Override
        public void after(@NotNull List<? extends VFileEvent> events) {
          // any modification of a file within the project refreshes the preview
          for (VFileEvent event : events) {
            if (event.getFile() != null) {
              if (event.getFile().equals(antoraFile)) {
                // Also update those in other AsciiDoc editors in the module
                EditorNotifications.updateAll();
                connection.disconnect();
                break;
              }
            }
          }
        }
      });

      return panel;
    };
  }
}
