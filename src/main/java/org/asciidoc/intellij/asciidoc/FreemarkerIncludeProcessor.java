package org.asciidoc.intellij.asciidoc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.lexer.AsciiDocFreemarker;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

/**
 * Reads files that are included from a FreeMarker template, and neutralizes them like the template itself.
 * The includes are prepared by {@link AsciiDocFreemarker#neutralize(String, File)}.
 */
public class FreemarkerIncludeProcessor extends IncludeProcessor {
  private volatile File boundary;

  /**
   * Find the folder that must contain all included files: the content root of the project that contains the file, or the folder of the file.
   */
  public static @Nullable File findBoundary(@NotNull Project project, @Nullable File file) {
    if (file == null) {
      return null;
    }
    File absolute = file.getAbsoluteFile();
    // the file might not exist on disk, for example when rendering a new file
    VirtualFile vf = null;
    for (File candidate = absolute; candidate != null && vf == null; candidate = candidate.getParentFile()) {
      vf = LocalFileSystem.getInstance().findFileByIoFile(candidate);
    }
    VirtualFile root = vf == null ? null : ProjectFileIndex.getInstance(project).getContentRootForFile(vf);
    return root != null ? new File(root.getPath()) : absolute.getParentFile();
  }

  public void setBoundary(@Nullable File boundary) {
    this.boundary = boundary;
  }

  @Override
  public boolean handles(String target) {
    return target.startsWith(AsciiDocFreemarker.INCLUDE_PREFIX);
  }

  @Override
  public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
    String path = target.substring(AsciiDocFreemarker.INCLUDE_PREFIX.length());
    File file = new File(path);
    File currentBoundary = boundary;
    if (currentBoundary == null || !AsciiDocFreemarker.isInside(file, currentBoundary)) {
      reader.restoreLine("Include is outside of the project - include::" + path + "[]");
      return;
    }
    String content;
    try {
      content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException | RuntimeException e) {
      reader.restoreLine("Unable to read " + path + " - include::" + path + "[]");
      return;
    }
    reader.pushInclude(AsciiDocFreemarker.neutralize(content, file, currentBoundary), path, path, 1, Collections.emptyMap());
  }
}
