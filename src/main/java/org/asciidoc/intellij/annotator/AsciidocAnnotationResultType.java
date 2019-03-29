package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciidocAnnotationResultType {

  private final Document document;

  private Pattern patternWithLine =
    // asciidoctor: WARNING: <stdin>: line 14: unterminated listing block
    Pattern.compile("asciidoctor: (?<severity>[A-Z]+): <stdin>: line (?<line>[0-9]+): (?<message>.*)");

  private Pattern patternWithoutLine =
    // asciidoctor: WARNING: image to embed not found or not readable: ...
    Pattern.compile("asciidoctor: (?<severity>[A-Z]+): (?<message>.*)");

  private List<Message> messages = new ArrayList<>();

  public AsciidocAnnotationResultType(Document document) {
    this.document = document;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public Document getDocument() {
    return document;
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

    public Integer getLine() {
      return line;
    }

    public String getMessage() {
      return message;
    }
  }

  public void addMessage(String message, int offsetLineNo) {
    Matcher matcherWithLine = patternWithLine.matcher(message);
    if (matcherWithLine.find()) {
      HighlightSeverity severity = toSeverity(matcherWithLine.group("severity"));
      int line = Integer.parseInt(matcherWithLine.group("line")) - offsetLineNo;
      if (line <= 0) {
        // can happend if we have an error in a prepended .asciidoctorconfig
        line = 0;
      }
      messages.add(new Message(severity, line, matcherWithLine.group("message")));
      return;
    }
    matcherWithLine = patternWithoutLine.matcher(message);
    if (matcherWithLine.find()) {
      HighlightSeverity severity = toSeverity(matcherWithLine.group("severity"));
      messages.add(new Message(severity, null,
        matcherWithLine.group("message")));
      return;
    }
    messages.add(new Message(HighlightSeverity.ERROR, null, message));
  }

  private HighlightSeverity toSeverity(String severity) {
    switch (severity) {
      case "WARNING":
        return HighlightSeverity.WARNING;
      case "ERROR":
        return HighlightSeverity.ERROR;
      default:
        return HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING;
    }
  }
}
