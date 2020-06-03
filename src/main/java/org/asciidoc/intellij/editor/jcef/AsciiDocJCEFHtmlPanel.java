package org.asciidoc.intellij.editor.jcef;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanel;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public class AsciiDocJCEFHtmlPanel extends JCEFHtmlPanel implements AsciiDocHtmlPanel {

  private final Logger log = Logger.getInstance(JavaFxHtmlPanel.class);

  private final Path imagesPath;

  private JBCefJSQuery myJSQuerySetScrollY;
  private JBCefJSQuery myRenderedIteration;
  private JBCefJSQuery myRenderedResult;
  private JBCefJSQuery myScrollEditorToLine;
  private JBCefJSQuery myBrowserLog;
  private JBCefJSQuery myOpenLink;
  private volatile CountDownLatch rendered = new CountDownLatch(1);
  private volatile CountDownLatch replaceResultLatch = new CountDownLatch(1);
  private final CefLoadHandler myCefLoadHandler;

  @NotNull
  private final ScrollPreservingListener myScrollPreservingListener = new ScrollPreservingListener();
  @NotNull
  private final BridgeSettingListener myBridgeSettingListener = new BridgeSettingListener();

  @NotNull
  private String base;
  private VirtualFile parentDirectory;
  private int lineCount;
  private volatile boolean forceRefresh = false;
  private volatile long stamp = 0;
  private volatile boolean replaceResult = false;
  private volatile String frameHtml = null;

  @NotNull
  private static final String OUR_CLASS_URL;

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      final Class<JavaFxHtmlPanel> clazz = JavaFxHtmlPanel.class;
      //noinspection StringBufferReplaceableByString
      return new StringBuilder()
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("scrollToElement.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("processLinks.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("pickSourceLine.js")).append("\"></script>\n")
        .append("<script type=\"text/x-mathjax-config\">\n" +
          "MathJax.Hub.Config({\n" +
          "  messageStyle: \"none\",\n" +
          "  EqnChunkDelay: 1," +
          "  imageFont: null," +
          "  tex2jax: {\n" +
          "    inlineMath: [[\"\\\\(\", \"\\\\)\"]],\n" +
          "    displayMath: [[\"\\\\[\", \"\\\\]\"]],\n" +
          "    ignoreClass: \"nostem|nolatexmath\"\n" +
          "  },\n" +
          "  asciimath2jax: {\n" +
          "    delimiters: [[\"\\\\$\", \"\\\\$\"]],\n" +
          "    ignoreClass: \"nostem|noasciimath\"\n" +
          "  },\n" +
          "  TeX: { equationNumbers: { autoNumber: \"none\" } }\n" +
          "});\n" +
          "MathJax.Hub.Register.MessageHook(\"Math Processing Error\",function (message) {\n" +
          " window.JavaPanelBridge && window.JavaPanelBridge.log(JSON.stringify(message)); \n" +
          "});" +
          "MathJax.Hub.Register.MessageHook(\"TeX Jax - parse error\",function (message) {\n" +
          " var errortext = document.getElementById('mathjaxerrortext'); " +
          " var errorformula = document.getElementById('mathjaxerrorformula'); " +
          " if (errorformula && errortext) { " +
          "   errortext.textContent = 'Math Formula problem: ' + message[1]; " +
          "   errorformula.textContent = '\\n' + message[2]; " +
          " } " +
          " window.JavaPanelBridge && window.JavaPanelBridge.log(JSON.stringify(message)); \n" +
          "});" +
          "</script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("MathJax/MathJax.js")).append("&amp;config=TeX-MML-AM_HTMLorMML\"></script>\n")
        .toString();
    }
  };

  static {
    String url = "about:blank";
    try {
      url = AsciiDocJCEFHtmlPanel.class.getResource(AsciiDocJCEFHtmlPanel.class.getSimpleName() + ".class").toExternalForm();
    } catch (Exception ignored) {
    }
    OUR_CLASS_URL = url;
  }

  private Editor editor;

  public AsciiDocJCEFHtmlPanel(Document document, Path imagesPath) {
    super(OUR_CLASS_URL + "@" + new Random().nextInt(Integer.MAX_VALUE));

    this.imagesPath = imagesPath;

    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      parentDirectory = file.getParent();
    }
    if (parentDirectory != null) {
      // parent will be null if we use Language Injection and Fragment Editor
      base = parentDirectory.getUrl().replaceAll("^file://", "")
        .replaceAll(":", "%3A");
    } else {
      base = "";
    }

    reregisterHandlers();

    myCefLoadHandler = new CefLoadHandlerAdapter() {
      @Override
      public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        myScrollPreservingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
        myBridgeSettingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
      }
    };
    getJBCefClient().addLoadHandler(myCefLoadHandler, getCefBrowser());

    try {
      Properties p = new Properties();
      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("/META-INF/asciidoctorj-version.properties")) {
        p.load(stream);
      }
      String asciidoctorVersion = p.getProperty("version.asciidoctor");
      myInlineCss = extractAndPatchAsciidoctorCss(asciidoctorVersion);

      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("darcula.css")) {
        myInlineCssDarcula = myInlineCss + IOUtils.toString(stream);
      }
      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("coderay-darcula.css")) {
        myInlineCssDarcula += IOUtils.toString(stream);
      }
      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/coderay-asciidoctor.css")) {
        myInlineCss += IOUtils.toString(stream);
      }
      myFontAwesomeCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("font-awesome/css/font-awesome.min.css") + "\">";
      myDejavuCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("dejavu/dejavu.css") + "\">";
      myGoogleFontsCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/googlefonts.css") + "\">";
    } catch (IOException e) {
      String message = "Unable to combine CSS resources: " + e.getMessage();
      log.error(message, e);
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      float uiZoom = (float) AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getZoom() / 100;
      getCefBrowser().setZoomLevel(JBUIScale.scale(uiZoom));
      // run later, to avoid blocking the UI
      setHtml(prepareHtml(wrapHtmlForPage("Initializing..."), Collections.emptyMap()));
    });
  }

  private void reregisterHandlers() {
    if (myJSQuerySetScrollY != null) {
      Disposer.dispose(myJSQuerySetScrollY);
    }
    if (myRenderedIteration != null) {
      Disposer.dispose(myRenderedIteration);
    }
    if (myRenderedResult != null) {
      Disposer.dispose(myRenderedResult);
    }
    if (myBrowserLog != null) {
      Disposer.dispose(myBrowserLog);
    }
    if (myScrollEditorToLine != null) {
      Disposer.dispose(myScrollEditorToLine);
    }
    if (myOpenLink != null) {
      Disposer.dispose(myOpenLink);
    }

    myJSQuerySetScrollY = JBCefJSQuery.create(this);
    myRenderedIteration = JBCefJSQuery.create(this);
    myRenderedResult = JBCefJSQuery.create(this);
    myBrowserLog = JBCefJSQuery.create(this);
    myScrollEditorToLine = JBCefJSQuery.create(this);
    myOpenLink = JBCefJSQuery.create(this);

    // TODO: click on image to save
    // TODO: mouse scroll to zoom -> try javascript in browser, mousewheellistener on component doesn't work

    myJSQuerySetScrollY.addHandler((scrollY) -> {
      try {
        myScrollPreservingListener.myScrollY = Integer.parseInt(scrollY);
      } catch (NumberFormatException e) {
        log.warn("unable to parse scroll Y", e);
      }
      return null;
    });

    myRenderedIteration.addHandler((r) -> {
      try {
        long iterationStamp = Integer.parseInt(r);
        if (stamp == iterationStamp) {
          rendered.countDown();
        }
      } catch (NumberFormatException e) {
        log.warn("unable set iteration stamp", e);
      }
      return null;
    });

    myRenderedResult.addHandler((r) -> {
      if (r != null && r.length() > 0) {
        replaceResult = Boolean.parseBoolean(r);
        replaceResultLatch.countDown();
      }
      return null;
    });

    myBrowserLog.addHandler((r) -> {
      Logger.getInstance(AsciiDocJCEFHtmlPanel.class).warn("log from browser:" + r);
      return null;
    });

    myScrollEditorToLine.addHandler((r) -> {
      try {
        int line = (int) Math.round(Double.parseDouble(r));
        scrollEditorToLine(line);
      } catch (NumberFormatException e) {
        log.warn("unable parse line number", e);
      }
      return null;
    });

    myOpenLink.addHandler((r) -> {
      openLink(r);
      return null;
    });
  }

  private String extractAndPatchAsciidoctorCss(String asciidoctorVersion) throws IOException {
    String css;

    try (InputStream steam = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
      + asciidoctorVersion
      + "/data/stylesheets/asciidoctor-default.css")) {
      css = IOUtils.toString(steam);
    }

    // the following lines have been added for JavaFX
    // TODO: revisit the following to find out if this is still true for JCEF

    // JavaFX doesn't support synthetic bold/italic, and Droid Sans Mono doesn't have bold/italic styles delivered
    // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/193
    // https://bugs.openjdk.java.net/browse/JDK-8089405
    css = css.replaceAll("(\"Droid Sans Mono\"),", "");

    return css;
  }

  @Nullable
  private String myInlineCss;
  @Nullable
  private String myInlineCssDarcula;
  @Nullable
  private String myFontAwesomeCssLink;
  @Nullable
  private String myDejavuCssLink;
  @Nullable
  private String myGoogleFontsCssLink;

  @Override
  public synchronized void setHtml(@NotNull String htmlParam, @NotNull Map<String, String> attributes) {
    rendered = new CountDownLatch(1);
    replaceResultLatch = new CountDownLatch(1);
    stamp += 1;
    if (stamp > 10000) {
      // force a refresh to avoid memory leaks
      forceRefresh = true;
      stamp = 0;
    }
    long iterationStamp = stamp;
    String emptyFrame = prepareHtml(wrapHtmlForPage(""), attributes);
    if (!emptyFrame.equals(frameHtml)) {
      forceRefresh = true;
      frameHtml = emptyFrame;
    }
    String html = htmlParam;
    if (isDarcula()) {
      // clear out coderay inline CSS colors as they are barely readable in darcula theme
      html = html.replaceAll("<span style=\"color:#[a-zA-Z0-9]*;?", "<span style=\"");
      html = html.replaceAll("<span style=\"background-color:#[a-zA-Z0-9]*;?", "<span style=\"");
    }
    boolean result = false;
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (!forceRefresh && settings.getAsciiDocPreviewSettings().isInplacePreviewRefresh() && html.contains("id=\"content\"")) {
      final String htmlToReplace = StringEscapeUtils.escapeEcmaScript(prepareHtml(html, attributes));
      // try to replace the HTML contents using JavaScript to avoid flickering MathML
      try {
        boolean ml = false; // set to "true" to test for memory leaks in HTML/JavaScript
        replaceResult = false;
        getCefBrowser().executeJavaScript(
          (ml ? "var x = 0; " : "") +
            "function finish() {" +
            "if ('__IntelliJTools' in window) {" +
            "__IntelliJTools.processLinks && __IntelliJTools.processLinks();" +
            "__IntelliJTools.pickSourceLine && __IntelliJTools.pickSourceLine(" + lineCount + ");" +
            (ml ? "window.setTimeout(function(){ updateContent(); }, 10); " : "") +
            "}" +
            "window.JavaPanelBridge && window.JavaPanelBridge.rendered(" + iterationStamp + ");" +
            "}" +
            "function updateContent() { var elem = document.getElementById('content'); if (elem && elem.parentNode) { " +
            "var div = document.createElement('div');" +
            "div.innerHTML = '" + htmlToReplace + "'; " +
            "var errortext = document.getElementById('mathjaxerrortext'); " +
            "var errorformula = document.getElementById('mathjaxerrorformula'); " +
            "if (errorformula && errortext) { " +
            "  errortext.textContent = ''; " +
            "  errorformula.textContent = ''; " +
            "} " +
            (ml ? "x = x + 1; " : "") +
            (ml ? "errortext.textContent = 'count:' + x; " : "") +
            "div.style.cssText = 'display: none'; " +
            // need to add the element to the DOM as MathJAX will use document.getElementById in some places
            "elem.appendChild(div); " +
            // use MathJax to set the formulas in advance if formulas are present - this takes ~100ms
            // re-evaluate the content element as it might have been replaced by a concurrent rendering
            "if ('MathJax' in window && MathJax.Hub.getAllJax().length > 0) { " +
            "MathJax.Hub.Typeset(div.firstChild, function() { " +
            "var elem2 = document.getElementById('content'); " +
            "__IntelliJTools.clearLinks && __IntelliJTools.clearLinks();" +
            "__IntelliJTools.clearSourceLine && __IntelliJTools.clearSourceLine();" +
            "elem2.parentNode.replaceChild(div.firstChild, elem2); " +
            "finish(); }); } " +
            // if no math was present before, replace contents, and do the MathJax typesetting afterwards in case Math has been added
            "else { " +
            "__IntelliJTools.clearLinks && __IntelliJTools.clearLinks();" +
            "__IntelliJTools.clearSourceLine && __IntelliJTools.clearSourceLine();" +
            "elem.parentNode.replaceChild(div.firstChild, elem); " +
            "MathJax.Hub.Typeset(div.firstChild); " +
            "finish(); " +
            "} " +
            "return true; } else { return false; }}; " + myRenderedResult.inject("updateContent()"),
          getCefBrowser().getURL(), 0);
        replaceResultLatch.await(1, TimeUnit.SECONDS);
        result = replaceResult;
      } catch (RuntimeException | InterruptedException e) {
        // might happen when rendered output is not valid HTML due to passtrough content
        log.warn("unable to use JavaScript for update", e);
      }
    }
    // if not successful using JavaScript (like on first rendering attempt), set full content
    if (!result) {
      forceRefresh = false;
      html = html + "<script>window.iterationStamp=" + iterationStamp + "; window.JavaPanelBridge && window.JavaPanelBridge.rendered(iterationStamp); </script>";
      html = wrapHtmlForPage(html);
      final String htmlToRender = prepareHtml(html, attributes);
      super.setHtml(htmlToRender);
    }
    try {
      // slow down the rendering of the next version of the preview until the rendering if the current version is complete
      // this prevents us building up a queue that would lead to a lagging preview
      if (htmlParam.length() > 0 && !rendered.await(3, TimeUnit.SECONDS)) {
        log.warn("rendering didn't complete in time, might be slow or broken");
        forceRefresh = true;
        reregisterHandlers();
      }
    } catch (InterruptedException e) {
      log.warn("interrupted while waiting for refresh to complete");
    }
  }

  @NotNull
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  private String prepareHtml(@NotNull String html, @NotNull Map<String, String> attributes) {
    /* for each image we'll calculate a MD5 sum of its content. Once the content changes, MD5 and therefore the URL
     * will change. The changed URL is necessary for the JavaFX web view to display the new content, as each URL
     * will be loaded only once by the JavaFX web view. */
    Pattern pattern = Pattern.compile("<img src=\"([^:\"]*)\"");
    Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8.name()); // restore "%20" as " "
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      String tmpFile = findTempImageFile(file, attributes.get("imagesdir"));
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        tmpFile = tmpFile.replaceAll("\\\\", "/");
        try {
          tmpFile = URLEncoder.encode(tmpFile, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
        replacement = "<img src=\"file://" + tmpFile + "?" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, base);
        replacement = "<img src=\"file://" + base.replaceAll("%3A", ":") + "/" + file + "?" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* the same as above for interactive SVGs */
    pattern = Pattern.compile("<object ([^>])*data=\"([^:\"]*)\"");
    matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String other = matchResult.group(1);
      if (other == null) {
        other = "";
      }
      String file = matchResult.group(2);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8.name()); // restore "%20" as " "
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      String tmpFile = findTempImageFile(file, attributes.get("imagesdir"));
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        tmpFile = tmpFile.replaceAll("\\\\", "/");
        try {
          tmpFile = URLEncoder.encode(tmpFile, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
        replacement = "<object " + other + "data=\"file://" + tmpFile + "?" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, base);
        replacement = "<object " + other + "data=\"file://" + base.replaceAll("%3A", ":") + "/" + file + "?" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    html = AsciiDoc.enrichPage(html, AsciiDocHtmlPanel.getCssLines(isDarcula() ? myInlineCssDarcula : myInlineCss) + myFontAwesomeCssLink + myGoogleFontsCssLink + myDejavuCssLink, attributes);

    html = html.replaceAll("<head>", "<head>\n" +
      "<meta http-equiv=\"Content-Security-Policy\" content=\"" + PreviewStaticServer.createCSP() + "\">");

    /* Add JavaScript for auto-scolling and clickable links */
    return html
      .replace("</body>", getScriptingLines() + "</body>");
  }

  @Override
  public void dispose() {
    super.dispose();
    getJBCefClient().removeLoadHandler(myCefLoadHandler, getCefBrowser());
    Disposer.dispose(myJSQuerySetScrollY);
    Disposer.dispose(myRenderedResult);
    Disposer.dispose(myRenderedIteration);
  }

  private String findTempImageFile(String filename, String imagesdir) {
    try {
      Path file = imagesPath.resolve(filename);
      if (Files.exists(file)) {
        return file.toFile().toString();
      }
    } catch (InvalidPathException e) {
      log.info("problem decoding decode filename " + filename, e);
    }
    // when {imagesoutdir} is set, files created by asciidoctor-diagram end up in the root path of that dir, but HTML will still prepend {imagesdir}
    // try again with removed {imagesdir}
    // https://github.com/asciidoctor/asciidoctor-diagram/issues/110
    if (imagesdir != null) {
      String prefix = imagesdir + "/";
      if (filename.startsWith(prefix)) {
        filename = filename.substring(prefix.length());
        try {
          Path file = imagesPath.resolve(filename);
          if (Files.exists(file)) {
            return file.toFile().toString();
          }
        } catch (InvalidPathException e) {
          log.info("problem decoding decode filename " + filename, e);
        }
      }
    }
    // a user might have specified multiple different imagesdir within the document, strip prefixes one-by-one and see if we find the image
    String shortenedFilename = filename;
    while (shortenedFilename.indexOf('/') != -1) {
      shortenedFilename = shortenedFilename.substring(shortenedFilename.indexOf('/') + 1);
      try {
        Path file = imagesPath.resolve(shortenedFilename);
        if (Files.exists(file)) {
          return file.toFile().toString();
        }
      } catch (InvalidPathException e) {
        log.info("problem decoding decode filename " + filename, e);
      }
    }
    return null;
  }

  private String calculateMd5(String file, String base) {
    String md5;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      try (FileInputStream fis = new FileInputStream((base != null ? base.replaceAll("%3A", ":") + "/" : "") + file)) {
        int nread;
        byte[] dataBytes = new byte[1024];
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        }
      }
      byte[] mdbytes = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte mdbyte : mdbytes) {
        sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
      }
      md5 = sb.toString();
    } catch (NoSuchAlgorithmException | IOException e) {
      md5 = "none";
    }
    return md5;
  }

  @NotNull
  private String wrapHtmlForPage(String html) {
    return "<html><head></head><body><div id=\"header\"></div><div style='position:fixed;top:0;left:0;background-color:#eeeeee;color:red;z-index:99;'><div id='mathjaxerrortext'></div><pre style='color:red' id='mathjaxerrorformula'></pre></div>"
      + html
      + "</body></html>";
  }

  @Override
  public void render() {

  }

  private boolean isDarcula() {
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    switch (settings.getAsciiDocPreviewSettings().getPreviewTheme()) {
      case INTELLIJ:
        return UIUtil.isUnderDarcula();
      case ASCIIDOC:
        return false;
      case DARCULA:
        return true;
      default:
        return false;
    }
  }


  @Override
  public void scrollToLine(int line, int lineCount) {
    this.lineCount = lineCount;
    getCefBrowser().executeJavaScript(
      "if ('__IntelliJTools' in window) " +
        "__IntelliJTools.scrollToLine(" + line + ", " + lineCount + ");",
      getCefBrowser().getURL(), 0);

    getCefBrowser().executeJavaScript(
      "var value = document.documentElement.scrollTop || document.body.scrollTop;" +
        myJSQuerySetScrollY.inject("value"),
      getCefBrowser().getURL(), 0);

  }

  @Override
  public Editor getEditor() {
    return editor;
  }

  @Override
  public void setEditor(Editor editor) {
    this.editor = editor;
  }

  private class ScrollPreservingListener extends CefLoadHandlerAdapter {
    @SuppressWarnings("checkstyle:VisibilityModifier")
    volatile int myScrollY = 0;

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
      if (isLoading) {
        getCefBrowser().executeJavaScript(
          "var value = document.documentElement.scrollTop || document.body.scrollTop;" +
            myJSQuerySetScrollY.inject("value"),
          getCefBrowser().getURL(), 0);
      } else {
        getCefBrowser().executeJavaScript("document.documentElement.scrollTop = ({} || document.body).scrollTop = " + myScrollY,
          getCefBrowser().getURL(), 0);
      }
    }
  }

  private class BridgeSettingListener extends CefLoadHandlerAdapter {
    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
      if (!isLoading) {
        getCefBrowser().executeJavaScript(
          "window.JavaPanelBridge = {" +
            "rendered : function(iteration) {" +
            myRenderedIteration.inject("iteration") +
            "}," +
            "log : function(log) {" +
            myBrowserLog.inject("log") +
            "}," +
            "openLink : function(link) {" +
            myOpenLink.inject("link") +
            "}," +
            "scrollEditorToLine : function(sourceLine) {" +
            myScrollEditorToLine.inject("sourceLine") +
            "}" +
            "};" +
            "if ('__IntelliJTools' in window) {" +
            "__IntelliJTools.processLinks && __IntelliJTools.processLinks();" +
            "__IntelliJTools.pickSourceLine && __IntelliJTools.pickSourceLine(" + lineCount + ");" +
            "}; " +
            "JavaPanelBridge.rendered(window.iterationStamp);",
          getCefBrowser().getURL(), 0);
      }
    }

  }

  public void openLink(@NotNull String link) {
    final URI uri;
    try {
      uri = new URI(link);
    } catch (URISyntaxException ex) {
      throw new RuntimeException("unable to parse URL " + link);
    }

    String scheme = uri.getScheme();
    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
      BrowserUtil.browse(uri);
    } else if ("file".equalsIgnoreCase(scheme) || scheme == null) {
      openInEditor(uri);
    } else {
      log.warn("won't open URI as it might be unsafe: " + uri);
    }
  }

  private void openInEditor(@NotNull URI uri) {
    ReadAction.compute(() -> {
      String anchor = uri.getFragment();
      String path = uri.getPath();
      final VirtualFile targetFile;
      VirtualFile tmpTargetFile = parentDirectory.findFileByRelativePath(path);
      if (tmpTargetFile == null) {
        // extension might be skipped if it is an .adoc file
        tmpTargetFile = parentDirectory.findFileByRelativePath(path + ".adoc");
      }
      if (tmpTargetFile == null && path.endsWith(".html")) {
        // might link to a .html in the rendered output, but might actually be a .adoc file
        tmpTargetFile = parentDirectory.findFileByRelativePath(path.replaceAll("\\.html$", ".adoc"));
      }
      if (tmpTargetFile == null) {
        log.warn("unable to find file for " + uri);
        return false;
      }
      targetFile = tmpTargetFile;

      Project project = ProjectUtil.guessProjectForContentFile(targetFile);
      if (project == null) {
        log.warn("unable to find project for " + uri);
        return false;
      }

      if (targetFile.isDirectory()) {
        ProjectView projectView = ProjectView.getInstance(project);
        projectView.changeView(ProjectViewPane.ID);
        projectView.select(null, targetFile, true);
      } else {
        boolean anchorFound = false;
        if (anchor != null) {
          List<PsiElement> ids = AsciiDocUtil.findIds(project, targetFile, anchor);
          if (!ids.isEmpty()) {
            anchorFound = true;
            ApplicationManager.getApplication().invokeLater(() -> PsiNavigateUtil.navigate(ids.get(0)));
          }
        }

        if (!anchorFound) {
          ApplicationManager.getApplication().invokeLater(() -> OpenFileAction.openFile(targetFile, project));
        }
      }
      return true;
    });
  }

  public void scrollEditorToLine(int sourceLine) {
    if (sourceLine <= 0) {
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("Setting cursor position", "line number " + sourceLine + " requested for cursor position, ignoring",
        NotificationType.INFORMATION, null);
      notification.setImportant(false);
      return;
    }
    ApplicationManager.getApplication().invokeLater(
      () -> {
        getEditor().getCaretModel().setCaretsAndSelections(
          Collections.singletonList(new CaretState(new LogicalPosition(sourceLine - 1, 0), null, null))
        );
        getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER_UP);
      }
    );
  }

}
