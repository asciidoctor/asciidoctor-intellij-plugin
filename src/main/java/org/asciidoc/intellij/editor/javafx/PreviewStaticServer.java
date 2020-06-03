package org.asciidoc.intellij.editor.javafx;

import com.intellij.ide.browsers.OpenInBrowserRequest;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.http.entity.ContentType;
import org.asciidoc.intellij.editor.browser.BrowserPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;
import org.jetbrains.io.Responses;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreviewStaticServer extends HttpRequestHandler {
  private Logger log = Logger.getInstance(PreviewStaticServer.class);

  private static final Logger LOG = Logger.getInstance(PreviewStaticServer.class);
  private static final String PREFIX = "/ead61b63-b0a6-4ff2-a49a-86be75ccfd1a/";
  private static final Pattern PAYLOAD_PATTERN = Pattern.compile("((?<contentType>[^/]*)/(?<fileName>[a-zA-Z0-9./_-]*))|(?<action>(source|image))");

  private static BrowserPanel browserPanel;

  // every time the plugin starts up, assume resources could have been modified
  private static final long LAST_MODIFIED = System.currentTimeMillis();

  public static PreviewStaticServer getInstance() {
    return HttpRequestHandler.Companion.getEP_NAME().findExtension(PreviewStaticServer.class);
  }

  @NotNull
  public static String createCSP() {
    return "default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' " + Urls.parseEncoded("http://localhost:" + BuiltInServerManager.getInstance().getPort() + PREFIX + "/scripts/").toExternalForm() + "; "
      + "style-src 'unsafe-inline' https: http: " + Urls.parseEncoded("http://localhost:" + BuiltInServerManager.getInstance().getPort() + PREFIX + "/styles/").toExternalForm() + "; "
      + "img-src file: localfile: *; connect-src 'none'; font-src *; " +
      "object-src file: localfile: *;" + // used for interactive SVGs
      "media-src 'none'; child-src 'none'; " +
      "frame-src https://player.vimeo.com/ https://www.youtube.com/"; // used for vimeo/youtube iframes
  }

  @NotNull
  private static String getStaticUrl(@NotNull String staticPath) {
    Url url = Urls.parseEncoded("http://localhost:" + BuiltInServerManager.getInstance().getPort() + PREFIX + staticPath);
    return BuiltInServerManager.getInstance().addAuthToken(Objects.requireNonNull(url)).toExternalForm();
  }

  @NotNull
  public static String getScriptUrl(@NotNull String scriptFileName) {
    return getStaticUrl("scripts/" + scriptFileName);
  }

  @NotNull
  public static String getStyleUrl(@NotNull String scriptFileName) {
    return getStaticUrl("styles/" + scriptFileName);
  }

  public static Url getFileUrl(OpenInBrowserRequest request, VirtualFile file) {
    Url url;
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("http://localhost:").append(BuiltInServerManager.getInstance().getPort()).append(PREFIX);
      if (file instanceof LightVirtualFile) {
        throw new IllegalStateException("unable to create a URL from a in-memory file");
      }
      String mac = getBrowserPanel().signFile(file.getPath()).replaceAll("&amp;", "&");
      sb.append("source?file=").append(mac);
      if (request.getProject().getPresentableUrl() != null) {
        sb.append("&projectUrl=").append(URLEncoder.encode(request.getProject().getPresentableUrl(), StandardCharsets.UTF_8.toString()));
      } else {
        sb.append("&projectName=").append(URLEncoder.encode(request.getProject().getName(), StandardCharsets.UTF_8.toString()));
      }
      url = Urls.parseEncoded(sb.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("can't encode");
    }
    if (request.isAppendAccessToken()) {
      url = BuiltInServerManager.getInstance().addAuthToken(Objects.requireNonNull(url));
    }
    return url;
  }

  @Override
  public boolean isSupported(@NotNull FullHttpRequest request) {
    return super.isSupported(request) && request.uri().startsWith(PREFIX);
  }

  @Override
  public boolean isAccessible(@NotNull HttpRequest request) {
    return request.uri().startsWith(PREFIX + "styles/") || request.uri().startsWith(PREFIX + "scripts/") || super.isAccessible(request);
  }

  @Override
  public boolean process(@NotNull QueryStringDecoder urlDecoder,
                         @NotNull FullHttpRequest request,
                         @NotNull ChannelHandlerContext context) {
    final String path = urlDecoder.path();
    if (!path.startsWith(PREFIX)) {
      throw new IllegalStateException("prefix should have been checked by #isSupported");
    }

    final String payLoad = path.substring(PREFIX.length());

    Matcher matcher = PAYLOAD_PATTERN.matcher(payLoad);

    if (!matcher.matches()) {
      log.warn("won't deliver resource to preview as it might be unsafe: " + payLoad);
      return false;
    }

    final String contentType = matcher.group("contentType");
    final String fileName = matcher.group("fileName");
    final String action = matcher.group("action");

    if ("scripts".equals(contentType)) {
      sendResource(request,
        context.channel(),
        fileName);
    } else if ("styles".equals(contentType)) {
      sendResource(request,
        context.channel(),
        fileName);
    } else if ("source".equals(action)) {
      String fileParameter = getParameter(urlDecoder, "file");
      String projectNameParameter = getParameter(urlDecoder, "projectName");
      String projectUrlParameter = getParameter(urlDecoder, "projectUrl");
      String mac = getParameter(urlDecoder, "mac");
      if (fileParameter == null || mac == null) {
        return false;
      }
      if (!getBrowserPanel().checkMac(fileParameter, mac)) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED,
          Unpooled.wrappedBuffer("<html><body>expired, please re-open from IDE (AsciiDoc plugin)</body></html>".getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, ContentType.TEXT_HTML);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
        Responses.send(response, context.channel(), request);
        return true;
      }
      if (projectNameParameter == null && projectUrlParameter == null) {
        return false;
      }
      VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(fileParameter);
      if (virtualFile == null) {
        log.warn("unable to to find file '" + fileParameter + "', therefore unable to render it");
        return false;
      }
      StringBuilder nonMatchingProjects = new StringBuilder();
      Project project = null;
      for (Project p : ProjectManager.getInstance().getOpenProjects()) {
        if ((projectNameParameter != null && projectNameParameter.equals(p.getName()))
          || (projectUrlParameter != null && projectUrlParameter.equals(p.getPresentableUrl()))) {
          project = p;
          break;
        } else {
          nonMatchingProjects.append("'").append(p.getName()).append("'/'").append(p.getPresentableUrl()).append("'");
        }
      }
      if (project == null) {
        log.warn("unable to determine project for '" + projectNameParameter + "'/'" + projectUrlParameter
          + "' out of projects " + nonMatchingProjects.toString()
          + ", therefore unable to render it");
        return false;
      }
      sendDocument(request, virtualFile, project, context.channel());
    } else if ("image".equals(action)) {
      String file = urlDecoder.parameters().get("file").get(0);
      String mac = urlDecoder.parameters().get("mac").get(0);
      return sendImage(request, file, mac, context.channel());
    } else {
      return false;
    }

    return true;
  }

  @Nullable
  private String getParameter(@NotNull QueryStringDecoder urlDecoder, @NotNull String parameter) {
    List<String> parameters = urlDecoder.parameters().get(parameter);
    if (parameters == null || parameters.size() != 1) {
      return null;
    }
    return parameters.get(0);
  }

  private boolean sendImage(FullHttpRequest request, String file, String mac, Channel channel) {
    byte[] image = getBrowserPanel().getImage(file, mac);
    if (image != null) {
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(image));
      if (file.endsWith(".png")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/png");
      } else if (file.endsWith(".jpg")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/jpeg");
      } else if (file.endsWith(".svg")) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/svg+xml");
      } else {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
      }
      response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=3600, private, must-revalidate");
      response.headers().set(HttpHeaderNames.ETAG, Long.toString(LAST_MODIFIED));
      Responses.send(response, channel, request);
      return true;
    } else {
      return false;
    }
  }

  @NotNull
  private static BrowserPanel getBrowserPanel() {
    synchronized (PreviewStaticServer.class) {
      if (browserPanel == null) {
        browserPanel = new BrowserPanel();
      }
    }
    return browserPanel;
  }

  private void sendDocument(FullHttpRequest request, @NotNull VirtualFile file, @NotNull Project project, @NotNull Channel channel) {
    String html = getBrowserPanel().getHtml(file, project);
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(html.getBytes(StandardCharsets.UTF_8)));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
    response.headers().set("Referrer-Policy", "no-referrer");
    Responses.send(response, channel, request);
  }

  private static void sendResource(@NotNull HttpRequest request,
                                   @NotNull Channel channel,
                                   @NotNull String resourceName) {
    /*
    // API incompatible with older versions of IntelliJ
    if (FileResponses.INSTANCE.checkCache(request, channel, lastModified)) {
      return;
    }
    */

    byte[] data;
    try (InputStream inputStream = JavaFxHtmlPanel.class.getResourceAsStream(resourceName)) {
      if (inputStream == null) {
        Responses.send(HttpResponseStatus.NOT_FOUND, channel, request);
        return;
      }

      data = FileUtilRt.loadBytes(inputStream);
    } catch (IOException e) {
      LOG.warn(e);
      Responses.send(HttpResponseStatus.INTERNAL_SERVER_ERROR, channel, request);
      return;
    }

    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(data));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.INSTANCE.getContentType(resourceName));
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=3600, private, must-revalidate");
    response.headers().set(HttpHeaderNames.ETAG, Long.toString(LAST_MODIFIED));
    Responses.send(response, channel, request);
  }

  @Override
  protected void finalize() {
    if (browserPanel != null) {
      browserPanel.close();
      browserPanel = null;
    }
  }
}
