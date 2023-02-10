package org.asciidoc.intellij.psi.search;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Index that specifically contains only the Antora Playbooks.
 * Created a special index for this as it would otherwise be difficult to match the
 * files that would be contained here efficiently.
 * <p>
 * List of virtual files can be retrieved by project.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAntoraPlaybookIndex {
  @NonNls
  public static final ID<String, Void> NAME = ID.create("AsciiDocAntoraPlaybookIndex");

  /**
   * Static key to be used for indexing, as we currently don't care about the name of the key, only want to retrieve the files.
   */
  public static final String PLAYBOOK_KEY = "antora-playbook.yml";

  public static Collection<VirtualFile> getVirtualFiles(@NotNull Project project) {
    Set<VirtualFile> files = CollectionFactory.createSmallMemoryFootprintSet();
    FileBasedIndex.getInstance().processValues(NAME, PLAYBOOK_KEY, null, (file, value) -> {
      if (AsciiDocUtil.findAntoraModuleDir(project, file) == null) {
        // if a playbook is located in an example folder, ignore it
        files.add(file);
      }
      return true;
    }, new AsciiDocSearchScope(project), null);
    return files;
  }

}
