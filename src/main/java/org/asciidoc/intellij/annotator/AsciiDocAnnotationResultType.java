package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import org.asciidoctor.log.LogRecord;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AsciiDocAnnotationResultType {

  private final Document document;
  private List<LogRecord> logRecords;

  public AsciiDocAnnotationResultType(Document document) {
    this.document = document;
  }

  public Document getDocument() {
    return document;
  }

  public List<LogRecord> getLogRecords() {
    return logRecords;
  }

  public static class Message {
    private final HighlightSeverity severity;
    private final Integer line;
    private final String message;

    public Message(HighlightSeverity severity, Integer line, String message) {
      this.severity = severity;
      this.line = line;
      this.message = message;
    }

    public HighlightSeverity getSeverity() {
      return severity;
    }

    @Nullable
    public Integer getLine() {
      return line;
    }

    public String getMessage() {
      return message;
    }
  }

  public void setLogRecords(List<LogRecord> logRecords) {
    this.logRecords = logRecords;
  }

}
