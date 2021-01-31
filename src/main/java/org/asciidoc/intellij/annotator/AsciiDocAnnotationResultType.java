package org.asciidoc.intellij.annotator;

import com.intellij.openapi.editor.Document;
import org.asciidoctor.log.LogRecord;

import java.util.List;

public class AsciiDocAnnotationResultType {

  public static class AsciiDocAnnotationLogRecord {
    private final LogRecord logRecord;
    private final Integer lineNumber;
    private final int lineNumberForAnnotation;

    public AsciiDocAnnotationLogRecord(int lineNumberForAnnotation, Integer lineNumber, LogRecord logRecord) {
      this.lineNumberForAnnotation = lineNumberForAnnotation;
      this.lineNumber = lineNumber;
      this.logRecord = logRecord;
    }

    public LogRecord getLogRecord() {
      return logRecord;
    }

    public Integer getLineNumber() {
      return lineNumber;
    }

    public int getLineNumberForAnnotation() {
      return lineNumberForAnnotation;
    }
  }

  private final Document document;
  private List<AsciiDocAnnotationLogRecord> logRecords;
  private String docname;

  public AsciiDocAnnotationResultType(Document document) {
    this.document = document;
  }

  public Document getDocument() {
    return document;
  }

  public List<AsciiDocAnnotationLogRecord> getLogRecords() {
    return logRecords;
  }

  public void setDocname(String docname) {
    this.docname = docname;
  }

  public String getDocname() {
    return docname;
  }

  public void setLogRecords(List<AsciiDocAnnotationLogRecord> logRecords) {
    this.logRecords = logRecords;
  }

}
