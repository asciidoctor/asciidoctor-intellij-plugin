package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.editor.AsciiDocSplitEditor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocIncludeTagInDocument extends AsciiDocASTWrapperPsiElement {
  public AsciiDocIncludeTagInDocument(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocIncludeTagInDocument> {

    @Override
    public AsciiDocIncludeTagInDocument handleContentChange(@NotNull AsciiDocIncludeTagInDocument element,
                                                            @NotNull TextRange range,
                                                            String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTR_VALUE) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement && range.getEndOffset() <= child.getTextLength()) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        AsciiDocPsiImplUtil.throwExceptionCantHandleContentChange(element, range, newContent);
      }
      ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        // save the content in all other editors as their content might be referenced in preview
        VirtualFile[] files = FileEditorManager.getInstance(element.getProject()).getSelectedFiles();
        FileEditor editor = files.length == 0 ? null : FileEditorManager.getInstance(element.getProject()).getSelectedEditor(files[0]);
        if (editor instanceof AsciiDocSplitEditor) {
          // trigger a save-all-and-refresh, as tags of references will have changed
          editor.selectNotify();
        }
      }), ModalityState.NON_MODAL);
      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocIncludeTagInDocument element) {
      PsiElement child = element.findChildByType(AsciiDocTokenTypes.ATTR_VALUE);
      if (child != null) {
        return TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength());
      } else {
        return TextRange.EMPTY_RANGE;
      }
    }
  }
}
