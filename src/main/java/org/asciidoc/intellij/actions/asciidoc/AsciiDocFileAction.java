package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public abstract class AsciiDocFileAction extends AsciiDocAction {
  @Override
  public void update(@NotNull AnActionEvent event) {
    VirtualFile file = event.getData(LangDataKeys.VIRTUAL_FILE);
    if (file == null) {
      event.getPresentation().setEnabledAndVisible(false);
      return;
    }
    if (!file.isValid() || file.isDirectory() || file.getFileType() != AsciiDocFileType.INSTANCE) {
      event.getPresentation().setEnabledAndVisible(false);
      return;
    }

    super.update(event);
  }

  /**
   * Find the folder to write the output to.
   * This is the file's parent, unless the parent is not on the local file system (for example inside an archive).
   * Then it is the first writable folder above it; for an archive this starts with the folder that contains the archive.
   *
   * @return the folder, or null if there is no parent (for example in a fragment editor), or no writable folder
   */
  @Nullable
  protected static VirtualFile findTargetDirectory(@NotNull VirtualFile file) {
    VirtualFile dir = file.getParent();
    if (dir == null || dir.isInLocalFileSystem()) {
      return dir;
    }
    while (dir != null) {
      if (dir.getFileSystem() instanceof JarFileSystem jarFileSystem) {
        VirtualFile archive = jarFileSystem.getVirtualFileForJar(dir);
        dir = archive == null ? null : archive.getParent();
        continue;
      }
      Path path = dir.getFileSystem().getNioPath(dir);
      if (path != null && Files.isWritable(path)) {
        return dir;
      }
      dir = dir.getParent();
    }
    return null;
  }

  @Nullable
  protected static VirtualFile findOutputFile(@Nullable VirtualFile target, @NotNull VirtualFile file, @NotNull String extension) {
    if (target == null) {
      return null;
    }
    Path path = target.getFileSystem().getNioPath(target);
    if (path == null) {
      return null;
    }
    return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(
      path.resolve(file.getName().replaceAll("\\.(adoc|asciidoc|ad)$", "." + extension))
    );
  }

  /**
   * Convert the file and write the output to the target folder.
   * Files that are not on the local file system are converted from a temporary copy of the content.
   */
  protected static boolean convert(@NotNull AsciiDocWrapper wrapper, @NotNull VirtualFile file, @NotNull String content,
                                   @Nullable VirtualFile target, String config, List<String> extensions,
                                   AsciiDocWrapper.FileType type) throws IOException {
    if (file.isInLocalFileSystem()) {
      return wrapper.convertTo(new File(Objects.requireNonNull(file.getCanonicalPath())), config, extensions, type);
    }
    Path tempDir = Files.createTempDirectory("asciidoctor-intellij-source");
    try {
      Path source = tempDir.resolve(file.getName());
      Files.writeString(source, content, StandardCharsets.UTF_8);
      return wrapper.convertTo(source.toFile(), config, extensions, type, Objects.requireNonNull(target).toNioPath());
    } finally {
      FileUtils.deleteDirectory(tempDir.toFile());
    }
  }
}
