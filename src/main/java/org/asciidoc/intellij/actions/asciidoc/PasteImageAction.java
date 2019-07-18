package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.util.ui.UIUtil;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.ui.RadioButtonDialog;
import org.jdesktop.swingx.action.BoundAction;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PasteImageAction extends AsciiDocAction {
  private static final String ACTION_COPY_FILE = "actionCopyFile";
  private static final String ACTION_INSERT_REFERENCE = "actionInsertReference";
  private static final String ACTION_SAVE_PNG = "actionSavePng";
  private static final String ACTION_SAVE_JPG = "actionSaveJpg";

  private Project project;

  private Editor editor;

  private VirtualFile parentDirectory;

  @Override
  public void actionPerformed(AnActionEvent event) {
    project = event.getProject();
    if (project == null) {
      return;
    }
    editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }
    parentDirectory = file.getParent();

    Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    if (systemClipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
      try {
        @SuppressWarnings({"unchecked"})
        java.util.List<File> fileList = (List<File>) systemClipboard.getData(DataFlavor.javaFileListFlavor);
        if (fileList.size() == 1) {
          File imageFile = fileList.get(0);
          List<Action> options = new ArrayList<>();
          options.add(new BoundAction("Copy file into project, then insert a reference.", ACTION_COPY_FILE));
          options.add(new BoundAction("Only insert a reference.", ACTION_INSERT_REFERENCE));
          RadioButtonDialog dialog = new RadioButtonDialog("Import image file", "Would you like to copy the image file or only import a reference?", options);
          dialog.show();

          if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            final int offset = editor.getCaretModel().getOffset();

            switch (dialog.getSelectedActionCommand()) {
              case ACTION_COPY_FILE:
                final FileSaverDescriptor descriptor = new FileSaverDescriptor("Copy Image to", "Choose the destination file");
                FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, (Project) null);
                VirtualFileWrapper destination = saveFileDialog.save(parentDirectory, imageFile.getName());
                if (destination != null) {
                  try {
                    Files.copy(imageFile.toPath(), destination.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

                    updateProjectView(destination.getVirtualFile());

                    CommandProcessor.getInstance().executeCommand(project,
                      () -> ApplicationManager.getApplication().runWriteAction(() -> {
                        insertImageReference(destination.getVirtualFile(), offset);
                      }), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
                  } catch (IOException ex) {
                    String message = "Can't save file: " + ex.getMessage();
                    Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
                      .createNotification("Error in plugin", message, NotificationType.ERROR, null);
                    // increase event log counter
                    notification.setImportant(true);
                    Notifications.Bus.notify(notification);
                  }
                }
                break;
              case ACTION_INSERT_REFERENCE:
                insertImageReference(LocalFileSystem.getInstance().findFileByIoFile(imageFile), offset);
                break;
              default:
            }
          }
        }
      } catch (UnsupportedFlavorException | IOException e) {
        String message = "Can't paste image, " + e.getMessage();
        Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
          .createNotification("Error in plugin", message, NotificationType.ERROR, null);
        // increase event log counter
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
      }
    } else if (systemClipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
      List<Action> options = new ArrayList<>();
      options.add(new BoundAction("PNG", ACTION_SAVE_PNG));
      options.add(new BoundAction("JPG", ACTION_SAVE_JPG));
      RadioButtonDialog dialog = new RadioButtonDialog("Import image data", "Which format do you want the image to be saved to?", options);
      dialog.show();
      if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        final int offset = editor.getCaretModel().getOffset();
        try {
          @SuppressWarnings({"unchecked"})
          Image image = (Image) systemClipboard.getData(DataFlavor.imageFlavor);
          BufferedImage bufferedImage = toBufferedImage(image);
          final FileSaverDescriptor descriptor = new FileSaverDescriptor("Save Image to", "Choose the destination file");
          FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, (Project) null);
          String ext = ACTION_SAVE_PNG.equals(dialog.getSelectedActionCommand()) ? "png" : "jpg";
          VirtualFileWrapper destination = saveFileDialog.save(parentDirectory, "file." + ext);
          if (destination != null) {
            boolean written = ImageIO.write(bufferedImage, ext, destination.getFile());
            if (written) {
              updateProjectView(destination.getVirtualFile());
              CommandProcessor.getInstance().executeCommand(project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                  insertImageReference(destination.getVirtualFile(), offset);
                }), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
            } else {
              String message = "Can't save image, no appropriate writer found for selected format.";
              Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
                .createNotification("Error in plugin", message, NotificationType.ERROR, null);
              // increase event log counter
              notification.setImportant(true);
              Notifications.Bus.notify(notification);
            }
          }
        } catch (UnsupportedFlavorException | IOException e) {
          String message = "Can't paste image, " + e.getMessage();
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
            .createNotification("Error in plugin", message, NotificationType.ERROR, null);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
        }
      }
    }
  }

  private void insertImageReference(VirtualFile imageFile, int offset) {
    String relativePath = VfsUtilCore.getRelativePath(imageFile, parentDirectory);
    String insert = "image::" + relativePath + "[]";
    editor.getDocument().insertString(offset, insert);
    editor.getCaretModel().moveToOffset(offset + insert.length() - 1);
  }

  private void updateProjectView(VirtualFile virtualFile) {
    //update project view
    ProjectView projectView = ProjectView.getInstance(project);
    projectView.changeView(ProjectViewPane.ID);
    projectView.select(null, virtualFile, true);
    ProjectView.getInstance(project).refresh();
  }

  private BufferedImage toBufferedImage(@NotNull Image image) {
    BufferedImage bufferedImage = UIUtil.createImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    Graphics2D bImageGraphics = bufferedImage.createGraphics();
    bImageGraphics.drawImage(image, null, null);
    return bufferedImage;
  }


}
