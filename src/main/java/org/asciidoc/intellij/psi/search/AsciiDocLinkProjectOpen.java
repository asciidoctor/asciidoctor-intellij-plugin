package org.asciidoc.intellij.psi.search;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Refreshes AsciiDoc files that link to a changed AsciiDoc file, so that their previews are updated.
 */
public class AsciiDocLinkProjectOpen implements StartupActivity, DumbAware {

  private static final int DELAY_MS = 500;
  private static final int RETRY_DELAY_MS = 1000;
  private static final int MAX_ATTEMPTS = 3;

  @Override
  public void runActivity(@NotNull Project project) {
    LinkRefresher refresher = new LinkRefresher(project);
    Disposer.register(project, refresher);
    refresher.connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        // resolve the files here: for a creation event, this looks up the file in its parent directory,
        // which fails on a background thread when another thread is deleting the file at the same time
        List<VirtualFile> files = new ArrayList<>();
        for (VFileEvent event : events) {
          if (!isRelevant(event)) {
            continue;
          }
          VirtualFile file = event.getFile();
          if (file != null) {
            files.add(file);
          }
        }
        if (!files.isEmpty()) {
          refresher.schedule(files);
        }
      }
    });
  }

  /**
   * Changes of attributes or timestamps don't change links, only content changes and renames do.
   */
  static boolean isRelevant(VFileEvent event) {
    if (event instanceof VFilePropertyChangeEvent propertyChange) {
      return VirtualFile.PROP_NAME.equals(propertyChange.getPropertyName());
    }
    return true;
  }

  /**
   * Collects the changes of a project and runs one lookup for all of them, as each lookup needs a read action
   * that can't be interrupted by a write action while the platform is updating its indexes.
   */
  private static final class LinkRefresher implements Disposable {
    private final Project project;
    private final MessageBusConnection connection;
    private final Alarm alarm;
    private final List<VirtualFile> pending = new ArrayList<>();
    private boolean scheduled;

    private LinkRefresher(Project project) {
      this.project = project;
      this.connection = project.getMessageBus().connect(this);
      this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    }

    private void schedule(List<VirtualFile> files) {
      synchronized (this) {
        pending.addAll(files);
        if (scheduled) {
          return;
        }
        scheduled = true;
      }
      alarm.addRequest(this::flush, DELAY_MS);
    }

    private void flush() {
      List<VirtualFile> files;
      synchronized (this) {
        files = new ArrayList<>(pending);
        pending.clear();
        scheduled = false;
      }
      Set<String> canonicalPaths = new HashSet<>();
      for (VirtualFile file : files) {
        if (project.isDisposed()) {
          return;
        }
        if (!file.isValid() || !file.getFileType().equals(AsciiDocFileType.INSTANCE)) {
          continue;
        }
        if (!DumbService.isDumb(project)) {
          String canonicalPath = file.getCanonicalPath();
          if (canonicalPath != null) {
            canonicalPaths.add(canonicalPath);
          }
        }
      }
      if (!canonicalPaths.isEmpty()) {
        refresh(canonicalPaths, 1);
      }
    }

    private void refresh(Set<String> canonicalPaths, int attempt) {
      // stop retrying when the project is gone, otherwise this re-schedules itself endlessly
      if (project.isDisposed()) {
        return;
      }
      try {
        AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
          // the index might not be available yet if indexing started after the file change was observed
          if (DumbService.isDumb(project)) {
            return;
          }
          Set<VirtualFile> linkSources = new HashSet<>();
          canonicalPaths.forEach(canonicalPath -> linkSources.addAll(AsciiDocLinkIndex.getLinkSources(project, canonicalPath)));
          if (!linkSources.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() ->
              ApplicationManager.getApplication().runWriteAction(() -> LocalFileSystem.getInstance().refreshFiles(linkSources)));
          }
        });
      } catch (ProcessCanceledException e) {
        if (attempt < MAX_ATTEMPTS) {
          // don't retry immediately, to give the write action and the index update time to complete
          alarm.addRequest(() -> refresh(canonicalPaths, attempt + 1), RETRY_DELAY_MS);
        }
      }
    }

    @Override
    public void dispose() {
      // the connection and the alarm are disposed with this parent
    }
  }
}
