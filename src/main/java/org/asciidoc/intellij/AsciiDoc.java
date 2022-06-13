/*
 * Copyright 2013 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.asciidoc.intellij;

import com.intellij.ide.plugins.CannotUnloadPluginException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.serviceContainer.AlreadyDisposedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.asciidoc.intellij.asciidoc.AntoraIncludeAdapter;
import org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter;
import org.asciidoc.intellij.asciidoc.AttributesRetriever;
import org.asciidoc.intellij.asciidoc.PrependConfig;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.editor.jcef.AsciiDocJCEFHtmlPanelProvider;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationDummy;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.AttributeDeclaration;
import org.asciidoc.intellij.psi.search.AsciiDocAntoraPlaybookIndex;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.jruby.AsciidoctorJRuby;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;
import org.intellij.lang.annotations.Language;
import org.jcodings.EncodingDB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jruby.exceptions.MainExitException;
import org.jruby.platform.Platform;
import org.jruby.util.ByteList;
import org.jruby.util.SafePropertyAccessor;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_YML;
import static org.asciidoc.intellij.psi.AsciiDocUtil.ATTRIBUTES;
import static org.asciidoc.intellij.psi.AsciiDocUtil.CAPTURE_FILE_EXTENSION;
import static org.asciidoc.intellij.psi.AsciiDocUtil.STRIP_FILE_EXTENSION;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraAttachmentsDirRelative;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraExamplesDir;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraImagesDirRelative;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraModuleDir;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraPagesDir;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findAntoraPartials;
import static org.asciidoc.intellij.psi.AsciiDocUtil.findSpringRestDocSnippets;

/**
 * @author Julien Viet
 */
public class AsciiDoc {

  private static final String GROUP_ID = "asciidoctor";
  /**
   * Base directory to look up includes.
   */
  private File fileBaseDir;
  private String name;

  private static final ReentrantLock LOCK = new ReentrantLock();

  public static NotificationGroup getNotificationGroup() {
    NotificationGroup notificationGroup = NotificationGroupManager
      .getInstance()
      .getNotificationGroup(GROUP_ID);
    if (notificationGroup == null) {
      // plugin might still be dynamically registering
      // see: https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/779
      notificationGroup = ApplicationManager.getApplication().runReadAction((Computable<NotificationGroup>) () -> NotificationGroupManager
        .getInstance()
        .getNotificationGroup(GROUP_ID));
    }
    return notificationGroup;
  }

  private static class MaxHashMap extends LinkedHashMap<String, Asciidoctor> {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Asciidoctor> eldest) {
      // cache up to three instances (for example: javafx, pdf, spring-restdocs)
      if (this.size() > 3) {
        eldest.getValue().shutdown();
        return true;
      } else {
        return false;
      }
    }
  }

  private static final MaxHashMap INSTANCES = new MaxHashMap();

  private static boolean shutdown = false;

  private static PrependConfig prependConfig;

  private static AntoraIncludeAdapter antoraIncludeAdapter;

  @Nullable
  private static AttributesRetriever attributesRetriever;

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDoc.class);

  static {
    SystemOutputHijacker.install();
  }

  public static void checkUnloadPlugin() {
    lock();
    try {
      if (INSTANCES.size() > 0) {
        // as beforePluginUnload() is incomplete, vote against reloading
        // as an incomplete unload would leave the user with disabled AsciiDoc functionality until the next restart.
        throw new CannotUnloadPluginException("expecting JRuby classloader issues, don't allow unloading");
      }
    } finally {
      unlock();
    }
  }

  public static void beforePluginUnload() {
    LOG.info("shutting down Asciidoctor instances");
    lock();
    try {
      shutdown = true;
      LOG.info("about to shutdown " + INSTANCES.size() + " instances");
      INSTANCES.forEach((key, value) -> {
        value.unregisterAllExtensions();
        value.close();
      });
      LOG.info("all instances shut down");
      INSTANCES.clear();
      if (SystemOutputHijacker.isInstalled()) {
        SystemOutputHijacker.uninstall();
      }
      try {
        Class<?> shutdownHooks = Class.forName("java.lang.ApplicationShutdownHooks");
        Field fieldHooks = shutdownHooks.getDeclaredField("hooks");
        fieldHooks.setAccessible(true);
        @SuppressWarnings("unchecked")
        IdentityHashMap<Thread, Thread> hooks = (IdentityHashMap<Thread, Thread>) fieldHooks.get(null);
        List<Thread> jrubyShutdownThreads = hooks.keySet().stream().filter(thread -> thread.getClass().getName().startsWith("org.jruby.util.JRubyClassLoader")).collect(Collectors.toList());
        jrubyShutdownThreads.forEach(thread -> {
          // as we want to run this shutdown thing now until it completes
          // noinspection CallToThreadRun
          thread.run();
          Runtime.getRuntime().removeShutdownHook(thread);
        });
      } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
        LOG.error("unable to de-register shutdown hook", e);
      }
      System.gc();
      // still, this is not enough; there are dangling ThreadLocals like "org.jruby.Ruby$FStringEqual"
      // in addition to that: classes are marked at "Held by JVM" and not unloaded. Reason is unknown, maybe
      // "custom class loaders when they are in the process of loading classes" as of
      // https://www.yourkit.com/docs/java/help/gc_roots.jsp
    } finally {
      unlock();
    }
  }

  /**
   * Update file name and folder. File name might change if file is renamed or moved.
   */
  public void updateFileName(File fileBaseDir, String name) {
    this.fileBaseDir = fileBaseDir;
    this.name = name;
  }

  /**
   * Images directory.
   */
  @Nullable
  private final Path imagesPath;
  private final String projectBasePath;
  private final Project project;

  public AsciiDoc(Project project, File fileBaseDir, @Nullable Path imagesPath, String name) {
    this.projectBasePath = project.getBasePath();
    this.fileBaseDir = fileBaseDir;
    this.imagesPath = imagesPath;
    this.name = name;
    this.project = project;
  }

  @SuppressWarnings("checkstyle:MethodLength")
  private Asciidoctor initWithExtensions(List<String> extensions, boolean springRestDocs, FileType format) {
    if (shutdown) {
      throw new ProcessCanceledException();
    }
    boolean extensionsEnabled;
    AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    if (extensions.size() > 0) {
      asciiDocApplicationSettings.setExtensionsPresent(projectBasePath, true);
    }
    String md;
    if (Boolean.TRUE.equals(asciiDocApplicationSettings.getExtensionsEnabled(project, projectBasePath))) {
      extensionsEnabled = true;
      md = calcMd(projectBasePath, extensions);
    } else {
      extensionsEnabled = false;
      md = calcMd(projectBasePath, Collections.emptyList());
    }
    if (springRestDocs) {
      md = md + ".restdoc";
    }
    if (format == FileType.JAVAFX || format == FileType.JCEF || format == FileType.HTML || format == FileType.BROWSER) {
      // special ruby extensions loaded for JAVAFX and HTML
      md = md + "." + format.name();
    }
    boolean krokiEnabled = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isKrokiEnabled();
    if (krokiEnabled) {
      md = md + ".kroki";
    }
    boolean diagramPresent = isDiagramPresent();
    if (diagramPresent) {
      md = md + ".diagram";
    }
    boolean pdfPresent = isPdfPresent();
    if (pdfPresent) {
      md = md + ".pdf";
    }
    Asciidoctor asciidoctor = INSTANCES.get(md);
    if (asciidoctor == null) {
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      LogHandler logHandler = new IntellijLogHandler("initialize");
      String oldEncoding = null;
      if (Platform.IS_WINDOWS) {
          /* There is an initialization procedure in Ruby.java that will abort
             when the encoding in file.encoding is not known to JRuby. Therefore default to UTF-8 in this case
             as a most sensible default. */
        String encoding = System.getProperty("file.encoding", "UTF-8");
        ByteList bytes = ByteList.create(encoding);
        EncodingDB.Entry entry = EncodingDB.getEncodings().get(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
        if (entry == null) {
          entry = EncodingDB.getAliases().get(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getBegin() + bytes.getRealSize());
        }
        if (entry == null) {
          // this happens for example with -Dfile.encoding=MS949 (Korean?)
          oldEncoding = encoding;
          LOG.warn("unsupported encoding " + encoding + " in JRuby, defaulting to UTF-8");
          System.setProperty("file.encoding", "UTF-8");
        }
      }
      try {
        asciidoctor = createInstance(extensionsEnabled ? extensions : Collections.emptyList());
        asciidoctor.registerLogHandler(logHandler);
        // require openssl library here to enable download content via https
        // requiring it later after other libraries have been loaded results in "undefined method `set_params' for #<OpenSSL::SSL::SSLContext"
        asciidoctor.requireLibrary("openssl");
        asciidoctor.javaExtensionRegistry().preprocessor(prependConfig);
        asciidoctor.javaExtensionRegistry().includeProcessor(antoraIncludeAdapter);
        if (format == FileType.JAVAFX || format == FileType.HTML || format == FileType.JCEF || format == FileType.DOCX || format == FileType.BROWSER) {
          asciidoctor.javaExtensionRegistry().postprocessor(attributesRetriever);
        }
        // disable JUL logging of captured messages
        // https://github.com/asciidoctor/asciidoctorj/issues/669
        Logger.getLogger("asciidoctor").setUseParentHandlers(false);

        if (!krokiEnabled && diagramPresent) {
          asciidoctor.requireLibrary("asciidoctor-diagram");
        } else if (!diagramPresent) {
          try (InputStream is = this.getClass().getResourceAsStream("/diagram-placeholder.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script diagram-placeholder.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        }

        if (format == FileType.JAVAFX || format == FileType.JCEF) {
          try (InputStream is = this.getClass().getResourceAsStream("/sourceline-treeprocessor.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script sourceline-treeprocessor.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is).treeprocessor("SourceLineTreeProcessor");
          }
        }

        if (format == FileType.JAVAFX && diagramPresent) {
          try (InputStream is = this.getClass().getResourceAsStream("/plantuml-png-patch.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script plantuml-png-patch.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        } else if (format == FileType.JCEF && diagramPresent) {
          try (InputStream is = this.getClass().getResourceAsStream("/plantuml-patch.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script plantuml-patch.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        }
        if (format.backend.equals("html5")) {
          try (InputStream is = this.getClass().getResourceAsStream("/html5-antora.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script html5-antora.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        } else if (format.backend.equals("pdf")) {
          try (InputStream is = this.getClass().getResourceAsStream("/pdf-antora.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script pdf-antora.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        } else if (format.backend.equals("docbook5")) {
          try (InputStream is = this.getClass().getResourceAsStream("/docbook5-antora.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script docbooc5-antora.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        }

        if (springRestDocs) {
          try (InputStream is = this.getClass().getResourceAsStream("/springrestdoc-operation-blockmacro.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script springrestdoc-operation-blockmacro.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        }

        if (krokiEnabled) {
          try (InputStream is = this.getClass().getResourceAsStream("/kroki-extension.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script kroki-extension.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is);
          }
        }

        if (extensionsEnabled) {
          for (String extension : extensions) {
            if (extension.toLowerCase().endsWith(".rb")) {
              asciidoctor.rubyExtensionRegistry().requireLibrary(extension);
            }
          }
        }
        INSTANCES.put(md, asciidoctor);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        if (oldEncoding != null) {
          System.setProperty("file.encoding", oldEncoding);
        }
        if (asciidoctor != null) {
          asciidoctor.unregisterLogHandler(logHandler);
        }
        SystemOutputHijacker.deregister();
        notify(boasOut, boasErr, Collections.emptyList());
      }
    }
    return asciidoctor;
  }

  private boolean isDiagramPresent() {
    boolean diagramPresent = AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJDiagram();
    if (!diagramPresent) {
      // try to find it in the class path for tests
      try (InputStream is = this.getClass().getResourceAsStream("/gems/asciidoctor-diagram-" +
        AsciiDocDownloaderUtil.ASCIIDOCTORJ_DIAGRAM_VERSION + "/lib/asciidoctor-diagram.rb")) {
        if (is != null) {
          diagramPresent = true;
        }
      } catch (IOException ex) {
        throw new RuntimeException("unable to open stream", ex);
      }
    }
    return diagramPresent;
  }

  private boolean isPdfPresent() {
    boolean pdfPresent = AsciiDocDownloaderUtil.getAsciidoctorJPdfFile().exists();
    if (!pdfPresent) {
      // try to find it in the class path for tests
      try (InputStream is = this.getClass().getResourceAsStream("/gems/asciidoctor-pdf-" +
        AsciiDocDownloaderUtil.ASCIIDOCTORJ_PDF_VERSION + "/lib/asciidoctor-pdf.rb")) {
        if (is != null) {
          pdfPresent = true;
        }
      } catch (IOException ex) {
        throw new RuntimeException("unable to open stream", ex);
      }
    }
    return pdfPresent;
  }

  /**
   * Create an instance of Asciidoctor.
   */
  private Asciidoctor createInstance(List<String> extensions) {
    ClassLoader cl = this.getClass().getClassLoader();
    List<URL> urls = new ArrayList<>();
    try {
      File file1 = AsciiDocDownloaderUtil.getAsciidoctorJPdfFile();
      if (file1.exists()) {
        urls.add(file1.toURI().toURL());
      }
      if (AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJDiagram()) {
        File file2 = AsciiDocDownloaderUtil.getAsciidoctorJDiagramFile();
        if (file2.exists()) {
          urls.add(file2.toURI().toURL());
        }
        File file3 = AsciiDocDownloaderUtil.getAsciidoctorJDiagramPlantumlFile();
        if (file3.exists()) {
          urls.add(file3.toURI().toURL());
        }
        File file4 = AsciiDocDownloaderUtil.getAsciidoctorJDiagramDitaaminiFile();
        if (file4.exists()) {
          urls.add(file4.toURI().toURL());
        }
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException("unable to add JAR AsciidoctorJ to class path", e);
    }
    File tempDirectory = null;
    for (String extension : extensions) {
      if (extension.toLowerCase().endsWith(".jar")) {
        File jar = new File(extension);
        try {
          if (jar.exists()) {
            // copy JAR to temporary folder to avoid locking the original file on Windows
            if (tempDirectory == null) {
              tempDirectory = Files.createTempDirectory("asciidoctor-intellij").toFile();
            }
            File target = new File(tempDirectory, jar.getName());
            FileUtils.copyFile(jar, target);
            urls.add(target.toURI().toURL());
          }
        } catch (MalformedURLException e) {
          throw new RuntimeException("unable to add JAR '" + extension + "' AsciidoctorJ to class path", e);
        } catch (IOException e) {
          throw new RuntimeException("unable to create temporary folder");
        }
      }
    }

    if (urls.size() > 0) {
      cl = new URLClassLoader(urls.toArray(new URL[]{}), cl);
    } else if (cl instanceof URLClassLoader) {
      // Wrap an existing URLClassLoader with an empty list to prevent scanning of JARs by Ruby Runtime during Unit Tests.
      cl = new URLClassLoader(new URL[]{}, cl);
    }

    ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
    try {
      // set classloader for current thread as otherwise JRubyAsciidoctor#processRegistrations() will not register extensions
      Thread.currentThread().setContextClassLoader(cl);
      AsciidoctorJRuby asciidoctorJRuby = AsciidoctorJRuby.Factory.create(cl);

      /* initialize these lazily, as they call service loader things */
      // TODO: have one instance matching each Asciidoctor instance we're using?
      if (prependConfig == null) {
        prependConfig = new PrependConfig();
      }
      if (antoraIncludeAdapter == null) {
        antoraIncludeAdapter = new AntoraIncludeAdapter();
      }
      if (attributesRetriever == null) {
        attributesRetriever = new AttributesRetriever();
      }

      return asciidoctorJRuby;
    } finally {
      Thread.currentThread().setContextClassLoader(oldCl);
    }

  }

  /**
   * Calculate a hash for the extensions.
   * Hash will change if the project has been changed, of the contents of files have changed.
   * This will also include all files in subdirectories of the extension when creating the hash.
   */
  private String calcMd(String projectBasePath, List<String> extensions) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(projectBasePath.getBytes(StandardCharsets.UTF_8));
      List<Path> folders = new ArrayList<>();
      for (String s : extensions) {
        try {
          try (InputStream is = new FileInputStream(s)) {
            md.update(IOUtils.toByteArray(is));
          }
          Path parent = FileSystems.getDefault().getPath(s).getParent();
          if (!folders.contains(parent)) {
            folders.add(parent);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, Files::isDirectory)) {
              for (Path p : stream) {
                scanForRubyFiles(p, md);
              }
            }
          }
        } catch (IOException e) {
          throw new RuntimeException("unable to read file", e);
        }
      }
      byte[] mdbytes = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte mdbyte : mdbytes) {
        sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("unknown hash", e);
    }
  }

  private void scanForRubyFiles(Path path, MessageDigest md) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path p : stream) {
        if (Files.isDirectory(p)) {
          scanForRubyFiles(p, md);
        }
        if (Files.isRegularFile(p) && Files.isReadable(p)) {
          try (InputStream is = Files.newInputStream(p)) {
            md.update(IOUtils.toByteArray(is));
          }
        }
      }
    }
  }

  private void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords) {
    notify(boasOut, boasErr, logRecords,
      !AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isShowAsciiDocWarningsAndErrorsInEditor());
  }

  public void notifyAlways(ByteArrayOutputStream boasOut, ByteArrayOutputStream
    boasErr, List<LogRecord> logRecords) {
    notify(boasOut, boasErr, logRecords, true);
  }

  private void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords,
                      boolean logAll) {
    String out = boasOut.toString(StandardCharsets.UTF_8);
    String err = boasErr.toString(StandardCharsets.UTF_8);
    // this is reported by com.intellij.util.lang.ZipResourceFile when loading JRuby libraries
    // see: https://youtrack.jetbrains.com/issue/IDEA-264777
    err = err.replaceAll("WARN: Do not use URL connection as JarURLConnection\r?\n", "");
    if (logAll) {
      // logRecords will not be handled in the org.asciidoc.intellij.annotator.ExternalAnnotator
      for (LogRecord logRecord : logRecords) {
        if (logRecord.getSeverity() == Severity.DEBUG) {
          continue;
        }
        new IntellijLogHandler(name).log(logRecord);
      }
    }
    if (out.length() > 0) {
      Notification notification = AsciiDoc.getNotificationGroup().createNotification("Message during rendering " + name, out,
        NotificationType.INFORMATION);
      notification.setImportant(false);
      Notifications.Bus.notify(notification);
    }
    if (err.length() > 0) {
      Notification notification = AsciiDoc.getNotificationGroup().createNotification("Error during rendering " + name, err,
        NotificationType.INFORMATION);
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
  }

  /**
   * Create a temporary image page. Use with folder of document parent.
   * Keep this for a while to allow Asciidoclet plugin to update.
   *
   * @deprecated use {@link #tempImagesPath(Path, Project)} instead
   */
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public static Path tempImagesPath() {
    return tempImagesPath(null, null);
  }

  /**
   * Call this method to create in UNSAFE mode a fresh temporary folder that will be used for temporary files when running Asciidoctor, or a relative folder to the parent in any other mode.
   * This handles the situation that a temporary folder that is not a sub-folder of the document's parent can't be read from or written to when
   * the mode is not UNSAFE.
   */
  @Nullable
  public static Path tempImagesPath(Path parent, @Nullable Project project) {
    Path tempImagesPath = null;
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (settings.getAsciiDocPreviewSettings().getSafeMode(project) != SafeMode.UNSAFE && parent != null) {
      tempImagesPath = parent.resolve(".asciidoctor/images");
    } else {
      try {
        tempImagesPath = Files.createTempDirectory("asciidoctor-intellij");
      } catch (IOException ex) {
        String message = "Can't create temp folder to render images: " + ex.getMessage();
        Notification notification = AsciiDoc.getNotificationGroup()
          .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR);
        // increase event log counter
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
      }
    }
    return tempImagesPath;
  }

  public static void cleanupImagesPath(Path tempImagesPath) {
    if (tempImagesPath != null && !tempImagesPath.endsWith(Path.of(".asciidoctor", "images"))) {
      try {
        FileUtils.deleteDirectory(tempImagesPath.toFile());
      } catch (IOException ex) {
        com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", ex);
      }
    }
  }

  @NotNull
  public static @Language("asciidoc")
  String config(Document document, Project project) {
    VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
    return config(currentFile, project);
  }

  @NotNull
  public static @Language("asciidoc")
  String config(VirtualFile currentFile, Project project) {
    StringBuilder tempContent = new StringBuilder();
    Collection<String> roots = AsciiDocUtil.getRoots(project);
    if (currentFile != null) {
      VirtualFile folder = currentFile.getParent();
      if (folder != null) {
        while (true) {
          for (String configName : new String[]{".asciidoctorconfig", ".asciidoctorconfig.adoc"}) {
            VirtualFile configFile = folder.findChild(configName);
            if (configFile != null &&
              !currentFile.equals(configFile)) {
              final VirtualFile folderFinal = folder;
              AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
                Document config = FileDocumentManager.getInstance().getDocument(configFile);
                if (config != null) {
                  // TODO: for traceability add current file name as a comment
                  // prepend the new config, followed by two newlines to avoid sticking-together content
                  tempContent.insert(0, "\n\n");
                  tempContent.insert(0, config.getText());
                  // prepend the location of the config file
                  tempContent.insert(0, ":asciidoctorconfigdir: " + folderFinal.getCanonicalPath() + "\n\n");
                }
              });
            }
          }
          if (roots.contains(folder.getName())) {
            break;
          }
          folder = folder.getParent();
          if (folder == null) {
            break;
          }
        }
      }
    }
    return tempContent.toString();
  }

  @FunctionalInterface
  public interface Notifier {
    void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords);
  }

  public String render(@Language("asciidoc") String text, List<String> extensions) {
    return render(text, "", extensions, this::notify);
  }

  public String render(@Language("asciidoc") String text, String config, List<String> extensions) {
    return render(text, config, extensions, this::notify);
  }

  public String render(@Language("asciidoc") String text, String config, List<String> extensions, Notifier notifier) {
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    FileType fileType;
    if (settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(AsciiDocJCEFHtmlPanelProvider.class.getName())) {
      fileType = FileType.JCEF;
    } else {
      fileType = FileType.JAVAFX;
    }
    return render(text, config, extensions, notifier, fileType);
  }

  public String render(@Language("asciidoc") String text,
                       String config,
                       List<String> extensions,
                       Notifier notifier,
                       FileType format) {
    VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
      project,
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    VirtualFile antoraModuleDir = findAntoraModuleDir(
      project,
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    Collection<AttributeDeclaration> attributes = populateAntoraAttributes(project, fileBaseDir, antoraModuleDir);
    attributes.addAll(populateDocumentAttributes(fileBaseDir, name));
    lock();
    try {
      if (shutdown) {
        throw new ProcessCanceledException();
      }
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      // SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format);
        asciidoctor.registerLogHandler(logHandler);
        prependConfig.setConfig(config);
        antoraIncludeAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        AntoraReferenceAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        try {
          return "<div id=\"content\"" + (antoraModuleDir != null ? " class=\"doc\"" : "") + ">\n" + asciidoctor.convert(text,
            getDefaultOptions(format, springRestDocsSnippets, attributes)) + "\n</div>";
        } finally {
          prependConfig.setConfig("");
          antoraIncludeAdapter.setAntoraDetails(null, null, null, null);
          asciidoctor.unregisterLogHandler(logHandler);
        }
      } catch (AlreadyDisposedException | ProcessCanceledException ex) {
        // AlreadyDisposedException: IDE is shutting down
        // ProcessCanceledException: reading interrupted by event dispatch thread
        throw ex;
      } catch (Exception | ServiceConfigurationError ex) {
        boolean exceptionInLog = checkIfExceptionShouldAppearInLog(ex);
        if (exceptionInLog) {
          logHandler.log(new LogRecord(Severity.FATAL, ex.getMessage()));
          LOG.warn("unable to render AsciiDoc document", ex);
        }
        StringBuilder response = new StringBuilder();
        response.append("<div id=\"content\"><p>unable to render AsciiDoc document</p>");
        Throwable t = ex;
        do {
          response.append("<p style='white-space: pre-wrap;'>").append(t.getClass().getCanonicalName()).append(": ").append(
            StringEscapeUtils.escapeHtml4(t.getMessage())
          );
          if (t instanceof MainExitException && t.getMessage().startsWith("unknown encoding name")) {
            response.append("<p>Either your local encoding is not supported by JRuby, or you passed an unrecognized value to the Java property 'file.encoding' either in the IntelliJ options file or via the JAVA_TOOL_OPTION environment variable.");
            String property = SafePropertyAccessor.getProperty("file.encoding", null);
            response.append("<p>encoding passed by system property 'file.encoding': ").append(property);
            response.append("<p>available encodings (excluding aliases): ");
            EncodingDB.getEncodings().forEach(entry -> response.append(entry.getEncoding().getCharsetName()).append(" "));
          }
          t = t.getCause();
        } while (t != null);
        if (exceptionInLog) {
          response.append("<p>(the full exception stack trace is available in the IDE's log file. Visit menu item 'Help | Show Log in Explorer' to see the log)");
        }
        response.append("</div>");
        return response.toString();
      } finally {
        // SystemOutputHijacker.deregister();
        notifier.notify(boasOut, boasErr, logHandler.getLogRecords());
      }
    } finally {
      unlock();
    }
  }

  /**
   * Don't log full exception and stack trace to IDE's log for well known exceptions that already include enough content.
   */
  private boolean checkIfExceptionShouldAppearInLog(Throwable ex) {
    return ex.getMessage() == null || !ex.getMessage().contains("PlantUML preprocessing failed");
  }

  private Collection<AttributeDeclaration> populateDocumentAttributes(File fileBaseDir, String name) {
    Collection<AttributeDeclaration> attributes = new ArrayList<>();
    attributes.add(new AsciiDocAttributeDeclarationDummy("docname", name.replaceAll(STRIP_FILE_EXTENSION, "")));
    if (name.contains(".")) {
      attributes.add(new AsciiDocAttributeDeclarationDummy("docfilesuffix", name.replaceAll(CAPTURE_FILE_EXTENSION, "$2")));
    }
    attributes.add(new AsciiDocAttributeDeclarationDummy("docfile", new File(fileBaseDir, name).getAbsolutePath()));
    attributes.add(new AsciiDocAttributeDeclarationDummy("docdir", fileBaseDir.getAbsolutePath()));
    return attributes;
  }

  private static int validateAccess() {
    /* This class will lock on AsciiDoc.java so that only one instance of Asciidoctor is running at any time. This
    allows re-using the instances that are expensive to create (both in terms of memory and cpu seconds).
    When rendering an AsciiDoc document, this requires read-access to document for example to resolve Antora information
    or includes.
    By ensuring no previous read or write lock exists, this avoids the following dead-lock situation:
    process 1: waiting for write lock, allowing no-one else to acquire a read lock -> will not proceed due to 3
    process 2: already acquired AsciiDoc lock, running AsciiDoctor rendering and waiting for a read-lock -> will not proceed due to 1
    process 3: already acquired read-lock, now waiting for AsciiDoc lock -> will not proceed due to 2
     */
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      throw new IllegalStateException("no write access should be allowed here as it might cause a deadlock");
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      // in unit test mode there is always read-access allowed
      return 0;
    }
    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      // the AsciiDocJavaDocInfoGenerator will get here with an existing ReadLock, use a timeout here to avoid a deadlock.
      Set<StackTraceElement> nonblocking = Arrays.stream(Thread.currentThread().getStackTrace()).filter(stackTraceElement ->
        stackTraceElement.getClassName().endsWith("AsciiDocJavaDocInfoGenerator") ||
          stackTraceElement.getClassName().contains("AsciiDocHandleUnloadActivity") ||
          stackTraceElement.getClassName().endsWith("AsciidocletJavaDocInfoGenerator")
      ).collect(Collectors.toSet());
      if (nonblocking.size() > 0) {
        return 20;
      }
      throw new IllegalStateException("no read access should be allowed here as it might cause a deadlock");
    }
    return 0;
  }

  public void convertTo(File file, String config, List<String> extensions, FileType format) {
    VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
      project,
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir));
    VirtualFile antoraModuleDir = findAntoraModuleDir(
      project,
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    Collection<AttributeDeclaration> attributes = populateAntoraAttributes(project, fileBaseDir, antoraModuleDir);

    lock();
    try {
      if (shutdown) {
        throw new ProcessCanceledException();
      }
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      // SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format);
        prependConfig.setConfig(config);
        antoraIncludeAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        AntoraReferenceAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        asciidoctor.registerLogHandler(logHandler);
        try {
          ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          if (indicator == null || !indicator.isCanceled()) {
            asciidoctor.convertFile(file, getExportOptions(
              getDefaultOptions(format, springRestDocsSnippets, attributes), format));
          }
        } finally {
          prependConfig.setConfig("");
          antoraIncludeAdapter.setAntoraDetails(null, null, null, null);
          asciidoctor.unregisterLogHandler(logHandler);
        }
      } catch (ProcessCanceledException ex) {
        throw ex;
      } catch (Exception | ServiceConfigurationError ex) {
        LOG.warn("unable to render AsciiDoc document", ex);
        logHandler.log(new LogRecord(Severity.FATAL, ex.getMessage()));
        StringBuilder response = new StringBuilder();
        response.append("unable to render AsciiDoc document");
        Throwable t = ex;
        do {
          response.append("<p>").append(t.getClass().getCanonicalName()).append(": ").append(t.getMessage());
          if (t instanceof MainExitException && t.getMessage().startsWith("unknown encoding name")) {
            response.append("<p>Either your local encoding is not supported by JRuby, or you passed an unrecognized value to the Java property 'file.encoding' either in the IntelliJ options file or via the JAVA_TOOL_OPTION environment variable.");
            String property = SafePropertyAccessor.getProperty("file.encoding", null);
            response.append("<p>encoding passed by system property 'file.encoding': ").append(property);
            response.append("<p>available encodings (excuding aliases): ");
            EncodingDB.getEncodings().forEach(entry -> response.append(entry.getEncoding().getCharsetName()).append(" "));
          }
          t = t.getCause();
        } while (t != null);
        response.append("<p>(the full exception stack trace is available in the IDE's log file. Visit menu item 'Help | Show Log in Explorer' to see the log)");
        try {
          boasErr.write(response.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new RuntimeException("Unable to write bytes");
        }
      } finally {
        // SystemOutputHijacker.deregister();
        Notifier notifier = this::notifyAlways;
        notifier.notify(boasOut, boasErr, logHandler.getLogRecords());
      }
    } finally {
      unlock();
    }
  }

  private static void lock() {
    int timeout = validateAccess();
    if (timeout == 0) {
      LOCK.lock();
    } else {
      try {
        if (!LOCK.tryLock(timeout, TimeUnit.SECONDS)) {
          LOG.warn("unable to acquire lock after timeout");
          throw new ProcessCanceledException(new RuntimeException("unable to acquire lock after timeout"));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("unable to acquire lock", e);
      }
    }
  }

  private static void unlock() {
    LOCK.unlock();
  }

  public static Collection<AttributeDeclaration> populateAntoraAttributes(@NotNull Project project, File fileBaseDir, VirtualFile
    antoraModuleDir) {
    Collection<AttributeDeclaration> result = new ArrayList<>();
    if (antoraModuleDir != null) {
      result.addAll(collectAntoraAttributes(antoraModuleDir, project));

      VirtualFile baseDir = LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir);
      if (baseDir == null) {
        baseDir = antoraModuleDir.getFileSystem().findFileByPath(fileBaseDir.getPath());
      }
      VirtualFile antoraPages = findAntoraPagesDir(project, baseDir);
      VirtualFile antoraPartials = findAntoraPartials(project, baseDir);
      String antoraImagesDir = findAntoraImagesDirRelative(project, baseDir);
      String antoraAttachmentsDir = findAntoraAttachmentsDirRelative(project, baseDir);
      VirtualFile antoraExamplesDir = findAntoraExamplesDir(project, baseDir);

      if (antoraPages != null) {
        result.add(new AsciiDocAttributeDeclarationDummy("pagesdir", antoraPages.getCanonicalPath()));
      }
      if (antoraPartials != null) {
        result.add(new AsciiDocAttributeDeclarationDummy("partialsdir", antoraPartials.getCanonicalPath()));
      }
      if (antoraImagesDir != null) {
        result.add(new AsciiDocAttributeDeclarationDummy("imagesdir", antoraImagesDir));
      }
      if (antoraAttachmentsDir != null) {
        result.add(new AsciiDocAttributeDeclarationDummy("attachmentsdir", antoraAttachmentsDir));
      }
      if (antoraExamplesDir != null) {
        result.add(new AsciiDocAttributeDeclarationDummy("examplesdir", antoraExamplesDir.getCanonicalPath()));
      }
    }
    return result;
  }

  public static Collection<AttributeDeclaration> collectAntoraAttributes(VirtualFile antoraModuleDir, Project project) {
    List<AttributeDeclaration> result = new ArrayList<>();
    result.add(new AsciiDocAttributeDeclarationDummy("icons", "font"));
    result.add(new AsciiDocAttributeDeclarationDummy("env-site", ""));
    result.add(new AsciiDocAttributeDeclarationDummy("site-gen", "antora"));
    result.add(new AsciiDocAttributeDeclarationDummy("site-gen-antora", ""));
    result.add(new AsciiDocAttributeDeclarationDummy("page-module", antoraModuleDir.getName()));

    if (antoraModuleDir.getParent() != null && antoraModuleDir.getParent().getParent() != null) {
      VirtualFile antoraFile = antoraModuleDir.getParent().getParent().findChild(ANTORA_YML);
      if (antoraFile != null) {
        AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
          for (VirtualFile playbook : AsciiDocAntoraPlaybookIndex.getVirtualFiles(project)) {
            result.addAll(getAntoraPlaybookAsciiDocAttributes(project, playbook));
          }

          result.addAll(getAntoraComponentDescriptorAsciiDocAttributes(project, antoraFile));
        });
      }
    }
    return result;
  }

  private static final Key<CachedValue<List<AttributeDeclaration>>> KEY_ASCIIDOC_ATTRIBUTES = new Key<>("asciidoc-attributes-in-yaml");
  private static final Key<CachedValue<Map<String, Object>>> KEY_ASCIIDOC_YAML_ATTRIBUTES = new Key<>("asciidoc-antora-yaml");

  private static List<AttributeDeclaration> getAntoraPlaybookAsciiDocAttributes(Project project, VirtualFile antoraFile) {
    PsiFile currentFile = PsiManager.getInstance(project).findFile(antoraFile);
    if (currentFile == null) {
      return Collections.emptyList();
    }
    return CachedValuesManager.getCachedValue(currentFile, KEY_ASCIIDOC_ATTRIBUTES,
      () -> {
        List<AttributeDeclaration> result = new ArrayList<>();
        try {
          Map<String, Object> antora;
          antora = AsciiDoc.readAntoraYaml(project, antoraFile);
          Object asciidoc = antora.get("asciidoc");
          if (asciidoc instanceof Map) {
            @SuppressWarnings("rawtypes") Object attributes = ((Map) asciidoc).get("attributes");
            if (attributes instanceof Map) {
              @SuppressWarnings("unchecked") Map<Object, Object> map = (Map<Object, Object>) attributes;
              map.forEach((k, v) -> {
                String vs;
                if (v == null) {
                  vs = null;
                } else if (v instanceof Boolean && !(Boolean) v) {
                  // false -> soft unset
                  vs = null;
                } else {
                  vs = v.toString();
                }
                result.add(new AsciiDocAttributeDeclarationDummy(k.toString(), vs));
              });
            }
          }
        } catch (YAMLException ignored) {
          // continue without detailed Antora information
        }
        return CachedValueProvider.Result.create(result, currentFile);
      }
    );
  }

  private static List<AttributeDeclaration> getAntoraComponentDescriptorAsciiDocAttributes(Project project, VirtualFile antoraFile) {
    PsiFile currentFile = PsiManager.getInstance(project).findFile(antoraFile);
    if (currentFile == null) {
      return Collections.emptyList();
    }
    return CachedValuesManager.getCachedValue(currentFile, KEY_ASCIIDOC_ATTRIBUTES,
      () -> {
        List<AttributeDeclaration> result = new ArrayList<>();
        try {
          Map<String, Object> antora = readAntoraYaml(project, antoraFile);
          mapAttribute(result, antora, "name", "page-component-name");
          mapAttribute(result, antora, "version", "page-component-version");
          mapAttribute(result, antora, "title", "page-component-title");
          mapAttribute(result, antora, "version", "page-version");
          mapAttribute(result, antora, "display-version", "page-display-version");
          Object asciidoc = antora.get("asciidoc");
          if (asciidoc instanceof Map) {
            @SuppressWarnings("rawtypes") Object attributes = ((Map) asciidoc).get("attributes");
            if (attributes instanceof Map) {
              @SuppressWarnings("unchecked") Map<Object, Object> map = (Map<Object, Object>) attributes;
              map.forEach((k, v) -> {
                String vs;
                if (v == null) {
                  vs = null;
                } else if (v instanceof Boolean && !(Boolean) v) {
                  // false -> soft unset
                  vs = null;
                } else {
                  vs = v.toString();
                }
                result.add(new AsciiDocAttributeDeclarationDummy(k.toString(), vs));
              });
            }
          }
        } catch (YAMLException ignored) {
          // continue without detailed Antora information
        }
        return CachedValueProvider.Result.create(result, currentFile);
      }
    );
  }

  public static @NotNull Map<String, Object> readAntoraYaml(Project project, VirtualFile antoraFile) {
    PsiFile currentFile = PsiManager.getInstance(project).findFile(antoraFile);
    if (currentFile == null) {
      YAMLException ex = new YAMLException("file not found");
      handleAntoraYamlException(ex, antoraFile.getCanonicalPath());
      throw ex;
    }
    return CachedValuesManager.getCachedValue(currentFile, KEY_ASCIIDOC_YAML_ATTRIBUTES,
      () -> {
        Map<String, Object> result;
        try {
          Yaml yaml = new Yaml();
          try {
            try (InputStream is = antoraFile.getInputStream()) {
              result = yaml.load(is);
            }
          } catch (IOException ex) {
            throw new YAMLException("unable to read file", ex);
          }
          if (result == null) {
            // result will be null if file is empty
            result = new HashMap<>();
          }
          // starting from Antora 3.0.0.alpha-3 a version can be empty. It will be treated internally as an empty string
          result.putIfAbsent("version", "");
          return CachedValueProvider.Result.create(result, currentFile);
        } catch (YAMLException ex) {
          handleAntoraYamlException(ex, antoraFile.getCanonicalPath());
          throw ex;
        }
      });
  }

  private static void handleAntoraYamlException(YAMLException ex, @Nullable String canonicalPath) {
    String message = canonicalPath + ": " + ex.getMessage();
    LOG.warn("Error reading Antora component information", ex);
    Notification notification = AsciiDoc.getNotificationGroup().createNotification("Error reading Antora component information", message,
      NotificationType.ERROR);
    notification.setImportant(true);
    Notifications.Bus.notify(notification);
  }

  public Options getExportOptions(Options options, FileType fileType) {
    if (fileType == FileType.HTML || fileType == FileType.BROWSER || fileType == FileType.DOCX) {
      options.setOption(Options.HEADER_FOOTER, true);
    }
    return options;
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  private Options getDefaultOptions(FileType fileType, VirtualFile
    springRestDocsSnippets, Collection<AttributeDeclaration> attributes) {
    AttributesBuilder builder = Attributes.builder()
      .showTitle(true)
      .backend(fileType.backend)
      .sourceHighlighter("coderay@")
      .attribute("coderay-css@", "style")
      .attribute("env", "idea")
      .attribute("skip-front-matter@")
      .attribute("env-idea");

    if (springRestDocsSnippets != null) {
      builder.attribute("snippets", springRestDocsSnippets.getCanonicalPath());
    }

    for (AttributeDeclaration entry : attributes) {
      if (entry.getAttributeValue() != null) {
        builder.attribute(entry.getAttributeName(), entry.getAttributeValue() + (entry.isSoft() ? "@" : ""));
      } else {
        builder.attribute("!" + entry.getAttributeName(), entry.isSoft() ? "@" : "");
      }
    }

    String graphvizDot = System.getenv("GRAPHVIZ_DOT");
    if (graphvizDot != null) {
      builder.attribute("graphvizdot@", graphvizDot);
    }

    Attributes attrs = builder.build();

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (imagesPath != null) {
      if (fileType == FileType.JAVAFX || fileType == FileType.JCEF || fileType == FileType.BROWSER) {
        if (settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())
          || settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(AsciiDocJCEFHtmlPanelProvider.class.getName()) || fileType == FileType.BROWSER) {
          // will only work in UNSAFE mode as Asciidoctor will otherwise report path is outside of jail; recovering automatically
          if (settings.getAsciiDocPreviewSettings().getSafeMode(project) == SafeMode.UNSAFE) {
            attrs.setAttribute("outdir", imagesPath.toAbsolutePath().normalize().toString());
            // this prevents asciidoctor diagram to render images to a folder {outdir}/{imagesdir} ...
            // ... that might then be outside of the temporary folder as {imagesdir} might traverse to a parent folder
            // beware that the HTML output will still prepends {imagesdir} that later needs to be removed from HTML output
            // https://github.com/asciidoctor/asciidoctor-diagram/issues/110
            attrs.setAttribute("imagesoutdir", imagesPath.toAbsolutePath().normalize().toString());
          } else {
            attrs.setAttribute("cachedir", new File(fileBaseDir, ".asciidoctor/diagram").getAbsolutePath());
            attrs.setAttribute("imagesoutdir", new File(fileBaseDir, ".asciidoctor/images").getAbsolutePath());
          }
        }
      }
    }

    if (settings.getAsciiDocPreviewSettings().isKrokiEnabled()) {
      String krokiUrl = settings.getAsciiDocPreviewSettings().getKrokiUrl();
      if (!StringUtils.isEmpty(krokiUrl)) {
        attrs.setAttribute("kroki-server-url", krokiUrl);
      }
      if (fileType == FileType.JAVAFX || fileType == FileType.JEDITOR) {
        attrs.setAttribute("kroki-default-format", "png");
      }
    }

    settings.getAsciiDocPreviewSettings().getAttributes().forEach(attrs::setAttribute);

    OptionsBuilder opts = Options.builder().safe(settings.getSafe(project)).backend(fileType.backend).headerFooter(false)
      .attributes(attrs)
      .option("sourcemap", "true")
      .baseDir(fileBaseDir);

    return opts.build();
  }

  public Map<String, String> getAttributes() {
    if (attributesRetriever == null) {
      // might have failed if Asciidoctor initialization has failed
      return Collections.emptyMap();
    }
    return attributesRetriever.getAttributes();
  }

  public enum FileType {
    PDF("pdf"),
    HTML("html5"),
    BROWSER("html5"),
    JAVAFX("html5"),
    JCEF("html5"),
    DOCX("docbook5"),
    JEDITOR("html5");

    private final String backend;

    FileType(String backend) {
      this.backend = backend;
    }

    @Override
    public String toString() {
      return backend;
    }

  }

  private static void mapAttribute(Collection<AttributeDeclaration> result, Map<String, Object> antora, String
    nameSource, String nameTarget) {
    Object value = antora.get(nameSource);
    if (value != null) {
      result.add(new AsciiDocAttributeDeclarationDummy(nameTarget, value.toString()));
    }
  }

  @NotNull
  public static String enrichPage(@NotNull String html, String
    standardCss, @NotNull Map<String, String> attributes, @Nullable Project project) {

    html = enrichPageMaxWidth(html, attributes);

    html = enrichPageDocTypeAndBodyClass(html, attributes);

    /* Add CSS line; if styles can't be loaded, use default styles. */
    String stylesheet = attributes.get("stylesheet");
    if (stylesheet != null && stylesheet.length() != 0) {
      String linkcss = attributes.get("linkcss");
      String stylesdir = attributes.get("stylesdir");
      if (linkcss != null) {
        String css = stylesheet;
        // stylesdir default value is ".", therefore ignore it
        if (stylesdir != null && !stylesdir.equals(".")) {
          css = stylesdir + "/" + stylesheet;
        }
        // Load remote stylesheet. Only if that succeeds, remove the standard stylesheet with JavaScript
        // a background color as a background color is necessary for OSR JCEF preview
        // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/954
        html = html
          .replace("<head>", "<head>" + "<link rel='stylesheet' type='text/css' href='" + css + "' onload=\"document.head.getElementsByTagName('link')[0].nextSibling.nextSibling.remove()\" />" +
            "<style>body { background-color: rgb(255, 255, 255); }</style>" + standardCss);
      } else {
        // custom stylesheet set
        VirtualFile stylesdirVf = LocalFileSystem.getInstance().findFileByPath(attributes.get("docdir"));
        if (stylesdirVf != null) {
          String css;
          if (stylesdir != null && stylesdir.length() != 0) {
            File stylesdirFile = new File(stylesdir);
            if (!stylesdirFile.isAbsolute()) {
              stylesdirVf = stylesdirVf.findFileByRelativePath(stylesdir);
            } else {
              stylesdirVf = LocalFileSystem.getInstance().findFileByIoFile(stylesdirFile);
            }
          }
          if (stylesdirVf == null) {
            css = "/* unable to find CSS at '" + stylesdir + "' */" + standardCss;
          } else {
            VirtualFile stylesheetVf = stylesdirVf.findFileByRelativePath(stylesheet);
            if (stylesheetVf != null) {
              try (InputStream is = stylesheetVf.getInputStream()) {
                css = IOUtils.toString(is, StandardCharsets.UTF_8);
              } catch (IOException ex) {
                css = "/* unable to read CSS from " + stylesdirVf.getCanonicalPath() + ": " + ex.getMessage() + " */ " + standardCss;
              }
            } else {
              css = "/* unable to find stylesheet '" + stylesheet + "' */ " + standardCss;
            }
          }
          // add a background color as a background color is necessary for OSR JCEF preview
          // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/954
          html = html
            .replace("<head>", "<head>" + "<style>body { background-color: rgb(255, 255, 255); }</style><style>" + css + "</style>");
        }
      }
    } else {
      // use standard stylesheet
      if (standardCss != null) {
        html = html
          .replace("<head>", "<head>" + standardCss);
      }
    }

    html = enrichPageHighlightJs(html, attributes, project);

    String docinfo = attributes.get("docinfo");
    if (docinfo != null && docinfo.length() != 0) {
      // custom stylesheet set
      String docinfodir = attributes.get("docinfodir");
      VirtualFile docinfodirVf = LocalFileSystem.getInstance().findFileByPath(attributes.get("docdir"));
      if (docinfodirVf != null) {
        if (docinfodir != null && docinfodir.length() != 0) {
          File docinfodirFile = new File(docinfodir);
          if (!docinfodirFile.isAbsolute()) {
            docinfodirVf = docinfodirVf.findFileByRelativePath(docinfodir);
          } else {
            docinfodirVf = LocalFileSystem.getInstance().findFileByIoFile(docinfodirFile);
          }
        }
        if (docinfodirVf != null) {
          StringTokenizer st = new StringTokenizer(docinfo, ",");
          while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.equals("shared") || token.equals("shared-head") || token.equals("private") || token.equals("private-head")) {
              String prefix = "";
              if (token.startsWith("private")) {
                prefix = attributes.get("docname") + "-";
              }
              VirtualFile file = docinfodirVf.findChild(prefix + "docinfo.html");
              if (file != null) {
                String content;
                try (InputStream is = file.getInputStream()) {
                  content = IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                  content = "<!-- unable to read contents from from " + file.getCanonicalPath() + ": " + ex.getMessage() + " -->";
                }
                content = replaceAttributes(content, attributes);
                html = html
                  .replace("</head>", content + "</head>");
              }
            }
            if (token.equals("shared") || token.equals("shared-footer") || token.equals("private") || token.equals("private-footer")) {
              String prefix = "";
              if (token.startsWith("private")) {
                prefix = attributes.get("docname") + "-";
              }
              VirtualFile file = docinfodirVf.findChild(prefix + "docinfo-footer.html");
              if (file != null) {
                String content;
                try (InputStream is = file.getInputStream()) {
                  content = IOUtils.toString(is, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                  content = "<!-- unable to read contents from from " + file.getCanonicalPath() + ": " + ex.getMessage() + " -->";
                }
                content = replaceAttributes(content, attributes);
                html = html
                  .replace("</body>", content + "</body>");
              }
            }
          }
        }
      }
    }
    return html;
  }

  private static String enrichPageDocTypeAndBodyClass(String html, @NotNull Map<String, String> attributes) {
    /* Add body classes */
    String doctype = attributes.get("doctype");
    String bodyClass = "";
    if (doctype != null && doctype.length() != 0) {
      bodyClass += doctype + " ";
    }

    String tocClass = attributes.get("toc-class");
    String toc = attributes.get("toc");
    if (tocClass != null && toc != null) {
      bodyClass += tocClass + " ";
      String tocPosition = attributes.get("toc-position");
      if (tocPosition != null && tocPosition.length() != 0) {
        bodyClass += "toc-" + tocPosition + " ";
      }
      if (tocClass.length() > 0) {
        // in embedded mode, the class of the toc is always set to 'toc'
        // this way it will not be placed left or right using the standard CSS
        // this replacement fixes it by setting it to the real 'toc-class'
        html = html.replaceAll("<div id=\"toc\" class=\"toc\">", "<div id=\"toc\" class=\"" + tocClass + "\">\n");
      }
    }

    if (bodyClass.length() > 0) {
      html = html
        .replace("<body>", "<body class='" + bodyClass + "' />");
    }
    return html;
  }

  private static String enrichPageMaxWidth(@NotNull String html, @NotNull Map<String, String> attributes) {
    /* Add max-width classes */
    String maxWidth = attributes.get("max-width");
    if (maxWidth != null) {
      html = html.replaceAll("(<div id=\"(content|footnotes)\")", "$1 " + Matcher.quoteReplacement("style=\"max-width: " + maxWidth + "\")"));
    }
    return html;
  }

  private static String enrichPageHighlightJs(@NotNull String html, @NotNull Map<String, String> attributes, @Nullable Project project) {
    /* Add HighlightJS line */
    if (Objects.equals(attributes.get("source-highlighter"), "highlightjs") || // will work both with and without a dot
      Objects.equals(attributes.get("source-highlighter"), "highlight.js")) {
      String theme = attributes.get("highlightjs-theme");
      if (theme == null) {
        theme = "github";
      }
      String dir = attributes.get("highlightjsdir");
      String css;
      StringBuilder js;
      if (dir == null) {
        css = PreviewStaticServer.getScriptUrl("highlightjs/styles/" + theme + ".min.css");
        js = new StringBuilder("<script src='" + PreviewStaticServer.getScriptUrl("highlightjs/highlight.min.js") + "'></script>");
      } else {
        if (dir.matches("^https?://.*")) {
          css = dir + "/styles/" + theme + ".min.css";
          js = new StringBuilder("<script src='" + dir + "/highlight.min.js" + "'></script>");
          String langs = attributes.get("highlightjs-languages");
          if (langs != null) {
            for (String lang : langs.split(",", -1)) {
              js.append("<script src='").append(dir).append("/languages/").append(lang.trim()).append(".min.js'></script>");
            }
          }
        } else {
          // if not an absolute path on linux or windows, prefix project root
          if (!dir.startsWith("/") && dir.indexOf(':') != 1) {
            String docfile = attributes.get("docfile");
            if (Platform.IS_WINDOWS) {
              docfile = docfile.replaceAll("\\\\", "/");
            }
            if (docfile != null && project != null) {
              for (String root : AsciiDocUtil.getRoots(project)) {
                if (docfile.startsWith(root)) {
                  dir = root + "/" + dir;
                  break;
                }
              }
            } else {
              dir = ""; // not allowed scenario
            }
          } else {
            if (project != null) {
              boolean found = false;
              for (String root : AsciiDocUtil.getRoots(project)) {
                if (dir.startsWith(root)) {
                  found = true;
                  break;
                }
              }
              if (!found) {
                dir = ""; // not allowed scenario
              }
            }
          }
          js = new StringBuilder("<script src='" + PreviewStaticServer.signFile(dir + "/highlight.min.js") + "'></script>");
          String langs = attributes.get("highlightjs-languages");
          if (langs != null) {
            for (String lang : langs.split(",", -1)) {
              js.append("<script src='").append(PreviewStaticServer.signFile(dir + "/languages/" + lang.trim() + ".min.js")).append("'></script>");
            }
          }
          css = PreviewStaticServer.signFile(dir + "/styles/" + theme + ".min.css");
        }
      }
      html = html
        .replace("<head>", "<head>" + "<link rel='stylesheet' type='text/css' href='" + css + "' />");
      html = html.replace("</body>", "" + js + "</body>");
      html = html.replace("</body>", "<script>\n" +
        "if (!hljs.initHighlighting.called) {\n" +
        "  hljs.initHighlighting.called = true;\n" +
        "  [].slice.call(document.querySelectorAll('pre.highlight > code')).forEach(function (el) { hljs.highlightElement(el) })\n" +
        "}\n" +
        "</script></body>");
    }
    return html;
  }

  private static String replaceAttributes(String template, Map<String, String> attributes) {
    Matcher matcher = ATTRIBUTES.matcher(template);
    Map<String, MutableInt> recursionProtection = new HashMap<>();
    while (matcher.find()) {
      String attributeName = matcher.group(1);
      String attributeValue = attributes.get(attributeName);
      if (attributeValue != null) {
        MutableInt recursionCounter = recursionProtection.computeIfAbsent(attributeName, s -> new MutableInt(0));
        if (recursionCounter.intValue() > 20 || recursionProtection.size() > 100) {
          return template;
        }
        recursionCounter.increment();
        template = new StringBuilder(template).replace(matcher.start(), matcher.end(), attributeValue).toString();
        matcher = ATTRIBUTES.matcher(template);
      }
    }
    return template;
  }

}
