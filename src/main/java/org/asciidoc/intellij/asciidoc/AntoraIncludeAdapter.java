package org.asciidoc.intellij.asciidoc;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.StringUtils;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_PREFIX_AND_FAMILY_PATTERN;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ATTRIBUTES;
import static org.asciidoc.intellij.psi.AsciiDocUtil.URL_PREFIX_PATTERN;

/**
 * This {@link IncludeProcessor} translates Antora style includes to standard AsciiDoc includes.
 */
public class AntoraIncludeAdapter extends IncludeProcessor {

  private Project project;
  private VirtualFile antoraModuleDir;
  private File fileBaseDir;
  private String name;

  private String recursionPrevention;

  @Override
  public boolean handles(String target) {
    if (Objects.equals(recursionPrevention, target)) {
      recursionPrevention = null;
      return false;
    }
    if (antoraModuleDir == null) {
      return false;
    }
    Matcher urlMatcher = URL_PREFIX_PATTERN.matcher(target);
    if (urlMatcher.find()) {
      return false;
    }
    // if the first character is a slash ('/'), this is probably an already expanded Linux path name
    if (target.startsWith("/")) {
      return false;
    }
    Matcher matcher = ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(target);
    if (matcher.find()) {
      // if the second character is a colon (':'), this is probably an already expanded windows path name
      if (matcher.group().length() == 2 && matcher.group().charAt(1) == ':' && target.length() > 2 && target.charAt(2) == '/') {
        return false;
      }
      return true;
    }
    if (ATTRIBUTES.matcher(target).find()) {
      return true;
    }
    return true;
  }

  @Override
  public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
    String readFile = reader.getFile();
    VirtualFile sourceDir = null;
    VirtualFile resolved = null;
    PsiFile psiFile = null;
    if (StringUtils.isNotBlank(readFile)) {
      resolved = LocalFileSystem.getInstance().findFileByPath(reader.getFile());
      if (!readFile.contains("/") && resolved == null) {
        // if the readFile doesn't contain a full path name, this indicates safe mode.
        // Use alternative strategy to look up file relatively to the start folder.
        resolved = LocalFileSystem.getInstance().findFileByPath(new File(fileBaseDir, name).toString());
      }
      if (!readFile.contains("/") && resolved == null) {
        // if the readFile doesn't contain a full path name, this indicates safe mode.
        // Use alternative strategy to look up file somewhere in the project.
        Collection<VirtualFile> filesByNames = AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> FilenameIndex.getVirtualFilesByName(readFile, GlobalSearchScope.projectScope(project)));
        if (filesByNames.size() == 1) {
          resolved = filesByNames.iterator().next();
        }
      }
    }

    // workaround to avoid a logged error
    // https://youtrack.jetbrains.com/issue/IDEA-306288
    if (resolved != null && !resolved.isValid()) {
      resolved = null;
    }

    if (resolved != null && ATTRIBUTES.matcher(target).find()) {
      if (resolved.isValid()) {
        VirtualFile finalResolved = resolved;
        psiFile = AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> PsiManager.getInstance(project).findFile(finalResolved));
      }
      if (psiFile != null) {
        PsiFile finalPsiFile = psiFile;
        String finalTarget = target;
        String newTarget = AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> AsciiDocUtil.resolveAttributes(finalPsiFile, finalTarget));
        if (newTarget != null) {
          target = newTarget;
        }
      }
    }

    // Do this replacement even for non-Antora prefixes to support Antora Collector
    // as it might relocate paths local to the component to a different location
    String oldTarget = target;
    // if we read from an include-file, use that to determine originating module
    VirtualFile localModule;
    if (resolved != null) {
      localModule = AsciiDocUtil.findAntoraModuleDir(project, resolved);
      sourceDir = resolved.getParent();
    } else {
      localModule = null;
    }
    if (localModule != null) {
      target = AsciiDocUtil.replaceAntoraPrefix(project, localModule, sourceDir, target, "page", "include").get(0);
    }
    if (sourceDir != null && !readFile.contains("/") && !oldTarget.equals(target)) {
      // if the readFile doesn't contain a full path name, this indicates safe mode. Use a relative path in that case.
      VirtualFile targetVf = LocalFileSystem.getInstance().findFileByPath(target);
      if (targetVf != null) {
        target = VfsUtil.findRelativePath(sourceDir, targetVf, '/');
      }
    }

    Matcher matcher = ANTORA_PREFIX_AND_FAMILY_PATTERN.matcher(oldTarget);
    // if the target is unchanged and begins with an Antora prefix, enhance the error message
    if (oldTarget.equals(target) && matcher.find()) {
      String file = reader.getFile();
      if (file != null && file.length() == 0) {
        file = null;
      }
      if (LightEdit.owns(project)) {
        log(new LogRecord(Severity.WARN,
          new AsciiDocCursor(file, reader.getDir(), reader.getDir(), reader.getLineNumber() - 1),
          "Can't resolve Antora prefix while in lightedit mode"));
        reader.restoreLine("Can't resolve Antora prefix while in lightedit mode - include::" + target + "[]");
      } else if (DumbService.isDumb(project)) {
        log(new LogRecord(Severity.WARN,
          new AsciiDocCursor(file, reader.getDir(), reader.getDir(), reader.getLineNumber() - 1),
          "Can't resolve Antora prefix while indexing is in progress"));
        reader.restoreLine("Can't resolve Antora prefix while indexing is in progress - include::" + target + "[]");
      } else {
        log(new LogRecord(Severity.ERROR,
          new AsciiDocCursor(file, reader.getDir(), reader.getDir(), reader.getLineNumber() - 1),
          "Can't resolve Antora prefix '" + matcher.group() + "' for target '" + target + "'"));
        reader.restoreLine("Unresolved Antora reference - include::" + target + "[]");
      }
      return;
    }

    StringBuilder data = new StringBuilder("include::");
    data.append(target).append("[");
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      data.append(entry.getKey()).append("='").append(entry.getValue()).append("',");
    }
    if (attributes.size() > 0) {
      data.setLength(data.length() - 1);
    }
    data.append("]");
    recursionPrevention = target;
    reader.pushInclude(data.toString(), null, null, reader.getLineNumber() - 1, Collections.emptyMap());
  }

  public void setAntoraDetails(Project project, VirtualFile antoraModuleDir, File fileBaseDir, String name) {
    this.project = project;
    this.antoraModuleDir = antoraModuleDir;
    this.fileBaseDir = fileBaseDir;
    this.name = name;
  }
}
