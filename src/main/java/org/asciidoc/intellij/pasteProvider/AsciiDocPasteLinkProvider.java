package org.asciidoc.intellij.pasteProvider;

import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.asciidoc.DocumentWriteAction;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Objects;

public class AsciiDocPasteLinkProvider implements PasteProvider {
  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      return;
    }
    DocumentWriteAction.run(editor.getProject(), () -> {
      Document document = editor.getDocument();
      Caret currentCaret = editor.getCaretModel().getCurrentCaret();
      if (currentCaret.getSelectionRange().getLength() == 0) {
        return;
      }
      Transferable produce = Objects.requireNonNull(dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER)).produce();
      if (produce.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        try {
          String url = produce.getTransferData(DataFlavor.stringFlavor).toString();
          SelectionModel selectionModel = editor.getSelectionModel();
          if (AsciiDocUtil.URL_PREFIX_PATTERN.matcher(url).find() || AsciiDocUtil.EMAIL_PATTERN.matcher(url).matches()) {
            int start = selectionModel.getSelectionStart();
            int end = selectionModel.getSelectionEnd();
            String oldText = document.getText(TextRange.create(start, end));
            String newText = oldText;
            if (oldText.contains("]")) {
              newText = newText.replaceAll("\\\\]", "\\\\\\\\]");
              newText = newText.replaceAll("]", "\\\\]");
            }
            String prefix = url + "[";
            if (url.contains("[")) {
              prefix = "link:++" + url + "++[";
            } else if (AsciiDocUtil.EMAIL_PATTERN.matcher(url).matches()) {
              prefix = "link:mailto:" + url + "[";
            }
            if (!oldText.equals(newText)) {
              document.replaceString(start, end, newText);
            }
            document.insertString(start, prefix);
            document.insertString(start + prefix.length() + newText.length(), "]");
            // If the cursor was at the beginning of the selected text, it is now at the beginning of the URL.
            // Move it to the beginning of the text again.
            if (editor.getCaretModel().getCurrentCaret().getOffset() < start + prefix.length()) {
              editor.getCaretModel().getCurrentCaret().moveToOffset(start + prefix.length());
            }
          }
        } catch (IOException | UnsupportedFlavorException ignored) {
          // ignored
        }
      }
    }, "Paste link");
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    if (!isPastePossible(dataContext)) {
      return false;
    }
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      return false;
    }
    Caret currentCaret = editor.getCaretModel().getCurrentCaret();
    if (currentCaret.getSelectionRange().getLength() == 0) {
      return false;
    }
    Transferable produce = Objects.requireNonNull(dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER)).produce();
    if (produce.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        String data = produce.getTransferData(DataFlavor.stringFlavor).toString();
        if (AsciiDocUtil.URL_PREFIX_PATTERN.matcher(data).find() || AsciiDocUtil.EMAIL_PATTERN.matcher(data).matches()) {
          return true;
        }
      } catch (IOException | UnsupportedFlavorException e) {
        return false;
      }
    }
    return false;
  }

  @Override
  public boolean isPastePossible(@NotNull DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      return false;
    }
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (file == null) {
      return false;
    }
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return false;
    }
    return true;
  }
}
