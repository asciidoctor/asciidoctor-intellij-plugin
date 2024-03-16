package org.asciidoc.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Index implementation to contain all Antora playbooks.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocLinkIndexImpl extends ScalarIndexExtension<String> {

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return AsciiDocLinkIndex.NAME;
  }

  /**
   * Map all entries to a single key, so we can later retrieve them with a single lookup.
   */
  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return inputData -> {
      String canonicalPath = inputData.getFile().getCanonicalPath();
      return Collections.singletonMap(canonicalPath, null);
    };
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
      if (!vf.getFileType().equals(AsciiDocFileType.INSTANCE)) {
        return false;
      }
      return !Objects.equals(vf.getPath(), vf.getCanonicalPath());
    };
  }

  @Override
  public @NotNull Collection<FileType> getFileTypesWithSizeLimitNotApplicable() {
    return super.getFileTypesWithSizeLimitNotApplicable();
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
    return 1;
  }

  @Override
  public boolean traceKeyHashToVirtualFileMapping() {
    return true;
  }
}
