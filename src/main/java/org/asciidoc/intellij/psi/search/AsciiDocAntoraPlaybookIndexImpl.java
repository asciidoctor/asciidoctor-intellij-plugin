package org.asciidoc.intellij.psi.search;

import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Index implementation to contain all Antora playbooks.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAntoraPlaybookIndexImpl extends ScalarIndexExtension<String> {

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return AsciiDocAntoraPlaybookIndex.NAME;
  }

  /**
   * Map all entries to a single key, so we can later retrieve them with a single lookup.
   */
  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return inputData -> Collections.singletonMap(AsciiDocAntoraPlaybookIndex.PLAYBOOK_KEY, null);
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }

  /**
   * Files to be indexed need to end with ".yml", and need to contain both "antora" and "playbook".
   */
  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return vf -> {
      String file = vf.getName();
      return file.endsWith(".yml") && file.contains("antora") && file.contains("playbook");
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Override
  public boolean indexDirectories() {
    return false;
  }

  @Override
  public int getVersion() {
    return 4;
  }

  @Override
  public boolean traceKeyHashToVirtualFileMapping() {
    return true;
  }
}
