package org.asciidoc.intellij.editor.javafx;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeEventQueue;
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
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.html.HTMLImageElement;

import javax.swing.*;
import java.awt.*;
import java.io.File;
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
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFxHtmlPanel extends AsciiDocHtmlPanel {

  private Logger log = Logger.getInstance(JavaFxHtmlPanel.class);

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

  @NotNull
  private final JPanel myPanelWrapper;
  @NotNull
  private final List<Runnable> myInitActions = new ArrayList<>();
  @Nullable
  private volatile JFXPanel myPanel;
  @Nullable
  private WebView myWebView;
  @Nullable
  private String myInlineCss;
  @Nullable
  private String myInlineCssDarcula;
  @Nullable
  private String myFontAwesomeCssLink;
  @Nullable
  private String myDejavuCssLink;
  @NotNull
  private final ScrollPreservingListener myScrollPreservingListener = new ScrollPreservingListener();
  @NotNull
  private final BridgeSettingListener myBridgeSettingListener = new BridgeSettingListener();

  @NotNull
  private String base;

  private int lineCount;

  private final Path imagesPath;

  private VirtualFile parentDirectory;
  private VirtualFile saveImageLastDir = null;

  private volatile CountDownLatch rendered = new CountDownLatch(1);
  private volatile boolean forceRefresh = false;
  private volatile long stamp = 0;

  JavaFxHtmlPanel(Document document, Path imagesPath) {

    //System.setProperty("prism.lcdtext", "false");
    //System.setProperty("prism.text", "t2k");

    this.imagesPath = imagesPath;

    myPanelWrapper = new JPanel(new BorderLayout());
    myPanelWrapper.setBackground(JBColor.background());
    lineCount = document.getLineCount();

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
    } catch (IOException e) {
      String message = "Unable to combine CSS resources: " + e.getMessage();
      log.error(message, e);
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }

    ApplicationManager.getApplication().invokeLater(() -> runFX(new Runnable() {
      @Override
      public void run() {
        if (new JavaFxHtmlPanelProvider().isAvailable() == AsciiDocHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE) {
          String message = "JavaFX unavailable, probably stuck";
          log.warn(message);
          return;
        }
        try {
          PlatformImpl.startup(() -> {
            myWebView = new WebView();

            updateFontSmoothingType(myWebView, false);
            registerContextMenu(JavaFxHtmlPanel.this.myWebView);
            myWebView.setContextMenuEnabled(false);
            myWebView.setZoom(JBUI.scale(1.f));
            myWebView.getEngine().loadContent(prepareHtml("<html><head></head><body>Initializing...</body>", null));

            myWebView.addEventFilter(ScrollEvent.SCROLL, scrollEvent -> {
              // touch pads send lots of events with deltaY == 0, not handling them here
              if (scrollEvent.isControlDown() && Double.compare(scrollEvent.getDeltaY(), 0.0) != 0) {
                double zoom = myWebView.getZoom();
                double factor = (scrollEvent.getDeltaY() > 0.0 ? 0.1 : -0.1)
                  // normalize scrolling
                  * (Math.abs(scrollEvent.getDeltaY()) / scrollEvent.getMultiplierY())
                  // adding one to make it a factor that we can multiply the zoom by
                  + 1;
                zoom = zoom * factor;
                // define minimum/maximum zoom
                if (zoom < JBUI.scale(0.1f)) {
                  zoom = 0.1;
                } else if (zoom > JBUI.scale(10.f)) {
                  zoom = 10;
                }
                myWebView.setZoom(zoom);
                scrollEvent.consume();
              }
            });

            myWebView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
              if (mouseEvent.isControlDown() && mouseEvent.getButton() == MouseButton.MIDDLE) {
                myWebView.setZoom(JBUI.scale(1f));
                mouseEvent.consume();
              }
            });

            final WebEngine engine = myWebView.getEngine();
            engine.getLoadWorker().stateProperty().addListener(myBridgeSettingListener);
            engine.getLoadWorker().stateProperty().addListener(myScrollPreservingListener);

            final Scene scene = new Scene(myWebView);

            ApplicationManager.getApplication().invokeLater(() -> runFX(() -> {
              try {
                synchronized (myInitActions) {
                  myPanel = new JFXPanelWrapper();
                  Platform.runLater(() -> myPanel.setScene(scene));
                  for (Runnable action : myInitActions) {
                    Platform.runLater(action);
                  }
                  myInitActions.clear();
                }
                myPanelWrapper.add(myPanel, BorderLayout.CENTER);
                myPanelWrapper.repaint();
              } catch (Throwable e) {
                log.warn("can't initialize JFXPanelWrapper", e);
              }
            }));
          });
        } catch (Throwable ex) {
          String message = "Error initializing JavaFX: " + ex.getMessage();
          log.error(message, ex);
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("Error rendering asciidoctor", message,
            NotificationType.ERROR, null);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
        }
      }
    }));

  }

  private String extractAndPatchAsciidoctorCss(String asciidoctorVersion) throws IOException {
    String css;

    try (InputStream steam = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
      + asciidoctorVersion
      + "/data/stylesheets/asciidoctor-default.css")) {
      css = IOUtils.toString(steam);
    }

    // asian characters won't display with text-rendering:optimizeLegibility
    // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/203
    css = css.replaceAll("text-rendering:", "disabled-text-rendering:");

    // word-wrap:break-word make the preview wrap listings when it shouldn't
    // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/350
    css = css.replaceAll("word-wrap:break-word", "disabled-word-wrap:break-word");

    // JavaFX doesn't load 'DejaVu Sans Mono' font when 'Droid Sans Mono' is listed first
    // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/193
    css = css.replaceAll("(\"Noto Serif\"|\"Open Sans\"|\"Droid Sans Mono\"),", "");

    return css;
  }

  private void registerContextMenu(WebView webView) {
    webView.setOnMousePressed(e -> {
      if (e.getButton() == MouseButton.SECONDARY) {
        JSObject object = getJavaScriptObjectAtLocation(webView, e);
        if (object instanceof HTMLImageElement) {
          String src = ((HTMLImageElement) object).getAttribute("src");
          ApplicationManager.getApplication().invokeLater(() -> saveImage(src));
        }
      }
    });
  }

  private JSObject getJavaScriptObjectAtLocation(WebView webView, MouseEvent e) {
    String script = String.format("document.elementFromPoint(%s,%s);", e.getX(), e.getY());
    return (JSObject) webView.getEngine().executeScript(script);
  }

  private void saveImage(@NotNull String path) {
    String parent = imagesPath.getFileName().toString();
    try {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      log.warn("unable to decode URL " + path, e);
      return;
    }
    int indexOfParent = path.indexOf(parent);
    if (indexOfParent == -1) {
      // parent string not found for image, image not generated by preview?
      log.warn("found path " + path + " without " + parent);
      return;
    }
    String subPath = path.substring(indexOfParent + parent.length() + 1);
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
        new File(file.getParent(), fileNameNoExt + ".svg").exists()) {
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

      SwingUtilities.invokeLater(() -> {
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
          } catch (IOException ex) {
            String message = "Can't save file: " + ex.getMessage();
            Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
              .createNotification("Error in plugin", message, NotificationType.ERROR, null);
            // increase event log counter
            notification.setImportant(true);
            Notifications.Bus.notify(notification);
          }
        }
      });
    } else {
      log.warn("file not found to save: " + path);
    }
  }

  private static void runFX(@NotNull Runnable r) {
    IdeEventQueue.unsafeNonblockingExecute(r);
  }

  private void runInPlatformWhenAvailable(@NotNull final Runnable runnable) {
    synchronized (myInitActions) {
      if (myPanel == null) {
        myInitActions.add(runnable);
      } else {
        Platform.runLater(runnable);
      }
    }
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

  private static void updateFontSmoothingType(@NotNull WebView view, boolean isGrayscale) {
    final FontSmoothingType typeToSet;
    if (isGrayscale) {
      typeToSet = FontSmoothingType.GRAY;
    } else {
      typeToSet = FontSmoothingType.LCD;
    }
    view.fontSmoothingTypeProperty().setValue(typeToSet);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanelWrapper;
  }

  @Override
  public synchronized void setHtml(@NotNull String htmlParam, String imagesdir) {
    rendered = new CountDownLatch(1);
    stamp += 1;
    if (stamp > 10000) {
      // force a refresh to avoid memory leaks
      forceRefresh = true;
      stamp = 0;
    }
    long iterationStamp = stamp;
    runInPlatformWhenAvailable(() -> {
      String html = htmlParam;
      if (isDarcula()) {
        // clear out coderay inline CSS colors as they are barely readable in darcula theme
        html = html.replaceAll("<span style=\"color:#[a-zA-Z0-9]*;?", "<span style=\"");
        html = html.replaceAll("<span style=\"background-color:#[a-zA-Z0-9]*;?", "<span style=\"");
      }
      boolean result = false;
      final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
      if (!forceRefresh && settings.getAsciiDocPreviewSettings().isInplacePreviewRefresh() && html.contains("id=\"content\"")) {
        final String htmlToReplace = StringEscapeUtils.escapeEcmaScript(prepareHtml(html, imagesdir));
        // try to replace the HTML contents using JavaScript to avoid flickering MathML
        try {
          boolean ml = false; // set to "true" to test for memory leaks in HTML/JavaScript
          result = (Boolean) JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
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
              "return true; } else { return false; }}; updateContent();"
          );
        } catch (RuntimeException e) {
          // might happen when rendered output is not valid HTML due to passtrough content
          log.warn("unable to use JavaScript for update", e);
        }
      }
      // if not successful using JavaScript (like on first rendering attempt), set full content
      if (!result) {
        forceRefresh = false;
        html = "<html><head></head><body><div style='position:fixed;top:0;left:0;background-color:#eeeeee;color:red;z-index:99;'><div id='mathjaxerrortext'></div><pre style='color:red' id='mathjaxerrorformula'></pre></div>"
          + html
          + "<script>window.iterationStamp=" + iterationStamp + " </script></body>";
        final String htmlToRender = prepareHtml(html, imagesdir);
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().loadContent(htmlToRender);
      }
    });
    try {
      // slow down the rendering of the next version of the preview until the rendering if the current version is complete
      // this prevents us building up a queue that would lead to a lagging preview
      if (htmlParam.length() > 0 && !rendered.await(3, TimeUnit.SECONDS)) {
        log.warn("rendering didn't complete in time, might be slow or broken");
        forceRefresh = true;
      }
    } catch (InterruptedException e) {
      log.warn("interrupted while waiting for refresh to complete");
    }
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

  private String prepareHtml(@NotNull String html, String imagesdir) {
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
      String tmpFile = findTempImageFile(file, imagesdir);
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
        if (JavaFxHtmlPanelProvider.isInitialized()) {
          replacement = "<img src=\"localfile://" + md5 + "/" + tmpFile + "\"";
        } else {
          replacement = "<img src=\"file://" + tmpFile + "\"";
        }
      } else {
        md5 = calculateMd5(file, base);
        if (JavaFxHtmlPanelProvider.isInitialized()) {
          replacement = "<img src=\"localfile://" + md5 + "/" + base + "/" + file + "\"";
        } else {
          replacement = "<img src=\"file://" + base.replaceAll("%3A", ":") + "/" + file + "\"";
        }
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
      String tmpFile = findTempImageFile(file, imagesdir);
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
        if (JavaFxHtmlPanelProvider.isInitialized()) {
          replacement = "<object " + other + "data=\"localfile://" + md5 + "/" + tmpFile + "\"";
        } else {
          replacement = "<object " + other + "data=\"file://" + tmpFile + "\"";
        }
      } else {
        md5 = calculateMd5(file, base);
        if (JavaFxHtmlPanelProvider.isInitialized()) {
          replacement = "<object " + other + "data=\"localfile://" + md5 + "/" + base + "/" + file + "\"";
        } else {
          replacement = "<object " + other + "data=\"file://" + base.replaceAll("%3A", ":") + "/" + file + "\"";
        }
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    // filter out Twitter's JavaScript, as it is problematic for JDK8 JavaFX
    // see: https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/235
    html = html.replaceAll("(?i)<script [a-z ]*src=\"https://platform\\.twitter\\.com/widgets\\.js\" [^>]*></script>", "");

    /* Add CSS line and JavaScript for auto-scolling and clickable links */
    return html
      .replace("<head>", "<head>" + getCssLines(isDarcula() ? myInlineCssDarcula : myInlineCss) + myFontAwesomeCssLink + myDejavuCssLink)
      .replace("</body>", getScriptingLines() + "</body>");
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

  @Override
  public void render() {
    runInPlatformWhenAvailable(() -> {
      JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().reload();
      ApplicationManager.getApplication().invokeLater(myPanelWrapper::repaint);
    });
  }

  @Override
  public void scrollToLine(final int line, final int lineCount) {
    this.lineCount = lineCount;
    runInPlatformWhenAvailable(() -> {
      JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
        "if ('__IntelliJTools' in window) " +
          "__IntelliJTools.scrollToLine(" + line + ", " + lineCount + ");"
      );
      final Object result = JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
        "document.documentElement.scrollTop || document.body.scrollTop");
      if (result instanceof Number) {
        myScrollPreservingListener.myScrollY = ((Number) result).intValue();
      }
    });
  }

  @Override
  public void dispose() {
    runInPlatformWhenAvailable(() -> {
      JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().load("about:blank");
      JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myScrollPreservingListener);
      JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myBridgeSettingListener);
    });
  }

  @NotNull
  private WebView getWebViewGuaranteed() {
    if (myWebView == null) {
      throw new IllegalStateException("WebView should be initialized by now. Check the caller thread");
    }
    return myWebView;
  }

  @NotNull
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  @SuppressWarnings("unused")
  public class JavaPanelBridge {

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

    public void rendered(long iterationStamp) {
      if (stamp == iterationStamp) {
        rendered.countDown();
      }
    }

    private boolean openInEditor(@NotNull URI uri) {
      return ReadAction.compute(() -> {
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

    public void log(@Nullable String text) {
      Logger.getInstance(JavaPanelBridge.class).warn(text);
    }
  }

  /* keep bridge in class instance to avoid cleanup of bridge due to weak references in
    JavaScript mappings starting from JDK 8 111
    see: https://bugs.openjdk.java.net/browse/JDK-8170515
   */
  private JavaPanelBridge bridge = new JavaPanelBridge();

  private class BridgeSettingListener implements ChangeListener<State> {
    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
      if (newValue == State.SUCCEEDED) {
        try {
          JSObject win
            = (JSObject) getWebViewGuaranteed().getEngine().executeScript("window");
          win.setMember("JavaPanelBridge", bridge);
          JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
            "if ('__IntelliJTools' in window) {" +
              "__IntelliJTools.processLinks && __IntelliJTools.processLinks();" +
              "__IntelliJTools.pickSourceLine && __IntelliJTools.pickSourceLine(" + lineCount + ");" +
              "}" +
              "JavaPanelBridge.rendered(window.iterationStamp);"
          );
        } catch (RuntimeException e) {
          // might happen when rendered output is not valid HTML due to passtrough content
          log.warn("unable to initialize page JavaScript", e);
          forceRefresh = true;
          rendered.countDown();
        }
      }
    }
  }

  private class ScrollPreservingListener implements ChangeListener<State> {
    private volatile int myScrollY = 0;

    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
      if (newValue == State.RUNNING) {
        final Object result =
          getWebViewGuaranteed().getEngine().executeScript("document.documentElement.scrollTop || document.body.scrollTop");
        if (result instanceof Number) {
          myScrollY = ((Number) result).intValue();
        }
      } else if (newValue == State.SUCCEEDED) {
        getWebViewGuaranteed().getEngine()
          .executeScript("document.documentElement.scrollTop = document.body.scrollTop = " + myScrollY);
      }
    }
  }
}
