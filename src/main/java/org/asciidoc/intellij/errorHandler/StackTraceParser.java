package org.asciidoc.intellij.errorHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The IDE hands over a {@link Throwable} without message and stack frames for errors reported from its error dialog;
 * only the stack trace text is available through the public API.
 * This rebuilds a throwable (including its causes) from that text, so that the frames can be reported.
 */
final class StackTraceParser {

  private static final Pattern FRAME = Pattern.compile("^\\s*at (?:[\\w.$-]+(?:@[\\w.$-]+)?/)?([\\w.$<>]+)\\.([\\w$<>]+)\\((?:([^:)]+)(?::(-?\\d+))?|Native Method|Unknown Source)\\)\\s*$");
  private static final String CAUSED_BY = "Caused by: ";

  private StackTraceParser() {
  }

  /**
   * Thrown in place of an exception which is only known by its text.
   * The original class name is part of the message.
   */
  static final class ReportedThrowable extends Throwable {
    ReportedThrowable(String message, @Nullable Throwable cause) {
      super(message, cause);
    }
  }

  static @NotNull Throwable parse(@NotNull String text) {
    List<String> headers = new ArrayList<>();
    List<List<StackTraceElement>> frames = new ArrayList<>();
    for (String line : text.lines().toList()) {
      Matcher matcher = FRAME.matcher(line);
      if (matcher.matches() && !frames.isEmpty()) {
        String file = matcher.group(3);
        int number = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : -1;
        frames.get(frames.size() - 1).add(new StackTraceElement(matcher.group(1), matcher.group(2), file, number));
      } else if (line.startsWith(CAUSED_BY)) {
        headers.add(line.substring(CAUSED_BY.length()));
        frames.add(new ArrayList<>());
      } else if (headers.isEmpty() && !line.isBlank()) {
        headers.add(line);
        frames.add(new ArrayList<>());
      } else if (headers.size() == frames.size() && !headers.isEmpty() && !line.isBlank()
        && frames.get(frames.size() - 1).isEmpty()) {
        // multi-line message
        headers.set(headers.size() - 1, headers.get(headers.size() - 1) + "\n" + line);
      }
    }
    if (headers.isEmpty()) {
      return new ReportedThrowable(text, null);
    }
    Throwable cause = null;
    for (int i = headers.size() - 1; i >= 0; i--) {
      Throwable current = new ReportedThrowable(headers.get(i), cause);
      current.setStackTrace(frames.get(i).toArray(new StackTraceElement[0]));
      cause = current;
    }
    return cause;
  }
}
