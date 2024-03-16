package org.asciidoc.intellij.psi.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Index that specifically contains only file system links
 * This should avoid a situation where IntelliJ sometime has outdated information about files that leads to PSI problems.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocLinkIndex {
  @NonNls
  public static final ID<String, Void> NAME = ID.create("AsciiDocLinkIndex");

  public static Collection<VirtualFile> getLinkSources(@NotNull Project project, String target) {
    Set<VirtualFile> files = CollectionFactory.createSmallMemoryFootprintSet();
    FileBasedIndex.getInstance().processValues(NAME, target, null, (file, value) -> {
      files.add(file);
      return true;
    }, new AsciiDocSearchScope(project), null);
    return files;
  }

}
