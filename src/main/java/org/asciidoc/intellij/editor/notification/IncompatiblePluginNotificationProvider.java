package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Notify user that an incompatible plugin has been installed.
 */
public class IncompatiblePluginNotificationProvider implements EditorNotificationProvider, DumbAware {
  private static final String INCOMPATIBLE_SCRIPTING_RUBY = "asciidoc.incompatible.scripting-ruby";
  public static final @NotNull PluginId PLUGIN_ID_SCRIPTING_RUBY = PluginId.getId("org.jetbrains.intellij.scripting-ruby");

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    // only in AsciiDoc files
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }

    // only if not previously disabled
    if (!PropertiesComponent.getInstance().getBoolean(INCOMPATIBLE_SCRIPTING_RUBY) &&
      isIncompatiblePluginIntelliJScriptingInstalled()) {
      return fileEditor -> {
        final EditorNotificationPanel panel = new EditorNotificationPanel();
        panel.setText("The plugin IntelliJ Scripting: Ruby is incompatible with the AsciiDoc plugin.");
        //noinspection DialogTitleCapitalization
        panel.createActionLabel("Disable plugin IntelliJ Scripting: Ruby and restart", ()
                -> {
          PluginManagerCore.disablePlugin(PLUGIN_ID_SCRIPTING_RUBY);
          ApplicationManager.getApplication().restart();
        });
        panel.createActionLabel("Do not show again", () -> {
          PropertiesComponent.getInstance().setValue(INCOMPATIBLE_SCRIPTING_RUBY, true);
          EditorNotifications.updateAll();
        });
        return panel;
      };
    }

    return null;
  }

  public static boolean isIncompatiblePluginIntelliJScriptingInstalled() {
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PLUGIN_ID_SCRIPTING_RUBY);
    return plugin != null && plugin.isEnabled();
  }
}
