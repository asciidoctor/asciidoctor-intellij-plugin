package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Balasubramanian Naagarajan(balabarath)
 */
public class CreateHtmlAction extends AsciiDocAction {

  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.CreateHtmlAction";

  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);
  private Project project;

  @Override
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
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
        for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
          FileDocumentManager.getInstance().saveDocument(unsavedDocument);
        }
      });
    }
    VirtualFile parent = file.getParent();
    boolean successful = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      Path tempImagesPath = AsciiDoc.tempImagesPath(parent != null ? parent.toNioPath() : null);
      try {
        File fileBaseDir = new File("");
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
        }
        AsciiDoc asciiDoc = new AsciiDoc(project, fileBaseDir, tempImagesPath, file.getName());
        String config = AsciiDoc.config(editor.getDocument(), project);
        List<String> extensions = extensionService.getExtensions(project);
        asciiDoc.convertTo(new File(file.getCanonicalPath()), config, extensions, AsciiDoc.FileType.HTML);
      } finally {
        AsciiDoc.cleanupImagesPath(tempImagesPath);
      }
    }, "Creating HTML", true, project);
    VirtualFile vf = ApplicationManager.getApplication().runWriteAction((Computable<? extends VirtualFile>) () -> {
      // write action is needed here to update the file system view via refreshAndFindFileByNioPath
      VirtualFile virtualFileHtml =  changeFileExtension(file);
      VirtualFile virtualFile = virtualFileHtml != null ? virtualFileHtml : parent;
      AsciiDocUtil.selectFileInProjectView(project, virtualFile);
      return virtualFileHtml;
    });
    if (vf != null) {
      if (successful) {
        // this must not be a write action, as on macOS this interacts with AWT
        ApplicationManager.getApplication().invokeLater(() -> BrowserUtil.browse(vf));
      }
    }
  }

  @Nullable
  private VirtualFile changeFileExtension(VirtualFile file) {
    Path path = file.getFileSystem().getNioPath(file);
    if (path == null) {
      return null;
    }
    return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(
      path.getParent().resolve(file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", ".html"))
    );
  }

}
