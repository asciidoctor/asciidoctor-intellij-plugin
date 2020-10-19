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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.ex.FileSaverDialogImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.mac.MacFileSaverDialog;
import com.intellij.util.Producer;
import com.intellij.util.ui.ImageUtil;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.ui.PasteImageDialog;
import org.jdesktop.swingx.action.BoundAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.asciidoc.intellij.actions.asciidoc.ImageService.getImageWidth;
import static org.asciidoc.intellij.ui.PasteImageDialog.createPasteImageDataDialog;

public class PasteImageAction extends AsciiDocAction {
  public static final String ID = "org.asciidoc.intellij.actions.asciidoc.PasteImageAction";

  private static final String ACTION_COPY_FILE = "actionCopyFile";
  private static final String ACTION_INSERT_REFERENCE = "actionInsertReference";
  private static final String ACTION_SAVE_PNG = "actionSavePng";
  private static final String ACTION_SAVE_JPEG = "actionSaveJpeg";

  /**
   * remember previously selected file format in current session.
   */
  private static volatile String previousFileFormat;
  /**
   * remember previously selected target folder scenario in current session.
   * To be used for new files outside of an Antora component context.
   */
  private static volatile String previousTargetAnyFile;
  /**
   * remember previously selected target folder per file.
   * Limited to last 100 files.
   */
  private static final Map<String, String> PREVIOUS_TARGET_DIRECTORY_BY_FILE = Collections.synchronizedMap(new LinkedHashMap<String, String>(100, (float) 0.7, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
      return size() > 100;
    }
  });
  /**
   * remember previously selected target folder per Antora module.
   * Limited to last 100 modules.
   */
  private static final Map<String, String> PREVIOUS_TARGET_DIRECTORY_BY_ANTORA_MODULE = Collections.synchronizedMap(new LinkedHashMap<String, String>(100, (float) 0.7, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
      return size() > 100;
    }
  });

  private Project project;

  private Editor editor;

  private VirtualFile file;
  private final ImageMacroAttributeService attributeService;

  public PasteImageAction() {
    this.attributeService = ServiceManager.getService(ImageMacroAttributeService.class);
  }

  public static boolean imageAvailable(Producer<Transferable> producer) {
    if (producer != null) {
      // if drag-and-drop, stop here and do standard processing
      return false;
    }
    CopyPasteManager manager = CopyPasteManager.getInstance();
    if (manager.areDataFlavorsAvailable(DataFlavor.javaFileListFlavor)) {
      java.util.List<File> fileList = manager.getContents(DataFlavor.javaFileListFlavor);
      if (fileList != null) {
        for (File f : fileList) {
          String name = f.getName().toLowerCase();
          if (name.endsWith(".png") || name.endsWith(".svg") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
            return true;
          }
        }
      }
    } else if (manager.areDataFlavorsAvailable(DataFlavor.imageFlavor)) {
      String string = manager.getContents(DataFlavor.stringFlavor);
      if (string != null && string.length() > 0) {
        // if the user copies contents from a word processor, prefer the text instead of a rendered image
        // user can still select "paste image" from editor action panel to page the contents as an image
        return false;
      }
      return true;
    }
    return false;
  }

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

    file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }

    VirtualFile initialTargetDirectory = retrieveMemorizedPreviousTarget();

    if (initialTargetDirectory == null || !initialTargetDirectory.exists()) {
      initialTargetDirectory = file.getParent();

      VirtualFile antoraImagesDir = AsciiDocUtil.findAntoraImagesDir(project.getBaseDir(), file.getParent());
      if (antoraImagesDir != null) {
        initialTargetDirectory = antoraImagesDir;
      }
    }

    CopyPasteManager manager = CopyPasteManager.getInstance();
    if (manager.areDataFlavorsAvailable(DataFlavor.javaFileListFlavor)) {
      pasteJavaFileListFlavour(initialTargetDirectory, manager);
    } else if (manager.areDataFlavorsAvailable(DataFlavor.imageFlavor)) {
      pasteImageFlavour(initialTargetDirectory, manager);
    } else {
      JPanel panel = new JPanel(new GridLayout(2, 0));
      panel.add(new JLabel("Clipboard doesn't contain an image."));
      panel.add(new JLabel("Please copy an image to the clipboard before using this action"));
      new DialogBuilder().title("Import Image from Clipboard").centerPanel(panel).resizable(false).show();
    }
  }

  private void pasteImageFlavour(VirtualFile initialTargetDirectory, CopyPasteManager manager) {
    List<Action> options = new ArrayList<>();
    BoundAction png = new BoundAction("PNG (good for screen shots, diagrams and line art)", ACTION_SAVE_PNG);
    options.add(png);
    BoundAction jpeg = new BoundAction("JPEG (good for photo images)", ACTION_SAVE_JPEG);
    options.add(jpeg);
    if (Objects.equals(previousFileFormat, ACTION_SAVE_JPEG)) {
      jpeg.setSelected(true);
    } else {
      png.setSelected(true);
    }

    try {
      final BufferedImage bufferedImage = Optional
        .<Image>ofNullable(manager.getContents(DataFlavor.imageFlavor))
        .map(this::toBufferedImage)
        .orElseThrow(() -> new IOException("Unable to read image from clipboard"));

      final CompletableFuture<Optional<Integer>> initialWidthFuture =
        CompletableFuture.completedFuture(Optional.of(bufferedImage.getWidth()));
      final PasteImageDialog dialog = createPasteImageDataDialog(options, initialWidthFuture);

      dialog.show();

      if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        previousFileFormat = dialog.getSelectedActionCommand();

        final int offset = editor.getCaretModel().getOffset();
        final FileSaverDescriptor descriptor = new FileSaverDescriptor("Save Image to", "Choose the destination file");
        FileSaverDialog saveFileDialog = createSaveFileDialog(descriptor, project);
        String ext = ACTION_SAVE_PNG.equals(dialog.getSelectedActionCommand()) ? "png" : "jpg";
        String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
        VirtualFileWrapper destination = saveFileDialog.save(initialTargetDirectory, "image-" + date + "." + ext);
        if (destination != null) {
          memorizeTargetFolder(destination);
          CommandProcessor.getInstance().executeCommand(project,
            () -> ApplicationManager.getApplication().runWriteAction(
              () -> {
                try {
                  VirtualFile target = createOrReplaceTarget(destination);
                  try (OutputStream outputStream = target.getOutputStream(this)) {
                    boolean written = ImageIO.write(bufferedImage, ext, outputStream);
                    if (written) {
                      insertImageReference(destination.getVirtualFile(), offset, attributeService.toAttributeString(dialog));
                      updateProjectView(target);
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
              }), "Paste Image", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
          );
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

  /**
   * Create FileSaverDialog where modified file name takes precedence over selected file in tree.
   * <p>
   * Uses original logic from FileChooserFactory.getInstance().createSaveFileDialog(),
   * but enhances it to cover the following situation:
   * <ol>
   * <li> user select file in tree
   * <li> user changes file name of selected file
   * <li> user clicks OK
   * </ol>
   * <b>Default behavior:</b> selected file in tree takes precedence
   * <br>
   * <b>This behavior:</b> when changing file name, the parent folder of the selected file is chosen in the tree,
   * therefore the edited file name takes precedence.
   */
  private FileSaverDialog createSaveFileDialog(FileSaverDescriptor descriptor, Project project) {
    return SystemInfo.isMac && Registry.is("ide.mac.native.save.dialog", true)
      ? new MacFileSaverDialog(descriptor, project) : new FileSaverDialogImpl(descriptor, project) {
      @Override
      protected void init() {
        super.init();
        myFileName.getDocument().addDocumentListener(new DocumentListener() {
          @Override
          public void insertUpdate(DocumentEvent e) {
            changed();
          }

          @Override
          public void removeUpdate(DocumentEvent e) {
            changed();
          }

          @Override
          public void changedUpdate(DocumentEvent e) {
            changed();
          }

          private void changed() {
            // when the file name changes AND has focus, then this is the user editing and not an automatic update after selecting a file.
            if (myFileName.hasFocus()) {
              // if the selected item is a file (not a directory), change the selection to the parent folder.
              // this is the symmetric logic as in FileSaverDialogImpl.getFile()
              if (myFileSystemTree.getSelectedFile() != null && !myFileSystemTree.getSelectedFile().isDirectory()) {
                // now switch to parent folder of the file
                myFileSystemTree.select(myFileSystemTree.getSelectedFile().getParent(), () -> {
                });
              }
            }
          }
        });
      }
    };
  }

  private void pasteJavaFileListFlavour(VirtualFile initialTargetDirectory, CopyPasteManager manager) {
    List<File> fileList = manager.getContents(DataFlavor.javaFileListFlavor);
    if (fileList != null) {
      for (File imageFile : fileList) {
        List<Action> options = new ArrayList<>();
        options.add(new BoundAction("Copy file to current directory, then insert a reference.", ACTION_COPY_FILE));
        BoundAction onlyReference = new BoundAction("Only insert a reference.", ACTION_INSERT_REFERENCE);
        VirtualFile imageVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(imageFile);
        // if project-local file, make reference the default
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        if (imageVirtualFile != null && projectFileIndex.isInContent(imageVirtualFile)) {
          onlyReference.setSelected(true);
        }
        options.add(onlyReference);

        final PasteImageDialog dialog = PasteImageDialog.createPasteImageFileDialog(options, getImageWidth(imageVirtualFile));

        dialog.show();

        if (dialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
          final int offset = editor.getCaretModel().getOffset();

          switch (dialog.getSelectedActionCommand()) {
            case ACTION_COPY_FILE:
              final FileSaverDescriptor descriptor = new FileSaverDescriptor("Copy Image to", "Choose the destination file");
              FileSaverDialog saveFileDialog = createSaveFileDialog(descriptor, project);
              VirtualFileWrapper destination = saveFileDialog.save(initialTargetDirectory, imageFile.getName());
              if (destination != null) {
                memorizeTargetFolder(destination);
                CommandProcessor.getInstance().executeCommand(project,
                  () -> ApplicationManager.getApplication().runWriteAction(
                    () -> {
                      try {
                        VirtualFile target = createOrReplaceTarget(destination);
                        try (OutputStream outputStream = target.getOutputStream(this)) {
                          Files.copy(imageFile.toPath(), outputStream);
                          insertImageReference(destination.getVirtualFile(), offset, attributeService.toAttributeString(dialog));
                          updateProjectView(target);
                        }
                      } catch (IOException ex) {
                        String message = "Can't save file: " + ex.getMessage();
                        Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
                          .createNotification("Error in plugin", message, NotificationType.ERROR, null);
                        // increase event log counter
                        notification.setImportant(true);
                        Notifications.Bus.notify(notification);
                      }
                    }
                  ), "Paste Image", AsciiDocFileType.INSTANCE.getName(), UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
                );
              }
              break;
            case ACTION_INSERT_REFERENCE:
              CommandProcessor.getInstance().executeCommand(project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                  insertImageReference(LocalFileSystem.getInstance().findFileByIoFile(imageFile), offset, attributeService.toAttributeString(dialog));
                  }
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
  }

  private void memorizeTargetFolder(VirtualFileWrapper destination) {
    VirtualFile targetFolder = LocalFileSystem.getInstance().findFileByIoFile(destination.getFile().getParentFile());
    if (targetFolder != null) {
      String destinationAsUrl = targetFolder.getUrl();
      PREVIOUS_TARGET_DIRECTORY_BY_FILE.put(file.getUrl(), destinationAsUrl);
      String antoraImagesDir = AsciiDocUtil.findAntoraImagesDirRelative(project.getBaseDir(), file.getParent());
      if (antoraImagesDir != null) {
        PREVIOUS_TARGET_DIRECTORY_BY_ANTORA_MODULE.put(antoraImagesDir, destinationAsUrl);
      }
      previousTargetAnyFile = destinationAsUrl;
    }
  }

  @Nullable
  private VirtualFile retrieveMemorizedPreviousTarget() {
    VirtualFile initialTargetDirectory = null;

    String previousTarget = PREVIOUS_TARGET_DIRECTORY_BY_FILE.get(file.getUrl());
    if (previousTarget == null) {
      String antoraImagesDir = AsciiDocUtil.findAntoraImagesDirRelative(project.getBaseDir(), file.getParent());
      if (antoraImagesDir != null) {
        previousTarget = PREVIOUS_TARGET_DIRECTORY_BY_ANTORA_MODULE.get(antoraImagesDir);
      } else {
        /* use previously used target folder only outside Antora, as Antora imagesdir should be the default
           when saving an image in a new Antora component. */
        previousTarget = previousTargetAnyFile;
      }
    }
    if (previousTarget != null) {
      initialTargetDirectory = VirtualFileManager.getInstance().findFileByUrl(previousTarget);
    }
    return initialTargetDirectory;
  }

  private VirtualFile createOrReplaceTarget(VirtualFileWrapper destination) throws IOException {
    VirtualFile target = LocalFileSystem.getInstance().findFileByIoFile(destination.getFile());
    if (target == null) {
      VirtualFile parentOfImage = LocalFileSystem.getInstance().findFileByIoFile(destination.getFile().getParentFile());
      if (parentOfImage == null) {
        throw new IOException("Unable to determine parent directory");
      }
      target = parentOfImage.createChildData(this, destination.getFile().getName());
    }
    return target;
  }

  private void insertImageReference(final VirtualFile imageFile, final int offset, final String attributes) {
    String relativePath = VfsUtil.findRelativePath(file, imageFile, '/');
    if (relativePath == null) {
      // null case happens if parent file and image file are on different file systems
      // in this case show the full original path of the image as this is what a user would expect
      // ... although this would never render in AsciiDoc - when the user sees the complete path, she/he can then decide what to do next.
      relativePath = imageFile.getCanonicalPath();
    }
    String antoraImagesDir = AsciiDocUtil.findAntoraImagesDirRelative(project.getBaseDir(), file.getParent());
    if (antoraImagesDir != null && relativePath != null) {
      antoraImagesDir = antoraImagesDir + "/";
      if (relativePath.startsWith(antoraImagesDir)) {
        relativePath = relativePath.substring(antoraImagesDir.length());
      }
    }

    String insert = "image::" + relativePath + "[" + attributes + "]";
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
  }

  private BufferedImage toBufferedImage(@NotNull Image image) {
    BufferedImage bufferedImage = ImageUtil.createImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
    Graphics2D bImageGraphics = bufferedImage.createGraphics();
    bImageGraphics.drawImage(image, null, null);
    return bufferedImage;
  }


}
