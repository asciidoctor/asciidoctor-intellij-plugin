package org.asciidoc.intellij.editor.browser;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.Base64;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanel;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserPanel implements Closeable {

  private final Path imagesPath;
  private Logger log = Logger.getInstance(JavaFxHtmlPanel.class);

  private String base;

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      //noinspection StringBufferReplaceableByString
      return new StringBuilder()
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("scrollToElement.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("processLinks.js")).append("\"></script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("pickSourceLine.js")).append("\"></script>\n")
        .append("<script type=\"text/x-mathjax-config\">\n" +
          "MathJax.Hub.Config({\n" +
          "  messageStyle: \"none\",\n" +
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
          "</script>\n")
        .append("<script src=\"").append(PreviewStaticServer.getScriptUrl("MathJax/MathJax.js")).append("&amp;config=TeX-MML-AM_HTMLorMML\"></script>\n")
        .toString();
    }
  };

  @NotNull
  private final JPanel myPanelWrapper;
  @Nullable
  private String myInlineCss;
  @Nullable
  private String myInlineCssDarcula;
  @Nullable
  private String myFontAwesomeCssLink;
  @Nullable
  private String myDejavuCssLink;

  private final byte[] macKey;

  public BrowserPanel() {
    macKey = new byte[8];
    new Random().nextBytes(new byte[8]);

    myPanelWrapper = new JPanel(new BorderLayout());
    myPanelWrapper.setBackground(JBColor.background());
    imagesPath = AsciiDoc.tempImagesPath();

    try {
      Properties p = new Properties();
      p.load(JavaFxHtmlPanel.class.getResourceAsStream("/META-INF/asciidoctorj-version.properties"));
      String asciidoctorVersion = p.getProperty("version.asciidoctor");
      myInlineCss = IOUtils.toString(JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/asciidoctor-default.css"));

      myInlineCssDarcula = myInlineCss + IOUtils.toString(JavaFxHtmlPanel.class.getResourceAsStream("darcula.css"));
      myInlineCssDarcula += IOUtils.toString(JavaFxHtmlPanel.class.getResourceAsStream("coderay-darcula.css"));
      myInlineCss += IOUtils.toString(JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/coderay-asciidoctor.css"));
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

  @NotNull
  public String getHtml(@NotNull VirtualFile file, @NotNull Project project) {
    Document document = FileDocumentManager.getInstance().getDocument(file);
    AtomicInteger offsetLineNo = new AtomicInteger();
    final String contentWithConfig = AsciiDoc.prependConfig(document, project, offsetLineNo::set);
    List<String> extensions = AsciiDoc.getExtensions(project);
    Objects.requireNonNull(file.getParent().getCanonicalPath(), "we will have files, these will always have a parent directory");
    AsciiDoc asciiDoc = new AsciiDoc(project.getBasePath(), new File(file.getParent().getCanonicalPath()),
      imagesPath, file.getName());
    String html = asciiDoc.render(contentWithConfig, extensions);
    if (file.getParent() != null) {
      // parent will be null if we use Language Injection and Fragment Editor
      base = file.getParent().getPath();
    } else {
      base = "";
    }
    if (isDarcula()) {
      // clear out coderay inline CSS colors as they are barely readable in darcula theme
      html = html.replaceAll("<span style=\"color:#[a-zA-Z0-9]*;?", "<span style=\"");
      html = html.replaceAll("<span style=\"background-color:#[a-zA-Z0-9]*;?", "<span style=\"");
    }
    html = "<html><head></head><body>" + html + "</body>";
    html = prepareHtml(html);
    return html;
  }


  private String findTempImageFile(String filename) {
    Path file = imagesPath.resolve(filename);
    if (Files.exists(file)) {
      return file.toFile().toString();
    }
    return null;
  }

  /**
   * Sign a file to be encoded in a URL inside a document.
   * The key will change every time the IDE is restarted.
   *
   * @param file filename
   * @return signed file including mac; ready to be added to a URL
   */
  private String signFile(String file) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(macKey, "HmacSHA256");
      mac.init(key);
      String hash = Base64.encode(mac.doFinal(file.getBytes(StandardCharsets.UTF_8)));
      return URLEncoder.encode(file, StandardCharsets.UTF_8.toString()) + "&amp;mac=" + hash;
    } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
      throw new IllegalStateException("unable to calculate mac", e);
    }
  }

  private boolean checkMac(String file, String mac) {
    try {
      Mac m = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(macKey, "HmacSHA256");
      m.init(key);
      String hash = Base64.encode(m.doFinal(file.getBytes(StandardCharsets.UTF_8)));
      mac = mac.replace(" ", "+");
      return hash.equals(mac);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("unable to calculate mac", e);
    }
  }

  private String prepareHtml(@NotNull String html) {
    /* for each image we'll calculate a MD5 sum of its content. Once the content changes, MD5 and therefore the URL
     * will change. The changed URL is necessary for the Browser to display the new content, as each URL
     * will be loaded only once due to caching. Also each URL to a local image will be signed so that it can be retrieved securely afterwards */
    Pattern pattern = Pattern.compile("<img src=\"([^:\"]*)\"");
    final Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      if (file.startsWith("image?")) {
        continue;
      }
      String tmpFile = findTempImageFile(file);
      String md5;
      String replacement;
      if (tmpFile != null) {
        md5 = calculateMd5(tmpFile, null);
        replacement = "<img src=\"image?file=" + signFile(tmpFile) + "&amp;hash=" + md5 + "\"";
      } else {
        md5 = calculateMd5(file, base);
        replacement = "<img src=\"image?file=" + signFile(base + "/" + file) + "&amp;hash=" + md5 + "\"";
      }
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* Add CSS line and JavaScript for auto-scolling and clickable links */
    return html
      .replace("<head>", "<head>" + getCssLines(isDarcula() ? myInlineCssDarcula : myInlineCss) + myFontAwesomeCssLink + myDejavuCssLink)
      .replace("</body>", getScriptingLines() + "</body>");
  }

  @NotNull
  private static String getCssLines(@Nullable String inlineCss) {
    StringBuilder result = new StringBuilder();

    if (inlineCss != null) {
      result.append("<style>\n").append(inlineCss).append("\n</style>\n");
    }
    return result.toString();
  }

  private String calculateMd5(String file, String base) {
    String md5;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      try (FileInputStream fis = new FileInputStream((base != null ? base + "/" : "") + file)) {
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
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  @Override
  public void close() {
    if (imagesPath != null) {
      try {
        FileUtils.deleteDirectory(imagesPath.toFile());
      } catch (IOException _ex) {
        Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", _ex);
      }
    }
  }

  /**
   * Retrieve an image that was previously referenced in a file.
   *
   * @param file absolute file name
   * @param mac  signature created when rendering the surrounding document
   * @return byte array for the image, or null if file not exists or signature is wrong
   */
  @Nullable
  public byte[] getImage(String file, String mac) {
    if (!checkMac(file, mac)) {
      Logger.getInstance(AsciiDocPreviewEditor.class).warn("wrong signature when retrieving file '" + file + "'");
      return null;
    }
    try {
      return FileUtils.readFileToByteArray(new File(file));
    } catch (IOException e) {
      return null;
    }
  }
}
