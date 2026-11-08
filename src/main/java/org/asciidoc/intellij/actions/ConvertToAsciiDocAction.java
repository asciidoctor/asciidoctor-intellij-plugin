package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.asciidoc.intellij.ui.OverwriteFileDialog;
import org.asciidoc.intellij.util.FilenameUtils;
import nl.jworks.markdown_to_asciidoc.ToAsciiDocSerializer;
import org.jetbrains.annotations.NotNull;
import org.pegdown.ParsingTimeoutException;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Converts the contents of an editor panel from Markdown to AsciiDoc.
 * <p/>
 * Created by erikp on 05/12/14.
 */
public class ConvertToAsciiDocAction extends AnAction {

  private static final String[] MARKDOWN_EXTENSIONS = {"markdown", "mkd", "md"};

  // pegdown's default of 2 seconds is too short for large Markdown files
  private static final long PARSING_TIMEOUT_MS = 60_000;
  // same extension flags as markdown-to-asciidoc's Converter
  private static final int PEGDOWN_OPTIONS = 65535;

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
    VirtualFile existingFile = virtualFile.getParent().findChild(newFileName);

    AtomicBoolean deleteFile = new AtomicBoolean(false);
    if (existingFile != null) {
      if (new OverwriteFileDialog(newFileName).showAndGet()) {
        deleteFile.set(true);
      } else {
        return;
      }
    }

    // convert before changing anything, as parsing a large file can take a while
    final String markdown = file.getText();
    final AtomicReference<CharSequence> converted = new AtomicReference<>();
    final AtomicReference<RuntimeException> failure = new AtomicReference<>();
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        converted.set(convertMarkdownToAsciiDoc(markdown, PARSING_TIMEOUT_MS));
      } catch (RuntimeException ex) {
        failure.set(ex);
      }
    }, "Converting Markdown to AsciiDoc", false, project);
    if (failure.get() != null) {
      if (isTimeout(failure.get())) {
        Messages.showErrorDialog(project, "The Markdown file is too complex to be converted in a reasonable time.", getName());
        return;
      }
      throw AsciiDocPsiImplUtil.getRuntimeException("Unable to convert Markdown content to AsciiDoc", markdown, failure.get());
    }
    final CharSequence asciiDocContent = converted.get();

    if (existingFile != null && !FileEditorManager.getInstance(project).getEditorList(existingFile).isEmpty()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        // closing the file might trigger a save, therefore, wrap in write action
        FileEditorManager.getInstance(project).closeFile(existingFile);
      });
    }

    if (!FileEditorManager.getInstance(project).getEditorList(virtualFile).isEmpty()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        // closing the file might trigger a save, therefore, wrap in write action
        FileEditorManager.getInstance(project).closeFile(virtualFile);
      });
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            PsiFile asciiDocFile = file.getContainingDirectory().findFile(newFileName);
            if (deleteFile.get() && asciiDocFile != null) {
              asciiDocFile.delete();
            } else if (asciiDocFile != null) {
              // file might have appeared "in between", stop here
              return;
            }
            asciiDocFile = PsiFileFactory.getInstance(project).createFileFromText(newFileName, AsciiDocFileType.INSTANCE, asciiDocContent);
            PsiFile newFile = (PsiFile) file.getContainingDirectory().add(asciiDocFile);
            file.delete();

            ApplicationManager.getApplication().invokeLater(() -> {
              if (newFile.isValid()) {
                newFile.navigate(true);
              }
            });
          }

        }, getName(), getGroupId(), UndoConfirmationPolicy.REQUEST_CONFIRMATION);
      }
    });
  }

  static String convertMarkdownToAsciiDoc(@NotNull String markdown, long timeoutMs) {
    PegDownProcessor processor = new PegDownProcessor(PEGDOWN_OPTIONS, timeoutMs);
    RootNode root = processor.parseMarkdown(markdown.toCharArray());
    return new ToAsciiDocSerializer(root, markdown).toAsciiDoc();
  }

  static boolean isTimeout(@NotNull Throwable throwable) {
    for (Throwable t = throwable; t != null; t = t.getCause()) {
      if (t instanceof ParsingTimeoutException) {
        return true;
      }
    }
    return false;
  }

  public String getName() {
    return "Convert Markdown to AsciiDoc";
  }

  public String getGroupId() {
    return AsciiDocFileType.INSTANCE.getName();
  }

  @Override
  public void update(AnActionEvent event) {
    VirtualFile file = event.getData(LangDataKeys.VIRTUAL_FILE);
    boolean enabled = false;

    if (file != null && file.getParent() != null) {
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
