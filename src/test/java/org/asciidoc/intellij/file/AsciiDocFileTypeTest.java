package org.asciidoc.intellij.file;

import org.junit.Test;

import static org.junit.Assert.*;

public class AsciiDocFileTypeTest {

  @Test
  public void testRemoveExtensionForAsciiDocFile() {
    assertEquals("test", AsciiDocFileType.removeAsciiDocExtension("test.adoc"));
  }

  @Test
  public void testKeepExtensionForOtherFiles() {
    assertEquals("test.txt", AsciiDocFileType.removeAsciiDocExtension("test.txt"));
  }

  @Test
  public void testHasAsciiDocExtension() {
    assertTrue(AsciiDocFileType.hasAsciiDocExtension("test.adoc"));
  }

  @Test
  public void testHasntAsciiDocExtension() {
    assertFalse(AsciiDocFileType.hasAsciiDocExtension("test.txt"));
  }
}
