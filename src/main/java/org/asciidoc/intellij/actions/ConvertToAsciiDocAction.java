package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.DocumentRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.util.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.laamella.markdown_to_asciidoc.Converter.*;

/**
 * Converts the contents of an editor panel from Markdown to AsciiDoc.
 * <p/>
 * Created by erikp on 05/12/14.
 */
public class ConvertToAsciiDocAction extends AnAction {

  public static final String[] MARKDOWN_EXTENSIONS = {"markdown", "mkd", "md"};

  @Override
  public void actionPerformed(@NotNull final AnActionEvent event) {

    final PsiFile file = event.getData(DataKeys.PSI_FILE);
    final Project project = event.getProject();

    final VirtualFile virtualFile = file.getVirtualFile();

    if(project != null) {

      final Document document = PsiDocumentManager.getInstance(project).getDocument(file);

      ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile);

      ApplicationManager.getApplication().runWriteAction(new DocumentRunnable(document, project) {
        @Override
        public void run() {
          CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {

              if (document != null) {
                try {
                  String newFileName = FilenameUtils.getBaseName(file.getName()) + ".adoc";
                  virtualFile.rename(this, newFileName);

                  document.setText(convertMarkdownToAsciiDoc(file.getText()));
                }
                catch (IOException e) {
                  e.printStackTrace();
                }
              }
            }
          }, getName(), getGroupId(), UndoConfirmationPolicy.REQUEST_CONFIRMATION);
        }
      });
    }

//    com.intellij.openapi.application.Application.runWriteAction()

//    CommandProcessor.getInstance().executeCommand();

//    com.intellij.openapi.application.Application.
//
//    try {
//
//    }
//    catch (IOException e) {
//      e.printStackTrace();
//    }

//    String text = file.getText();


//    FileDocumentManager.saveDocument(FileDocumentManager.getDocument(VirtualFile)).

//    new AsciiDoc(new File(file.getOriginalFile().getParent().getVirtualFile().getCanonicalPath())).render(file.getText());
  }

  public String getName() {
    return "Convert Markdown to AsciiDoc";
  }

  public String getGroupId() {
    return "AsciiDoc";
  }


  @Override
  public void update(AnActionEvent event) {
    PsiFile file = event.getData(DataKeys.PSI_FILE);
    boolean enabled = false;

    if (file != null) {
      for (String ext : MARKDOWN_EXTENSIONS) {
        if (file.getName().endsWith("." + ext)) {
          enabled = true;
          break;
        }
      }
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}
