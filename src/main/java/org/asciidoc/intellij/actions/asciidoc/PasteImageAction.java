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
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
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
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PasteImageAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.PasteImageAction";

  private static final String ACTION_COPY_FILE = "actionCopyFile";
  private static final String ACTION_INSERT_REFERENCE = "actionInsertReference";
  private static final String ACTION_SAVE_PNG = "actionSavePng";
  private static final String ACTION_SAVE_JPEG = "actionSaveJpeg";

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

    CopyPasteManager manager = CopyPasteManager.getInstance();
    if (manager.areDataFlavorsAvailable(DataFlavor.javaFileListFlavor)) {
      java.util.List<File> fileList = manager.getContents(DataFlavor.javaFileListFlavor);
      if (fileList != null) {
        for (File imageFile : fileList) {
          List<Action> options = new ArrayList<>();
          options.add(new BoundAction("Copy file to current directory, then insert a reference.", ACTION_COPY_FILE));
          BoundAction onlyReference = new BoundAction("Only insert a reference.", ACTION_INSERT_REFERENCE);
          // if project-local file
          VirtualFile imageVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(imageFile);
          if (imageVirtualFile != null && VfsUtilCore.getRelativePath(imageVirtualFile, parentDirectory) != null) {
            onlyReference.setSelected(true);
          }
          options.add(onlyReference);
          RadioButtonDialog dialog = new RadioButtonDialog("Import Image File from Clipboard", "Would you like to copy the image file or only import a reference?", options);
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
                      () -> ApplicationManager.getApplication().runWriteAction(
                        () -> insertImageReference(destination.getVirtualFile(), offset)
                      ), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
                    );
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
                CommandProcessor.getInstance().executeCommand(project,
                  () -> ApplicationManager.getApplication().runWriteAction(() ->
                    insertImageReference(LocalFileSystem.getInstance().findFileByIoFile(imageFile), offset)
                  ), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
                );
                break;
              default:
            }
          } else {
            break;
          }
        }
      }
    } else if (manager.areDataFlavorsAvailable(DataFlavor.imageFlavor)) {
      List<Action> options = new ArrayList<>();
      options.add(new BoundAction("PNG (good for screen shots, diagrams and line art)", ACTION_SAVE_PNG));
      options.add(new BoundAction("JPEG (good for photo images)", ACTION_SAVE_JPEG));
      RadioButtonDialog dialog = new RadioButtonDialog("Import Image Data from Clipboard", "Which format do you want the image to be saved to?", options);
      dialog.show();
      if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        final int offset = editor.getCaretModel().getOffset();
        try {
          Image image = manager.getContents(DataFlavor.imageFlavor);
          if (image == null) {
            throw new IOException("Unable to read image from clipboard");
          }
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
                () -> ApplicationManager.getApplication().runWriteAction(
                  () -> insertImageReference(destination.getVirtualFile(), offset)
                ), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
              );
            } else {
              String message = "Can't save image, no appropriate writer found for selected format.";
              Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
                .createNotification("Error in plugin", message, NotificationType.ERROR, null);
              // increase event log counter
              notification.setImportant(true);
              Notifications.Bus.notify(notification);
            }
          }
        } catch (IOException e) {
          String message = "Can't paste image, " + e.getMessage();
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
            .createNotification("Error in plugin", message, NotificationType.ERROR, null);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
        }
      }
    } else {
      JPanel panel = new JPanel(new GridLayout(2, 0));
      panel.add(new JLabel("Clipboard doesn't contain an image."));
      panel.add(new JLabel("Please copy an image to the clipboard before using this action"));
      new DialogBuilder().title("Import Image from Clipboard").centerPanel(panel).resizable(false).show();
    }
  }

  private void insertImageReference(VirtualFile imageFile, int offset) {
    String relativePath = VfsUtilCore.getRelativePath(imageFile, parentDirectory);
    if (relativePath == null) {
      relativePath = imageFile.getCanonicalPath();
    }
    String insert = "image::" + relativePath + "[]";
    if (offset > 0 && editor.getDocument().getCharsSequence().charAt(offset - 1) != '\n') {
      insert = "\n" + insert;
    }
    int cursorOffset = insert.length();
    if (offset < editor.getDocument().getTextLength() && editor.getDocument().getCharsSequence().charAt(offset) != '\n') {
      insert = insert + "\n";
    }
    editor.getDocument().insertString(offset, insert);
    editor.getCaretModel().moveToOffset(offset + cursorOffset);
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
