package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This {@link IncludeProcessor} translates Antora style includes to standard AsciiDoc includes.
 */
public class AntoraIncludeAdapter extends IncludeProcessor {

  private static final Pattern ANTORA = Pattern.compile("^([a-z]*)\\$");

  @Override
  public boolean handles(String target) {
    Matcher matcher = ANTORA.matcher(target);
    return matcher.find();
  }

  @Override
  public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
    Matcher matcher = ANTORA.matcher(target);
    if (matcher.find()) {
      String match = matcher.group(1);
      String antoraprefix = null;
      // example -> examplesdir, partial -> partialsdir, etc.
      String val = (String) document.getAttribute(match + "sdir");
      if (val != null) {
        antoraprefix = val + "/";
      }
      if (antoraprefix == null) {
        // nothing found, show an error
        String file = reader.getFile();
        if (file != null && file.length() == 0) {
          file = null;
        }
        log(new LogRecord(Severity.ERROR,
          new AsciiDocCursor(file, reader.getDir(), reader.getDir(), reader.getLineNumber() - 1),
          "Can't resolve Antora prefix " + match));
        reader.restoreLine("Unresolved Antora prefix '" + match + "'- include::" + target + "[]");
        return;
      }
      target = matcher.replaceFirst(Matcher.quoteReplacement(antoraprefix));
    } else {
      throw new RuntimeException("matcher didn't match despite handles() method: " + target);
    }
    StringBuilder data = new StringBuilder("include::");
    data.append(target).append("[");
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      data.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
    }
    data.append("]");
    reader.push_include(data.toString(), null, null, reader.getLineNumber() - 1, Collections.emptyMap());
  }
}
