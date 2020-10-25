package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileIntentionAction;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Run Asciidoc and use the warnings and errors as annotations in the file.
 *
 * @author Alexander Schwartz 2019
 */
public class ExternalAnnotator extends com.intellij.lang.annotation.ExternalAnnotator<
  AsciiDocInfoType, AsciiDocAnnotationResultType> {

  public static final String INCLUDE_FILE_NOT_FOUND = "include file not found";

  private final AsciiDocExtensionService extensionService = ServiceManager.getService(AsciiDocExtensionService.class);

  @Nullable
  @Override
  public AsciiDocInfoType collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    final String config = AsciiDoc.config(editor.getDocument(), file.getProject());
    List<String> extensions = extensionService.getExtensions(file.getProject());
    return new AsciiDocInfoType(file, editor, editor.getDocument().getText(), config, extensions);
  }

  @Nullable
  @Override
  public AsciiDocAnnotationResultType doAnnotate(AsciiDocInfoType collectedInfo) {
    PsiFile file = collectedInfo.getFile();
    Editor editor = collectedInfo.getEditor();
    File fileBaseDir = new File("");
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    String name = "unkown";
    if (virtualFile != null) {
      name = file.getName();
      VirtualFile parent = virtualFile.getParent();
      if (parent != null && parent.getCanonicalPath() != null) {
        // parent will be null if we use Language Injection and Fragment Editor
        fileBaseDir = new File(parent.getCanonicalPath());
      }
    }

    AsciiDocAnnotationResultType asciidocAnnotationResultType = new AsciiDocAnnotationResultType(editor.getDocument());

    if (!AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isShowAsciiDocWarningsAndErrorsInEditor()) {
      asciidocAnnotationResultType.setLogRecords(Collections.emptyList());
      return asciidocAnnotationResultType;
    }

    Path tempImagesPath = AsciiDoc.tempImagesPath();
    try {
      AsciiDoc asciiDoc = new AsciiDoc(file.getProject(), fileBaseDir,
        tempImagesPath, name);
      asciidocAnnotationResultType.setDocname(new File(fileBaseDir, name).getAbsolutePath());
      asciiDoc.render(collectedInfo.getContent(), collectedInfo.getConfig(), collectedInfo.getExtensions(), (boasOut, boasErr, logRecords)
        -> asciidocAnnotationResultType.setLogRecords(logRecords));
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
  public void apply(@NotNull PsiFile file, AsciiDocAnnotationResultType annotationResult, @NotNull AnnotationHolder holder) {
    WolfTheProblemSolver theProblemSolver = WolfTheProblemSolver.getInstance(file.getProject());
    Collection<Problem> problems = new ArrayList<>();
    for (LogRecord logRecord : annotationResult.getLogRecords()) {
      if (logRecord.getSeverity() == Severity.DEBUG) {
        continue;
      }
      if (logRecord.getMessage() == null) {
        continue;
      }
      if (logRecord.getMessage().startsWith("possible invalid reference:")) {
        /* TODO: these messages are not helpful in IntelliJ as they have no line number
            and provide too many false positives for split documents  */
        continue;
      }
      HighlightSeverity severity = toSeverity(logRecord.getSeverity());
      // the line number as shown in the IDE (starting with 1)
      Integer lineNumber = null;
      // the line number used for creating the annotation (starting with 0)
      int lineNumberForAnnotation = 0;
      if (logRecord.getCursor() != null
        && (logRecord.getCursor().getFile() == null || logRecord.getCursor().getFile().equals(annotationResult.getDocname()))
        && logRecord.getCursor().getLineNumber() >= 0) {
        lineNumber = logRecord.getCursor().getLineNumber();
        lineNumberForAnnotation = lineNumber - 1;
        if (lineNumberForAnnotation < 0) {
          // logRecords created in the prepended .asciidoctorconfig elements - will be shown on line zero
          lineNumberForAnnotation = 0;
        }
        if (lineNumberForAnnotation >= annotationResult.getDocument().getLineCount()) {
          /* an extension (like spring-boot-rest-docs) might run sub-instances of Asciidoctor to parse document snippets.
          the error messages might have line numbers greater than the current document */
          lineNumberForAnnotation = 0;
        }
      } else if (logRecord.getMessage().startsWith(INCLUDE_FILE_NOT_FOUND)) {
        lineNumberForAnnotation = findLineByInclude(file, logRecord, 0);
      }
      AnnotationBuilder ab = holder.newAnnotation(severity,
        logRecord.getMessage()).range(
        TextRange.from(
          annotationResult.getDocument().getLineStartOffset(lineNumberForAnnotation),
          annotationResult.getDocument().getLineEndOffset(lineNumberForAnnotation) - annotationResult.getDocument().getLineStartOffset(lineNumberForAnnotation))
      );
      StringBuilder sb = new StringBuilder();
      sb.append(StringEscapeUtils.escapeHtml4(logRecord.getMessage()));
      if (logRecord.getCursor() != null) {
        if (logRecord.getCursor().getFile() == null || logRecord.getCursor().getFile().equals(annotationResult.getDocname())) {
          sb.append("<br>(")
            .append(StringEscapeUtils.escapeHtml4(file.getVirtualFile().getName()));
          if (lineNumber != null) {
            sb.append(", line ").append(lineNumber);
          }
          sb.append(")");
        } else {
          sb.append("<br>(")
            .append(StringEscapeUtils.escapeHtml4(logRecord.getCursor().getFile()))
            .append(", line ")
            .append(logRecord.getCursor().getLineNumber())
            .append(")");
        }
      }
      if (StringUtils.isNotEmpty(logRecord.getSourceFileName())) {
        sb.append("<br>(")
          .append(StringEscapeUtils.escapeHtml4(logRecord.getSourceFileName().replaceAll("[<>]", "")))
          .append(":")
          .append(StringEscapeUtils.escapeHtml4(logRecord.getSourceMethodName()))
          .append(")");
      }
      ab = ab.tooltip(sb.toString());
      if (logRecord.getMessage() != null && logRecord.getMessage().startsWith(INCLUDE_FILE_NOT_FOUND)) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document != null) {
          PsiElement element = file.findElementAt(document.getLineStartOffset(lineNumberForAnnotation));
          if (element != null && element.getParent() instanceof AsciiDocBlockMacro) {
            ab = ab.withFix(new AsciiDocCreateMissingFileIntentionAction(element.getParent()));
          }
        }
      }
      if (severity.compareTo(HighlightSeverity.ERROR) >= 0) {
        problems.add(theProblemSolver.convertToProblem(file.getVirtualFile(), lineNumberForAnnotation, 0, new String[]{logRecord.getMessage()}));
      }
      ab.create();
    }
    // consider using reportProblemsFromExternalSource available from 2019.x?
    theProblemSolver.reportProblems(file.getVirtualFile(), problems);
  }

  /**
   * For a given log record, find the source in the include tree, then traverse upwards to propagate the line number.
   */
  private int findLineByInclude(PsiFile psiFile, LogRecord log, int level) {
    // prevent too many recursions
    if (level > 64) {
      return -1;
    }
    String file = log.getCursor().getFile();
    int line = log.getCursor().getLineNumber();
    if (psiFile.getVirtualFile().getCanonicalPath() != null && psiFile.getVirtualFile().getCanonicalPath().equals(file)) {
      return line;
    }
    Collection<AsciiDocBlockMacro> includes = PsiTreeUtil.findChildrenOfType(psiFile, AsciiDocBlockMacro.class);
    for (AsciiDocBlockMacro macro : includes) {
      if (!"include".equals(macro.getMacroName())) {
        continue;
      }
      List<PsiReference> references = Arrays.asList(macro.getReferences());
      Collections.reverse(references);
      for (PsiReference reference : references) {
        if (reference instanceof AsciiDocFileReference) {
          AsciiDocFileReference fileReference = (AsciiDocFileReference) reference;
          if (!fileReference.isFolder()) {
            PsiElement resolved = fileReference.resolve();
            if (resolved instanceof AsciiDocFile) {
              int targetLine = findLineByInclude((PsiFile) resolved, log, level + 1);
              if (targetLine != -1) {
                Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
                if (document != null) {
                  return document.getLineNumber(macro.getTextOffset());
                }
              }
            }
          }
          break;
        }
      }
    }
    return -1;
  }

  private HighlightSeverity toSeverity(Severity severity) {
    switch (severity) {
      case DEBUG:
      case INFO:
      case WARN:
        return HighlightSeverity.WARNING;
      case ERROR:
      case FATAL:
        return HighlightSeverity.ERROR;
      case UNKNOWN:
      default:
        return HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING;
    }
  }

}
