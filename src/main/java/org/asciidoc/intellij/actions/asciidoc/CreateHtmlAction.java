package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.ui.FileAccessProblem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Balasubramanian Naagarajan(balabarath)
 */
public class CreateHtmlAction extends AsciiDocFileAction {

  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreateHtmlAction";

  private static final Logger LOG = Logger.getInstance(CreateHtmlAction.class);

  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);
  private Project project;

  public CreateHtmlAction() {
    // this doesn't have an icon, therefore show text
    getTemplatePresentation().putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    project = event.getProject();
    if (project == null) {
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
    VirtualFile target = findTargetDirectory(file);
    if (target == null && parent != null) {
      new FileAccessProblem("Can't find a writable folder for the HTML file of " + file.getPath()).show();
      return;
    }
    String content = editor.getDocument().getText();
    final Project currentProject = project;
    final AtomicBoolean successful = new AtomicBoolean(false);
    ProgressManager.getInstance().run(new Task.Backgroundable(currentProject, "Creating HTML", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Path tempImagesPath = AsciiDocWrapper.tempImagesPathForFolder(target, currentProject);
        try {
          AsciiDocWrapper asciiDocWrapper = new AsciiDocWrapper(currentProject, target, tempImagesPath, file.getName());
          String config = AsciiDocWrapper.config(editor.getDocument(), currentProject);
          List<String> extensions = extensionService.getExtensions(currentProject);
          successful.set(convert(asciiDocWrapper, file, content, target, config, extensions, AsciiDocWrapper.FileType.HTML));
        } catch (IOException ex) {
          LOG.warn("unable to create HTML", ex);
          ApplicationManager.getApplication().invokeLater(() -> new FileAccessProblem("Unable to create HTML: " + ex.getMessage()).show());
        } finally {
          AsciiDocWrapper.cleanupImagesPath(tempImagesPath);
        }
      }

      @Override
      public void onFinished() {
        // runs on the EDT, regardless if the task was successful, failed or cancelled
        VirtualFile vf = ApplicationManager.getApplication().runWriteAction((Computable<? extends VirtualFile>) () -> {
          // write action is needed here to update the file system view via refreshAndFindFileByNioPath
          VirtualFile virtualFileHtml = findOutputFile(target, file, "html");
          VirtualFile virtualFile = virtualFileHtml != null ? virtualFileHtml : target;
          AsciiDocUtil.selectFileInProjectView(currentProject, virtualFile);
          return virtualFileHtml;
        });
        if (vf != null && successful.get()) {
          // this must not be a write action, as on macOS this interacts with AWT
          ApplicationManager.getApplication().invokeLater(() -> BrowserUtil.browse(vf));
        }
      }
    });
  }

}
