package org.asciidoc.intellij.psi;

import com.intellij.mock.MockVirtualFile;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.TreeSet;

public class AsciiDocUtilTest {

  @Test
  public void shouldAddOneEntry() {
    // given...
    TreeSet<String> roots = new TreeSet<>();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    Assertions.assertThat(roots).containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDeduplicateTwoIdenticalEntries() {
    // given...
    TreeSet<String> roots = new TreeSet<>();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    Assertions.assertThat(roots).containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDominateLongerRootByShorterRootWithShorterPathFirst() {
    // given...
    TreeSet<String> roots = new TreeSet<>();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root/subpath"));

    // then...
    Assertions.assertThat(roots).containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

  @Test
  public void shouldDominateLongerRootByShorterRootWithLongerPathFirst() {
    // given...
    TreeSet<String> roots = new TreeSet<>();

    // when...
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root/subpath"));
    AsciiDocUtil.addRoot(roots, new MockVirtualFile(true, "root"));

    // then...
    Assertions.assertThat(roots).containsExactlyInAnyOrder("MOCK_ROOT:/root");
  }

}
