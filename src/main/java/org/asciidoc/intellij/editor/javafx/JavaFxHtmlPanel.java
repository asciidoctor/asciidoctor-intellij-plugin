package org.asciidoc.intellij.editor.javafx;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.ui.JBColor;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFxHtmlPanel extends AsciiDocHtmlPanel {

  private static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("asciidoctor",
      NotificationDisplayType.NONE, true);

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      final Class<JavaFxHtmlPanel> clazz = JavaFxHtmlPanel.class;
      //noinspection StringBufferReplaceableByString
      return new StringBuilder()
          .append("<script src=\"").append(clazz.getResource("scrollToElement.js")).append("\"></script>\n")
          .append("<script src=\"").append(clazz.getResource("processLinks.js")).append("\"></script>\n")
          .toString();
    }
  };

  @NotNull
  private final JPanel myPanelWrapper;
  @NotNull
  private final List<Runnable> myInitActions = new ArrayList<Runnable>();
  @Nullable
  private JFXPanel myPanel;
  @Nullable
  private WebView myWebView;
  @Nullable
  private String myInlineCss;
  @NotNull
  private final ScrollPreservingListener myScrollPreservingListener = new ScrollPreservingListener();
  @NotNull
  private final BridgeSettingListener myBridgeSettingListener = new BridgeSettingListener();

  @NotNull
  private String base;

  public JavaFxHtmlPanel(Document document) {
    //System.setProperty("prism.lcdtext", "false");
    //System.setProperty("prism.text", "t2k");
    myPanelWrapper = new JPanel(new BorderLayout());
    myPanelWrapper.setBackground(JBColor.background());

    base = FileDocumentManager.getInstance().getFile(document).getParent().getUrl().replaceAll("^file://", "")
        .replaceAll(":", "%3A");

    try {
      myInlineCss = IOUtils.toString(JavaFxHtmlPanel.class.getResourceAsStream("default.css"));
    }
    catch (IOException e) {
      String message = "Error rendering asciidoctor: " + e.getMessage();
      Notification notification = NOTIFICATION_GROUP.createNotification("Error rendering asciidoctor", message,
          NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        PlatformImpl.startup(new Runnable() {
          @Override
          public void run() {
            myWebView = new WebView();

            updateFontSmoothingType(myWebView,
                true);
            myWebView.setContextMenuEnabled(false);

            final WebEngine engine = myWebView.getEngine();
            engine.getLoadWorker().stateProperty().addListener(myBridgeSettingListener);
            engine.getLoadWorker().stateProperty().addListener(myScrollPreservingListener);

            final Scene scene = new Scene(myWebView);

            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                JavaFxHtmlPanel.this.setHtml("Initializing...");
                myPanel = new JFXPanelWrapper();
                myPanel.setScene(scene);

                for (Runnable action : myInitActions) {
                  Platform.runLater(action);
                }
                myInitActions.clear();

                myPanelWrapper.add(myPanel, BorderLayout.CENTER);
                myPanelWrapper.repaint();
              }
            });
          }
        });
      }
    });

  }

  private void runInPlatformWhenAvailable(@NotNull final Runnable runnable) {
    if (myPanel == null) {
      myInitActions.add(runnable);
    }
    else {
      /* normally you should queue all runable for JavaFX. But I don't want to flood
         JavaFX and instead this is building up some backpressure to the AsciiDoc rendering task.
         The slower the JavaFX component is, the fewer updates we'll send to it.
       */
      final CountDownLatch doneSignal = new CountDownLatch(1);
      Runnable wrappedRunnable = new Runnable() {
        @Override
        public void run() {
          try {
            runnable.run();
          }
          finally {
            doneSignal.countDown();
          }
        }
      };
      Platform.runLater(wrappedRunnable);
      try {
        /* this is limited to 10 seconds to avoid a permanent lock condition (in case the Runable is not
        executed for any reason */
        doneSignal.await(10, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        // ignore interruption
      }
    }
  }


  private static void updateFontSmoothingType(@NotNull WebView view, boolean isGrayscale) {
    final FontSmoothingType typeToSet;
    if (isGrayscale) {
      typeToSet = FontSmoothingType.GRAY;
    }
    else {
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
  public void setHtml(@NotNull String html) {
    html = "<html><head></head><body>" + html + "</body>";
    final String htmlToRender = prepareHtml(html);

    runInPlatformWhenAvailable(new Runnable() {
      @Override
      public void run() {
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().loadContent(htmlToRender);
      }
    });
  }

  private String prepareHtml(@NotNull String html) {
    Pattern pattern = Pattern.compile("<img src=\"([^:\"]*)\"");
    final Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      String md5 = calculateMd5(file);
      String replacement = "<img src=\"localfile://" + md5 + "/" + base + "/" + file + "\"";
      html = html.substring(0, matchResult.start()) +
          replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    return html
        .replace("<head>", "<head>" + getCssLines(myInlineCss))
        .replace("</body>", getScriptingLines() + "</body>");
  }

  private String calculateMd5(String file) {
    String md5;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      FileInputStream fis = new FileInputStream(base.replaceAll("%3A", ":") + "/" + file);
      int nread = 0;
      byte[] dataBytes = new byte[1024];
      while ((nread = fis.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }
      byte[] mdbytes = md.digest();
      StringBuffer sb = new StringBuffer();
      for (int i = 0;i < mdbytes.length;i++) {
        sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      md5 = sb.toString();
    }
    catch (NoSuchAlgorithmException | IOException e) {
      md5 = "none";
    }
    return md5;
  }

  @Override
  public void render() {
    runInPlatformWhenAvailable(new Runnable() {
      @Override
      public void run() {
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().reload();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            myPanelWrapper.repaint();
          }
        });
      }
    });
  }

  @Override
  public void scrollToLine(final int line, final int lineCount) {
    runInPlatformWhenAvailable(new Runnable() {
      @Override
      public void run() {
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
            "if ('__IntelliJTools' in window) " +
                "__IntelliJTools.scrollToLine(" + line + ", " + lineCount + ");"
        );
        final Object result = JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
            "document.documentElement.scrollTop || document.body.scrollTop");
        if (result instanceof Number) {
          myScrollPreservingListener.myScrollY = ((Number)result).intValue();
        }
      }
    });
  }

  @Override
  public void dispose() {
    runInPlatformWhenAvailable(new Runnable() {
      @Override
      public void run() {
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().load("about:blank");
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myScrollPreservingListener);
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myBridgeSettingListener);
      }
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
  public static class JavaPanelBridge {
    public void openInExternalBrowser(@NotNull String link) {
      if (!BrowserUtil.isAbsoluteURL(link)) {
        try {
          link = new URI("http", link, null).toURL().toString();
        }
        catch (Exception ignore) {
          ignore.printStackTrace();
        }
      }

      BrowserUtil.browse(link);
    }

    public void log(@Nullable String text) {
      Logger.getInstance(JavaPanelBridge.class).warn(text);
    }
  }

  private class BridgeSettingListener implements ChangeListener<State> {
    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
      if (newValue == State.SUCCEEDED) {
        JSObject win
            = (JSObject)getWebViewGuaranteed().getEngine().executeScript("window");
        win.setMember("JavaPanelBridge", new JavaPanelBridge());
        JavaFxHtmlPanel.this.getWebViewGuaranteed().getEngine().executeScript(
            "if ('__IntelliJTools' in window) " +
                "__IntelliJTools.processLinks();"
        );
      }
    }
  }

  private class ScrollPreservingListener implements ChangeListener<State> {
    volatile int myScrollY = 0;

    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
      if (newValue == State.RUNNING) {
        final Object result =
            getWebViewGuaranteed().getEngine().executeScript("document.documentElement.scrollTop || document.body.scrollTop");
        if (result instanceof Number) {
          myScrollY = ((Number)result).intValue();
        }
      }
      else if (newValue == State.SUCCEEDED) {
        getWebViewGuaranteed().getEngine()
            .executeScript("document.documentElement.scrollTop = document.body.scrollTop = " + myScrollY);
      }
    }
  }
}
