package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.Executor;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;

public interface AsciiDocRunner {
  ExtensionPointName<AsciiDocRunner> EP_NAME = ExtensionPointName.create("org.asciidoc.intellij.asciidocRunner");

  boolean isApplicable(Language language);

  boolean run(String command, Project project, VirtualFile virtualFile, Executor executor);

  @Nls
  String getTitle();
}
