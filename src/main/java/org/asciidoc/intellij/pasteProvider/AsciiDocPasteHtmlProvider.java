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
import org.asciidoc.intellij.actions.asciidoc.PasteHtmlAction;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Objects;

public class AsciiDocPasteHtmlProvider implements PasteProvider {

  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      return;
    }
    AnAction action = ActionManager.getInstance().getAction(PasteHtmlAction.ID);
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
    AsciiDocApplicationSettings instance = AsciiDocApplicationSettings.getInstance();
    AsciiDocPreviewSettings asciiDocPreviewSettings = instance.getAsciiDocPreviewSettings();
    if (!asciiDocPreviewSettings.isConversionOfClipboardTextEnabled()) {
      return false;
    }
    Transferable produce = Objects.requireNonNull(dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER)).produce();
    //noinspection RedundantIfStatement
    if (produce.isDataFlavorSupported(DataFlavor.allHtmlFlavor)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    return isPastePossible(dataContext);
  }
}
