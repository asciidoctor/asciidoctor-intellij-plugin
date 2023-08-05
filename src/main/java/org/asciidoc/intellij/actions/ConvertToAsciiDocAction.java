package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.serviceContainer.AlreadyDisposedException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.asciidoc.intellij.ui.OverwriteFileDialog;
import org.asciidoc.intellij.util.FilenameUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static nl.jworks.markdown_to_asciidoc.Converter.convertMarkdownToAsciiDoc;

/**
 * Converts the contents of an editor panel from Markdown to AsciiDoc.
 * <p/>
 * Created by erikp on 05/12/14.
 */
public class ConvertToAsciiDocAction extends AnAction {
  private final Logger log = Logger.getInstance(ConvertToAsciiDocAction.class);

  private static final String[] MARKDOWN_EXTENSIONS = {"markdown", "mkd", "md"};

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent event) {

    final PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    final Project project = event.getProject();

    if (file == null || project == null) {
      return;
    }

    final VirtualFile virtualFile = file.getVirtualFile();
    ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singleton(virtualFile));

    String newFileName = FilenameUtils.getBaseName(file.getName()) + "." + AsciiDocFileType.INSTANCE.getDefaultExtension();
    PsiFile existingFile = file.getContainingDirectory().findFile(newFileName);

    AtomicBoolean deleteFile = new AtomicBoolean(false);
    if (existingFile != null) {
      if (new OverwriteFileDialog(newFileName).showAndGet()) {
        deleteFile.set(true);
      } else {
        return;
      }
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            try {
              PsiFile asciiDocFile = file.getContainingDirectory().findFile(newFileName);
              if (deleteFile.get() && asciiDocFile != null) {
                asciiDocFile.delete();
              } else if (asciiDocFile != null) {
                // file might have appeared "in between", stop here
                return;
              }
              @NotNull @NonNls CharSequence asciiDocContent;
              try {
                asciiDocContent = convertMarkdownToAsciiDoc(file.getText());
              } catch (RuntimeException ex) {
                throw AsciiDocPsiImplUtil.getRuntimeException("Unable to convert Markdown content to AsciiDoc", file.getText(), ex);
              }
              asciiDocFile = PsiFileFactory.getInstance(project).createFileFromText(newFileName, AsciiDocFileType.INSTANCE, asciiDocContent);
              PsiFile newFile = (PsiFile) file.getContainingDirectory().add(asciiDocFile);

              newFile.navigate(true);

              deleteVirtualFile();
            } catch (AlreadyDisposedException ex) {
              // ignored
            }
          }

          private void deleteVirtualFile() {
            try {
              virtualFile.delete(this);
            } catch (IOException e) {
              log.error("unable to delete file", e);
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
    try {
      VirtualFile file = event.getData(LangDataKeys.VIRTUAL_FILE);
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
    } catch (AlreadyDisposedException ex) {
      // noop
    }
  }

}
