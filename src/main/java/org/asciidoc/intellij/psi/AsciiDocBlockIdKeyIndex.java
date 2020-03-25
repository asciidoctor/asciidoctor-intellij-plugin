package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AsciiDocBlockIdKeyIndex extends StringStubIndexExtension<AsciiDocBlockId> {
  public static final StubIndexKey<String, AsciiDocBlockId> KEY = StubIndexKey.createIndexKey("asciidocBlockId.index");

  private static final AsciiDocBlockIdKeyIndex OUR_INSTANCE = new AsciiDocBlockIdKeyIndex();

  public static AsciiDocBlockIdKeyIndex getInstance() {
    return OUR_INSTANCE;
  }

  @Override
  @NotNull
  public StubIndexKey<String, AsciiDocBlockId> getKey() {
    return KEY;
  }

  @Override
  public Collection<AsciiDocBlockId> get(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
    return StubIndex.getElements(getKey(), key, project, scope, AsciiDocBlockId.class);
  }
}
