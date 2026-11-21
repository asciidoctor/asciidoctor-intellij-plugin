package org.asciidoc.intellij.antora;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.search.AsciiDocAntoraPlaybookIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides the site manifests of the Antora Atlas extension that are configured for the project.
 * <p>
 * Manifests are configured with the attributes {@code primary-site-url} or {@code primary-site-manifest-url},
 * either in an Antora playbook or in the {@code asciidoc.attributes} of an {@code antora.yml}.
 * These are the attributes the Atlas extension uses.
 * Manifests are cached on disk, and are checked for updates at most once every {@link #REVALIDATE_AFTER_MS} using their ETag.
 * Retrieving manifests from a URL is skipped if the project isn't trusted.
 */
@Service(Service.Level.PROJECT)
public final class AntoraSiteManifestService {
  private static final Logger LOG = Logger.getInstance(AntoraSiteManifestService.class);

  /**
   * Notified after the available manifests have changed, so that previews can be rendered again.
   */
  public static final com.intellij.util.messages.Topic<Runnable> MANIFESTS_CHANGED =
    com.intellij.util.messages.Topic.create("Antora site manifests changed", Runnable.class);

  public static final String ATTRIBUTE_PRIMARY_SITE_URL = "primary-site-url";
  public static final String ATTRIBUTE_PRIMARY_SITE_MANIFEST_URL = "primary-site-manifest-url";
  private static final String DEFAULT_MANIFEST_NAME = "site-manifest.json";

  private static final long REVALIDATE_AFTER_MS = TimeUnit.MINUTES.toMillis(30);
  private static final long REDISCOVER_AFTER_MS = TimeUnit.SECONDS.toMillis(30);

  private final Project project;
  private volatile List<AntoraSiteManifest> manifests = Collections.emptyList();
  private volatile CompletableFuture<Void> loading;
  private volatile long lastDiscovery;
  private volatile long lastModificationCount = -1;
  private final Map<String, Long> lastValidated = new HashMap<>();
  private final Map<String, AntoraSiteManifest> parsed = new HashMap<>();

  public AntoraSiteManifestService(Project project) {
    this.project = project;
  }

  public static AntoraSiteManifestService getInstance(@NotNull Project project) {
    return project.getService(AntoraSiteManifestService.class);
  }

  /**
   * Where a manifest is configured.
   */
  record Source(String location, @Nullable VirtualFile base) {
    boolean isRemote() {
      return location.startsWith("http://") || location.startsWith("https://");
    }
  }

  /**
   * Get the manifests that are currently available.
   * This will never block; if manifests need to be loaded or refreshed, this happens in the background.
   */
  public @NotNull List<AntoraSiteManifest> getManifests() {
    refreshIfNeeded();
    return manifests;
  }

  /**
   * Get the manifests, and wait if they are currently loading. Don't call this from a read action or the UI thread.
   */
  public @NotNull List<AntoraSiteManifest> getManifestsAndWait(long timeoutMs) {
    refreshIfNeeded();
    CompletableFuture<Void> future = loading;
    if (future != null) {
      try {
        future.get(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException | TimeoutException e) {
        LOG.debug("unable to wait for site manifests", e);
      }
    }
    return manifests;
  }

  /**
   * Result of looking up a reference in the manifests.
   */
  public static final class Lookup {
    private final AntoraSiteManifest manifest;
    private final AntoraSiteManifest.Page page;

    Lookup(AntoraSiteManifest manifest, AntoraSiteManifest.Page page) {
      this.manifest = manifest;
      this.page = page;
    }

    public @NotNull AntoraSiteManifest getManifest() {
      return manifest;
    }

    /**
     * @return the page, or {@code null} if the manifest knows component and version, but not the page
     */
    public @Nullable AntoraSiteManifest.Page getPage() {
      return page;
    }

    public @Nullable String getAbsoluteUrl() {
      return page == null ? null : manifest.getAbsoluteUrl(page);
    }
  }

  /**
   * Look up a reference like {@code component::page.adoc}.
   *
   * @return a result if a manifest knows the component and version, otherwise {@code null}
   */
  public @Nullable Lookup lookup(@NotNull String reference, @NotNull List<AntoraSiteManifest> available) {
    AntoraSiteManifest.Reference ref = AntoraSiteManifest.parseReference(reference);
    if (ref == null) {
      return null;
    }
    AntoraSiteManifest known = null;
    for (AntoraSiteManifest manifest : available) {
      AntoraSiteManifest.Page page = manifest.findPage(ref);
      if (page != null) {
        return new Lookup(manifest, page);
      }
      if (known == null && manifest.knowsComponentVersion(ref)) {
        known = manifest;
      }
    }
    return known == null ? null : new Lookup(known, null);
  }

  private void refreshIfNeeded() {
    CompletableFuture<Void> future = loading;
    if (future != null && !future.isDone()) {
      return;
    }
    long now = System.currentTimeMillis();
    long modificationCount = com.intellij.psi.util.PsiModificationTracker.getInstance(project).getModificationCount();
    if (now - lastDiscovery < REDISCOVER_AFTER_MS || (modificationCount == lastModificationCount && lastDiscovery != 0
      && now - lastDiscovery < REVALIDATE_AFTER_MS)) {
      return;
    }
    synchronized (this) {
      future = loading;
      if (future != null && !future.isDone()) {
        return;
      }
      lastDiscovery = now;
      lastModificationCount = modificationCount;
      loading = CompletableFuture.runAsync(this::load, AppExecutorUtil.getAppExecutorService());
    }
  }

  private void load() {
    try {
      List<Source> sources = ReadAction.nonBlocking(this::discoverSources).inSmartMode(project).executeSynchronously();
      // first use what is cached, so manifests are available without waiting for the network
      publish(loadAll(sources, false));
      publish(loadAll(sources, true));
    } catch (ProcessCanceledException e) {
      // project closed or index changed, will retry on next access
      lastDiscovery = 0;
    } catch (RuntimeException e) {
      LOG.warn("unable to load site manifests", e);
    }
  }

  private List<AntoraSiteManifest> loadAll(List<Source> sources, boolean network) {
    List<AntoraSiteManifest> result = new ArrayList<>();
    for (Source source : sources) {
      if (project.isDisposed()) {
        throw new ProcessCanceledException();
      }
      AntoraSiteManifest manifest = source.isRemote() ? loadRemote(source, network) : loadLocal(source);
      if (manifest != null && !result.contains(manifest)) {
        result.add(manifest);
      }
    }
    return result;
  }

  private void publish(List<AntoraSiteManifest> result) {
    List<AntoraSiteManifest> before = manifests;
    manifests = result;
    boolean changed = before.size() != result.size();
    for (int i = 0; !changed && i < result.size(); i++) {
      changed = before.get(i) != result.get(i);
    }
    if (changed) {
      restartInspections();
      if (!project.isDisposed()) {
        project.getMessageBus().syncPublisher(MANIFESTS_CHANGED).run();
      }
    }
  }

  private void restartInspections() {
    ApplicationManager.getApplication().invokeLater(() -> DaemonCodeAnalyzer.getInstance(project).restart("Antora site manifest changed"),
      project.getDisposed());
  }

  private @NotNull List<Source> discoverSources() {
    List<Source> sources = new ArrayList<>();
    Collection<VirtualFile> files = new ArrayList<>(AsciiDocAntoraPlaybookIndex.getVirtualFiles(project));
    files.addAll(FilenameIndex.getVirtualFilesByName(AsciiDocUtil.ANTORA_YML, new AsciiDocSearchScope(project)));
    for (VirtualFile file : files) {
      Map<String, Object> yaml;
      try {
        yaml = AsciiDocWrapper.readAntoraYaml(project, file);
      } catch (YAMLException e) {
        continue;
      }
      if (!(yaml.get("asciidoc") instanceof Map<?, ?> asciidoc) || !(asciidoc.get("attributes") instanceof Map<?, ?> attributes)) {
        continue;
      }
      String location = null;
      if (attributes.get(ATTRIBUTE_PRIMARY_SITE_MANIFEST_URL) instanceof String manifestUrl) {
        location = manifestUrl;
      } else if (attributes.get(ATTRIBUTE_PRIMARY_SITE_URL) instanceof String siteUrl) {
        if (siteUrl.equals(".")) {
          // "same site" mode in Atlas, there is no other site to refer to
          continue;
        }
        location = siteUrl.replaceAll("/+$", "") + "/" + DEFAULT_MANIFEST_NAME;
      }
      if (location == null || location.isBlank()) {
        continue;
      }
      Source source = new Source(location.trim(), file.getParent());
      if (sources.stream().noneMatch(s -> s.isRemote() == source.isRemote() && s.location().equals(source.location())
        && (source.isRemote() || s.base() == source.base()))) {
        sources.add(source);
      }
    }
    return sources;
  }

  private String cacheKey(Source source) {
    return source.isRemote() || source.base() == null ? source.location() : source.base().getPath() + "/" + source.location();
  }

  private @Nullable AntoraSiteManifest loadLocal(Source source) {
    VirtualFile file = null;
    if (source.base() != null) {
      file = source.base().findFileByRelativePath(source.location());
    }
    if (file == null) {
      file = LocalFileSystem.getInstance().findFileByPath(source.location());
    }
    if (file == null || file.isDirectory()) {
      // Atlas silently ignores a missing manifest
      return null;
    }
    String key = cacheKey(source) + "@" + file.getModificationStamp();
    synchronized (parsed) {
      AntoraSiteManifest cached = parsed.get(key);
      if (cached != null) {
        return cached;
      }
    }
    try (InputStream in = AntoraSiteManifestLoader.open(file.getName(), file.getInputStream())) {
      AntoraSiteManifest manifest = AntoraSiteManifest.parse(in, null);
      synchronized (parsed) {
        parsed.put(key, manifest);
        parsed.put(cacheKey(source), manifest);
      }
      return manifest;
    } catch (IOException e) {
      LOG.info("unable to read site manifest " + file.getPath(), e);
      return null;
    }
  }

  private @Nullable AntoraSiteManifest loadRemote(Source source, boolean network) {
    String url = source.location();
    String defaultSiteUrl = url.substring(0, url.lastIndexOf('/'));
    String name = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
    Path file = cacheFile(url, name.endsWith(".gz"));
    AntoraSiteManifest manifest;
    synchronized (parsed) {
      manifest = parsed.get(url);
    }
    if (manifest == null) {
      manifest = AntoraSiteManifestLoader.readCached(file, name, defaultSiteUrl);
      if (manifest != null) {
        synchronized (parsed) {
          parsed.put(url, manifest);
        }
      }
    }
    // never retrieve anything from the network for an untrusted project (safe mode)
    if (!network || !TrustedProjects.isProjectTrusted(project)) {
      return manifest;
    }
    long now = System.currentTimeMillis();
    Long validated;
    synchronized (lastValidated) {
      validated = lastValidated.get(url);
    }
    if (manifest != null && validated != null && now - validated < REVALIDATE_AFTER_MS) {
      return manifest;
    }
    try {
      boolean updated = AntoraSiteManifestLoader.download(url, file);
      synchronized (lastValidated) {
        lastValidated.put(url, now);
      }
      if (updated || manifest == null) {
        AntoraSiteManifest fresh = AntoraSiteManifestLoader.readCached(file, name, defaultSiteUrl);
        if (fresh != null) {
          manifest = fresh;
          synchronized (parsed) {
            parsed.put(url, fresh);
          }
        }
      }
    } catch (IOException e) {
      // offline or not available: use the cached copy and try again later
      LOG.info("unable to retrieve site manifest " + url + ": " + e.getMessage());
      synchronized (lastValidated) {
        lastValidated.put(url, now);
      }
    }
    return manifest;
  }

  static Path cacheFile(String url, boolean gz) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
      return PathManager.getSystemDir().resolve("asciidoc").resolve("antora-site-manifests")
        .resolve(HexFormat.of().formatHex(hash, 0, 16) + (gz ? ".json.gz" : ".json"));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
