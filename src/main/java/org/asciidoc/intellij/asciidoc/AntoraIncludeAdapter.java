package org.asciidoc.intellij.asciidoc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_AND_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;

/**
 * This {@link IncludeProcessor} translates Antora style includes to standard AsciiDoc includes.
 */
public class AntoraIncludeAdapter extends IncludeProcessor {

  private Project project;
  private VirtualFile antoraModuleDir;

  @Override
  public boolean handles(String target) {
    if (antoraModuleDir == null) {
      return false;
    }
    Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(target);
    if (urlMatcher.find()) {
      return false;
    }
    Matcher matcher = ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(target);
    if (matcher.find()) {
      // if the second character is a colon, this is probably an already expanded windows path name
      if (matcher.group().length() == 2 && matcher.group().charAt(1) == ':' && target.length() > 2 && target.charAt(2) == '/') {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
    Matcher matcher = ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(target);
    if (matcher.find()) {
      String oldTarget = target;
      // if we read from an include-file, use that to determine originating module
      VirtualFile localModule = antoraModuleDir;
      String readFile = reader.getFile();
      if (StringUtils.isNotBlank(readFile)) {
        VirtualFile resolved = LocalFileSystem.getInstance().findFileByPath(reader.getFile());
        if (resolved != null) {
          localModule = AsciiDocUtil.findAntoraModuleDir(project.getBaseDir(), resolved);
        } else {
          localModule = null;
        }
      }
      if (localModule != null) {
        target = AsciiDocUtil.replaceAntoraPrefix(project, localModule, target, null).get(0);
      }
      if (oldTarget.equals(target)) {
        String file = reader.getFile();
        if (file != null && file.length() == 0) {
          file = null;
        }
        log(new LogRecord(Severity.ERROR,
          new AsciiDocCursor(file, reader.getDir(), reader.getDir(), reader.getLineNumber() - 1),
          "Can't resolve Antora prefix " + matcher.group()));
        reader.restoreLine("Unresolved Antora prefix '" + matcher.group() + "'- include::" + target + "[]");
        return;
      }
    } else {
      throw new RuntimeException("matcher didn't find a match");
    }
    StringBuilder data = new StringBuilder("include::");
    data.append(target).append("[");
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      data.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
    }
    data.append("]");
    reader.push_include(data.toString(), null, null, reader.getLineNumber() - 1, Collections.emptyMap());
  }

  public void setAntoraDetails(Project project, VirtualFile antoraModuleDir) {
    this.project = project;
    this.antoraModuleDir = antoraModuleDir;
  }
}
