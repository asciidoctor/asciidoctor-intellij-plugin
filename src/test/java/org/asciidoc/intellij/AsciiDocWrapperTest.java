package org.asciidoc.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.jcef.AsciiDocJCEFHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.asciidoctor.SafeMode;
import org.junit.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Schwartz 2018
 */
public class AsciiDocWrapperTest extends BasePlatformTestCase {
  private final Logger log = Logger.getInstance(AsciiDocWrapperTest.class);

  private AsciiDocWrapper asciidocWrapper;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    asciidocWrapper = new AsciiDocWrapper(getProject(), LocalFileSystem.getInstance().findFileByIoFile(new File(System.getProperty("java.io.tmpdir"))), null, "test");
  }

  public void testShouldRenderPlainAsciidoc() {
    String html = asciidocWrapper.render("this is *bold*.", Collections.emptyList());
    assertThat(html).withFailMessage("should contain formatted output").contains("<strong>bold</strong>");
    assertThat(html).withFailMessage("should contain data line to allow navigation to source line in preview").contains("data-line-stdin-1");
  }

  public void testShouldUseCustomStylesheet() throws IOException {
    File testCss = new File(System.getProperty("java.io.tmpdir"), "test.css");
    try (BufferedWriter writer = Files.newBufferedWriter(testCss.toPath(), UTF_8)) {
      writer.write("/* testcss */");
      writer.flush();
      String html = asciidocWrapper.render(":stylesheet: test.css", Collections.emptyList());
      html = "<head></head>" + html;
      html = AsciiDocWrapper.enrichPage(html, "/* standardcss */", null, null, asciidocWrapper.getAttributes(), getProject());
      assertThat(html).withFailMessage("should contain testcss").contains("testcss");
      assertThat(html).withFailMessage("should not contain standardcss").doesNotContain("standardcss");
    }
    if (!testCss.delete()) {
      throw new RuntimeException("unable to delete file");
    }
  }

  public void testShouldUseLinkedStylesheetAndDir() {
    String html = asciidocWrapper.render("""
      :linkcss:
      :stylesdir: https://example.com
      :stylesheet: dark.css""", Collections.emptyList());
    html = "<head></head>" + html;
    html = AsciiDocWrapper.enrichPage(html, "/* standardcss */", null, null, asciidocWrapper.getAttributes(), getProject());
    assertThat(html).withFailMessage("should contain testcss").containsPattern("<link[^>]*https://example.com/dark.css");
    assertThat(html).withFailMessage("should contain standardcss as backup").contains("standardcss");
  }

  public void testShouldUseLinkedStylesheetWithoutDir() {
    String html = asciidocWrapper.render(":linkcss:\n" +
      ":stylesheet: https://example.com/dark.css", Collections.emptyList());
    html = "<head></head>" + html;
    html = AsciiDocWrapper.enrichPage(html, "/* standardcss */", null, null, asciidocWrapper.getAttributes(), getProject());
    assertThat(html).withFailMessage("should contain testcss").containsPattern("<link[^>]*https://example.com/dark.css");
    assertThat(html).withFailMessage("should contain standardcss as backup").contains("standardcss");
  }

  public void testShouldUseDocInfoHeader() throws IOException {
    File docinfoHeader = new File(System.getProperty("java.io.tmpdir"), "docinfo.html");
    File docinfoFooter = new File(System.getProperty("java.io.tmpdir"), "docinfo-footer.html");
    try (BufferedWriter writerHeader = Files.newBufferedWriter(docinfoHeader.toPath(), UTF_8);
         BufferedWriter writerFooter = Files.newBufferedWriter(docinfoFooter.toPath(), UTF_8)) {
      writerHeader.write("<!-- myHeader -->");
      writerHeader.flush();
      writerFooter.write("<!-- myFooter -->");
      writerFooter.flush();
      String html = asciidocWrapper.render(":docinfo: shared", Collections.emptyList());
      html = "<head></head><body>" + html + "</body>";
      html = AsciiDocWrapper.enrichPage(html, "/* standardcss */", null, null, asciidocWrapper.getAttributes(), getProject());
      assertThat(html).contains("myHeader");
      assertThat(html).contains("myFooter");
    }
    if (!docinfoHeader.delete()) {
      throw new RuntimeException("unable to delete file");
    }
    if (!docinfoFooter.delete()) {
      throw new RuntimeException("unable to delete file");
    }
  }

  // disabled due to classloader problems; plugin's Snake YAML is not used
  public void disabledTestShouldRenderPdf() throws IOException {
    // given...
    File asciidoc = File.createTempFile("asciidocforapdf", ".adoc");
    File pdf = new File(asciidoc.getAbsoluteFile().getAbsolutePath().replaceAll("\\.adoc$", ".pdf"));
    try {
      assertThat(pdf.getName()).withFailMessage("replacement should have worked").endsWith(".pdf");
      if (pdf.exists()) {
        fail("PDF already exists, but shouldn't before runnning AsciiDoc");
      }
      Writer fw = Files.newBufferedWriter(asciidoc.toPath(), UTF_8);
      fw.write("Hello world.");
      fw.close();

      // when...
      this.asciidocWrapper.convertTo(asciidoc, "", new ArrayList<>(), AsciiDocWrapper.FileType.PDF);

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
    File diagram = new File(asciidoc.getAbsoluteFile().getAbsolutePath().replaceAll("asciidocforhtml.*adoc$", "uml-example.png"));
    try {
      assertThat(html.getName()).withFailMessage("replacement should have worked").endsWith(".html");
      assertThat(diagram.getName()).withFailMessage("replacement should have worked").endsWith(".png");
      if (html.exists()) {
        fail("HTML already exists, but shouldn't before running AsciiDoc");
      }
      if (diagram.exists()) {
        fail("Diagram already exists, but shouldn't before running AsciiDoc");
      }
      Writer fw = Files.newBufferedWriter(asciidoc.toPath(), UTF_8);
      fw.write("""
        Hello world.

        [plantuml, uml-example, png]
        ----
        @startuml
        Alice -> Bob: Authentication Request
        @enduml
        ----""");
      fw.close();

      // when...
      this.asciidocWrapper.convertTo(asciidoc, "", new ArrayList<>(), AsciiDocWrapper.FileType.HTML);

      // then...
      Assert.assertTrue(html.exists());
      Assert.assertTrue(diagram.exists());

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
      if (diagram.exists()) {
        if (!diagram.delete()) {
          log.warn("unable to delete destination file");
        }
      }
    }
  }

  public void testShouldRenderAttributesAsciidoc() {
    String expectedContent = "should replace attribute placeholder with value";
    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getAttributes().put("attr", expectedContent);
    String html = asciidocWrapper.render("{attr}", Collections.emptyList());
    assertThat(html).contains(expectedContent);
  }

  public void testShouldRenderBlockdiagWithSubstUsingKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      AsciiDocJCEFHtmlPanelProvider.INFO,
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
      "",
      true,
      true,
      true,
      1,
      false,
      ""));
    try {
      String html = asciidocWrapper.render("""
        :action: generates

        [blockdiag,block-diag,svg,subs=+attributes]
        ----
        blockdiag {
          Kroki -> {action} -> "Block diagrams";
          Kroki -> is -> "very easy!";
        }
        ----
        """, Collections.emptyList());
      assertThat(html).contains("https://kroki.io/blockdiag/svg/eNpLyslPzk7JTExXqOZSUPAuys_OVNC1U0hPzUstSixJLQZxlJxAihRAqooSc4uVrJFVZkKUlKUWVSqkJhZXKgKlawGuixqn");
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderErdUsingKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      AsciiDocJCEFHtmlPanelProvider.INFO,
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
      "",
      true,
      true,
      true,
      1,
      false,
      ""));
    try {
      String html = asciidocWrapper.render("""
        [erd]
        ----
        [Person]
        *name
        height
        weight
        +birth_location_id

        [Location]
        *id
        city
        state
        country

        Person *--1 Location
        ----
        """, Collections.emptyList());
      // on Mac ARM, the JavaFX provider is not available, therefore it won't switch to PNG by default
      assertThat(html).containsPattern("https://kroki.io/erd/(svg|png)/eNqLDkgtKs7Pi" +
        "-XSykvMTeXKSM1MzyjhKodQ2kmZRSUZ8Tn5yYklmfl58ZkpXFzRPlAeUAuQn5xZUslVXJJYksqVnF-aV1JUycUFMVJBS1fXUAGmGgCFAiQX");
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderNomnomlUsingALocalKroki() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      AsciiDocJCEFHtmlPanelProvider.INFO,
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
      "http://internal.secure.domain/kroki",
      true,
      true,
      true,
      1,
      false,
      ""));
    try {
      String html = asciidocWrapper.render("""
        [nomnoml]
        ----
        [Pirate|eyeCount: Int|raid();pillage()|
          [beard]--[parrot]
          [beard]-:>[foul mouth]
        ]
        ----
        """, Collections.emptyList());
      assertThat(html).contains("http://internal.secure.domain/kroki/nomnoml/svg/eNqLDsgsSixJrUmtTHXOL80rsVLwzCupKUrMTNHQtC7IzMlJTE_V0KzhUlCITkpNLEqJ1dWNLkgsKsoviUUSs7KLTssvzVHIzS8tyYjligUAMhEd0g==");
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderWaveDrom() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      AsciiDocJCEFHtmlPanelProvider.INFO,
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
      "",
      true,
      true,
      true,
      1,
      false,
      ""));
    try {
      String html = asciidocWrapper.render("""
        [wavedrom]
        ....
        { signal: [
          { name: "clk",         wave: "p.....|..." },
          { name: "Data",        wave: "x.345x|=.x", data: ["head", "body", "tail", "data"] },
          { name: "Request",     wave: "0.1..0|1.0" },
          {},
          { name: "Acknowledge", wave: "1.....|01." }
        ]}
        ....
        """, Collections.emptyList());
      assertThat(html).contains("https://kroki.io/wavedrom/svg/eNqrVijOTM9LzLFSiOZSUKhWyEvMTbVSUErOyVbSUYCB8sQykGCBHgjUALGSQq0OsnKXxJJEhHqo8go9YxPTihpbvQqgVApQBdAOpYzUxBQgVykpP6USRJckZuaAaJC8UiyasUGphaWpxSVQk6HGGugZ6ukZ1BjqGcBcgarJMTk7L788JzUlPRWoEarJEOJ0A0OQ07liawGPW0Gr");
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }

  public void testShouldRenderVega() {
    AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
      SplitFileEditor.SplitEditorLayout.SPLIT,
      AsciiDocJCEFHtmlPanelProvider.INFO,
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
      "",
      true,
      true,
      true,
      1,
      false,
      ""));
    try {
      String html = asciidocWrapper.render("""
        [vega]
        ....
        {
          "$schema": "https://vega.github.io/schema/vega/v5.json",
          "width": 400,
          "height": 200,
          "padding": 5,

          "data": [
            {
              "name": "table",
              "values": [
                {"category": "A", "amount": 28},
                {"category": "B", "amount": 55},
                {"category": "C", "amount": 43},
                {"category": "D", "amount": 91},
                {"category": "E", "amount": 81},
                {"category": "F", "amount": 53},
                {"category": "G", "amount": 19},
                {"category": "H", "amount": 87}
              ]
            }
          ],

          "signals": [
            {
              "name": "tooltip",
              "value": {},
              "on": [
                {"events": "rect:mouseover", "update": "datum"},
                {"events": "rect:mouseout",  "update": "{}"}
              ]
            }
          ],

          "scales": [
            {
              "name": "xscale",
              "type": "band",
              "domain": {"data": "table", "field": "category"},
              "range": "width",
              "padding": 0.05,
              "round": true
            },
            {
              "name": "yscale",
              "domain": {"data": "table", "field": "amount"},
              "nice": true,
              "range": "height"
            }
          ],

          "axes": [
            { "orient": "bottom", "scale": "xscale" },
            { "orient": "left", "scale": "yscale" }
          ],

          "marks": [
            {
              "type": "rect",
              "from": {"data":"table"},
              "encode": {
                "enter": {
                  "x": {"scale": "xscale", "field": "category"},
                  "width": {"scale": "xscale", "band": 1},
                  "y": {"scale": "yscale", "field": "amount"},
                  "y2": {"scale": "yscale", "value": 0}
                },
                "update": {
                  "fill": {"value": "steelblue"}
                },
                "hover": {
                  "fill": {"value": "red"}
                }
              }
            },
            {
              "type": "text",
              "encode": {
                "enter": {
                  "align": {"value": "center"},
                  "baseline": {"value": "bottom"},
                  "fill": {"value": "#333"}
                },
                "update": {
                  "x": {"scale": "xscale", "signal": "tooltip.category", "band": 0.5},
                  "y": {"scale": "yscale", "signal": "tooltip.amount", "offset": -2},
                  "text": {"signal": "tooltip.amount"},
                  "fillOpacity": [
                    {"test": "datum === tooltip", "value": 0},
                    {"value": 1}
                  ]
                }
              }
            }
          ]
        }
        ....
        """, Collections.emptyList());
      // on Mac ARM, the JavaFX provider is not available, therefore it won't switch to PNG by default
      assertThat(html).containsPattern("https://kroki" +
        ".io/vega/(svg|png)" +
        "/eNqVVcmSmzAQvfsrKCVHgrE9VGZc5UP23PIBKR8ENKAZgSgQjikX_x5JLELYcpzLMDTv9fq6fVk5DnpfRxnkGO0dlHFe1vv1-gQp9lLCsyb0CFv3AGVdnwLvtWYFciX1D4l5JohPvq_eMyBpxoVhOxhKHMekSIUlcFfSEGMuI_0W_zvORf0V1gLnIONzHFJQrpX5hGkD9QRXFBRhDimrWon_hFwH4Zw1hQr63LkW4GcDGARW4BcD-LSzAr8awJeNFfjNAD7bgd_NHO2hfxjAzYsV-NMM_bEbcEf1lG_Hfio1SQtM6zuDYYxyUi5GI75cpuBIiMKcFJyg4NIpqiDie5FHDewElcyqKYUSlGvxbHJk1HCT2HDBmxMvHbIXFGEKd-o5K4Auh7elsoe4iLU1ZjkmsqrLqNtRoQ5KCNBYWqaG605UuEiVu34_JrveBt_zAw0XA5KueNVAX4h7O-t2kfVD-Q3z19kVJIIh2nXGwwYv-4nP826KWVcElKhQyDhnuYzYJ6ebO5Uxh1NIuAFuR7AOluPq7cbsxhlJTegeJJWIrjswNEBXC0XEYqXUSWDCxoUK5yZhPCsvyyLuT1oRxyN4k6wEJZbUpLQmvL2OtZxaT9vaeOM6-t2En1H10hgVJ4RS5XBko5oD0FC-3PaTqfX9p5sK4rmD1fy51PY4VQ7n2VQfnhqm4nSZ0aMeaLYuxDVQUoAJHcRrQq_rebfb7dD_dNaqpf7Qzi6qN4lKi8X3ggflcu1u0I34xpKkBrlzH7amN9Vp5dDGvu7HrxJHhLfGge9vNYeaT2fcORwOzvRbMZelu6CNXzbd7MPRphl5G1bdX_2bNmU=");
    } finally {
      AsciiDocApplicationSettings.getInstance().setAsciiDocPreviewSettings(AsciiDocPreviewSettings.DEFAULT);
    }
  }
}
