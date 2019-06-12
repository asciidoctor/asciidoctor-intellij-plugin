package org.asciidoc.intellij.editor.javafx;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;
import org.jetbrains.io.Responses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreviewStaticServer extends HttpRequestHandler {
  private Logger log = Logger.getInstance(PreviewStaticServer.class);

  public static final String INLINE_CSS_FILENAME = "inline.css";
  private static final Logger LOG = Logger.getInstance(PreviewStaticServer.class);
  private static final String PREFIX = "/ead61b63-b0a6-4ff2-a49a-86be75ccfd1a/";
  private static final Pattern PAYLOAD_PATTERN = Pattern.compile("(?<contentType>[^/]*)/(?<fileName>[a-zA-Z0-9./_-]*)");

  // every time the plugin starts up, assume resources could have been modified
  private static final long LAST_MODIFIED = System.currentTimeMillis();

  @Nullable
  private byte[] myInlineStyleBytes = null;
  private long myInlineStyleTimestamp = 0;

  public static PreviewStaticServer getInstance() {
    return HttpRequestHandler.Companion.getEP_NAME().findExtension(PreviewStaticServer.class);
  }

  @NotNull
  public static String createCSP(@NotNull List<String> scripts, @NotNull List<String> styles) {
    return "default-src 'none'; script-src " + StringUtil.join(scripts, " ") + "; "
           + "style-src https: " + StringUtil.join(styles, " ") + "; "
           + "img-src file: *; connect-src 'none'; font-src *; " +
           "object-src 'none'; media-src 'none'; child-src 'none';";
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

  public void setInlineStyle(@Nullable String inlineStyle) {
    myInlineStyleBytes = inlineStyle == null ? null : inlineStyle.getBytes(StandardCharsets.UTF_8);
    myInlineStyleTimestamp = System.currentTimeMillis();
  }

  @Override
  public boolean isSupported(@NotNull FullHttpRequest request) {
    return super.isSupported(request) && request.uri().startsWith(PREFIX);
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

    if ("scripts".equals(contentType)) {
      sendResource(request,
                   context.channel(),
                   JavaFxHtmlPanel.class,
                   fileName);
    } else if ("styles".equals(contentType)) {
      sendResource(request,
                   context.channel(),
        JavaFxHtmlPanel.class,
                   fileName);
    } else {
      return false;
    }

    return true;
  }

  private static void sendResource(@NotNull HttpRequest request,
                                   @NotNull Channel channel,
                                   @NotNull Class<?> clazz,
                                   @NotNull String resourceName) {
    /*
    // API incompatible with older versions of IntelliJ
    if (FileResponses.INSTANCE.checkCache(request, channel, lastModified)) {
      return;
    }
    */

    byte[] data;
    try (InputStream inputStream = clazz.getResourceAsStream(resourceName)) {
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
}
