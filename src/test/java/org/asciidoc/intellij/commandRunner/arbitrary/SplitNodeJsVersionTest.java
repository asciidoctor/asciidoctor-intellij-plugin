package org.asciidoc.intellij.commandRunner.arbitrary;

import com.google.common.primitives.ImmutableIntArray;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertArrayEquals;

/**
 * Test {@link SplitNodeJsVersion}.
 */
public class SplitNodeJsVersionTest {
  @Test
  public void sortTest() {
    File[] versions = new File[]{
      new File("v12.13.14"),
      new File("v44.13.0"),
      new File("v14.10"),
      new File("v44.13.0-rc1"),
      new File("v19"),
      new File("v44.13"),
      new File("v44.13.0-rc3")
    };
    SplitNodeJsVersion[] sortedNodeJsVersions = Arrays.stream(versions)
      .map(SplitNodeJsVersion::parse)
      .filter(Objects::nonNull)
      .sorted()
      .toArray(SplitNodeJsVersion[]::new);

    SplitNodeJsVersion[] expected = new SplitNodeJsVersion[]{
      new SplitNodeJsVersion(new File("v44.13.0"), ImmutableIntArray.of(44, 13, 0), null),
      new SplitNodeJsVersion(new File("v44.13.0-rc3"), ImmutableIntArray.of(44, 13, 0), "rc3"),
      new SplitNodeJsVersion(new File("v44.13.0-rc1"), ImmutableIntArray.of(44, 13, 0), "rc1"),
      new SplitNodeJsVersion(new File("v44.13"), ImmutableIntArray.of(44, 13), null),
      new SplitNodeJsVersion(new File("v19"), ImmutableIntArray.of(19), null),
      new SplitNodeJsVersion(new File("v14.10"), ImmutableIntArray.of(14, 10), null),
      new SplitNodeJsVersion(new File("v12.13.14"), ImmutableIntArray.of(12, 13, 14), null),
    };
    assertArrayEquals(sortedNodeJsVersions, expected);
  }
}
