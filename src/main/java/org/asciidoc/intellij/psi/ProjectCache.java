package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ProjectCache<ITEM> {

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected final Map<Project, ITEM> projectItems = new HashMap<>();
  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected final Map<Project, MessageBusConnection> projectConnections = new HashMap<>();

  public void cache(Project project, ITEM roots) {
    synchronized (projectItems) {
      try {
        if (!project.isDisposed()) {
          if (projectItems.get(project) == null) {
            // Listen to any file modification in the project, so that we can clear the cache
            MessageBusConnection connection = project.getMessageBus().connect();
            projectConnections.put(project, connection);
            connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
              @Override
              public void after(@NotNull List<? extends VFileEvent> events) {
                processEvent(events, project);
              }
            });
            Disposer.register(project, () -> clear(project));
          }
          projectItems.put(project, roots);
        }
      } catch (AlreadyDisposedException ex) {
        // noop - project already disposed
      }
    }
  }

  protected abstract void processEvent(@NotNull List<? extends VFileEvent> events, Project project);

  @Nullable
  public ITEM retrieve(Project project) {
    synchronized (projectItems) {
      return projectItems.get(project);
    }
  }

  private void clear(Project project) {
    synchronized (projectItems) {
      projectItems.remove(project);
      MessageBusConnection messageBusConnection = projectConnections.get(project);
      if (messageBusConnection != null) {
        messageBusConnection.disconnect();
        projectConnections.remove(project);
      }
    }
  }

}
