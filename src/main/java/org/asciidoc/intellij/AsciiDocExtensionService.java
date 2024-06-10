package org.asciidoc.intellij;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.compress.utils.Lists.newArrayList;
import static org.asciidoc.intellij.psi.AsciiDocUtil.getRoots;

@Service
public final class AsciiDocExtensionService {

  private static final List<String> EXTENSION_FILE_EXTENSIONS = Arrays.asList("rb", "jar");

  public @NotNull List<String> getExtensions(Project project) {
    return getRoots(project).stream()
      .flatMap(dir -> getExtensionFileInDir(dir).stream())
      .distinct()
      .toList();
  }

  private @NotNull List<String> getExtensionFileInDir(VirtualFile value) {
    return Optional.ofNullable(value)
      .filter(VirtualFile::isValid)
      .flatMap(dir -> Optional.ofNullable(dir.findChild(".asciidoctor")))
      .flatMap(file -> Optional.ofNullable(file.findChild("lib")))
      .flatMap(file -> Optional.ofNullable(file.getChildren()))
      .map(this::getExtensionFilePaths)
      .orElse(newArrayList());
  }

  @NotNull
  private List<String> getExtensionFilePaths(@NotNull final VirtualFile[] virtualFiles) {
    return Arrays.stream(virtualFiles)
      .filter(this::isExtensionFile)
      .map(VirtualFile::getCanonicalPath)
      .filter(Objects::nonNull)
      .collect(toList());
  }

  private boolean isExtensionFile(@NotNull final VirtualFile virtualFile) {
    return EXTENSION_FILE_EXTENSIONS.contains(virtualFile.getExtension());
  }
}
