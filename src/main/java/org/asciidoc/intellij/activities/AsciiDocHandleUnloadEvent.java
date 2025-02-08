package org.asciidoc.intellij.activities;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDocPlugin;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This takes care of unloading the plugin on uninstalls or updates.
 * WARNING: A dynamic unload will usually only succeed when the application is NOT in debug mode;
 * classes might be marked as "JNI Global" due to this, and not reclaimed, and then unloading fails.
 */
public class AsciiDocHandleUnloadEvent implements DynamicPluginListener {

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocHandleUnloadEvent.class);

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
    if (Objects.equals(pluginDescriptor.getPluginId().getIdString(), AsciiDocPlugin.PLUGIN_ID)) {
      LOG.info("beforePluginUnload");
      AsciiDocWrapper.beforePluginUnload();
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {

        // Possibly not necessary in the future if IntelliJ doesn't hold on to references
        FileEditorManager fem = FileEditorManager.getInstance(project);
        for (FileEditor editor : fem.getAllEditors()) {
          if ((editor instanceof AsciiDocSplitEditor)) {
            ApplicationManager.getApplication().runReadAction(() -> {
              VirtualFile vFile = editor.getFile();
              if (vFile != null && vFile.isValid()) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                  // closing the file might trigger a save, therefore, wrap in write action
                  if (!project.isDisposed()) {
                    fem.closeFile(vFile);
                  }
                });
              }
            });
          }
        }
      }
    }
  }

}
