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
    assertEquals("// <#import \"x\" as x>\nSee `+<@links.ha id=\"a\" />+` and this and <b>bold</b>.\n", result);
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

  public void testSimpleMacrosAreExpanded() throws Exception {
    java.nio.file.Path root = java.nio.file.Files.createTempDirectory("freemarker");
    try {
      java.nio.file.Path guide = root.resolve("guides/guide.adoc");
      java.nio.file.Files.createDirectories(guide.getParent());
      java.nio.file.Files.createDirectories(root.resolve("templates"));
      java.nio.file.Files.writeString(root.resolve("templates/links.adoc"), """
        <#macro ha id anchor="">link:{links_ha_${id}_url}<#if anchor != "">#${anchor}</#if>[{links_ha_${id}_name}]</#macro>
        """);
      java.nio.file.Files.writeString(root.resolve("templates/profile.adoc"), """
        <#macro ifProduct>
        ifeval::[{project_product} == true]
        <#nested>
        endif::[]
        </#macro>

        <#macro complex>
        line one
        line two
        <#nested>
        </#macro>
        """);
      String result = AsciiDocFreemarker.neutralize("""
        <#import "/templates/links.adoc" as links>
        <#import "/templates/profile.adoc" as profile>
        See <@links.ha id="a"/> and <@links.ha id='b' anchor="c" />, <@links.ha id=x/>.
        <@profile.ifProduct>
        product text
        </@profile.ifProduct>
        <@profile.complex>
        text
        </@profile.complex>
        """, guide.toFile(), root.toFile());
      assertEquals("""
        // <#import "/templates/links.adoc" as links>
        // <#import "/templates/profile.adoc" as profile>
        See link:{links_ha_a_url}[{links_ha_a_name}] and link:{links_ha_b_url}#c[{links_ha_b_name}], `+<@links.ha id=x/>+`.
        ifeval::[{project_product} == true]
        product text
        endif::[]
        // <@profile.complex>
        text
        // </@profile.complex>
        """, result);
    } finally {
      try (var files = java.nio.file.Files.walk(root)) {
        files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
      }
    }
  }

  public void testSelfClosingMacroCallOnItsOwnLineIsVisible() {
    String result = AsciiDocFreemarker.neutralize("<#import \"x\" as kc>\n\n<@kc.build parameters=\"--a=1\n--b=2\"/>\n\n<@kc.wrap a=\"b\">\ntext\n</@kc.wrap>\n");
    assertEquals("// <#import \"x\" as kc>\n\n`+<@kc.build parameters=\"--a=1 --b=2\"/>+`\n\n\n\ntext\n// </@kc.wrap>\n", result);
  }

  public void testLinksToGuidesAreResolved() throws Exception {
    java.nio.file.Path root = java.nio.file.Files.createTempDirectory("freemarker");
    try {
      java.nio.file.Path guide = root.resolve("high-availability/single-cluster/deploy.adoc");
      java.nio.file.Files.createDirectories(guide.getParent());
      java.nio.file.Files.createDirectories(root.resolve("high-availability/multi-cluster"));
      java.nio.file.Files.createDirectories(root.resolve("templates"));
      java.nio.file.Files.writeString(root.resolve("high-availability/introduction.adoc"), "<@tmpl.guide\ntitle=\"Intro\"\n>\n");
      java.nio.file.Files.writeString(root.resolve("high-availability/multi-cluster/concepts.adoc"), "<@tmpl.guide\ntitle=\"Multi concepts\"\n>\n");
      java.nio.file.Files.writeString(root.resolve("templates/links.adoc"), """
        <#macro ha id anchor="">link:{links_high-availability_${id}_url}<#if anchor != "">#${anchor}</#if>[{links_high-availability_${id}_name}]</#macro>
        """);
      String result = AsciiDocFreemarker.neutralize("""
        <#import "/templates/links.adoc" as links>
        See <@links.ha id="introduction"/>, <@links.ha id="multi-cluster-concepts" anchor="top"/> and <@links.ha id="unknown"/>.
        """, guide.toFile(), root.toFile());
      assertEquals("""
        // <#import "/templates/links.adoc" as links>
        See xref:../introduction.adoc[Intro], xref:../multi-cluster/concepts.adoc#top[Multi concepts] and link:{links_high-availability_unknown_url}[{links_high-availability_unknown_name}].
        """, result);
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
