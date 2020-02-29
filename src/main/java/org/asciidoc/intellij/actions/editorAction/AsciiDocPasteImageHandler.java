package org.asciidoc.intellij.actions.editorAction;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorTextInsertHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Producer;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.actions.asciidoc.PasteImageAction;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;

/**
 * Page an image from the clipboard when in editor.
 * Inspired by {@link com.intellij.codeInsight.editorActions.PasteHandler} and
 * https://plugins.jetbrains.com/plugin/8446-paste-images-into-markdown who showed me how to use this API.
 */
public class AsciiDocPasteImageHandler extends EditorActionHandler implements EditorTextInsertHandler {

  private final EditorTextInsertHandler myOriginalHandler;

  public AsciiDocPasteImageHandler(EditorTextInsertHandler originalAction) {
    myOriginalHandler = originalAction;
  }

  @Override
  public void doExecute(@NotNull final Editor editor, Caret caret, final DataContext dataContext) {
    assert caret == null : "Invocation of 'paste' operation for specific caret is not supported";
    execute(editor, dataContext, null);
  }

  @Override
  public void execute(Editor editor, DataContext dataContext, Producer<Transferable> producer) {
    if (editor instanceof EditorEx && editor.getProject() != null) {
      VirtualFile virtualFile = ((EditorEx) editor).getVirtualFile();
      if (virtualFile != null) {
        PsiFile file = PsiManager.getInstance(editor.getProject()).findFile(virtualFile);
        if (file != null) {
          PsiElement element = AsciiDocUtil.getStatementAtCaret(editor, file);
          // handle both language injection and start of empty file
          if ((element != null && AsciiDocLanguage.INSTANCE.equals(element.getLanguage())) ||
            AsciiDocFileType.INSTANCE.equals(virtualFile.getFileType())) {
            if (PasteImageAction.imageAvailable(producer)) {
              PasteImageAction action = new PasteImageAction();
              AnActionEvent event = createAnEvent(action, dataContext);
              action.actionPerformed(event);
              return;
            }
          }
        }
      }
    }

    if (myOriginalHandler != null) {
      myOriginalHandler.execute(editor, dataContext, producer);
    }
  }

  private AnActionEvent createAnEvent(AnAction action, @NotNull DataContext context) {
    Presentation presentation = action.getTemplatePresentation().clone();
    return new AnActionEvent(null, context, ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), 0);
  }
}
