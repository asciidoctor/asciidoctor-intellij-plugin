package org.asciidoc.intellij.antora;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * The site manifest of the Antora Atlas extension.
 * It lists all pages of a published Antora site, grouped by component and version.
 * This allows to validate references to pages in other components without having their sources available.
 *
 * @see <a href="https://gitlab.com/antora/antora-atlas-extension">Antora Atlas extension</a>
 */
public final class AntoraSiteManifest {
  public static final String ROOT_MODULE = "ROOT";

  private final String siteUrl;
  private final Map<String, Component> components;

  private AntoraSiteManifest(String siteUrl, Map<String, Component> components) {
    this.siteUrl = siteUrl;
    this.components = components;
  }

  /**
   * The URL of the site without trailing slash, or an empty string if unknown.
   */
  public @NotNull String getSiteUrl() {
    return siteUrl;
  }

  public @NotNull Map<String, Component> getComponents() {
    return components;
  }

  public @Nullable Component getComponent(@NotNull String name) {
    return components.get(name);
  }

  /**
   * A page of a component version.
   * The path is relative to the pages folder of the module.
   */
  public static final class Page {
    private final String module;
    private final String path;
    private final String title;
    private final String navtitle;
    private final String url;
    private final boolean alias;

    Page(String module, String path, String title, String navtitle, String url, boolean alias) {
      this.module = module;
      this.path = path;
      this.title = title;
      this.navtitle = navtitle;
      this.url = url;
      this.alias = alias;
    }

    public @NotNull String getModule() {
      return module;
    }

    public @NotNull String getPath() {
      return path;
    }

    /**
     * The title as plain text, or {@code null} if the page has none (for example for an alias).
     */
    public @Nullable String getTitle() {
      return title == null ? null : StringEscapeUtils.unescapeHtml4(title);
    }

    /**
     * The title as HTML, as it is stored in the manifest.
     */
    public @Nullable String getTitleHtml() {
      return title;
    }

    public @Nullable String getNavtitle() {
      return navtitle == null ? null : StringEscapeUtils.unescapeHtml4(navtitle);
    }

    /**
     * The URL relative to the site root, starting with a slash.
     */
    public @Nullable String getUrl() {
      return url;
    }

    public boolean isAlias() {
      return alias;
    }
  }

  public static final class Version {
    private final String version;
    private final String displayVersion;
    private final boolean prerelease;
    private final List<Page> pages = new ArrayList<>();
    private final Map<String, Page> index = new HashMap<>();

    Version(String version, String displayVersion, boolean prerelease) {
      this.version = version;
      this.displayVersion = displayVersion;
      this.prerelease = prerelease;
    }

    void add(Page page) {
      pages.add(page);
      index.put(page.getModule() + ":" + page.getPath(), page);
    }

    public @NotNull String getVersion() {
      return version;
    }

    public @NotNull String getDisplayVersion() {
      return displayVersion == null ? version : displayVersion;
    }

    public boolean isPrerelease() {
      return prerelease;
    }

    public @NotNull List<Page> getPages() {
      return Collections.unmodifiableList(pages);
    }

    public @Nullable Page getPage(@Nullable String module, @NotNull String path) {
      return index.get((module == null || module.isEmpty() ? ROOT_MODULE : module) + ":" + path);
    }
  }

  public static final class Component {
    private final String name;
    private String title;
    private String latest;
    private final Map<String, Version> versions = new LinkedHashMap<>();

    Component(String name) {
      this.name = name;
    }

    public @NotNull String getName() {
      return name;
    }

    public @Nullable String getTitle() {
      return title == null ? null : StringEscapeUtils.unescapeHtml4(title);
    }

    public @NotNull Map<String, Version> getVersions() {
      return Collections.unmodifiableMap(versions);
    }

    /**
     * Find a version of this component.
     *
     * @param version the version, or {@code null} for the latest version
     */
    public @Nullable Version getVersion(@Nullable String version) {
      if (version != null) {
        return versions.get(version);
      }
      if (latest != null && versions.containsKey(latest)) {
        return versions.get(latest);
      }
      return versions.values().stream().findFirst().orElse(null);
    }
  }

  /**
   * An Antora resource ID, split into its parts.
   * The component is mandatory, as all other references are resolved locally.
   */
  public static final class Reference {
    private final String version;
    private final String component;
    private final String module;
    private final String path;

    Reference(String version, String component, String module, String path) {
      this.version = version;
      this.component = component;
      this.module = module;
      this.path = path;
    }

    /**
     * @return the version, or {@code null} if not specified
     */
    public @Nullable String getVersion() {
      return version;
    }

    public @NotNull String getComponent() {
      return component;
    }

    public @NotNull String getModule() {
      return module;
    }

    public @NotNull String getPath() {
      return path;
    }
  }

  /**
   * Split a reference to a page like {@code 1.0@component:module:page$folder/page.adoc#anchor}.
   *
   * @return the parts, or {@code null} if the reference doesn't specify a component or points to another family
   */
  public static @Nullable Reference parseReference(@NotNull String reference) {
    String key = reference;
    String version = null;
    Matcher versionMatcher = AsciiDocUtil.VERSION.matcher(key);
    if (versionMatcher.find()) {
      version = versionMatcher.group("version");
      if (version.equals("_")) {
        version = "";
      }
      key = versionMatcher.replaceFirst("");
    }
    Matcher componentModule = AsciiDocUtil.COMPONENT_MODULE.matcher(key);
    if (!componentModule.find()) {
      return null;
    }
    String component = componentModule.group("component");
    String module = componentModule.group("module");
    if (component.isEmpty()) {
      return null;
    }
    if (module.isEmpty()) {
      module = ROOT_MODULE;
    }
    key = componentModule.replaceFirst("");
    Matcher family = AsciiDocUtil.FAMILY.matcher(key);
    if (family.find()) {
      if (!family.group("family").equals("page")) {
        return null;
      }
      key = family.replaceFirst("");
    }
    int anchor = key.indexOf('#');
    if (anchor != -1) {
      key = key.substring(0, anchor);
    }
    return new Reference(version, component, module, key);
  }

  /**
   * Find the page a reference points to.
   */
  public @Nullable Page findPage(@NotNull Reference reference) {
    Component component = components.get(reference.getComponent());
    if (component == null) {
      return null;
    }
    Version version = component.getVersion(reference.getVersion());
    if (version == null) {
      return null;
    }
    return version.getPage(reference.getModule(), reference.getPath());
  }

  /**
   * Check if the manifest knows both component and version of a reference.
   */
  public boolean knowsComponentVersion(@NotNull Reference reference) {
    Component component = components.get(reference.getComponent());
    return component != null && component.getVersion(reference.getVersion()) != null;
  }

  /**
   * A prefix of a resource ID that points to a module of a component, for example {@code component::}.
   *
   * @param lookup the text to complete, without the final colon
   */
  public record Prefix(String lookup, String component, String module, String title) {
    public String getPresentableText() {
      return lookup + ":";
    }
  }

  /**
   * All modules of the latest versions of all components.
   */
  public @NotNull List<Prefix> getPrefixes() {
    List<Prefix> result = new ArrayList<>();
    for (Component component : components.values()) {
      Version version = component.getVersion(null);
      if (version == null) {
        continue;
      }
      Set<String> modules = new LinkedHashSet<>();
      for (Page page : version.pages) {
        modules.add(page.getModule());
      }
      for (String module : modules) {
        String lookup = component.getName() + ":" + (module.equals(ROOT_MODULE) ? "" : module);
        result.add(new Prefix(lookup, component.getName(), module, component.getTitle()));
      }
    }
    return result;
  }

  /**
   * A file or a folder inside the pages of a module.
   */
  public record Entry(String name, boolean directory, String title) {
  }

  /**
   * Find the next path segment of the pages in a module.
   *
   * @param base the text before the segment, for example {@code component::folder/}
   */
  public @NotNull List<Entry> getEntries(@NotNull String base) {
    if (base.indexOf('#') != -1) {
      return new ArrayList<>();
    }
    Reference ref = parseReference(base + "x");
    if (ref == null) {
      return new ArrayList<>();
    }
    String folder = ref.getPath().substring(0, ref.getPath().length() - 1);
    Component component = components.get(ref.getComponent());
    Version version = component == null ? null : component.getVersion(ref.getVersion());
    if (version == null) {
      return new ArrayList<>();
    }
    Map<String, Entry> result = new LinkedHashMap<>();
    for (Page page : version.pages) {
      if (!page.getModule().equals(ref.getModule()) || !page.getPath().startsWith(folder) || page.isAlias()) {
        continue;
      }
      String rest = page.getPath().substring(folder.length());
      int slash = rest.indexOf('/');
      if (slash == -1) {
        result.put(rest, new Entry(rest, false, page.getTitle()));
      } else {
        result.putIfAbsent(rest.substring(0, slash), new Entry(rest.substring(0, slash), true, null));
      }
    }
    return new ArrayList<>(result.values());
  }

  public @NotNull String getAbsoluteUrl(@NotNull Page page) {
    String url = page.getUrl() == null ? "" : page.getUrl();
    return siteUrl + url;
  }

  /**
   * Parse a manifest.
   *
   * @param stream            the JSON file
   * @param defaultSiteUrl    the URL to use if the manifest doesn't contain one, can be {@code null}
   * @throws IOException      if the file is not a valid manifest
   */
  public static @NotNull AntoraSiteManifest parse(@NotNull InputStream stream, @Nullable String defaultSiteUrl) throws IOException {
    String url = null;
    Map<String, Component> components = new LinkedHashMap<>();
    try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        if (name.equals("url") && reader.peek() == JsonToken.STRING) {
          url = reader.nextString();
        } else if (name.equals("components") && reader.peek() == JsonToken.BEGIN_OBJECT) {
          readComponents(reader, components);
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
    } catch (IllegalStateException | NumberFormatException e) {
      throw new IOException("Unable to parse site manifest", e);
    }
    if (url == null) {
      url = defaultSiteUrl == null ? "" : defaultSiteUrl;
    }
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return new AntoraSiteManifest(url, components);
  }

  private static void readComponents(JsonReader reader, Map<String, Component> components) throws IOException {
    reader.beginObject();
    while (reader.hasNext()) {
      Component component = new Component(reader.nextName());
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        reader.skipValue();
        continue;
      }
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        if (name.equals("title") && reader.peek() == JsonToken.STRING) {
          component.title = reader.nextString();
        } else if (name.equals("latest") && reader.peek() == JsonToken.STRING) {
          component.latest = reader.nextString();
        } else if (name.equals("versions") && reader.peek() == JsonToken.BEGIN_OBJECT) {
          readVersions(reader, component);
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      components.put(component.name, component);
    }
    reader.endObject();
  }

  private static void readVersions(JsonReader reader, Component component) throws IOException {
    reader.beginObject();
    while (reader.hasNext()) {
      String versionName = reader.nextName();
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        reader.skipValue();
        continue;
      }
      String displayVersion = null;
      boolean prerelease = false;
      List<Page> pages = new ArrayList<>();
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        if (name.equals("displayVersion") && reader.peek() == JsonToken.STRING) {
          displayVersion = reader.nextString();
        } else if (name.equals("prerelease") && reader.peek() == JsonToken.BOOLEAN) {
          prerelease = reader.nextBoolean();
        } else if (name.equals("pages") && reader.peek() == JsonToken.BEGIN_ARRAY) {
          readPages(reader, pages);
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      Version version = new Version(versionName, displayVersion, prerelease);
      pages.forEach(version::add);
      component.versions.put(versionName, version);
    }
    reader.endObject();
  }

  private static void readPages(JsonReader reader, List<Page> pages) throws IOException {
    reader.beginArray();
    while (reader.hasNext()) {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        reader.skipValue();
        continue;
      }
      String module = ROOT_MODULE;
      String path = null;
      String title = null;
      String navtitle = null;
      String url = null;
      boolean alias = false;
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        JsonToken token = reader.peek();
        if (name.equals("module") && token == JsonToken.STRING) {
          module = reader.nextString();
        } else if (name.equals("path") && token == JsonToken.STRING) {
          path = reader.nextString();
        } else if (name.equals("title") && token == JsonToken.STRING) {
          title = reader.nextString();
        } else if (name.equals("navtitle") && token == JsonToken.STRING) {
          navtitle = reader.nextString();
        } else if (name.equals("url") && token == JsonToken.STRING) {
          url = reader.nextString();
        } else if (name.equals("alias")) {
          alias = true;
          reader.skipValue();
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      if (path != null) {
        pages.add(new Page(module, path, title, navtitle, url, alias));
      }
    }
    reader.endArray();
  }
}
