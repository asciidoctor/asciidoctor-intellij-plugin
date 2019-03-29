package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Run Asciidoc and use the warnings and errors as annotations in the file.
 * @author Alexander Schwartz 2019
 */
public class ExternalAnnotator extends com.intellij.lang.annotation.ExternalAnnotator<
  AsciidocInfoType, AsciidocAnnotationResultType> {

  @Nullable
  @Override
  public AsciidocInfoType collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    AtomicInteger offsetLineNo = new AtomicInteger(0);
    final String contentWithConfig = AsciiDoc.prependConfig(editor.getDocument(), file.getProject(), offsetLineNo::set);
    List<String> extensions = AsciiDoc.getExtensions(file.getProject());
    return new AsciidocInfoType(file, editor, contentWithConfig, extensions, offsetLineNo.get());
  }

  @Nullable
  @Override
  public AsciidocAnnotationResultType doAnnotate(AsciidocInfoType collectedInfo) {
    PsiFile file = collectedInfo.getFile();
    Editor editor = collectedInfo.getEditor();
    File fileBaseDir = new File("");
    VirtualFile parent = FileDocumentManager.getInstance().getFile(editor.getDocument()).getParent();
    if (parent != null) {
      // parent will be null if we use Language Injection and Fragment Editor
      fileBaseDir = new File(parent.getCanonicalPath());
    }

    AsciidocAnnotationResultType asciidocAnnotationResultType = new AsciidocAnnotationResultType(editor.getDocument());
    Path tempImagesPath = AsciiDoc.tempImagesPath();
    try {
      AsciiDoc asciiDoc = new AsciiDoc(file.getProject().getBasePath(), fileBaseDir,
        tempImagesPath, FileDocumentManager.getInstance().getFile(editor.getDocument()).getName());

      asciiDoc.render(collectedInfo.getContentWithConfig(), collectedInfo.getExtensions(), (boasOut, boasErr) -> {
        String[] err = boasErr.toString().split("\n");
        for (String e : err) {
          if (e.startsWith("asciidoctor:"))
            asciidocAnnotationResultType.addMessage(e, collectedInfo.getOffsetLineNo());
        }
      });

    } finally {
      if (tempImagesPath != null) {
        try {
          FileUtils.deleteDirectory(tempImagesPath.toFile());
        } catch (IOException _ex) {
          Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", _ex);
        }
      }
    }

    return asciidocAnnotationResultType;
  }

  @Override
  public void apply(@NotNull PsiFile file, AsciidocAnnotationResultType annotationResult, @NotNull AnnotationHolder holder) {
    for (AsciidocAnnotationResultType.Message m : annotationResult.getMessages()) {
      if (m.getLine() != null) {
        holder.createAnnotation(m.getSeverity(),
          TextRange.from(
            annotationResult.getDocument().getLineStartOffset(m.getLine() - 1),
            annotationResult.getDocument().getLineEndOffset(m.getLine() - 1) - annotationResult.getDocument().getLineStartOffset(m.getLine() - 1)),
          m.getMessage());
      } else {
        holder.createAnnotation(m.getSeverity(),
          TextRange.from(annotationResult.getDocument().getLineStartOffset(0),
            annotationResult.getDocument().getLineEndOffset(0) - annotationResult.getDocument().getLineStartOffset(0)),
          m.getMessage());
      }
    }
  }
}
