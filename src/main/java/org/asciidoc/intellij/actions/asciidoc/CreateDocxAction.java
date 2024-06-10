package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.download.PandocInfo;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.ui.FileAccessProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CreateDocxAction extends AsciiDocFileAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreateDocxAction";

  private static final Logger LOG = Logger.getInstance(CreateDocxAction.class);
  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);

  private Project project;

  @Override
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    if (event.getPresentation().isVisible() && !PandocInfo.identify().supported) {
      event.getPresentation().setVisible(false);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    project = event.getProject();
    if (project == null || project.isDisposed()) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }
    if (file.getCanonicalPath() == null) {
      return;
    }

    if (!AsciiDocDownloaderUtil.downloadCompletePandoc()) {
      // download all element, as we can't detect if the PDF contains maybe diagrams
      AsciiDocDownloaderUtil.downloadPandoc(project, () -> {
        Notifications.Bus
          .notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.download.title"),
            AsciiDocBundle.message("asciidoc.download.pandoc.success"),
            NotificationType.INFORMATION));
        this.actionPerformed(event);
      }, e -> LOG.warn("unable to download", e));
      return;
    }

    if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
            FileDocumentManager.getInstance().saveDocument(unsavedDocument);
          }
        } catch (RuntimeException ex) {
          LOG.warn("Unable to save other file (might be a problem in another plugin", ex);
        }
      });
    }
    VirtualFile parent = file.getParent();
    if (parent == null || parent.getCanonicalPath() == null) {
      return;
    }
    boolean successful;
    try {
      successful = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        String docbookFile = file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".xml");
        String docxFile = file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".docx");
        Path tempImagesPath = AsciiDocWrapper.tempImagesPath(parent.toNioPath(), project);
        Process process = null;
        try {
          AsciiDocWrapper asciiDocWrapper = new AsciiDocWrapper(project, parent, tempImagesPath, file.getName());
          String config = AsciiDocWrapper.config(editor.getDocument(), project);
          List<String> extensions = extensionService.getExtensions(project);
          if (!asciiDocWrapper.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDocWrapper.FileType.DOCX)) {
            return false;
          }
          File finalFile = new File(parent.getCanonicalPath(), docxFile);
          if (finalFile.exists()) {
            if (!finalFile.delete()) {
              ApplicationManager.getApplication().invokeLater(() -> {
                new FileAccessProblem("Unable to delete target file " + docxFile).show();
              });
              return false;
            }
          }
          List<String> cmd = new ArrayList<>(Arrays.asList(
            AsciiDocDownloaderUtil.getPanddocFile().getAbsolutePath(),
            "-s",
            parent.getCanonicalPath() + File.separator + docbookFile,
            "-t",
            "docx",
            "-f",
            "docbook",
            "--resource-path",
            parent.getCanonicalPath(),
            "-o",
            parent.getCanonicalPath() + File.separator + docxFile));
          Collection<VirtualFile> reference = ApplicationManager.getApplication().runReadAction((Computable<Collection<VirtualFile>>)
            () -> FilenameIndex.getVirtualFilesByName("reference.docx", GlobalSearchScope.projectScope(project))
          );
          if (reference.size() == 1) {
            cmd.add("--reference-doc");
            //noinspection OptionalGetWithoutIsPresent
            cmd.add(reference.stream().findFirst().get().getCanonicalPath());

          }
          process = new ProcessBuilder(cmd.toArray(String[]::new)).directory(new File(parent.getCanonicalPath())).start();
          String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
          String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

          // to kill the process
          // process.destroy();

          int exitCode = process.waitFor();
          if (exitCode != 0) {
            ApplicationManager.getApplication().invokeLater(() -> {
              new FileAccessProblem("Error creating DOCX from AsciiDoc file: " + stdout + " / " + stderr).show();
            });
            return false;
          }

          File xml = new File(parent.getCanonicalPath() + File.separator + docbookFile);
          if (!xml.delete()) {
            String message = "Can't delete temporary XML file " + xml.getCanonicalPath();
            ApplicationManager.getApplication().invokeLater(() -> {
              new FileAccessProblem(message).show();
            });
          }

        } catch (IOException e) {
          if (process != null) {
            process.destroy();
          }
          Notification notification = AsciiDocWrapper.getNotificationGroup()
            .createNotification("Error creating DOCX from AsciiDoc file", e.getMessage(), NotificationType.ERROR);
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
          return false;
        } finally {
          AsciiDocWrapper.cleanupImagesPath(tempImagesPath);
        }
        return true;
      }, "Creating DOCX file", true, project);
    } catch (InterruptedException e) {
      successful = false;
    }
    boolean finalSuccessful = successful;
    ApplicationManager.getApplication().runWriteAction(() -> {
      VirtualFile virtualFileDocx = changeFileExtensionDocx(file);
      VirtualFile virtualFile = virtualFileDocx != null ? virtualFileDocx : parent;
      AsciiDocUtil.selectFileInProjectView(project, virtualFile);
      if (virtualFileDocx != null) {
        if (finalSuccessful) {
          ApplicationManager.getApplication().invokeLater(() -> {
            new OpenFileDescriptor(project, virtualFileDocx).navigate(true);
          });
        }
      }
    });
  }

  @Nullable
  private VirtualFile changeFileExtensionDocx(VirtualFile file) {
    Path path = file.getFileSystem().getNioPath(file);
    if (path == null) {
      return null;
    }
    return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(
      path.getParent().resolve(file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".docx"))
    );
  }

}
