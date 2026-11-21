package org.asciidoc.intellij.antora;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

/**
 * Downloads a site manifest and caches it on disk.
 * The ETag of the response is stored next to the cached file, so that the next request can be conditional
 * and the file will only be downloaded again if it has changed.
 */
public final class AntoraSiteManifestLoader {
  private static final Logger LOG = Logger.getInstance(AntoraSiteManifestLoader.class);

  // the manifest of a large site can be 20 MB, protect against endless streams
  static final long MAX_SIZE = 200L * 1024 * 1024;
  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int READ_TIMEOUT_MS = 30_000;

  private AntoraSiteManifestLoader() {
  }

  public static @NotNull Path etagFile(@NotNull Path file) {
    return file.resolveSibling(file.getFileName() + ".etag");
  }

  /**
   * Download the manifest if it has changed on the server.
   *
   * @param url  the URL of the manifest
   * @param file the file where the manifest is cached; an ETag file will be stored next to it
   * @return {@code true} if the cached file has been updated, {@code false} if the server reported it as not modified
   * @throws IOException if the file couldn't be retrieved
   */
  public static boolean download(@NotNull String url, @NotNull Path file) throws IOException {
    Path etagFile = etagFile(file);
    String etag = null;
    if (Files.exists(file) && Files.exists(etagFile)) {
      etag = Files.readString(etagFile, StandardCharsets.UTF_8).trim();
      if (etag.isEmpty()) {
        etag = null;
      }
    }
    final String ifNoneMatch = etag;
    Files.createDirectories(file.getParent());
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
    try {
      return HttpRequests.request(url)
        .connectTimeout(CONNECT_TIMEOUT_MS)
        .readTimeout(READ_TIMEOUT_MS)
        .tuner(connection -> {
          if (ifNoneMatch != null) {
            connection.setRequestProperty("If-None-Match", ifNoneMatch);
          }
        })
        .connect(request -> {
          if (request.getConnection() instanceof HttpURLConnection http && http.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return false;
          }
          String newEtag = request.getConnection().getHeaderField("ETag");
          copyLimited(request.getInputStream(), tmp);
          Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
          if (newEtag != null && !newEtag.isEmpty()) {
            Files.writeString(etagFile, newEtag, StandardCharsets.UTF_8);
          } else {
            Files.deleteIfExists(etagFile);
          }
          return true;
        });
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private static void copyLimited(InputStream in, Path target) throws IOException {
    try (in; var out = Files.newOutputStream(target)) {
      byte[] buffer = new byte[64 * 1024];
      long total = 0;
      int read;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > MAX_SIZE) {
          throw new IOException("Site manifest is larger than " + MAX_SIZE + " bytes");
        }
        out.write(buffer, 0, read);
      }
    }
  }

  /**
   * Open a manifest, and unzip it if the name ends with {@code .gz}.
   */
  public static @NotNull InputStream open(@NotNull String name, @NotNull InputStream raw) throws IOException {
    InputStream in = new BufferedInputStream(raw);
    if (name.endsWith(".gz")) {
      return new GZIPInputStream(in);
    }
    return in;
  }

  public static @Nullable AntoraSiteManifest readCached(@NotNull Path file, @NotNull String name, @Nullable String defaultSiteUrl) {
    if (!Files.isRegularFile(file)) {
      return null;
    }
    try (InputStream in = open(name, Files.newInputStream(file))) {
      return AntoraSiteManifest.parse(in, defaultSiteUrl);
    } catch (IOException e) {
      LOG.info("unable to read cached site manifest " + file, e);
      return null;
    }
  }
}
