package org.asciidoc.intellij.editor.jcef;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.CaretState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JBCefPsiNavigationUtils;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

public class AsciiDocJCEFHtmlPanel extends JCEFHtmlPanel implements AsciiDocHtmlPanel {

  private static final Logger LOG = Logger.getInstance(AsciiDocJCEFHtmlPanel.class);

  private final Path imagesPath;

  private JBCefJSQuery myJSQuerySetScrollY;
  private JBCefJSQuery myRenderedIteration;
  private JBCefJSQuery myRenderedResult;
  private JBCefJSQuery myScrollEditorToLine;
  private JBCefJSQuery myZoomDelta;
  private JBCefJSQuery myZoomReset;
  private JBCefJSQuery mySaveImage;
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
  private volatile int lineCount;
  private volatile int line;
  private volatile boolean forceRefresh = true;
  private volatile long stamp = 0;
  private volatile boolean replaceResult = false;
  private volatile String frameHtml = null;

  @NotNull
  private static final String OUR_CLASS_URL;

  private double uiZoom;

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      final Class<JavaFxHtmlPanel> clazz = JavaFxHtmlPanel.class;
      //noinspection StringBufferReplaceableByString
      return new StringBuilder()
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("scrollToElement.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("processLinks.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("processImages.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("pickSourceLine.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("mouseEvents.js")).append("\"></script>\n")
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

      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/coderay-asciidoctor.css")) {
        myInlineCss += IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("rouge-github.css")) {
        myInlineCss += IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream stream = JavaFxHtmlPanel.class.getResourceAsStream("darcula.css")) {
        myInlineCssDarcula = myInlineCss + IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
      myFontAwesomeCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("font-awesome/css/font-awesome.min.css") + "\">";
      myDejavuCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("dejavu/dejavu.css") + "\">";
      myGoogleFontsCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/googlefonts.css") + "\">";
    } catch (IOException e) {
      String message = "Unable to combine CSS resources: " + e.getMessage();
      LOG.error(message, e);
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
    uiZoom = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getZoom() / 100.0;

    ApplicationManager.getApplication().invokeLater(() -> setHtml(prepareHtml(wrapHtmlForPage("Initializing..."), Collections.emptyMap())));
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
    if (myZoomDelta != null) {
      Disposer.dispose(myZoomDelta);
    }
    if (myZoomReset != null) {
      Disposer.dispose(myZoomReset);
    }
    if (mySaveImage != null) {
      Disposer.dispose(mySaveImage);
    }
    if (myOpenLink != null) {
      Disposer.dispose(myOpenLink);
    }

    myJSQuerySetScrollY = JBCefJSQuery.create(this);
    myRenderedIteration = JBCefJSQuery.create(this);
    myRenderedResult = JBCefJSQuery.create(this);
    myBrowserLog = JBCefJSQuery.create(this);
    myScrollEditorToLine = JBCefJSQuery.create(this);
    myZoomDelta = JBCefJSQuery.create(this);
    myZoomReset = JBCefJSQuery.create(this);
    mySaveImage = JBCefJSQuery.create(this);
    myOpenLink = JBCefJSQuery.create(this);

    myJSQuerySetScrollY.addHandler((scrollY) -> {
      try {
        if (scrollY != null && scrollY.length() > 0) {
          myScrollPreservingListener.myScrollY = (int) Double.parseDouble(scrollY);
        }
      } catch (NumberFormatException e) {
        LOG.warn("unable to parse scroll Y", e);
      }
      return null;
    });

    myRenderedIteration.addHandler((r) -> {
      try {
        if (r != null && r.length() > 0 && !r.equals("undefined")) {
          long iterationStamp = Integer.parseInt(r);
          if (stamp == iterationStamp) {
            rendered.countDown();
          }
        } else if (r != null && r.equals("undefined")) {
          // TODO: find out why the first result is undefined
          if (stamp == 1) {
            rendered.countDown();
          }
        }
      } catch (NumberFormatException e) {
        LOG.warn("unable set iteration stamp", e);
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
        if (r.length() != 0) {
          int line = (int) Math.round(Double.parseDouble(r));
          scrollEditorToLine(line);
        }
      } catch (NumberFormatException e) {
        LOG.warn("unable parse line number: " + r, e);
      }
      return null;
    });

    myZoomDelta.addHandler((r) -> {
      try {
        double deltaY = Double.parseDouble(r);
        if (Double.compare(deltaY, 0.0) != 0) {
          uiZoom = getCefBrowser().getZoomLevel() + 1;
          double factor = (deltaY > 0.0 ? -0.2 : +0.2);
          uiZoom = uiZoom + factor;
          // define minimum/maximum zoom
          if (uiZoom < JBUIScale.scale(0.1f)) {
            uiZoom = 0.1;
          } else if (uiZoom > JBUIScale.scale(10.f)) {
            uiZoom = 10;
          }
          getCefBrowser().setZoomLevel(uiZoom - 1);
        }
      } catch (NumberFormatException e) {
        LOG.warn("unable parse line number: " + r, e);
      }
      return null;
    });

    myZoomReset.addHandler((r) -> {
      uiZoom = 1.0;
      getCefBrowser().setZoomLevel(uiZoom - 1);
      return null;
    });

    mySaveImage.addHandler(r -> {
      ApplicationManager.getApplication().invokeLater(() ->
          saveImage(r),
        ModalityState.stateForComponent(getComponent()));
      return null;
    });

    myOpenLink.addHandler((r) -> {
      if (JBCefPsiNavigationUtils.INSTANCE.navigateTo(r)) {
        return null;
      }
      openLink(r);
      return null;
    });
  }

  private VirtualFile saveImageLastDir = null;

  private void saveImage(@NotNull String path) {
    String parent = imagesPath.getFileName().toString();
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      LOG.warn("unable to decode URL " + path, e);
      return;
    }
    int indexOfParent = path.indexOf(parent);
    if (indexOfParent == -1) {
      // parent string not found for image, image not generated by preview?
      LOG.warn("found path " + path + " without " + parent);
      return;
    }
    String subPath = path.substring(indexOfParent + parent.length() + 1);
    if (subPath.contains("?")) {
      subPath = subPath.substring(0, subPath.indexOf("?"));
    }
    Path imagePath = imagesPath.resolve(subPath);
    if (imagePath.toFile().exists()) {
      File file = imagePath.toFile();
      String fileName = imagePath.getFileName().toString();
      ArrayList<String> extensions = new ArrayList<>();
      int lastDotIndex = fileName.lastIndexOf('.');
      if (lastDotIndex > 0 && !fileName.endsWith(".")) {
        extensions.add(fileName.substring(lastDotIndex + 1));
      }
      // set static file name if image name has been generated dynamically
      final String fileNameNoExt;
      if (fileName.matches("diag-[0-9a-f]{32}\\.[a-z]+")) {
        fileNameNoExt = "image";
      } else {
        fileNameNoExt = lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
      }
      // check if also a SVG exists for the provided PNG
      if (extensions.contains("png") &&
        new File(file.getParent(), fileName.substring(0, lastDotIndex) + ".svg").exists()) {
        extensions.add("svg");
      }
      final FileSaverDescriptor descriptor = new FileSaverDescriptor("Export Image to", "Choose the destination file",
        extensions.toArray(new String[]{}));
      FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, (Project) null);

      VirtualFile baseDir = saveImageLastDir;

      if (baseDir == null) {
        baseDir = parentDirectory;
      }

      VirtualFile finalBaseDir = baseDir;

      VirtualFileWrapper destination = saveFileDialog.save(finalBaseDir, fileNameNoExt);
      if (destination != null) {
        try {
          saveImageLastDir = LocalFileSystem.getInstance().findFileByIoFile(destination.getFile().getParentFile());
          Path src = imagePath;
          // if the destination ends with .svg, but the source doesn't, patch the source file name as the user chose a different file type
          if (destination.getFile().getAbsolutePath().endsWith(".svg") && !src.endsWith(".svg")) {
            src = new File(src.toFile().getAbsolutePath().replaceAll("\\.png$", ".svg")).toPath();
          }
          Files.copy(src, destination.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
          LocalFileSystem.getInstance().refreshAndFindFileByIoFile(destination.getFile());
        } catch (IOException ex) {
          String message = "Can't save file: " + ex.getMessage();
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
            .createNotification("Error in plugin", message, NotificationType.ERROR, null);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
        }
      }
    } else {
      LOG.warn("file not found to save: " + path);
    }
  }

  private String extractAndPatchAsciidoctorCss(String asciidoctorVersion) throws IOException {
    String css;

    try (InputStream steam = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
      + asciidoctorVersion
      + "/data/stylesheets/asciidoctor-default.css")) {
      css = IOUtils.toString(steam, StandardCharsets.UTF_8);
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

  private boolean tobeDisposed = false;
  private volatile boolean hasLoadedOnce = false;

  @Override
  public synchronized void setHtml(@NotNull String htmlParam, @NotNull Map<String, String> attributes) {
    if (tobeDisposed) {
      return;
    }
    if (getCefBrowser().getFocusedFrame() == null && hasLoadedOnce) {
      // the CEF browser might have terminated after an initial rendering (seen with 202.6109.22)
      // dispose this component so that it will be reloaded.
      disposeMyself();
      return;
    }
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
            "__IntelliJTools.processImages && __IntelliJTools.processImages();" +
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
        LOG.warn("unable to use JavaScript for update", e);
      }
    }
    // if not successful using JavaScript (like on first rendering attempt), set full content
    if (!result) {
      forceRefresh = false;
      html = html + "<script>window.iterationStamp=" + iterationStamp + ";</script>";
      html = wrapHtmlForPage(html);
      final String htmlToRender = prepareHtml(html, attributes);
      super.setHtml(htmlToRender);
      getCefBrowser().setZoomLevel(uiZoom - 1);
    }
    try {
      // slow down the rendering of the next version of the preview until the rendering if the current version is complete
      // this prevents us building up a queue that would lead to a lagging preview
      if (htmlParam.length() > 0 && !rendered.await(3, TimeUnit.SECONDS)) {
        LOG.warn("rendering didn't complete in time, might be slow or broken");
        if (getCefBrowser().getFocusedFrame() == null || this.isDisposed()) {
          disposeMyself();
        } else {
          forceRefresh = true;
          reregisterHandlers();
        }
      }
    } catch (InterruptedException e) {
      LOG.warn("interrupted while waiting for refresh to complete");
    }
  }

  private void disposeMyself() {
    LOG.warn("triggering disposal of preview");
    ApplicationManager.getApplication().getMessageBus()
      .syncPublisher(AsciiDocPreviewEditor.RefreshPreviewListener.TOPIC)
      .refreshPreview(this);
    tobeDisposed = true;
  }

  @NotNull
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  private String prepareHtml(@NotNull String html, @NotNull Map<String, String> attributes) {
    // Antora plugin might resolve some absolute URLs, convert them to localfile so they get their MD5 that prevents caching
    Pattern pattern = Pattern.compile("<img src=\"file:///([^\"?]*)\"");
    Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8.name()); // restore "%20" as " "
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      String tmpFile = findTempImageFile(file, null);
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        tmpFile = tmpFile.replaceAll("\\\\", "/");
        replacement = "<img src=\"file://" + tmpFile + "?" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, base);
        replacement = "<img src=\"file://" + base.replaceAll("%3A", ":") + "/" + file + "?" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* for each image we'll calculate a MD5 sum of its content. Once the content changes, MD5 and therefore the URL
     * will change. The changed URL is necessary for the JavaFX web view to display the new content, as each URL
     * will be loaded only once by the JavaFX web view. */
    pattern = Pattern.compile("<img src=\"([^:\"]*)\"");
    matcher = pattern.matcher(html);
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
    Disposer.dispose(myBrowserLog);
    Disposer.dispose(myScrollEditorToLine);
    Disposer.dispose(myZoomDelta);
    Disposer.dispose(myZoomReset);
    Disposer.dispose(mySaveImage);
    Disposer.dispose(myOpenLink);
  }

  private String findTempImageFile(String filename, String imagesdir) {
    try {
      Path file = imagesPath.resolve(filename);
      if (Files.exists(file)) {
        return file.toFile().toString();
      }
    } catch (InvalidPathException e) {
      LOG.info("problem decoding decode filename " + filename, e);
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
          LOG.info("problem decoding decode filename " + filename, e);
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
        LOG.info("problem decoding decode filename " + filename, e);
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
    this.line = line;
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
        if (!hasLoadedOnce && myScrollPreservingListener.myScrollY == 0) {
          scrollToLine(line, lineCount);
        }
        hasLoadedOnce = true;
        getCefBrowser().executeJavaScript("document.documentElement.scrollTop = ({} || document.body).scrollTop = " + myScrollY,
          getCefBrowser().getURL(), 0);
      }
    }
  }

  private class BridgeSettingListener extends CefLoadHandlerAdapter {
    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
      getCefBrowser().setZoomLevel(uiZoom - 1);
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
            "}," +
            "zoomDelta : function(deltaY) {" +
            myZoomDelta.inject("deltaY") +
            "}," +
            "zoomReset : function() {" +
            myZoomReset.inject(null) +
            "}," +
            "saveImage : function(src) {" +
            mySaveImage.inject("src") +
            "}" +
            "};" +
            "if ('__IntelliJTools' in window) {" +
            "__IntelliJTools.processLinks && __IntelliJTools.processLinks();" +
            "__IntelliJTools.processImages && __IntelliJTools.processImages();" +
            "__IntelliJTools.pickSourceLine && __IntelliJTools.pickSourceLine(" + lineCount + ");" +
            "__IntelliJTools.addMouseHandler && __IntelliJTools.addMouseHandler();" +
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
      LOG.warn("won't open URI as it might be unsafe: " + uri);
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
        LOG.warn("unable to find file for " + uri);
        return false;
      }
      targetFile = tmpTargetFile;

      Project project = ProjectUtil.guessProjectForContentFile(targetFile);
      if (project == null) {
        LOG.warn("unable to find project for " + uri);
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
