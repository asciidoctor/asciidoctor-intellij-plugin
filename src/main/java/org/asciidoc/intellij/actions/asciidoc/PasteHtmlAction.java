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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.download.PandocInfo;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PasteHtmlAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.PasteHtmlAction";

  private static final Logger LOG = Logger.getInstance(PasteHtmlAction.class);

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    if (event.getPresentation().isVisible() && !PandocInfo.identify().supported) {
      event.getPresentation().setVisible(false);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    perform(event);
  }

  private void perform(AnActionEvent event) {
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
        perform(event);
      }, e -> LOG.warn("unable to download", e));
      return;
    }

    CopyPasteManager manager = CopyPasteManager.getInstance();
    String html = readHtmlFromClipboard(manager);
    if (html == null) {
      pasteFromClipboard(project, editor);
      return;
    }
    String plainText = manager.getContents(DataFlavor.stringFlavor);
    String basePath = project.getBasePath();
    String htmlToConvert = stripHtmlClipboardHeader(html);

    // starting and waiting for the external process might block, therefore don't do this on the EDT
    ProgressManager.getInstance().run(new Task.Backgroundable(project, AsciiDocBundle.message("asciidoc.paste.html.converting"), true) {
      private String result;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        result = convertHtmlToAsciiDoc(htmlToConvert, basePath, plainText);
      }

      @Override
      public void onFinished() {
        if (project.isDisposed() || editor.isDisposed()) {
          return;
        }
        if (result != null) {
          CommandProcessor.getInstance().executeCommand(project,
            () -> ApplicationManager.getApplication().runWriteAction(() -> {
              EditorCopyPasteHelper.getInstance().pasteTransferable(editor, new StringAsTransferable(result));
            }), "Paste Formatted Text", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
          );
        } else {
          pasteFromClipboard(project, editor);
        }
      }
    });
  }

  @Nullable
  private static String readHtmlFromClipboard(CopyPasteManager manager) {
    // depending on the platform, the HTML flavor is delivered as a String, a Reader or a stream
    Object data = manager.getContents(DataFlavor.allHtmlFlavor);
    try {
      if (data instanceof String) {
        return (String) data;
      } else if (data instanceof Reader) {
        try (Reader reader = (Reader) data) {
          StringWriter writer = new StringWriter();
          reader.transferTo(writer);
          return writer.toString();
        }
      } else if (data instanceof InputStream) {
        try (InputStream stream = (InputStream) data) {
          return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    } catch (IOException e) {
      LOG.warn("unable to read HTML from clipboard", e);
    }
    return null;
  }

  private static void pasteFromClipboard(Project project, Editor editor) {
    // fallback, trigger standard paste procedure
    CommandProcessor.getInstance().executeCommand(project,
      () -> ApplicationManager.getApplication().runWriteAction(() -> {
        EditorCopyPasteHelper.getInstance().pasteFromClipboard(editor);
      }), "Paste Formatted Text", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
    );
  }

  private static String stripHtmlClipboardHeader(String html) {
    // strip HTML clipboard format (HTML_CF header)
    // https://learn.microsoft.com/en-us/windows/win32/dataxchg/html-clipboard-format
    int start = html.indexOf("<html");
    if (start > 0) {
      return html.substring(start);
    } else if (start == -1) {
      start = html.indexOf("<HTML");
      if (start > 0) {
        return html.substring(start);
      }
    }
    return html;
  }

  private String convertHtmlToAsciiDoc(String html, String basePath, String plainText) {
    Process process = null;
    try {
      List<String> cmd = new ArrayList<>(Arrays.asList(AsciiDocDownloaderUtil.getPanddocFile().getAbsolutePath(), "--wrap=none", "-f", "html", "-t", "asciidoctor"));
      process = new ProcessBuilder(cmd.toArray(String[]::new)).directory(new File(basePath)).start();

      OutputStream outputStream = process.getOutputStream();
      outputStream.write(html.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        Notification notification = AsciiDocWrapper.getNotificationGroup().createNotification("Error creating AsciiDOc from HTML clipboard contents", stdout + " / " + stderr, NotificationType.ERROR);
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
        if (plainText == null) {
          return null;
        }
        return plainText.replaceAll("\r\n", "\n");
      }

      stdout = stdout.replaceAll("\r\n", "\n");
      stdout = stdout.replaceAll("\u00A0", " ");

      return stdout;

    } catch (IOException | InterruptedException e) {
      if (process != null) {
        process.destroy();
      }
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
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
