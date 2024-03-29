package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;

public class AsciiDocSectionKeyIndex extends AsciiDocStringStubIndexExtension<AsciiDocSection> {
  public static final StubIndexKey<String, AsciiDocSection> KEY = StubIndexKey.createIndexKey("asciidocSection.index");

  private static final AsciiDocSectionKeyIndex OUR_INSTANCE = new AsciiDocSectionKeyIndex();

  public static AsciiDocSectionKeyIndex getInstance() {
    return OUR_INSTANCE;
  }

  @Override
  @NotNull
  public StubIndexKey<String, AsciiDocSection> getKey() {
    return KEY;
  }

  @Override
  public Class<AsciiDocSection> requiredClass() {
    return AsciiDocSection.class;
  }

  @Override
  public Collection<AsciiDocSection> get(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
    if (key.equals(AsciiDocSectionStubElementType.SECTION_WITH_VAR)) {
      return StubIndex.getElements(getKey(), key, project, scope, requiredClass());
    } else {
      String normalizedKey = AsciiDocSectionImpl.INVALID_SECTION_ID_CHARS.matcher(key.toLowerCase(Locale.US)).replaceAll("");
      normalizedKey = AsciiDocSectionStubElementType.NORMALIZED_CHARS_IN_INDEX.matcher(normalizedKey).replaceAll("");
      return StubIndex.getElements(getKey(), normalizedKey, project, scope, requiredClass());
    }
  }
}
