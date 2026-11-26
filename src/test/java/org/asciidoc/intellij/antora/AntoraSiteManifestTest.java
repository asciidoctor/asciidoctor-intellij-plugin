package org.asciidoc.intellij.antora;

import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.inspections.AsciiDocCodeInsightFixtureTestCase;
import org.asciidoc.intellij.inspections.AsciiDocLinkResolveInspection;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.assertj.core.api.Assertions;

import java.io.IOException;
import java.util.List;

public class AntoraSiteManifestTest extends AsciiDocCodeInsightFixtureTestCase {

  private static final String MANIFEST = "{\"$id\":\"x\",\"url\":\"https://docs.example.org\",\"components\":{"
    + "\"remote\":{\"title\":\"Remote\",\"latest\":\"1.0\",\"versions\":{\"1.0\":{\"url\":\"/remote/1.0/index.html\",\"pages\":["
    + "{\"path\":\"index.adoc\",\"url\":\"/remote/1.0/index.html\",\"title\":\"What&#8217;s new\"},"
    + "{\"path\":\"guide/start.adoc\",\"url\":\"/remote/1.0/guide/start.html\",\"title\":\"Start\"},"
    + "{\"path\":\"a.adoc\",\"module\":\"admin\",\"url\":\"/remote/1.0/admin/a.html\",\"title\":\"A\"}]}}}}}";

  private void configurePage(String text) {
    myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("modules/ROOT/pages/test.adoc", text.replace("<caret>", "")).getVirtualFile());
    int offset = text.indexOf("<caret>");
    if (offset >= 0) {
      myFixture.getEditor().getCaretModel().moveToOffset(offset);
    }
  }

  public void testParse() throws IOException {
    AntoraSiteManifest manifest = AntoraSiteManifest.parse(
      new java.io.ByteArrayInputStream(MANIFEST.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "https://docs.example.org");
    AntoraSiteManifest.Reference ref = AntoraSiteManifest.parseReference("remote::guide/start.adoc#top");
    Assertions.assertThat(ref).isNotNull();
    Assertions.assertThat(manifest.getPrefixes()).extracting(AntoraSiteManifest.Prefix::toString).isNotNull();
    Assertions.assertThat(AntoraSiteManifest.parseReference("start.adoc")).isNull();
    Assertions.assertThat(AntoraSiteManifest.parseReference("remote::image$a.png")).isNull();
  }

  public void testInspectionAndPreviewLookup() {
    myFixture.addFileToProject("antora.yml", "name: local\nversion: ~\nasciidoc:\n  attributes:\n    primary-site-manifest-url: ./site-manifest.json\n");
    myFixture.addFileToProject("site-manifest.json", MANIFEST);
    configurePage(
      "xref:remote::index.adoc[]\n\nxref:remote::guide/start.adoc[]\n\nxref:remote::missing.adoc[]\n\nxref:unknown::missing.adoc[]\n");
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class, org.asciidoc.intellij.inspections.AsciiDocAntoraModuleResolveInspection.class);
    AntoraSiteManifestService service = AntoraSiteManifestService.getInstance(getProject());
    List<AntoraSiteManifest> manifests = service.getManifestsAndWait(10_000);
    Assertions.assertThat(manifests).hasSize(1);
    AntoraSiteManifestService.Lookup lookup = service.lookup("remote::index.adoc", manifests);
    Assertions.assertThat(lookup).isNotNull();
    Assertions.assertThat(lookup.getPage().getTitleHtml()).isEqualTo("What&#8217;s new");
    Assertions.assertThat(lookup.getAbsoluteUrl()).isEqualTo("https://docs.example.org/remote/1.0/index.html");
    Assertions.assertThat(PsiTreeUtil.findChildrenOfType(myFixture.getFile(), AsciiDocLink.class)).hasSize(4);
    Assertions.assertThat(myFixture.doHighlighting()).extracting(h -> h.getDescription() + "@" + h.getText())
      .contains("Page is not listed in the Antora site manifest of https://docs.example.org@remote::missing.adoc")
      .doesNotContain("Antora Module doesn't resolve@remote:")
      .noneMatch(d -> d.contains("Page is not listed") && d.contains("unknown") || d.contains("index.adoc") || d.contains("start.adoc"));
  }

  public void testCompletion() {
    myFixture.addFileToProject("antora.yml", "name: local\nversion: ~\nasciidoc:\n  attributes:\n    primary-site-manifest-url: ./site-manifest.json\n");
    myFixture.addFileToProject("site-manifest.json", MANIFEST);
    configurePage("xref:rem<caret>\n");
    AntoraSiteManifestService.getInstance(getProject()).getManifestsAndWait(10_000);
    myFixture.completeBasic();
    List<String> strings = myFixture.getLookupElementStrings();
    // a single match is auto-inserted
    if (strings != null) {
      Assertions.assertThat(strings).contains("remote:");
    } else {
      Assertions.assertThat(myFixture.getEditor().getDocument().getText()).contains("xref:remote:");
    }
  }

  public void testDownloadUsesEtag() throws Exception {
    java.util.concurrent.atomic.AtomicInteger full = new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.atomic.AtomicReference<String> etag = new java.util.concurrent.atomic.AtomicReference<>("\"v1\"");
    com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      exchange.getResponseHeaders().add("ETag", etag.get());
      if (etag.get().equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
        exchange.sendResponseHeaders(304, -1);
      } else {
        full.incrementAndGet();
        byte[] body = MANIFEST.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
      }
      exchange.close();
    });
    server.start();
    try {
      java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("manifest-test");
      java.nio.file.Path file = dir.resolve("m.json");
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/site-manifest.json";
      Assertions.assertThat(AntoraSiteManifestLoader.download(url, file)).isTrue();
      Assertions.assertThat(java.nio.file.Files.readString(AntoraSiteManifestLoader.etagFile(file)).trim()).isEqualTo("\"v1\"");
      Assertions.assertThat(AntoraSiteManifestLoader.download(url, file)).isFalse();
      Assertions.assertThat(full.get()).isEqualTo(1);
      etag.set("\"v2\"");
      Assertions.assertThat(AntoraSiteManifestLoader.download(url, file)).isTrue();
      Assertions.assertThat(full.get()).isEqualTo(2);
      Assertions.assertThat(AntoraSiteManifestLoader.readCached(file, "m.json", null)).isNotNull();
    } finally {
      server.stop(0);
    }
  }
}
