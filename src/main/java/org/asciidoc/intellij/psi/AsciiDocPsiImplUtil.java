package org.asciidoc.intellij.psi;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.regex.Pattern;

public class AsciiDocPsiImplUtil {

  public static String getName(AsciiDocSection element) {
    return element.getTitle();
  }

  public static ItemPresentation getPresentation(final AsciiDocSection element) {
    return new ItemPresentation() {
      @Nullable
      @Override
      public String getPresentableText() {
        return element.getTitle();
      }

      @Nullable
      @Override
      public String getLocationString() {
        PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? null : containingFile.getName();
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return AsciiDocIcons.Structure.SECTION;
      }
    };
  }

  private static final  Pattern REMOVE_CONTENT = Pattern.compile("\\p{Alnum}", Pattern.UNICODE_CHARACTER_CLASS);

  @NotNull
  public static RuntimeException getRuntimeException(@NotNull String message, @NotNull String content, RuntimeException e) {
    content = REMOVE_CONTENT.matcher(content).replaceAll("x");
    return new RuntimeExceptionWithAttachments(message, e, new Attachment("doc.adoc", content));
  }

  @NotNull
  public static RuntimeException getRuntimeException(@NotNull String message, @NotNull PsiElement element, RuntimeException e) {
    String psiTree = DebugUtil.psiToString(element, false, true);
    // keep only structure in the attachment, clear out any text content to anonymize data
    psiTree = psiTree.replaceAll("\\('.*'\\)", "");
    String content = REMOVE_CONTENT.matcher(element.getText()).replaceAll("x");
    return new RuntimeExceptionWithAttachments(message, e, new Attachment("psi.txt", psiTree), new Attachment("doc.adoc", content));
  }

}
