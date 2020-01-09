package org.asciidoc.intellij.psi;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AntoraModule implements Comparable<AntoraModule>  {
  private final String prefix;
  private final String component;
  private final String module;
  private final VirtualFile file;
  private String title;

  public AntoraModule(String prefix, String component, String module, String title, VirtualFile file) {
    this.prefix = prefix;
    this.component = component;
    this.module = module;
    this.title = title;
    this.file = file;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getComponent() {
    return component;
  }

  public String getModule() {
    return module;
  }

  public VirtualFile getFile() {
    return file;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public int compareTo(@NotNull AntoraModule o) {
    return prefix.compareTo(o.prefix);
  }

}
