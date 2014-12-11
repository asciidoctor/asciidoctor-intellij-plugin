package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.util.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.laamella.markdown_to_asciidoc.Converter.convertMarkdownToAsciiDoc;

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

    if (file == null || project == null) {
      return;
    }

    final VirtualFile virtualFile = file.getVirtualFile();
    ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            String newFileName = FilenameUtils.getBaseName(file.getName()) + "." + AsciiDocFileType.INSTANCE.getDefaultExtension();
            PsiFile asciiDocFile = PsiFileFactory.getInstance(project).createFileFromText(newFileName, AsciiDocFileType.INSTANCE, convertMarkdownToAsciiDoc(file.getText()));

            PsiFile newFile = (PsiFile)file.getContainingDirectory().add(asciiDocFile);
            newFile.navigate(true);

            try {
              virtualFile.delete(this);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }, getName(), getGroupId(), UndoConfirmationPolicy.REQUEST_CONFIRMATION);
      }
    });
  }

  public String getName() {
    return "Convert Markdown to AsciiDoc";
  }

  public String getGroupId() {
    return AsciiDocFileType.INSTANCE.getName();
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
