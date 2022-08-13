package org.asciidoc.intellij.editor.browser;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanel;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserPanel implements Disposable {

  private final Path globalImagesPath;
  private final Logger log = Logger.getInstance(JavaFxHtmlPanel.class);
  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);

  private String base;

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = NotNullLazyValue.lazy(() -> {
    //noinspection StringBufferReplaceableByString
    return new StringBuilder()
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
  });

  @Nullable
  private String myInlineCss;
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

  private final SignWithMac signWithMac = new SignWithMac();

  public BrowserPanel() {
    globalImagesPath = AsciiDocWrapper.tempImagesPath(null, null);
    Disposer.register(ApplicationManager.getApplication(), this);

    try {
      Properties p = new Properties();
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("/META-INF/asciidoctorj-version.properties")) {
        p.load(is);
      }
      String asciidoctorVersion = p.getProperty("version.asciidoctor");
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/asciidoctor-default.css")) {
        myInlineCss = IOUtils.toString(is, StandardCharsets.UTF_8);
        // otherwise embedded SVG images will be skewed to full height of one browser window
        myInlineCss = myInlineCss.replaceAll(Pattern.quote("object,embed{height:100%}"), "");
      }
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("/gems/asciidoctor-"
        + asciidoctorVersion
        + "/data/stylesheets/coderay-asciidoctor.css")) {
        myInlineCss += IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("rouge-github.css")) {
        myInlineCss += IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      try (InputStream is = JavaFxHtmlPanel.class.getResourceAsStream("darcula.css")) {
        myInlineCssDarcula = myInlineCss + IOUtils.toString(is, StandardCharsets.UTF_8);
      }
      myAntoraCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("antora/preview.css") + "\">";
      myAntoraDarculaCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("antora/preview-darcula.css") + "\">";
      myFontAwesomeCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("font-awesome/css/font-awesome.min.css") + "\">";
      myDejavuCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("dejavu/dejavu.css") + "\">";
      myGoogleFontsCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/googlefonts.css") + "\">";
      myDroidSansMonoCssLink = "<link rel=\"stylesheet\" href=\"" + PreviewStaticServer.getStyleUrl("googlefonts/droidsansmono.css") + "\">";
    } catch (IOException e) {
      String message = "Unable to combine CSS resources: " + e.getMessage();
      log.error(message, e);
      Notification notification = AsciiDocWrapper.getNotificationGroup()
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR);
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
    Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> FileDocumentManager.getInstance().getDocument(file));
    Objects.requireNonNull(document);
    final String config = AsciiDocWrapper.config(document, project);
    List<String> extensions = extensionService.getExtensions(project);
    Objects.requireNonNull(file.getParent().getCanonicalPath(), "we will have files, these will always have a parent directory");
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    Path imagesPath = globalImagesPath;
    VirtualFile parent = file.getParent();
    if (settings.getAsciiDocPreviewSettings().getSafeMode() != SafeMode.UNSAFE) {
      if (parent != null && parent.getCanonicalPath() != null) {
        imagesPath = AsciiDocWrapper.tempImagesPath(Path.of(parent.getCanonicalPath()), project);
      }
    }
    AsciiDocWrapper asciiDocWrapper = new AsciiDocWrapper(project, new File(file.getParent().getCanonicalPath()),
      imagesPath, file.getName());
    String html = asciiDocWrapper.render(document.getText(), config, extensions, asciiDocWrapper::notifyAlways, AsciiDocWrapper.FileType.BROWSER);
    if (file.getParent() != null) {
      // parent will be null if we use Language Injection and Fragment Editor
      base = file.getParent().getPath();
    } else {
      base = "";
    }
    html = "<html><head></head><body><div id=\"header\"></div>" + html + "<div id=\"footer\"></div></body></html>";
    html = prepareHtml(html, project, asciiDocWrapper.getAttributes(), imagesPath, AsciiDocUtil.findAntoraModuleDir(project, file.getParent()) != null, file);
    return html;
  }


  private String findTempImageFile(String filename, String imagesdir, Path imagesPath) {
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

  public String signFile(String file) {
    return signWithMac.signFile(file);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean checkMac(@NotNull String file, @NotNull String mac) {
    return signWithMac.checkMac(file, mac);
  }

  @SuppressWarnings("checkstyle:MethodLength")
  private String prepareHtml(@NotNull String html, Project project, Map<String, String> attributes, Path imagesPath, boolean isAntora, @NotNull VirtualFile originalFile) {
    // Antora plugin might resolve some absolute URLs, convert them to localfile so they get their MD5 that prevents caching
    Pattern pattern = Pattern.compile("<img src=\"file:///([^\"]*)\"");
    Matcher matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      String tmpFile = findTempImageFile(file, null, imagesPath);
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

    /* for each image we'll calculate a MD5 sum of its content. Once the content changes, MD5 and therefore the URL
     * will change. The changed URL is necessary for the Browser to display the new content, as each URL
     * will be loaded only once due to caching. Also each URL to a local image will be signed so that it can be retrieved securely afterwards */
    pattern = Pattern.compile("<img src=\"([^:\"]*)\"");
    matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String file = matchResult.group(1);
      if (file.startsWith("image?")) {
        continue;
      }
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      String replacement = null;
      String tmpFile = findTempImageFile(file, attributes.get("imagesdir"), imagesPath);
      if (tmpFile != null) {
        replacement = calculateFileAndMd5(tmpFile, null);
      }
      if (replacement == null) {
        replacement = calculateFileAndMd5(file, base);
      }
      if (replacement == null && file.startsWith("/")) {
        VirtualFile hugoStaticFile = AsciiDocUtil.findHugoStaticFolder(project, originalFile);
        if (hugoStaticFile != null) {
          replacement = calculateFileAndMd5(file.substring(1), hugoStaticFile.getCanonicalPath());
        }
      }
      if (replacement == null && attributes.get("imagesdir") != null && attributes.get("imagesdir").length() > 0 && file.startsWith("/")) {
        // For image file names starting with a slash, the imagesdir is not being added automatically.
        // Try to use it to find the file - imagesdir might be relative to the base directory, or an absolute path.
        replacement = calculateFileAndMd5(attributes.get("imagesdir") + file, base);
        if (replacement == null) {
          replacement = calculateFileAndMd5(file, attributes.get("imagesdir"));
        }
      }
      if (replacement == null) {
        // some fallback
        replacement = base + "/" + file + "&amp;hash=none";
      }
      replacement = "<img src=\"image?file=" + replacement + "\"";
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* the same as above for links to local resources */
    pattern = Pattern.compile("<a ([^>])*href=\"([^#:\"]*)\"");
    matcher = pattern.matcher(html);
    while (matcher.find()) {
      final MatchResult matchResult = matcher.toMatchResult();
      String other = matchResult.group(1);
      if (other == null) {
        other = "";
      }
      String file = matchResult.group(2);
      if (file.startsWith("image?") || file.startsWith("source?")) {
        continue;
      }
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      // type 'image' will deliver a binary file
      String type = "image";
      String suffix = "";
      if (AsciiDocFileType.hasAsciiDocExtension(file)) {
        // type 'source' will be an AsciiDoc converted to HTML on the fly
        type = "source";
      } else if (file.endsWith(".html")) {
        if (!new File(base + "/" + file).exists()) {
          String adocFile = file.replaceAll("\\.html$", attributes.get("docfilesuffix"));
          if (new File(base + "/" + adocFile).exists()) {
            // if the file points to an HTML that doesn't exist, but an AsciiDoc file with the same name exists, use the AsciiDoc file and convert it on the fly
            file = adocFile;
            type = "source";
          }
        }
      } else if (new File(base + "/" + file + "." + attributes.get("docfilesuffix")).exists()) {
        // if the file points to a file without extension, but an AsciiDoc file with the same name exists, use the AsciiDoc file and convert it on the fly
        file = file + "." + attributes.get("docfilesuffix");
        type = "source";
      }
      if (type.equals("source")) {
        if (project.getPresentableUrl() != null) {
          suffix = "&amp;projectUrl=" + URLEncoder.encode(project.getPresentableUrl(), StandardCharsets.UTF_8);
        } else {
          suffix = "&amp;projectName=" + URLEncoder.encode(project.getName(), StandardCharsets.UTF_8);
        }
      }
      String replacement;
      replacement = "<a " + other + "href=\"" + type + "?file=" + signFile(base + "/" + file) + suffix + "\"";
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
      if (file.startsWith("image?")) {
        continue;
      }
      try {
        file = URLDecoder.decode(file, StandardCharsets.UTF_8); // restore "%20" as " "
      } catch (IllegalArgumentException e) {
        // ignored, this must be a manually entered URL with a percentage sign
        continue;
      }
      // type 'image' will deliver a binary file
      String type = "image";
      String replacement;
      replacement = "<object " + other + "data=\"" + type + "?file=" + signFile(base + "/" + file) + "\"";
      html = html.substring(0, matchResult.start()) +
        replacement + html.substring(matchResult.end());
      matcher.reset(html);
    }

    /* Add CSS line and JavaScript */
    if (isAntora) {
      html = AsciiDocWrapper.enrichPage(html, (isDarcula() ? myAntoraDarculaCssLink : myAntoraCssLink) + myFontAwesomeCssLink, attributes, project);
    } else {
      html = AsciiDocWrapper.enrichPage(html, getCssLines(isDarcula() ? myInlineCssDarcula : myInlineCss) + myFontAwesomeCssLink + myGoogleFontsCssLink + myDroidSansMonoCssLink + myDejavuCssLink, attributes, project);
    }
    html = html.replace("</body>", getScriptingLines() + "</body>");
    return html;
  }

  @NotNull
  private static String getCssLines(@Nullable String inlineCss) {
    StringBuilder result = new StringBuilder();

    if (inlineCss != null) {
      result.append("<style>\n").append(inlineCss).append("\n</style>\n");
    }
    return result.toString();
  }

  private String calculateFileAndMd5(String file, String base) {
    file = file.replaceAll("\\\\", "/");
    String md5 = calculateMd5(file, base);
    if (!md5.equals("none")) {
      return signFile((base != null ? base + "/" : "") + file) + "&amp;hash=" + md5;
    } else {
      return null;
    }
  }

  public static String calculateMd5(String file, String base) {
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
  public void dispose() {
    AsciiDocWrapper.cleanupImagesPath(globalImagesPath);
  }

  /**
   * Retrieve an image that was previously referenced in a file.
   *
   * @param file absolute file name
   * @param mac  signature created when rendering the surrounding document
   * @return byte array for the image, or null if file not exists or signature is wrong
   */
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
