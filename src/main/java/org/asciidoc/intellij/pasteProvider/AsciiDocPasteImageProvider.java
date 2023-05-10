package org.asciidoc.intellij.pasteProvider;

import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.asciidoc.PasteImageAction;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AsciiDocPasteImageProvider implements PasteProvider {
  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      return;
    }
    AnAction action = ActionManager.getInstance().getAction(PasteImageAction.ID);
    if (action != null) {
      action.actionPerformed(AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext));
    }
  }

  /**
   * Find out if this one should handle paste.
   * Will not be called for files copied externally (for example from Windows explorer), but will be called for files copied from IntelliJ.
   */
  @Override
  public boolean isPastePossible(@NotNull DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (editor == null) {
      return false;
    }
    if (file == null) {
      return false;
    }
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return false;
    }
    Transferable produce = Objects.requireNonNull(dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER)).produce();
    if (produce.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor())) {
      /* if the contents are text, prefer standard paste operation before trying to paste it as an image
      the user can still try the "paste-as-image" operation from the editor toolbar */
      return false;
    }
    if (produce.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      return true;
    }
    if (produce.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      java.util.List<File> fileList;
      try {
        //noinspection unchecked
        fileList = (List<File>) produce.getTransferData(DataFlavor.javaFileListFlavor);
      } catch (UnsupportedFlavorException | IOException e) {
        return false;
      }
      //noinspection ConstantConditions -- as this
      if (fileList == null) {
        throw new IllegalStateException("The class implementation of " + produce.getClass().getName() + " did return null for getTransferData() when it shouldn't. Please report to authors!");
      }
      for (File f : fileList) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png") || name.endsWith(".svg") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    return isPastePossible(dataContext);
  }
}
