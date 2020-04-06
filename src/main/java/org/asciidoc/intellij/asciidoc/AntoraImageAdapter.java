package org.asciidoc.intellij.asciidoc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntoraImageAdapter extends Postprocessor {

  public void setAntoraDetails(Project project, VirtualFile antoraModuleDir, File fileBaseDir) {
    this.project = project;
    this.antoraModuleDir = antoraModuleDir;
    this.fileBaseDir = fileBaseDir;
  }

  private Project project;
  private VirtualFile antoraModuleDir;
  private File fileBaseDir;

  @Override
  public String process(Document document, String html) {
    if (antoraModuleDir != null) {
      Pattern pattern = Pattern.compile("<img src=\"((?<component>[a-zA-Z0-9._-]*):((?<module>[a-zA-Z0-9._-]*):)?(?<resource>[^:\"/][^:\"]*))\"");
      Matcher matcher = pattern.matcher(html);
      while (matcher.find()) {
        final MatchResult matchResult = matcher.toMatchResult();
        String file = matchResult.group(1);
        file = prepareFile(file);
        if (file == null) {
          continue;
        }
        String replacement = "<img src=\"" + file + "\"";
        html = html.substring(0, matchResult.start()) +
          replacement + html.substring(matchResult.end());
        matcher.reset(html);
      }
      pattern = Pattern.compile("\"<object ([^>])*data=\"((?<component>[a-zA-Z0-9._-]*):((?<module>[a-zA-Z0-9._-]*):)?(?<resource>[^:\"/][^:\"]*))\"");
      matcher = pattern.matcher(html);
      while (matcher.find()) {
        final MatchResult matchResult = matcher.toMatchResult();
        String file = matchResult.group(1);
        String other = matchResult.group(1);
        if (other == null) {
          other = "";
        }
        file = prepareFile(file);
        if (file == null) {
          continue;
        }
        String replacement = "<object " + other + "data=\"" + file + "\"";
        html = html.substring(0, matchResult.start()) +
          replacement + html.substring(matchResult.end());
        matcher.reset(html);
      }
    }
    return html;
  }

  @Nullable
  private String prepareFile(String file) {
    List<String> replaced = AsciiDocUtil.replaceAntoraPrefix(project, antoraModuleDir, file, "image");
    if (replaced.size() == 1 && replaced.get(0).equals(file)) {
      // unable to replace; avoid loop
      return null;
    }
    file = replaced.get(0);
    String relativePath = FileUtil.getRelativePath(fileBaseDir, new File(file));
    if (relativePath != null) {
      relativePath = relativePath.replaceAll("\\\\", "/");
      file = relativePath;
    }
    return file;
  }
}
