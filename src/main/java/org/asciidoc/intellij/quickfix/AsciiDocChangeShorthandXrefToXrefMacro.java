package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Change {@code <<file.adoc#anchor,text>>} to {@code xref:file.adoc#anchor[text]}.
 */
public class AsciiDocChangeShorthandXrefToXrefMacro implements LocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.changeShorthandXrefToXrefMacro");
  }

  /**
   * A reference points to another document if there is a file name before the hash.
   */
  public static boolean isInterDocument(@NotNull AsciiDocRef ref) {
    PsiElement target = getTarget(ref);
    return target != null && target.getText().indexOf('#') > 0;
  }

  @Nullable
  public static PsiElement getTarget(@NotNull AsciiDocRef ref) {
    return ref.getNode().findChildByType(AsciiDocTokenTypes.REF) != null
      ? ref.getNode().findChildByType(AsciiDocTokenTypes.REF).getPsi() : null;
  }

  public boolean canFix(@NotNull AsciiDocRef ref) {
    PsiElement start = ref.getFirstChild();
    PsiElement end = ref.getLastChild();
    if (!(start instanceof LeafPsiElement) || start.getNode().getElementType() != AsciiDocTokenTypes.REFSTART
      || end == null || end.getNode().getElementType() != AsciiDocTokenTypes.REFEND) {
      return false;
    }
    String text = getText(ref);
    // a closing bracket in the text would need escaping in a way that differs between Asciidoctor versions
    return text == null || text.indexOf(']') == -1;
  }

  @Nullable
  private static String getText(@NotNull AsciiDocRef ref) {
    var textNode = ref.getNode().findChildByType(AsciiDocTokenTypes.REFTEXT);
    return textNode != null ? textNode.getText() : null;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    if (!(descriptor.getPsiElement() instanceof AsciiDocRef ref) || !canFix(ref)) {
      return;
    }
    PsiElement target = getTarget(ref);
    if (target == null) {
      return;
    }
    String path = target.getText();
    int hash = path.indexOf('#');
    String file = path.substring(0, hash);
    // the shorthand syntax implied the AsciiDoc extension for files without an extension
    String lastSegment = file.substring(file.lastIndexOf('/') + 1);
    if (lastSegment.indexOf('.') == -1 && lastSegment.indexOf(':') == -1 && lastSegment.indexOf('{') == -1) {
      path = file + ".adoc" + path.substring(hash);
    }
    String text = getText(ref);
    StringBuilder sb = new StringBuilder("xref:").append(path).append('[');
    if (text != null) {
      // commas or equal signs would be interpreted as attributes
      if (text.indexOf(',') != -1 || text.indexOf('=') != -1) {
        sb.append('"').append(text.replace("\"", "\\\"")).append('"');
      } else {
        sb.append(text);
      }
    }
    sb.append(']');
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Document document = documentManager.getDocument(ref.getContainingFile());
    if (document != null) {
      TextRange range = ref.getTextRange();
      document.replaceString(range.getStartOffset(), range.getEndOffset(), sb.toString());
      documentManager.commitDocument(document);
    }
  }
}
