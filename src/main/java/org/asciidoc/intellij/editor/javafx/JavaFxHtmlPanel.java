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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
                myPanel = new JFXPanelWrapper();
                myPanel.setScene(scene);

                JavaFxHtmlPanel.this.setHtml("");
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

  private void runInPlatformWhenAvailable(@NotNull Runnable runnable) {
    if (myPanel == null) {
      myInitActions.add(runnable);
    }
    else {
      Platform.runLater(runnable);
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
    return html
        .replace("<head>", "<head>" + getCssLines(myInlineCss))
        .replace("<head>", "<head><base href=\"localfile://" + System.currentTimeMillis() + "/" + base + "/\" />\n");
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

  @SuppressWarnings("unused")
  public static class JavaPanelBridge {
    public void openInExternalBrowser(@NotNull String link) {
      if (!BrowserUtil.isAbsoluteURL(link)) {
        try {
          link = new URI("http", link, null).toURL().toString();
        }
        catch (Exception ignore) {
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
      JSObject win
          = (JSObject)getWebViewGuaranteed().getEngine().executeScript("window");
      win.setMember("JavaPanelBridge", new JavaPanelBridge());
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
