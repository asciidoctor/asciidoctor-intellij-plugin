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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.asciidoc.AntoraIncludeAdapter;
import org.asciidoc.intellij.asciidoc.AntoraReferenceAdapter;
import org.asciidoc.intellij.asciidoc.AttributesRetriever;
import org.asciidoc.intellij.asciidoc.PrependConfig;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static org.asciidoc.intellij.psi.AsciiDocUtil.ANTORA_YML;
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

  private static final PrependConfig PREPEND_CONFIG = new PrependConfig();

  private static final AntoraIncludeAdapter ANTORA_INCLUDE_ADAPTER = new AntoraIncludeAdapter();

  private static final AttributesRetriever ATTRIBUTES_RETRIEVER = new AttributesRetriever();

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDoc.class);

  static {
    SystemOutputHijacker.install();
  }

  /**
   * Base directory to look up includes.
   */
  private final File fileBaseDir;

  /**
   * Images directory.
   */
  private final Path imagesPath;
  private final String name;
  private final String projectBasePath;
  private final Project project;

  public AsciiDoc(Project project, File fileBaseDir, Path imagesPath, String name) {
    this.projectBasePath = project.getBasePath();
    this.fileBaseDir = fileBaseDir;
    this.imagesPath = imagesPath;
    this.name = name;
    this.project = project;
  }

  private Asciidoctor initWithExtensions(List<String> extensions, boolean springRestDocs, FileType format) {
    synchronized (AsciiDoc.class) {
      boolean extensionsEnabled;
      AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
      if (extensions.size() > 0) {
        asciiDocApplicationSettings.setExtensionsPresent(projectBasePath, true);
      }
      String md;
      if (Boolean.TRUE.equals(asciiDocApplicationSettings.getExtensionsEnabled(projectBasePath))) {
        extensionsEnabled = true;
        md = calcMd(projectBasePath, extensions);
      } else {
        extensionsEnabled = false;
        md = calcMd(projectBasePath, Collections.emptyList());
      }
      if (springRestDocs) {
        md = md + ".restdoc";
      }
      if (format == FileType.JAVAFX || format == FileType.HTML) {
        // special ruby extensions loaded for JAVAFX and HTML
        md = md + "." + format.name();
      }
      boolean krokiEnabled = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isKrokiEnabled();
      if (krokiEnabled) {
        md = md + ".kroki";
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
            // this happes for example with -Dfile.encoding=MS949 (Korean?)
            oldEncoding = encoding;
            LOG.warn("unsupported encoding " + encoding + " in JRuby, defaulting to UTF-8");
            System.setProperty("file.encoding", "UTF-8");
          }
        }
        try {
          asciidoctor = createInstance();
          asciidoctor.registerLogHandler(logHandler);
          // require openssl library here to enable download content via https
          // requiring it later after other libraries have been loaded results in "undefined method `set_params' for #<OpenSSL::SSL::SSLContext"
          asciidoctor.requireLibrary("openssl");
          asciidoctor.javaExtensionRegistry().preprocessor(PREPEND_CONFIG);
          asciidoctor.javaExtensionRegistry().includeProcessor(ANTORA_INCLUDE_ADAPTER);
          if (format == FileType.JAVAFX || format == FileType.HTML) {
            asciidoctor.javaExtensionRegistry().postprocessor(ATTRIBUTES_RETRIEVER);
          }
          // disable JUL logging of captured messages
          // https://github.com/asciidoctor/asciidoctorj/issues/669
          Logger.getLogger("asciidoctor").setUseParentHandlers(false);

          if (!krokiEnabled) {
            asciidoctor.requireLibrary("asciidoctor-diagram");
          }

          if (format == FileType.JAVAFX) {
            try (InputStream is = this.getClass().getResourceAsStream("/sourceline-treeprocessor.rb")) {
              if (is == null) {
                throw new RuntimeException("unable to load script sourceline-treeprocessor.rb");
              }
              asciidoctor.rubyExtensionRegistry().loadClass(is).treeprocessor("SourceLineTreeProcessor");
            }
          }

          if (format == FileType.JAVAFX) {
            try (InputStream is = this.getClass().getResourceAsStream("/plantuml-png-patch.rb")) {
              if (is == null) {
                throw new RuntimeException("unable to load script plantuml-png-patch.rb");
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
              asciidoctor.rubyExtensionRegistry().requireLibrary(extension);
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
  }

  /**
   * Create an instance of Asciidoctor.
   */
  private Asciidoctor createInstance() {
    ClassLoader cl = AsciiDocAction.class.getClassLoader();
    if (cl instanceof URLClassLoader) {
      // Wrap an existing URLClassLoader with an empty list to prevent scanning of JARs by Ruby Runtime during Unit Tests.
      cl = new URLClassLoader(new URL[]{}, cl);
    }
    return AsciidoctorJRuby.Factory.create(cl);
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
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, path -> Files.isDirectory(path))) {
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

  public void notifyAlways(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords) {
    notify(boasOut, boasErr, logRecords, true);
  }

  private void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords,
                      boolean logAll) {
    String out = boasOut.toString();
    String err = boasErr.toString();
    if (logAll) {
      // logRecords will not be handled in the org.asciidoc.intellij.annotator.ExternalAnnotator
      for (LogRecord logRecord : logRecords) {
        if (logRecord.getSeverity() == Severity.DEBUG) {
          continue;
        }
        StringBuilder message = new StringBuilder();
        message.append("Error during rendering ").append(name).append("; ").append(logRecord.getSeverity().name()).append(" ");
        if (logRecord.getCursor() != null && logRecord.getCursor().getFile() != null) {
          message.append(logRecord.getCursor().getFile()).append(":").append(logRecord.getCursor().getLineNumber());
        }
        message.append(" ").append(logRecord.getMessage());
        Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("Message during rendering " + name,
          message.toString(), NotificationType.INFORMATION, null);
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
      }
    }
    if (out.length() > 0) {
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("Message during rendering " + name, out,
        NotificationType.INFORMATION, null);
      notification.setImportant(false);
      Notifications.Bus.notify(notification);
    }
    if (err.length() > 0) {
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("Error during rendering " + name, err,
        NotificationType.INFORMATION, null);
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
  }

  @Nullable
  public static Path tempImagesPath() {
    Path tempImagesPath = null;
    try {
      tempImagesPath = Files.createTempDirectory("asciidoctor-intellij");
    } catch (IOException _ex) {
      String message = "Can't create temp folder to render images: " + _ex.getMessage();
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
    return tempImagesPath;
  }

  @NotNull
  public static @Language("asciidoc")
  String config(Document document, Project project) {
    VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
    StringBuilder tempContent = new StringBuilder();
    if (currentFile != null) {
      VirtualFile folder = currentFile.getParent();
      if (folder != null) {
        while (true) {
          for (String configName : new String[]{".asciidoctorconfig", ".asciidoctorconfig.adoc"}) {
            VirtualFile configFile = folder.findChild(configName);
            if (configFile != null &&
              !currentFile.equals(configFile)) {
              final VirtualFile folderFinal = folder;
              ApplicationManager.getApplication().runReadAction(() -> {
                Document config = FileDocumentManager.getInstance().getDocument(configFile);
                if (config != null) {
                  // TODO: for tracibility add current file name as a comment
                  // prepend the new config, followed by two newlines to avoid sticking-together content
                  tempContent.insert(0, "\n\n");
                  tempContent.insert(0, config.getText());
                  // prepend the location of the config file
                  tempContent.insert(0, ":asciidoctorconfigdir: " + folderFinal.getCanonicalPath() + "\n\n");
                }
              });
            }
          }
          if (folder.getPath().equals(project.getBasePath())) {
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

  @NotNull
  public static List<String> getExtensions(Project project) {
    VirtualFile lib = project.getBaseDir().findChild(".asciidoctor");
    if (lib != null) {
      lib = lib.findChild("lib");
    }

    List<String> extensions = new ArrayList<>();
    if (lib != null) {
      for (VirtualFile vf : lib.getChildren()) {
        if ("rb".equals(vf.getExtension())) {
          extensions.add(vf.getCanonicalPath());
        }
      }
    }
    return extensions;
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
    return render(text, config, extensions, notifier, FileType.JAVAFX);
  }

  public String render(@Language("asciidoc") String text, String config, List<String> extensions, Notifier notifier, FileType format) {
    VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
      LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    VirtualFile antoraModuleDir = findAntoraModuleDir(
      LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    validateAccess();
    Map<String, String> attributes = populateAntoraAttributes(projectBasePath, fileBaseDir, antoraModuleDir);
    attributes.putAll(populateDocumentAttributes(fileBaseDir, name));
    synchronized (AsciiDoc.class) {
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format);
        asciidoctor.registerLogHandler(logHandler);
        PREPEND_CONFIG.setConfig(config);
        ANTORA_INCLUDE_ADAPTER.setAntoraDetails(project, antoraModuleDir);
        AntoraReferenceAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        try {
          return "<div id=\"content\"" + (antoraModuleDir != null ? " class=\"doc\"" : "") + ">\n" + asciidoctor.convert(text,
            getDefaultOptions(FileType.JAVAFX, springRestDocsSnippets, attributes)) + "\n</div>";
        } finally {
          PREPEND_CONFIG.setConfig("");
          ANTORA_INCLUDE_ADAPTER.setAntoraDetails(null, null);
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
        return response.toString();
      } finally {
        SystemOutputHijacker.deregister();
        notifier.notify(boasOut, boasErr, logHandler.getLogRecords());
      }
    }
  }

  private Map<String, String> populateDocumentAttributes(File fileBaseDir, String name) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put("docname", name.replaceAll("\\..*$", ""));
    if (name.contains(".")) {
      attributes.put("docfilesuffix", name.replaceAll("^(.*)(\\..*)$", "$2"));
    }
    attributes.put("docfile", new File(fileBaseDir, name).getAbsolutePath());
    attributes.put("docdir", fileBaseDir.getAbsolutePath());
    return attributes;
  }

  private void validateAccess() {
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
      throw new IllegalStateException("no read access should be allowed here as it might cause a deadlock");
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      // in unit test mode there is always read-access allowed
      return;
    }
    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      throw new IllegalStateException("no read access should be allowed here as it might cause a deadlock");
    }
  }

  public void convertTo(File file, String config, List<String> extensions, FileType format) {
    VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
      LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir));
    VirtualFile antoraModuleDir = findAntoraModuleDir(
      LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
      LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
    );
    Map<String, String> attributes = populateAntoraAttributes(projectBasePath, fileBaseDir, antoraModuleDir);
    validateAccess();
    synchronized (AsciiDoc.class) {
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format);
        PREPEND_CONFIG.setConfig(config);
        ANTORA_INCLUDE_ADAPTER.setAntoraDetails(project, antoraModuleDir);
        AntoraReferenceAdapter.setAntoraDetails(project, antoraModuleDir, fileBaseDir, name);
        asciidoctor.registerLogHandler(logHandler);
        try {
          ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          if (indicator == null || !indicator.isCanceled()) {
            asciidoctor.convertFile(file, getExportOptions(
              getDefaultOptions(format, springRestDocsSnippets, attributes), format));
          }
        } finally {
          PREPEND_CONFIG.setConfig("");
          ANTORA_INCLUDE_ADAPTER.setAntoraDetails(null, null);
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
        SystemOutputHijacker.deregister();
        Notifier notifier = this::notifyAlways;
        notifier.notify(boasOut, boasErr, logHandler.getLogRecords());
      }
    }
  }

  public static Map<String, String> populateAntoraAttributes(String projectBasePath, File fileBaseDir, VirtualFile antoraModuleDir) {
    Map<String, String> result = new HashMap<>();
    if (antoraModuleDir != null) {
      result.putAll(collectAntoraAttributes(antoraModuleDir));

      VirtualFile projectBase = LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath));
      VirtualFile baseDir = LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir);
      VirtualFile antoraPages = findAntoraPagesDir(projectBase, baseDir);
      VirtualFile antoraPartials = findAntoraPartials(projectBase, baseDir);
      String antoraImagesDir = findAntoraImagesDirRelative(projectBase, baseDir);
      String antoraAttachmentsDir = findAntoraAttachmentsDirRelative(projectBase, baseDir);
      VirtualFile antoraExamplesDir = findAntoraExamplesDir(projectBase, baseDir);

      if (antoraPages != null) {
        result.put("pagesdir", antoraPages.getCanonicalPath());
      }
      if (antoraPartials != null) {
        result.put("partialsdir", antoraPartials.getCanonicalPath());
      }
      if (antoraImagesDir != null) {
        result.put("imagesdir", antoraImagesDir);
      }
      if (antoraAttachmentsDir != null) {
        result.put("attachmentsdir", antoraAttachmentsDir);
      }
      if (antoraExamplesDir != null) {
        result.put("examplesdir", antoraExamplesDir.getCanonicalPath());
      }
    }
    return result;
  }

  public static Map<String, String> collectAntoraAttributes(VirtualFile antoraModuleDir) {
    Map<String, String> result = new HashMap<>();
    result.put("icons", "font");
    result.put("env-site", "");
    result.put("site-gen", "antora");
    result.put("site-gen-antora", "");
    result.put("page-module", antoraModuleDir.getName());

    if (antoraModuleDir.getParent() != null && antoraModuleDir.getParent().getParent() != null) {
      VirtualFile antoraFile = antoraModuleDir.getParent().getParent().findChild(ANTORA_YML);
      if (antoraFile != null) {
        ApplicationManager.getApplication().runReadAction(() -> {
          Document document = FileDocumentManager.getInstance().getDocument(antoraFile);
          if (document != null) {
            try {
              Map<String, Object> antora = readAntoraYaml(antoraFile);
              mapAttribute(result, antora, "name", "page-component-name");
              mapAttribute(result, antora, "version", "page-component-version");
              mapAttribute(result, antora, "title", "page-component-title");
              mapAttribute(result, antora, "version", "page-version");
              mapAttribute(result, antora, "display-version", "page-display-version");
              Object asciidoc = antora.get("asciidoc");
              if (asciidoc instanceof Map) {
                @SuppressWarnings("rawtypes") Object attributes = ((Map) asciidoc).get("attributes");
                if (attributes instanceof Map) {
                  @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) attributes;
                  map.forEach((k, v) -> {
                    String vs;
                    if (v == null) {
                      // null -> not allowed in YAML file as attribute value
                      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification("AsciiDoc attribute '" + k + "' is null in " + antoraFile.getCanonicalPath(),
                        "Will be treated as unset. Use either false to explicitly unset, or set by providing for example an empty string ''",
                        NotificationType.ERROR, null);
                      notification.setImportant(true);
                      Notifications.Bus.notify(notification);
                      vs = null;
                    } else if (v instanceof Boolean && !(Boolean) v) {
                      // false -> soft unset
                      vs = null;
                    } else if (v instanceof String && v.equals("~")) {
                      // "~" -> hard unset
                      vs = null;
                    } else {
                      vs = v.toString();
                      if (vs.endsWith("@")) {
                        // "...@" -> soft set
                        vs = vs.substring(0, vs.length() - 1);
                      }
                    }
                    result.put(k, vs);
                  });
                }
              }
            } catch (YAMLException ignored) {
              // continue without detailed Antora information
            }
          }
        });
      }
    }
    return result;
  }

  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("asciidoctor",
    NotificationDisplayType.NONE, true);


  public static Map<String, Object> readAntoraYaml(VirtualFile antoraFile) {
    try {
      Document document = FileDocumentManager.getInstance().getDocument(antoraFile);
      if (document == null) {
        throw new YAMLException("unable to read file");
      }
      Yaml yaml = new Yaml();
      return yaml.load(document.getText());
    } catch (YAMLException ex) {
      handleAntoraYamlException(ex, antoraFile.getCanonicalPath());
      throw ex;
    }
  }

  public static Map<String, Object> readAntoraYaml(PsiFile antoraFile) {
    try {
      Yaml yaml = new Yaml();
      return yaml.load(antoraFile.getText());
    } catch (YAMLException ex) {
      String fileName = null;
      VirtualFile virtualFile = antoraFile.getVirtualFile();
      if (virtualFile != null) {
        fileName = virtualFile.getCanonicalPath();
      }
      handleAntoraYamlException(ex, fileName);
      throw new YAMLException("Error when reading file " + fileName);
    }
  }

  private static void handleAntoraYamlException(YAMLException ex, @Nullable String canonicalPath) {
    String message = canonicalPath + ": " + ex.getMessage();
    LOG.warn("Error reading Antora component information", ex);
    Notification notification = NOTIFICATION_GROUP.createNotification("Error reading Antora component information", message,
      NotificationType.ERROR, null);
    notification.setImportant(true);
    Notifications.Bus.notify(notification);
  }

  public Map<String, Object> getExportOptions(Map<String, Object> options, FileType fileType) {
    if (fileType == FileType.HTML) {
      options.put(Options.HEADER_FOOTER, true);
    }
    return options;
  }

  @SuppressWarnings("checkstyle:ParameterNumber")
  private Map<String, Object> getDefaultOptions(FileType fileType, VirtualFile springRestDocsSnippets, Map<String, String> attributes) {
    AttributesBuilder builder = AttributesBuilder.attributes()
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

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      builder.attribute(entry.getKey(), entry.getValue());
    }

    String graphvizDot = System.getenv("GRAPHVIZ_DOT");
    if (graphvizDot != null) {
      builder.attribute("graphvizdot@", graphvizDot);
    }

    Attributes attrs = builder.get();

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (imagesPath != null) {
      if (fileType == FileType.JAVAFX) {
        if (settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
          attrs.setAttribute("outdir", imagesPath.toAbsolutePath().normalize().toString());
          // this prevents asciidoctor diagram to render images to a folder {outdir}/{imagesdir} ...
          // ... that might then be outside of the temporary folder as {imagesdir} might traverse to a parent folder
          // beware that the HTML output will still prepends {imagesdir} that later needs to be removed from HTML output
          // https://github.com/asciidoctor/asciidoctor-diagram/issues/110
          attrs.setAttribute("imagesoutdir", imagesPath.toAbsolutePath().normalize().toString());
        }
      }
    }

    if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isKrokiEnabled()) {
      String krokiUrl = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getKrokiUrl();
      if (!StringUtils.isEmpty(krokiUrl)) {
        attrs.setAttribute("kroki-server-url", krokiUrl);
      }
    }

    settings.getAsciiDocPreviewSettings().getAttributes().forEach(attrs::setAttribute);

    OptionsBuilder opts = OptionsBuilder.options().safe(settings.getSafe()).backend(fileType.backend).headerFooter(false)
      .attributes(attrs)
      .option("sourcemap", "true")
      .baseDir(fileBaseDir);

    return opts.asMap();
  }

  public Map<String, String> getAttributes() {
    return ATTRIBUTES_RETRIEVER.getAttributes();
  }

  public enum FileType {
    PDF("pdf"),
    HTML("html5"),
    JAVAFX("html5"),
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

  private static void mapAttribute(Map<String, String> result, Map<String, Object> antora, String nameSource, String nameTarget) {
    Object value = antora.get(nameSource);
    if (value != null) {
      result.put(nameTarget, value.toString());
    }
  }

  @NotNull
  public static String enrichPage(@NotNull String html, String standardCss, @NotNull Map<String, String> attributes) {
    /* Add CSS line */
    String stylesheet = attributes.get("stylesheet");
    if (stylesheet != null && stylesheet.length() != 0) {
      // custom stylesheet set
      String stylesdir = attributes.get("stylesdir");
      VirtualFile stylesdirVf = LocalFileSystem.getInstance().findFileByPath(attributes.get("docdir"));
      if (stylesdirVf != null) {
        if (stylesdir != null && stylesdir.length() != 0) {
          File stylesdirFile = new File(stylesdir);
          if (!stylesdirFile.isAbsolute()) {
            stylesdirVf = stylesdirVf.findFileByRelativePath(stylesdir);
          } else {
            stylesdirVf = LocalFileSystem.getInstance().findFileByIoFile(stylesdirFile);
          }
        }
        if (stylesdirVf != null) {
          VirtualFile stylesheetVf = stylesdirVf.findChild(stylesheet);
          if (stylesheetVf != null) {
            String css;
            try (InputStream is = stylesheetVf.getInputStream()) {
              css = IOUtils.toString(is);
            } catch (IOException ex) {
              css = "/* unable to read CSS from " + stylesdirVf.getCanonicalPath() + ": " + ex.getMessage() + " */";
            }
            html = html
              .replace("<head>", "<head>" + "<style>" + css + "</style>");
          }
        }
      }
    } else {
      // use standard stylesheet
      if (standardCss != null) {
        html = html
          .replace("<head>", "<head>" + standardCss);
      }
    }

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
                  content = IOUtils.toString(is);
                } catch (IOException ex) {
                  content = "<!-- unable to read contents from from " + file.getCanonicalPath() + ": " + ex.getMessage() + " -->";
                }
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
                  content = IOUtils.toString(is);
                } catch (IOException ex) {
                  content = "<!-- unable to read contents from from " + file.getCanonicalPath() + ": " + ex.getMessage() + " -->";
                }
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

}
