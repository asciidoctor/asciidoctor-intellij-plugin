package org.asciidoc.intellij.annotator;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import java.util.List;

public class AsciiDocInfoType {
  private final PsiFile file;
  private final Editor editor;
  private final String content;
  private final String config;
  private final List<String> extensions;

  public AsciiDocInfoType(PsiFile file, Editor editor, String content, String config, List<String> extensions) {
    this.file = file;
    this.editor = editor;
    this.content = content;
    this.config = config;
    this.extensions = extensions;
  }

  public PsiFile getFile() {
    return file;
  }

  public Editor getEditor() {
    return editor;
  }

  public String getContent() {
    return content;
  }

  public String getConfig() {
    return config;
  }

  public List<String> getExtensions() {
    return extensions;
  }
}
