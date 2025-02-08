package org.asciidoc.intellij.editor.jcef;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JBCefPsiNavigationUtils;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.ui.scale.JBUIScale;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.psi.AsciiDocFileUtil;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiDocJCEFHtmlPanel extends JCEFHtmlPanel implements AsciiDocHtmlPanel {

  private static final Logger LOG = Logger.getInstance(AsciiDocJCEFHtmlPanel.class);

  private final Path imagesPath;
  @NotNull
  private final Document document;

  private JBCefJSQuery myJSQuerySetScrollY;
  private JBCefJSQuery myRenderedIteration;
  private JBCefJSQuery myRenderedResult;
  private JBCefJSQuery myScrollEditorToLine;
  private Boolean isAntoraCache;

  @Override
  public void printToPdf(String target, Consumer<Boolean> success) {
    getCefBrowser().printToPDF(target, null, (s, b) -> success.accept(b));
  }

  @Override
  public boolean isPrintingSupported() {
    return true;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    if (getComponent().getComponents().length == 0) {
      return getComponent();
    }
    return (JComponent) getComponent().getComponents()[0];
  }

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

  private volatile int lineCount;
  private volatile int line;
  private volatile boolean forceRefresh = true;
  private volatile long stamp = 0;
  private volatile boolean replaceResult = false;
  private volatile String frameHtml = null;

  @NotNull
  private static final String OUR_CLASS_URL;

  private double uiZoom;

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = NotNullLazyValue.lazy(() -> {
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
  });

  static {
    String url = "about:blank";
    try {
      // JCEF will complain with a "Not allowed to load local resource" error when non-ascii characters are included in this random URL
      // therefore using toASCIIString()
      url = AsciiDocJCEFHtmlPanel.class.getResource(AsciiDocJCEFHtmlPanel.class.getSimpleName() + ".class").toURI().toASCIIString();
    } catch (Exception ex) {
      LOG.warn("unable to initialize url", ex);
    }
    OUR_CLASS_URL = url;
  }

  @Nullable
  private Editor editor;

  public AsciiDocJCEFHtmlPanel(Document document, Path imagesPath, Runnable forceRefresh) {
    super(isOffScreenRenderingEnabled(), null, OUR_CLASS_URL + "@" + new Random().nextInt(Integer.MAX_VALUE));

    this.imagesPath = imagesPath;

    this.document = document;

    registerHandlers();

    myCefLoadHandler = new CefLoadHandlerAdapter() {
      @Override
      public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        myScrollPreservingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
        myBridgeSettingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
      }

      @Override
      public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        // The user might have clicked on a URL that navigates somewhere else. If that happens, reset the HTML view
        if (browser.getURL() != null && !browser.getURL().startsWith("file:///jbcefbrowser/")) {
          LOG.warn("Noticed that the user navigated to " + browser.getURL() + ", resetting the preview");
          previousDigest = null;
          forceRefresh.run();
        }
      }
    };
    getJBCefClient().addLoadHandler(myCefLoadHandler, getCefBrowser());

    try {
      java.util.Properties p = new java.util.Properties();
      try (InputStream stream = PreviewStaticServer.class.getResourceAsStream("/META-INF/asciidoctorj-version.properties")) {
        p.load(stream);
      }
      String asciidoctorVersion = p.getProperty("version.asciidoctor");
      myInlineCss = extractAndPatchAsciidoctorCss(asciidoctorVersion);

      try (InputStream is = PreviewStaticServer.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/coderay-asciidoctor.css")) {
        myInlineCss += IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream is = PreviewStaticServer.class.getResourceAsStream("rouge-github.css")) {
        myInlineCss += IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream is = PreviewStaticServer.class.getResourceAsStream("/tabs/data/css/tabs.css")) {
        myTabsCss = IOUtils.toString(is, StandardCharsets.UTF_8);
        myInlineCss = myInlineCss + myTabsCss;
      }
      try (InputStream is = PreviewStaticServer.class.getResourceAsStream("darcula.css")) {
        myInlineCssDarcula = myInlineCss + IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream is = PreviewStaticServer.class.getResourceAsStream("/tabs/data/css/tabs-darcula.css")) {
        myTabsCssDarcula = IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      myAntoraCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("antora/preview.css") + "\">";
      myAntoraDarculaCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("antora/preview-darcula.css") + "\">";
      myFontAwesomeCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("font-awesome/css/font-awesome.min.css") + "\">";
      myDejavuCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("dejavu/dejavu.css") + "\">";
      myGoogleFontsCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/googlefonts.css") + "\">";
      myDroidSansMonoCssLink = "<link rel=\"stylesheet\" data-default href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/droidsansmono.css") + "\">";
      myMermaidScript = "<script src=\"" + PreviewStaticServer.getScriptUrl("mermaid/mermaid.min.js") + "\"></script>" +
        "<script>mermaid.initialize(); window.mermaid = mermaid; </script>";
      myAsciidoctorTabsScript = "<script src=\"" + PreviewStaticServer.getScriptUrl("tabs.js") + "\"></script>";
    } catch (IOException e) {
      String message = "Unable to combine CSS resources: " + e.getMessage();
      LOG.error(message, e);
      Notification notification = AsciiDocWrapper.getNotificationGroup()
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
    uiZoom = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getZoom() / 100.0;

    myCefClient.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
      @Override
      public void onAfterCreated(CefBrowser browser) {
        // don't queue old content as it might overtake the setHtml() when is added as deferred content
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          if (stamp == 0) {
            synchronized (this) {
              if (stamp == 0) {
                // ensure that this is still the first call; avoid to overwrite a different text
                setHtml("<div id=\"content\">Initializing...</div>", Collections.emptyMap());
              }
            }
          }
        });
      }
    }, getCefBrowser());

  }

  private void registerHandlers() {
    myJSQuerySetScrollY = JBCefJSQuery.create((JBCefBrowserBase) this);
    myRenderedIteration = JBCefJSQuery.create((JBCefBrowserBase) this);
    myRenderedResult = JBCefJSQuery.create((JBCefBrowserBase) this);
    myBrowserLog = JBCefJSQuery.create((JBCefBrowserBase) this);
    myScrollEditorToLine = JBCefJSQuery.create((JBCefBrowserBase) this);
    myZoomDelta = JBCefJSQuery.create((JBCefBrowserBase) this);
    myZoomReset = JBCefJSQuery.create((JBCefBrowserBase) this);
    mySaveImage = JBCefJSQuery.create((JBCefBrowserBase) this);
    myOpenLink = JBCefJSQuery.create((JBCefBrowserBase) this);

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

    myScrollEditorToLine.addHandler(this::handleScrollEditorLine);

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

  private void disposeHandlers() {
    if (myJSQuerySetScrollY != null) {
      myJSQuerySetScrollY.clearHandlers();
      Disposer.dispose(myJSQuerySetScrollY);
    }
    if (myRenderedIteration != null) {
      myRenderedIteration.clearHandlers();
      Disposer.dispose(myRenderedIteration);
    }
    if (myRenderedResult != null) {
      myRenderedResult.clearHandlers();
      Disposer.dispose(myRenderedResult);
    }
    if (myBrowserLog != null) {
      myBrowserLog.clearHandlers();
      Disposer.dispose(myBrowserLog);
    }
    if (myScrollEditorToLine != null) {
      myScrollEditorToLine.clearHandlers();
      Disposer.dispose(myScrollEditorToLine);
    }
    if (myZoomDelta != null) {
      myZoomDelta.clearHandlers();
      Disposer.dispose(myZoomDelta);
    }
    if (myZoomReset != null) {
      myZoomReset.clearHandlers();
      Disposer.dispose(myZoomReset);
    }
    if (mySaveImage != null) {
      mySaveImage.clearHandlers();
      Disposer.dispose(mySaveImage);
    }
    if (myOpenLink != null) {
      myOpenLink.clearHandlers();
      Disposer.dispose(myOpenLink);
    }
  }

  @Nullable
  private JBCefJSQuery.Response handleScrollEditorLine(String r) {
    try {
      if (r.length() != 0) {
        int split = r.indexOf(':');
        int line = (int) Math.round(Double.parseDouble(r.substring(0, split)));
        String file = r.substring(split + 1);
        if (file.equals("stdin")) {
          scrollEditorToLine(line);
        } else {
          ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
              VirtualFile targetFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file);
              if (targetFile != null) {
                Project project = ProjectUtil.guessProjectForContentFile(targetFile);
                if (project != null) {
                  if (LightEdit.owns(project)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                      OpenFileAction.openFile(targetFile, project);
                    });
                  } else {
                    int offset = -1;
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(targetFile);
                    if (psiFile != null && line > 0) {
                      offset = StringUtil.lineColToOffset(psiFile.getText(), line - 1, 0);
                    }
                    Navigatable navigatable = PsiNavigationSupport.getInstance().createNavigatable(project, targetFile, offset);
                    ApplicationManager.getApplication().invokeLater(() -> {
                      navigatable.navigate(true);
                    });
                  }
                } else {
                  LOG.warn("unable to identify project: " + targetFile);
                }
              }
            });
          });
        }
      }
    } catch (NumberFormatException e) {
      LOG.warn("unable parse line number: " + r, e);
    }
    return null;
  }

  private VirtualFile saveImageLastDir = null;

  private void saveImage(@NotNull String path) {
    String parent = imagesPath.getFileName().toString();
    path = URLDecoder.decode(path, StandardCharsets.UTF_8);
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
        baseDir = getParentDirectory();
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
          Notification notification = AsciiDocWrapper.getNotificationGroup()
            .createNotification("Error in plugin", message, NotificationType.ERROR);
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

    try (InputStream steam = PreviewStaticServer.class.getResourceAsStream("/gems/asciidoctor-"
      + asciidoctorVersion
      + "/data/stylesheets/asciidoctor-default.css")) {
      css = IOUtils.toString(steam, StandardCharsets.UTF_8);
    }

    // ensure that preamble renderes even if classes are appended with line numbers
    // https://github.com/asciidoctor/asciidoctor/issues/4564
    css = css.replaceAll(Pattern.quote("#preamble>.sectionbody>[class=paragraph]"), Matcher.quoteReplacement("#preamble>.sectionbody>[class^='paragraph has-source-line']"));

    // the following lines have been added for JavaFX
    // TODO: revisit the following to find out if this is still true for JCEF

    // otherwise embedded SVG images will be skewed to full height of one browser window
    css = css.replaceAll(Pattern.quote("object,embed{height:100%}"), "");

    return css;
  }

  @Nullable
  private String myInlineCss;
  @Nullable
  private String myTabsCss;
  @Nullable
  private String myTabsCssDarcula;
  @Nullable
  private String myInlineCssDarcula;
  @Nullable
  private String myFontAwesomeCssLink;
  @Nullable
  private String myAntoraCssLink;
  @Nullable
  private String myAntoraDarculaCssLink;
  @Nullable
  private String myDejavuCssLink;
  @Nullable
  private String myGoogleFontsCssLink;
  @Nullable
  private String myDroidSansMonoCssLink;

  @Nullable
  private String myMermaidScript;
  @Nullable
  private String myAsciidoctorTabsScript;

  private volatile boolean hasLoadedOnce = false;
  private byte[] previousDigest;

  @Override
  public synchronized void setHtml(@NotNull String htmlParam, @NotNull Map<String, String> attributes) {
    if (isDisposed()) {
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
    String emptyFrame = prepareHtml(wrapHtmlForPage("<!--start-->" + htmlParam + "<!--end-->"), attributes).replaceAll("(?ms)<!--start-->.*<!--end-->", "");
    if (!emptyFrame.equals(frameHtml)) {
      forceRefresh = true;
      frameHtml = emptyFrame;
    }
    String html = htmlParam;
    boolean result = false;
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (hasLoadedOnce && !forceRefresh && settings.getAsciiDocPreviewSettings().isInplacePreviewRefresh() && html.contains("id=\"content\"")) {
      String preparedHtml = prepareHtml(html, attributes);
      // If the HTML is identical, don't replace the preview
      try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(preparedHtml.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        if (Arrays.equals(digest, previousDigest)) {
          return;
        }
        previousDigest = digest;
      } catch (NoSuchAlgorithmException e) {
        // ignored
      }
      final String htmlToReplace = StringEscapeUtils.escapeEcmaScript(preparedHtml);
      // try to replace the HTML contents using JavaScript to avoid flickering MathML
      try {
        replaceResult = false;
        getCefBrowser().executeJavaScript(
            "function finish() {" +
            "if (window.mermaid !== undefined) window.mermaid.run(); " +
            "if (window.initTabs !== undefined) window.initTabs(); " +
            "if ('__IntelliJTools' in window) {" +
            "__IntelliJTools.processLinks && __IntelliJTools.processLinks();" +
            "__IntelliJTools.processImages && __IntelliJTools.processImages();" +
            "__IntelliJTools.pickSourceLine && __IntelliJTools.pickSourceLine(" + lineCount + ");" +
            "}" +
            "window.JavaPanelBridge && window.JavaPanelBridge.rendered(" + iterationStamp + ");" +
            "}" +
            "function updateContent() { " +
            "var elem = document.getElementById('content'); if (elem && elem.parentNode) { " +
            "var div = document.createElement('div');" +
            "div.innerHTML = '" + htmlToReplace + "'; " +
            "var errortext = document.getElementById('mathjaxerrortext'); " +
            "var errorformula = document.getElementById('mathjaxerrorformula'); " +
            "if (errorformula && errortext) { " +
            "  errortext.textContent = ''; " +
            "  errorformula.textContent = ''; " +
            "} " +
            "div.style.cssText = 'display: none'; " +
            // need to add the element to the DOM as MathJAX will use document.getElementById in some places
            "elem.appendChild(div); " +
            "if(typeof hljs !== 'undefined') { [].slice.call(div.querySelectorAll('pre.highlight > code')).forEach(function (el) { hljs.highlightElement(el) }) } " +
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
        if (replaceResultLatch.await(2, TimeUnit.SECONDS)) {
          result = replaceResult;
        }
      } catch (RuntimeException | InterruptedException e) {
        // might happen when rendered output is not valid HTML due to passtrough content
        LOG.warn("unable to use JavaScript for update", e);
      }
    }
    // if not successful using JavaScript (like on first rendering attempt), set full content
    if (!result) {
      previousDigest = null;
      forceRefresh = false;
      html = html + "<script>window.iterationStamp=" + iterationStamp + ";</script>";
      html = wrapHtmlForPage(html);
      final String htmlToRender = prepareHtml(html, attributes);
      loadHTML(htmlToRender);
      getCefBrowser().setZoomLevel(uiZoom - 1);
    }
    try {
      // slow down the rendering of the next version of the preview until the rendering if the current version is complete
      // this prevents us building up a queue that would lead to a lagging preview
      if (htmlParam.length() > 0 && !rendered.await(3, TimeUnit.SECONDS)) {
        // error handling only if:
        // the preview has rendered once -- as we don't want to close a window that is opening
        // this preview hasn't been disposed -- as this wouldn't make much sense
        if (!this.isDisposed() && hasLoadedOnce) {
          LOG.warn("rendering didn't complete in time, might be slow or broken");
          forceRefresh = true;
        }
      }
    } catch (InterruptedException e) {
      LOG.warn("interrupted while waiting for refresh to complete");
    }
  }

  @NotNull
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  private static final Pattern ESCAPED_COLON = Pattern.compile("%3A");
  private static final Pattern IMAGE_FROM_ANTORA = Pattern.compile("<img src=\"file:///([^\"?]*)\"");
  private static final Pattern IMAGE_RELATIVE = Pattern.compile("<img src=\"([^:\"]*)\"");
  private static final Pattern IMAGE_AS_OBJECT = Pattern.compile("<object ([^>])*data=\"([^:\"]*)\"");

  private String getBase() {
    VirtualFile parentDirectory = getParentDirectory();
    String base;
    if (parentDirectory != null) {
      // parent will be null if we use Language Injection and Fragment Editor
      base = parentDirectory.getUrl().replaceAll("^file://", "")
        .replaceAll(":", "%3A");
    } else {
      base = "";
    }
    return base;
  }

  @Nullable
  private VirtualFile getParentDirectory() {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    VirtualFile parentDirectory = null;
    if (file != null) {
      parentDirectory = file.getParent();
    }
    return parentDirectory;
  }

  private String prepareHtml(@NotNull String html, @NotNull Map<String, String> attributes) {
    // Antora plugin might resolve some absolute URLs, convert them to local file, so they get their MD5 that prevents caching
    String baseForHtml = ESCAPED_COLON.matcher(getBase()).replaceAll(":");
    Matcher matcher = IMAGE_FROM_ANTORA.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      String tmpFile = findTempImageFile(file, null);
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        tmpFile = tmpFile.replaceAll("\\\\", "/");
        replacement = "<img src=\"file://" + tmpFile + "?" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, baseForHtml);
        replacement = "<img src=\"file://" + baseForHtml + "/" + file + "?" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* for each image we'll calculate a MD5 sum of its content. Once the content changes, MD5 and therefore the URL
     * will change. The changed URL is necessary for the JavaFX web view to display the new content, as each URL
     * will be loaded only once by the JavaFX web view. */
    matcher = IMAGE_RELATIVE.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      String replacement = null;
      String tmpFile = findTempImageFile(file, attributes.get("imagesdir"));
      if (tmpFile != null) {
        replacement = calculateFileAndMd5(tmpFile, null);
      }
      if (replacement == null) {
        replacement = calculateFileAndMd5(file, baseForHtml);
      }
      if (replacement == null && file.startsWith("/") && editor != null) {
        VirtualFile hugoStaticFile = AsciiDocUtil.findHugoStaticFolder(editor.getProject(), getParentDirectory());
        if (hugoStaticFile != null) {
          replacement = calculateFileAndMd5(file.substring(1), hugoStaticFile.getCanonicalPath());
        }
      }
      if (replacement == null && attributes.get("imagesdir") != null && attributes.get("imagesdir").length() > 0 && file.startsWith("/")) {
        // For image file names starting with a slash, the imagesdir is not being added automatically.
        // Try to use it to find the file - imagesdir might be relative to the base directory, or an absolute path.
        replacement = calculateFileAndMd5(attributes.get("imagesdir") + file, baseForHtml);
        if (replacement == null) {
          replacement = calculateFileAndMd5(file, attributes.get("imagesdir"));
        }
      }
      if (replacement == null) {
        // some fallback
        replacement = baseForHtml + "/" + file + "?none";
      }
      replacement = "<img src=\"file://" + replacement + "\"";
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* the same as above for interactive SVGs */
    matcher = IMAGE_AS_OBJECT.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String other = matchResult.group(1);
      if (other == null) {
        other = "";
      }
      String file = matchResult.group(2);
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      String tmpFile = findTempImageFile(file, attributes.get("imagesdir"));
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        replacement = "<object " + other + "data=\"file://" + tmpFile + "?" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, baseForHtml);
        replacement = "<object " + other + "data=\"file://" + baseForHtml + "/" + file + "?" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    if (isAntora()) {
      html = AsciiDocWrapper.enrichPage(html, (isDarcula() ? myAntoraDarculaCssLink : myAntoraCssLink) + myFontAwesomeCssLink + AsciiDocHtmlPanel.getCssLines(myTabsCss + (isDarcula() ? myTabsCssDarcula : "")), myMermaidScript, myAsciidoctorTabsScript, attributes, editor != null ? editor.getProject() : null);
    } else {
      html = AsciiDocWrapper.enrichPage(html, AsciiDocHtmlPanel.getCssLines(isDarcula() ? myInlineCssDarcula + myTabsCssDarcula : myInlineCss) + myFontAwesomeCssLink + myDroidSansMonoCssLink + myGoogleFontsCssLink + myDejavuCssLink, myMermaidScript, myAsciidoctorTabsScript, attributes, editor != null ? editor.getProject() : null);
    }

    html = html.replaceAll("<head>", "<head>\n" +
      "<meta http-equiv=\"Content-Security-Policy\" content=\"" + PreviewStaticServer.createCSP(attributes) + "\">");

    /* Add JavaScript for auto-scolling and clickable links */
    return html
      .replace("</body>", getScriptingLines() + "</body>");
  }

  private synchronized boolean isAntora() {
    if (isAntoraCache == null) {
      AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
          isAntoraCache = editor != null && editor.getProject() != null
            && AsciiDocUtil.findAntoraPagesDir(editor.getProject(), getParentDirectory()) != null;
      });
    }
    return isAntoraCache;
  }

  private String calculateFileAndMd5(String file, String base) {
    file = file.replaceAll("\\\\", "/");
    String md5 = calculateMd5(file, base);
    if (!md5.equals("none")) {
      return (base != null ? base + "/" : "") + file + "?" + md5;
    } else {
      return null;
    }
  }

  @Override
  public void dispose() {
    getJBCefClient().removeLoadHandler(myCefLoadHandler, getCefBrowser());
    disposeHandlers();
    super.dispose();
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

  // use pattern for %3A, and move that to the caller
  private String calculateMd5(String file, String baseForHtml) {
    String md5;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      try (FileInputStream fis = new FileInputStream((baseForHtml != null ? baseForHtml + "/" : "") + file)) {
        int nread;
        byte[] dataBytes = new byte[10240];
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        }
      }
      md5 = new BigInteger(1, md.digest()).toString(16);
    } catch (NoSuchAlgorithmException | IOException e) {
      md5 = "none";
    }
    return md5;
  }

  @NotNull
  private String wrapHtmlForPage(String html) {
    return "<html><head></head><body><div id=\"header\"></div><div style='position:fixed;margin:0;padding:0;top:0;left:0;background-color:#eeeeee;color:red;z-index:99;'><div id='mathjaxerrortext'></div><pre style='color:red;margin:0;padding:0' id='mathjaxerrorformula'></pre></div>"
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
        return !JBColor.isBright();
      case ASCIIDOC:
        return false;
      case DARCULA:
        return true;
      default:
        return false;
    }
  }


  @Override
  public synchronized void scrollToLine(int line, int lineCount) {
    if (this.lineCount == lineCount && this.line == line) {
      return;
    }
    this.lineCount = lineCount;
    this.line = line;
    scrollToLineInBrowser(line, lineCount);
  }

  /**
   * Scroll line in browser, which is independent of the locking, so it can be called from onLoadingStateChange to set the scrolling.
   */
  private void scrollToLineInBrowser(int line, int lineCount) {
    try {
      getCefBrowser().executeJavaScript(
        "if ('__IntelliJTools' in window) " +
          "__IntelliJTools.scrollToLine(" + line + ", " + lineCount + ");",
        getCefBrowser().getURL(), 0);

      getCefBrowser().executeJavaScript(
        "var value = document.documentElement.scrollTop || document.body.scrollTop;" +
          myJSQuerySetScrollY.inject("value"),
        getCefBrowser().getURL(), 0);
    } catch (IllegalStateException ex) {
      if (ex.getMessage().equals("the JS query has been disposed")) {
        LOG.info("JS query has already been disposed, can't place cursor in preview");
      } else {
        throw ex;
      }
    }
  }

  @Override
  public @Nullable Editor getEditor() {
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
        if (!hasLoadedOnce) {
          if (SystemInfoRt.isWindows) {
            // set focus once to ensure that focus is set correctly on startup for open editors
            ApplicationManager.getApplication().executeOnPooledThread(() -> getCefBrowser().setFocus(getPreferredFocusedComponent().hasFocus()));
          }
        }
        if (!hasLoadedOnce && myScrollPreservingListener.myScrollY == 0) {
          scrollToLineInBrowser(line, lineCount);
        } else {
          getCefBrowser().executeJavaScript("document.documentElement.scrollTop = ({} || document.body).scrollTop = " + myScrollY,
            getCefBrowser().getURL(), 0);
        }
        hasLoadedOnce = true;
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
      if (SystemInfoRt.isWindows && link.matches("[A-Z]:/[^/].*")) {
        link = "file:///" + link;
      }
      if (link.contains(" ")) {
        link = link.replaceAll(" ", "%20");
      }
      uri = new URI(link);
    } catch (URISyntaxException ex) {
      throw new RuntimeException("unable to parse URL " + link);
    }

    String scheme = uri.getScheme();
    if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
      BrowserUtil.browse(uri);
    } else if ("file".equalsIgnoreCase(scheme) || scheme == null) {
      AsciiDocFileUtil.openInEditor(uri, editor, getParentDirectory());
    } else {
      LOG.warn("won't open URI as it might be unsafe: " + uri);
    }
  }

  public void scrollEditorToLine(int sourceLine) {
    if (sourceLine <= 0) {
      Notification notification = AsciiDocWrapper.getNotificationGroup().createNotification("Setting cursor position", "line number " + sourceLine + " requested for cursor position, ignoring",
        NotificationType.INFORMATION);
      notification.setImportant(false);
      return;
    }
    ApplicationManager.getApplication().invokeLater(
      () -> {
        Editor editor = getEditor();
        if (editor != null && !editor.isDisposed()) {
          editor.getCaretModel().setCaretsAndSelections(
            Collections.singletonList(new CaretState(new LogicalPosition(sourceLine - 1, 0), null, null))
          );
          editor.getScrollingModel().scrollToCaret(ScrollType.CENTER_UP);
        }
      }
    );
  }

  private static boolean isOffScreenRenderingEnabled() {
    return Registry.is("ide.browser.jcef.asciidocView.osr.enabled", true) && JBCefApp.isOffScreenRenderingModeEnabled();
  }

}
