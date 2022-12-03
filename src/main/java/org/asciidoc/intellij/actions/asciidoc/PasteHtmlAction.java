package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCopyPasteHelper;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PasteHtmlAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.PasteHtmlAction";

  private static final Logger LOG = Logger.getInstance(PasteHtmlAction.class);

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    if (!AsciiDocDownloaderUtil.downloadCompletePandoc()) {
      // download pandoc asynchronously for the next attempt.
      AsciiDocDownloaderUtil.downloadPandoc(editor.getProject(), () -> {
        Notifications.Bus.notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.download.title"), AsciiDocBundle.message("asciidoc.download.pandoc.success"), NotificationType.INFORMATION));
        actionPerformed(event);
      }, e -> LOG.warn("unable to download", e));
      return;
    }

    String result = getAsciiDocContentFromClipboard(editor);
    if (result != null) {
      CommandProcessor.getInstance().executeCommand(project,
        () -> ApplicationManager.getApplication().runWriteAction(() -> {
          EditorCopyPasteHelper.getInstance().pasteTransferable(editor, new StringAsTransferable(result));
        }), "Paste Formatted Text", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
      );
    } else {
      // fallback, trigger standard paste procedure
      CommandProcessor.getInstance().executeCommand(project,
        () -> ApplicationManager.getApplication().runWriteAction(() -> {
          EditorCopyPasteHelper.getInstance().pasteFromClipboard(editor);
        }), "Paste Formatted Text", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
      );
    }
  }

  private String getAsciiDocContentFromClipboard(Editor editor) {
    Process process = null;
    try {
      List<String> cmd = new ArrayList<>(Arrays.asList(AsciiDocDownloaderUtil.getPanddocFile().getAbsolutePath(), "--wrap=none", "-f", "html", "-t", "asciidoctor"));
      process = new ProcessBuilder(cmd.toArray(String[]::new)).directory(new File(editor.getProject().getBasePath())).start();

      CopyPasteManager manager = CopyPasteManager.getInstance();
      String html = manager.getContents(DataFlavor.allHtmlFlavor);
      if (html == null) {
        return null;
      }
      // strip HTML clipboard format (HTML_CF header)
      // https://learn.microsoft.com/en-us/windows/win32/dataxchg/html-clipboard-format
      int start = html.indexOf("<html");
      if (html.indexOf("<html") > 0) {
        html = html.substring(start);
      } else if (start == -1) {
        start = html.indexOf("<HTML");
        if (start > 0) {
          html = html.substring(start);
        }
      }
      OutputStream outputStream = process.getOutputStream();
      outputStream.write(html.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

      // to kill the process
      // process.destroy();

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        Notification notification = AsciiDocWrapper.getNotificationGroup().createNotification("Error creating AsciiDOc from HTML clipboard contents", stdout + " / " + stderr, NotificationType.ERROR);
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
        String text = manager.getContents(DataFlavor.getTextPlainUnicodeFlavor());
        if (text == null) {
          return null;
        }
        text = text.replaceAll("\r\n", "\n");
        return text;
      }

      stdout = stdout.replaceAll("\r\n", "\n");
      stdout = stdout.replaceAll("\u00A0", " ");

      return stdout;

    } catch (IOException | InterruptedException e) {
      if (process != null) {
        process.destroy();
      }
      Notification notification = AsciiDocWrapper.getNotificationGroup().createNotification("Error creating AsciiDoc from HTML", e.getMessage(), NotificationType.ERROR);
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
      return null;
    }
  }

  private static class StringAsTransferable implements Transferable {
    private final String text;

    StringAsTransferable(String text) {
      this.text = text;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[]{DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor.equals(DataFlavor.stringFlavor);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (flavor.equals(DataFlavor.stringFlavor)) {
        return text;
      }
      throw new UnsupportedFlavorException(flavor);
    }

  }
}
