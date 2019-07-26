package org.asciidoc.intellij.annotator;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import java.util.List;

public class AsciiDocInfoType {
  private final PsiFile file;
  private final Editor editor;
  private final String contentWithConfig;
  private final List<String> extensions;
  private final int offsetLineNo;

  public AsciiDocInfoType(PsiFile file, Editor editor, String contentWithConfig, List<String> extensions, int offsetLineNo) {
    this.file = file;
    this.editor = editor;
    this.contentWithConfig = contentWithConfig;
    this.extensions = extensions;
    this.offsetLineNo = offsetLineNo;
  }

  public PsiFile getFile() {
    return file;
  }

  public Editor getEditor() {
    return editor;
  }

  public String getContentWithConfig() {
    return contentWithConfig;
  }

  public List<String> getExtensions() {
    return extensions;
  }

  public int getOffsetLineNo() {
    return offsetLineNo;
  }
}
