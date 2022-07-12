package org.asciidoc.intellij.psi;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

  private static final Pattern REMOVE_CONTENT = Pattern.compile("\\p{Alnum}", Pattern.UNICODE_CHARACTER_CLASS);

  @NotNull
  public static RuntimeException getRuntimeException(@NotNull String message, @NotNull String content, RuntimeException e) {
    content = REMOVE_CONTENT.matcher(content).replaceAll("x");
    Attachment attachment = new Attachment("doc.adoc", content);
    attachment.setIncluded(true); // Include it by default as it has been anonymized. The user can still change it.
    return new RuntimeExceptionWithAttachments(message, e, attachment);
  }

  @NotNull
  public static RuntimeException getRuntimeException(@NotNull String message, @NotNull PsiElement element, RuntimeException e, Attachment... attachments) {
    String psiTree = DebugUtil.psiToString(element, false, true);
    // keep only structure in the attachment, clear out any text content to anonymize data
    psiTree = psiTree.replaceAll("\\('.*'\\)", "");
    String content = REMOVE_CONTENT.matcher(element.getText()).replaceAll("x");
    List<Attachment> list = new ArrayList<>();
    list.add(new Attachment("psi.txt", psiTree));
    list.add(new Attachment("doc.adoc", content));
    for (Attachment attachment : attachments) {
      String attachmentContent = new String(attachment.getBytes(), StandardCharsets.UTF_8);
      attachmentContent = REMOVE_CONTENT.matcher(attachmentContent).replaceAll("x");
      list.add(new Attachment(attachment.getName(), attachmentContent));
    }
    list.forEach(a -> a.setIncluded(true)); // Include it by default as it has been anonymized. The user can still change it.
    return new RuntimeExceptionWithAttachments(message, e, list.toArray(new Attachment[]{}));
  }

  public static void throwExceptionCantHandleContentChange(PsiElement element, TextRange range, String newContent) {
    throw AsciiDocPsiImplUtil.getRuntimeException("Can't handle content change " + range, element, null, new Attachment("newcontent.txt", newContent));
  }
}
