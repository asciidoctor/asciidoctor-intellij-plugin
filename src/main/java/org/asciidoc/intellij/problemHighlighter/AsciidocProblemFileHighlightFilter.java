package org.asciidoc.intellij.problemHighlighter;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * Include asciidoctor files for project view error highlighting.
 *
 * @author Alexander Schwartz 2019
 */
public class AsciidocProblemFileHighlightFilter implements Condition<VirtualFile> {

  @Override
  public boolean value(VirtualFile virtualFile) {
    return virtualFile.getFileType() == AsciiDocFileType.INSTANCE;
  }
}
