package org.asciidoc.intellij;

import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Schwartz 2018
 */
public class CollectingLogHandler implements LogHandler {

  private List<LogRecord> logRecords = new ArrayList<>();

  @Override
  public void log(LogRecord logRecord) {
    logRecords.add(logRecord);
  }

  public List<LogRecord> getLogRecords() {
    return logRecords;
  }
}
