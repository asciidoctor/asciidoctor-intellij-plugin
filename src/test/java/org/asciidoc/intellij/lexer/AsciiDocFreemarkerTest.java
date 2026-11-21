package org.asciidoc.intellij.lexer;

import junit.framework.TestCase;

import java.io.File;

public class AsciiDocFreemarkerTest extends TestCase {

  public void testDetection() {
    assertTrue(AsciiDocFreemarker.isTemplate("<#import \"/templates/guide.adoc\" as tmpl>\n\n= Title\n"));
    assertTrue(AsciiDocFreemarker.isTemplate("= Title\n\n  <@kc.start parameters=\"--x\"/>\n"));
    assertFalse(AsciiDocFreemarker.isTemplate("= Title\n\ntext <#import> text\n"));
    assertFalse(AsciiDocFreemarker.isTemplate("= Title\n\n<@user> and <#else>\n"));
    assertFalse(AsciiDocFreemarker.isTemplate("= Title\n\n----\n#include <stdio.h>\n----\n"));
  }

  public void testUnchangedIfNotATemplate() {
    String text = "* one\n<#else>\n* two\n";
    assertSame(text, AsciiDocFreemarker.neutralize(text));
  }

  public void testDirectivesBecomeCommentsAndKeepLineCount() {
    String text = "<#import \"/templates/guide.adoc\" as tmpl>\n<@tmpl.guide\ntitle=\"a > b\"\nsummary=\"x\">\n\n* one\n<#if x>\n* two\n</#if>\n</@tmpl.guide>";
    String result = AsciiDocFreemarker.neutralize(text);
    assertEquals("// <#import \"/templates/guide.adoc\" as tmpl>\n// <@tmpl.guide\n// title=\"a > b\"\n// summary=\"x\">\n\n* one\n// <#if x>\n* two\n// </#if>\n// </@tmpl.guide>", result);
    assertEquals(text.split("\n", -1).length, result.split("\n", -1).length);
  }

  public void testInlineTags() {
    String result = AsciiDocFreemarker.neutralize("<#import \"x\" as x>\nSee <@links.ha id=\"a\" /> and <#if x>this</#if> and <b>bold</b>.\n");
    assertEquals("// <#import \"x\" as x>\nSee pass:[<code>links.ha id=\"a\"</code>] and this and <b>bold</b>.\n", result);
  }

  public void testNoCommentAfterEmptyLineSoListContinues() {
    String result = AsciiDocFreemarker.neutralize(". one\n\n<#if x>\n. two\n</#if>\n\n. three\n");
    assertEquals(". one\n\n\n. two\n// </#if>\n\n. three\n", result);
  }

  public void testIncludeBecomesAsciiDocInclude() throws Exception {
    java.nio.file.Path root = java.nio.file.Files.createTempDirectory("freemarker");
    try {
      java.nio.file.Path guide = root.resolve("guides/sub/guide.adoc");
      java.nio.file.Files.createDirectories(guide.getParent());
      java.nio.file.Files.createDirectories(root.resolve("guides/partials"));
      java.nio.file.Files.writeString(root.resolve("guides/partials/a.adoc"), "a");
      java.nio.file.Files.writeString(guide.getParent().resolve("b.adoc"), "b");
      String result = AsciiDocFreemarker.neutralize(
        "<#import \"/partials/a.adoc\" as a>\n<#include \"/partials/a.adoc\" />\n<#include \"b.adoc\">\n<#include \"missing.adoc\">\n",
        guide.toFile(), root.toFile());
      String prefix = "include::" + AsciiDocFreemarker.INCLUDE_PREFIX;
      assertEquals("// <#import \"/partials/a.adoc\" as a>\n"
        + prefix + root.resolve("guides/partials/a.adoc").toString().replace('\\', '/') + "[]\n"
        + prefix + guide.getParent().resolve("b.adoc").toString().replace('\\', '/') + "[]\n"
        + "// <#include \"missing.adoc\">\n", result);
    } finally {
      try (var files = java.nio.file.Files.walk(root)) {
        files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
      }
    }
  }

  public void testIncludeOutsideOfBoundaryIsNotResolved() throws Exception {
    java.nio.file.Path root = java.nio.file.Files.createTempDirectory("freemarker");
    try {
      java.nio.file.Path guide = root.resolve("project/guides/guide.adoc");
      java.nio.file.Files.createDirectories(guide.getParent());
      java.nio.file.Files.writeString(root.resolve("secret.adoc"), "secret");
      java.nio.file.Files.writeString(root.resolve("project/inside.adoc"), "inside");
      File boundary = root.resolve("project").toFile();
      assertNull(AsciiDocFreemarker.resolve("../../secret.adoc", guide.toFile(), boundary));
      assertNull(AsciiDocFreemarker.resolve("/secret.adoc", guide.toFile(), boundary));
      assertNull(AsciiDocFreemarker.resolve("/etc/passwd", guide.toFile(), boundary));
      assertNotNull(AsciiDocFreemarker.resolve("/inside.adoc", guide.toFile(), boundary));
    } finally {
      try (var files = java.nio.file.Files.walk(root)) {
        files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
      }
    }
  }

  public void testVerbatimBlocks() {
    String result = AsciiDocFreemarker.neutralize("<#import \"x\" as x>\n----\n<#noparse>\n${x} <@a.b />\n</#noparse>\n----\n<#if x>\n");
    assertEquals("// <#import \"x\" as x>\n----\n\n${x} \n\n----\n// <#if x>\n", result);
  }
}
