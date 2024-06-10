package org.asciidoc.intellij.psi;

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AsciiDocUtilTest {

  @Test
  public void shouldAddOneEntry() {
    // given...
    TreeSet<VirtualFile> roots = initialRoots();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    assertThat(roots)
      .containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDeduplicateTwoIdenticalEntries() {
    // given...
    TreeSet<VirtualFile> roots = initialRoots();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    assertThat(roots)
      .containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDominateLongerRootByShorterRootWithShorterPathFirst() {
    // given...
    TreeSet<VirtualFile> roots = initialRoots();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root/subpath"));

    // then...
    assertThat(roots)
      .containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDominateLongerRootByShorterRootWithLongerPathFirst() {
    // given...
    TreeSet<VirtualFile> roots = initialRoots();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root/subpath"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    assertThat(roots)
      .containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  private static @NotNull ListAssert<String> assertThat(TreeSet<VirtualFile> roots) {
    return Assertions.assertThat(roots.stream().map(VirtualFile::getPath).collect(Collectors.toList()));
  }

  private static @NotNull TreeSet<VirtualFile> initialRoots() {
    return new TreeSet<>(Comparator.comparing(VirtualFile::getPath));
  }

}
