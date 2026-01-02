package org.asciidoc.intellij.psi.search;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows update notification.
 */
public class AsciiDocLinkProjectOpen implements StartupActivity, DumbAware, Disposable {

  private MessageBusConnection connection;

  @Override
  public void runActivity(@NotNull Project project) {
    connection = project.getMessageBus().connect(this);
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        // any modification of a file within the project refreshes the preview
        Set<String> canonicalPaths = new HashSet<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          for (VFileEvent event : events) {
            if (event.getFile() != null) {
              if (!project.isDisposed()) {
                VirtualFile file = event.getFile();
                if (!file.isValid()) {
                  continue;
                }
                if (!file.getFileType().equals(AsciiDocFileType.INSTANCE)) {
                  continue;
                }
                if (!DumbService.isDumb(project)) {
                  String canonicalPath = file.getCanonicalPath();
                  if (canonicalPath != null) {
                    canonicalPaths.add(canonicalPath);
                  }
                }
              }
            }
          }
          if (!canonicalPaths.isEmpty()) {
            runInBackground(canonicalPaths, project);
          }
        });
      }
    });
  }

  private static void runInBackground(Set<String> canonicalPaths, @NonNull Project project) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
          Set<VirtualFile> linkSources = new HashSet<>();
          canonicalPaths.forEach(canonicalPath -> linkSources.addAll(AsciiDocLinkIndex.getLinkSources(project, canonicalPath)));
          if (!linkSources.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() ->
              ApplicationManager.getApplication().runWriteAction(() -> LocalFileSystem.getInstance().refreshFiles(linkSources)));
          }
        });
      } catch (ProcessCanceledException e) {
        runInBackground(canonicalPaths, project);
      }
    });
  }

  @Override
  public void dispose() {
    connection.disconnect();
  }
}
