package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Support for the <a href="https://docs.antora.org/collector-extension/latest/configure-scan/">scan action of the Antora Collector extension</a>.
 * It maps the virtual location of a file in an Antora component (for example <code>modules/ROOT/partials/generated.adoc</code>)
 * to the file in the directory that is scanned by the collector.
 * Scan directories outside the repository are ignored.
 */
public final class AntoraCollector {

  private AntoraCollector() {
  }

  private record Scan(String dir, String into, @Nullable String files) {
  }

  /**
   * @param antoraComponentDir the directory that contains the <code>antora.yml</code>
   * @param antora             the parsed <code>antora.yml</code>
   * @param file               the absolute path of a file or directory inside the Antora component
   * @return the file in a scan directory that replaces the file, or an empty list. The last scan wins as it overwrites previous ones.
   */
  public static @NotNull List<String> resolve(@NotNull Project project, @NotNull VirtualFile antoraComponentDir,
                                              @NotNull Map<String, Object> antora, @NotNull String file) {
    List<Scan> scans = getScans(antora);
    if (scans.isEmpty()) {
      return Collections.emptyList();
    }
    String componentPath = antoraComponentDir.getCanonicalPath();
    if (componentPath == null) {
      return Collections.emptyList();
    }
    VirtualFile root = null;
    String result = null;
    for (Scan scan : scans) {
      String base = componentPath + (scan.into().isEmpty() ? "" : "/" + scan.into());
      String rest;
      String normalizedFile = file.replace('\\', '/');
      if (normalizedFile.equals(base)) {
        rest = "";
      } else if (normalizedFile.startsWith(base + "/")) {
        rest = normalizedFile.substring(base.length() + 1);
      } else {
        continue;
      }
      if (root == null) {
        root = findRoot(project, antoraComponentDir);
      }
      String candidate = map(root, antoraComponentDir, componentPath, scan, rest);
      if (candidate != null) {
        result = candidate;
      }
    }
    return result == null ? Collections.emptyList() : Collections.singletonList(result);
  }

  private static @Nullable String map(VirtualFile root, VirtualFile antoraComponentDir, String componentPath, Scan scan, String rest) {
    // the scan directory is typically excluded and not indexed, therefore use the virtual file system and not the index
    String dir = scan.dir();
    String scanDirPath;
    if (new File(dir).isAbsolute() || dir.startsWith("/")) {
      scanDirPath = dir;
    } else if (dir.equals(".") || dir.startsWith("./")) {
      scanDirPath = componentPath + "/" + dir;
    } else {
      scanDirPath = root.getPath() + "/" + dir;
    }
    VirtualFile scanDir = antoraComponentDir.getFileSystem().findFileByPath(FileUtil.toCanonicalPath(scanDirPath));
    if (scanDir == null) {
      return null;
    }
    String normalizedRest = FileUtil.toCanonicalPath(rest);
    if (normalizedRest == null || normalizedRest.startsWith("../") || normalizedRest.equals("..")) {
      return null;
    }
    VirtualFile candidate = normalizedRest.isEmpty() ? scanDir : scanDir.findFileByRelativePath(normalizedRest);
    if (candidate == null) {
      return null;
    }
    // never leave the repository, not even via symbolic links
    VirtualFile realCandidate = candidate.getCanonicalFile();
    VirtualFile realScanDir = scanDir.getCanonicalFile();
    VirtualFile realRoot = root.getCanonicalFile();
    if (realCandidate == null || realScanDir == null || realRoot == null
      || !VfsUtilCore.isAncestor(realRoot, realCandidate, false)
      || !VfsUtilCore.isAncestor(realRoot, realScanDir, false)
      || !VfsUtilCore.isAncestor(realScanDir, realCandidate, false)) {
      return null;
    }
    if (!normalizedRest.isEmpty() && scan.files() != null && !matches(scan.files(), normalizedRest)) {
      return null;
    }
    return candidate.getPath();
  }

  private static boolean matches(String pattern, String relativePath) {
    Path path = Paths.get(relativePath);
    List<String> patterns = new ArrayList<>();
    patterns.add(pattern);
    String stripped = pattern;
    // micromatch also matches files in the root of the directory with "**/"
    while (stripped.startsWith("**/")) {
      stripped = stripped.substring(3);
      patterns.add(stripped);
    }
    for (String p : patterns) {
      try {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + p);
        if (matcher.matches(path)) {
          return true;
        }
      } catch (IllegalArgumentException e) {
        // unsupported pattern, ignore it
      }
    }
    return false;
  }

  /**
   * The root of the repository, as scan directories must not leave it.
   */
  private static VirtualFile findRoot(Project project, VirtualFile antoraComponentDir) {
    for (VirtualFile dir = antoraComponentDir; dir != null; dir = dir.getParent()) {
      if (dir.findChild(".git") != null) {
        return dir;
      }
    }
    VirtualFile contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(antoraComponentDir);
    return contentRoot != null ? contentRoot : antoraComponentDir;
  }

  private static List<Scan> getScans(Map<String, Object> antora) {
    List<Scan> result = new ArrayList<>();
    Object ext = antora.get("ext");
    if (!(ext instanceof Map<?, ?> extMap)) {
      return result;
    }
    Object collector = extMap.get("collector");
    if (collector instanceof List<?> list) {
      for (Object item : list) {
        addScans(item, result);
      }
    } else {
      addScans(collector, result);
    }
    return result;
  }

  private static void addScans(Object item, List<Scan> result) {
    if (!(item instanceof Map<?, ?> itemMap)) {
      return;
    }
    Object scan = itemMap.get("scan");
    if (scan instanceof List<?> list) {
      for (Object entry : list) {
        addScan(entry, result);
      }
    } else {
      addScan(scan, result);
    }
  }

  private static void addScan(Object entry, List<Scan> result) {
    String dir;
    String into = "";
    String files = null;
    if (entry instanceof String string) {
      dir = string;
    } else if (entry instanceof Map<?, ?> map) {
      if (!(map.get("dir") instanceof String dirValue)) {
        return;
      }
      dir = dirValue;
      // "base" is the deprecated name of "into"
      Object intoValue = map.get("into") != null ? map.get("into") : map.get("base");
      if (intoValue instanceof Map<?, ?> intoMap) {
        if (intoMap.get("name") != null || intoMap.get("version") != null) {
          // scans into a different component version, this is not supported
          return;
        }
        intoValue = intoMap.get("dir");
      }
      if (intoValue != null) {
        into = intoValue.toString().replace('\\', '/').replaceAll("^/+|/+$", "");
        if (("/" + into + "/").contains("/../")) {
          return;
        }
      }
      if (map.get("files") instanceof String filesValue) {
        files = filesValue;
      }
    } else {
      return;
    }
    result.add(new Scan(dir, into, files));
  }
}
