package org.asciidoc.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.asciidoctor.SafeMode;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Alexander Schwartz 2018
 */
public class AsciiDocTest extends LightPlatformCodeInsightFixtureTestCase {
  private Logger log = Logger.getInstance(AsciiDocTest.class);

  private AsciiDoc asciidoc;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    asciidoc = new AsciiDoc(getProject(), new File("."), null, "test");
  }

  public void testShouldRenderPlantUmlAsPng() {
    String html = asciidoc.render("[plantuml,test,format=svg]\n" +
      "----\n" +
      "List <|.. ArrayList\n" +
      "----\n", Collections.emptyList());
    Assert.assertTrue(html.contains("src=\"test.png\""));
  }

  public void testShouldRenderPlainAsciidoc() {
    String html = asciidoc.render("this is *bold*.", Collections.emptyList());
    Assert.assertTrue("should contain formatted output",
      html.contains("<strong>bold</strong>"));
    Assert.assertTrue("should contain data line to allow navigation to source line in preview",
      html.contains("data-line-stdin-1"));
  }

  public void testShouldRenderPdf() throws IOException {
    // given...
    File asciidoc = File.createTempFile("asciidocforapdf", ".adoc");
    File pdf = new File(asciidoc.getAbsoluteFile().getAbsolutePath().replaceAll("\\.adoc$", ".pdf"));
    try {
      Assert.assertTrue("replacemante should have worked", pdf.getName().endsWith(".pdf"));
      if (pdf.exists()) {
        fail("PDF already exists, but shouldn't before runnning AsciiDoc");
      }
      Writer fw = Files.newBufferedWriter(asciidoc.toPath(), UTF_8);
      fw.write("Hello world.");
      fw.close();

      // when...
      this.asciidoc.convertTo(asciidoc, "", new ArrayList<>(), AsciiDoc.FileType.PDF);

      // then...
      Assert.assertTrue(pdf.exists());

    } finally {
      // cleanup...
      if (asciidoc.exists()) {
        if (!asciidoc.delete()) {
          log.warn("unable to delete source file");
        }
      }
      if (pdf.exists()) {
        if (!pdf.delete()) {
          log.warn("unable to delete destination file");
        }
      }
    }
  }

  public void testShouldRenderHtml() throws IOException {
    // given...
    File asciidoc = File.createTempFile("asciidocforhtml", ".adoc");
    File html = new File(asciidoc.getAbsoluteFile().getAbsolutePath().replaceAll("\\.adoc$", ".html"));
    try {
      Assert.assertTrue("replacement should have worked", html.getName().endsWith(".html"));
      if (html.exists()) {
        fail("HTML already exists, but shouldn't before running AsciiDoc");
      }
      Writer fw = Files.newBufferedWriter(asciidoc.toPath(), UTF_8);
      fw.write("Hello world.");
      fw.close();

      // when...
      this.asciidoc.convertTo(asciidoc, "", new ArrayList<>(), AsciiDoc.FileType.HTML);

      // then...
      Assert.assertTrue(html.exists());

    } finally {
      // cleanup...
      if (asciidoc.exists()) {
        if (!asciidoc.delete()) {
          log.warn("unable to delete source file");
        }
      }
      if (html.exists()) {
        if (!html.delete()) {
          log.warn("unable to delete destination file");
        }
      }
    }
  }

  public void testShouldRenderAttributesAsciidoc() {
    String expectedContent = "should replace attribute placeholder with value";
    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getAttributes().put("attr", expectedContent);
    String html = asciidoc.render("{attr}", Collections.emptyList());
    Assert.assertTrue(html.contains(expectedContent));
  }

  public void testShouldRenderBlockdiagWithSubstUsingKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      JavaFxHtmlPanelProvider.INFO,
      AsciiDocHtmlPanel.PreviewTheme.INTELLIJ,
      SafeMode.UNSAFE,
      new HashMap<>(),
      true,
      true,
      true,
      "",
      "",
      true,
      true,
      true,
      ""));
    try {
      String html = asciidoc.render(":action: generates\n" +
        "\n" +
        "[blockdiag,block-diag,svg,subs=+attributes]\n" +
        "----\n" +
        "blockdiag {\n" +
        "  Kroki -> {action} -> \"Block diagrams\";\n" +
        "  Kroki -> is -> \"very easy!\";\n" +
        "}\n" +
        "----\n", Collections.emptyList());
      Assert.assertTrue(html.contains("https://kroki.io/blockdiag/png/eNpLyslPzk7JTExXqOZSUPAuys_OVNC1U0hPzUstSixJLQZxlJxAihRAqooSc4uVrJFVZkKUlKUWVSqkJhZXKgKlawGuixqn"));
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderErdUsingKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      JavaFxHtmlPanelProvider.INFO,
      AsciiDocHtmlPanel.PreviewTheme.INTELLIJ,
      SafeMode.UNSAFE,
      new HashMap<>(),
      true,
      true,
      true,
      "",
      "",
      true,
      true,
      true,
      ""));
    try {
      String html = asciidoc.render("[erd]\n" +
        "----\n" +
        "[Person]\n" +
        "*name\n" +
        "height\n" +
        "weight\n" +
        "+birth_location_id\n" +
        "\n" +
        "[Location]\n" +
        "*id\n" +
        "city\n" +
        "state\n" +
        "country\n" +
        "\n" +
        "Person *--1 Location\n" +
        "----\n", Collections.emptyList());
      Assert.assertTrue(html.contains("https://kroki.io/erd/png/eNqLDkgtKs7Pi-XSykvMTeXKSM1MzyjhKodQ2kmZRSUZ8Tn5yYklmfl58ZkpXFzRPlAeUAuQn5xZUslVXJJYksqVnF-aV1JUycUFMVJBS1fXUAGmGgCFAiQX"));
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderNomnomlUsingALocalKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      JavaFxHtmlPanelProvider.INFO,
      AsciiDocHtmlPanel.PreviewTheme.INTELLIJ,
      SafeMode.UNSAFE,
      new HashMap<>(),
      true,
      true,
      true,
      "",
      "",
      true,
      true,
      true,
      "http://internal.secure.domain/kroki"));
    try {
      String html = asciidoc.render("[nomnoml]\n" +
        "----\n" +
        "[Pirate|eyeCount: Int|raid();pillage()|\n" +
        "  [beard]--[parrot]\n" +
        "  [beard]-:>[foul mouth]\n" +
        "]\n" +
        "----\n", Collections.emptyList());
      Assert.assertTrue(html.contains("http://internal.secure.domain/kroki/nomnoml/svg/eNqLDsgsSixJrUmtTHXOL80rsVLwzCupKUrMTNHQtC7IzMlJTE_V0KzhUlCITkpNLEqJ1dWNLkgsKsoviUUSs7KLTssvzVHIzS8tyYjligUAMhEd0g=="));
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }
}
