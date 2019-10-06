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
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.asciidoc.PrependConfig;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;
import org.jcodings.EncodingDB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jruby.exceptions.MainExitException;
import org.jruby.platform.Platform;
import org.jruby.util.ByteList;
import org.jruby.util.SafePropertyAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.logging.Logger;

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

  private static MaxHashMap instances = new MaxHashMap();

  private static PrependConfig prependConfig = new PrependConfig();

  private com.intellij.openapi.diagnostic.Logger log =
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

  public AsciiDoc(String projectBasePath, File fileBaseDir, Path imagesPath, String name) {
    this.projectBasePath = projectBasePath;
    this.fileBaseDir = fileBaseDir;
    this.imagesPath = imagesPath;
    this.name = name;
  }

  private Asciidoctor initWithExtensions(List<String> extensions, boolean springRestDocs, String format) {
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
      if (format.equals("javafx")) {
        // special plantuml-png-patch.rb only loaded here
        md = md + "." + format;
      }
      Asciidoctor asciidoctor = instances.get(md);
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
            log.warn("unsupported encoding " + encoding + " in JRuby, defaulting to UTF-8");
            System.setProperty("file.encoding", "UTF-8");
          }
        }
        try {
          asciidoctor = Asciidoctor.Factory.create();
          asciidoctor.registerLogHandler(logHandler);
          asciidoctor.javaExtensionRegistry().preprocessor(prependConfig);
          // disable JUL logging of captured messages
          // https://github.com/asciidoctor/asciidoctorj/issues/669
          Logger.getLogger("asciidoctor").setUseParentHandlers(false);
          asciidoctor.requireLibrary("asciidoctor-diagram");

          try (InputStream is = this.getClass().getResourceAsStream("/sourceline-treeprocessor.rb")) {
            if (is == null) {
              throw new RuntimeException("unable to load script sourceline-treeprocessor.rb");
            }
            asciidoctor.rubyExtensionRegistry().loadClass(is).treeprocessor("SourceLineTreeProcessor");
          }

          if (format.equals("javafx")) {
            try (InputStream is = this.getClass().getResourceAsStream("/plantuml-png-patch.rb")) {
              if (is == null) {
                throw new RuntimeException("unable to load script plantuml-png-patch.rb");
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

          if (extensionsEnabled) {
            for (String extension : extensions) {
              asciidoctor.rubyExtensionRegistry().requireLibrary(extension);
            }
          }
          instances.put(md, asciidoctor);
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
          InputStream is = new FileInputStream(s);
          try {
            md.update(IOUtils.toByteArray(is));
          } finally {
            IOUtils.closeQuietly(is);
          }
          Path parent = FileSystems.getDefault().getPath(s).getParent();
          if (!folders.contains(parent)) {
            folders.add(parent);
            for (Path p : Files.newDirectoryStream(parent, path -> Files.isDirectory(path))) {
              scanForRubyFiles(p, md);
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
    for (Path p : Files.newDirectoryStream(path)) {
      if (Files.isDirectory(p)) {
        scanForRubyFiles(p, md);
      }
      if (Files.isRegularFile(p) && Files.isReadable(p)) {
        InputStream is = Files.newInputStream(p);
        try {
          md.update(IOUtils.toByteArray(is));
        } finally {
          IOUtils.closeQuietly(is);
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
  public static String config(Document document, Project project) {
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
              Document config = FileDocumentManager.getInstance().getDocument(configFile);
              if (config != null) {
                // prepend the new config, followed by two newlines to avoid sticking-together content
                tempContent.insert(0, "\n\n");
                tempContent.insert(0, config.getText());
                // prepend the location of the config file
                tempContent.insert(0, ":asciidoctorconfigdir: " + folder.getCanonicalPath() + "\n\n");
              }
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
          Document extension = FileDocumentManager.getInstance().getDocument(vf);
          if (extension != null) {
            extensions.add(vf.getCanonicalPath());
          }
        }
      }
    }
    return extensions;
  }

  @FunctionalInterface
  public interface Notifier {
    void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr, List<LogRecord> logRecords);
  }

  public String render(String text, List<String> extensions) {
    return render(text, "", extensions, this::notify);
  }

  public String render(String text, String config, List<String> extensions) {
    return render(text, config, extensions, this::notify);
  }

  public String render(String text, String config, List<String> extensions, Notifier notifier) {
    return render(text, config, extensions, notifier, "javafx");
  }

  public String render(String text, String config, List<String> extensions, Notifier notifier, String format) {
    synchronized (AsciiDoc.class) {
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ClassLoader old = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
        LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
        LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir)
      );
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, format);
        asciidoctor.registerLogHandler(logHandler);
        prependConfig.setConfig(config);
        try {
          return "<div id=\"content\">\n" + asciidoctor.convert(text, getDefaultOptions("html5", springRestDocsSnippets)) + "\n</div>";
        } finally {
          prependConfig.setConfig("");
          asciidoctor.unregisterLogHandler(logHandler);
        }
      } catch (Exception | ServiceConfigurationError ex) {
        log.warn("unable to render AsciiDoc document", ex);
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
        Thread.currentThread().setContextClassLoader(old);
      }
    }
  }

  public void renderPdf(File file, String config, List<String> extensions) {
    Notifier notifier = this::notifyAlways;
    synchronized (AsciiDoc.class) {
      CollectingLogHandler logHandler = new CollectingLogHandler();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      ClassLoader old = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
      VirtualFile springRestDocsSnippets = findSpringRestDocSnippets(
        LocalFileSystem.getInstance().findFileByIoFile(new File(projectBasePath)),
        LocalFileSystem.getInstance().findFileByIoFile(fileBaseDir));
      try {
        Asciidoctor asciidoctor = initWithExtensions(extensions, springRestDocsSnippets != null, "pdf");
        prependConfig.setConfig(config);
        asciidoctor.registerLogHandler(logHandler);
        try {
          asciidoctor.convertFile(file, getDefaultOptions("pdf", springRestDocsSnippets));
        } finally {
          prependConfig.setConfig("");
          asciidoctor.unregisterLogHandler(logHandler);
        }
      } catch (Exception | ServiceConfigurationError ex) {
        log.warn("unable to render AsciiDoc document", ex);
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
        notifier.notify(boasOut, boasErr, logHandler.getLogRecords());
        Thread.currentThread().setContextClassLoader(old);
      }
    }
  }

  private Map<String, Object> getDefaultOptions(String backend, VirtualFile springRestDocsSnippets) {
    AttributesBuilder builder = AttributesBuilder.attributes()
      .showTitle(true)
      .backend(backend)
      .sourceHighlighter("coderay")
      .attribute("coderay-css", "style")
      .attribute("env", "idea")
      .attribute("env-idea");

    if (springRestDocsSnippets != null) {
      builder.attribute("snippets", springRestDocsSnippets.getCanonicalPath());
    }

    String graphvizDot = System.getenv("GRAPHVIZ_DOT");
    if (graphvizDot != null) {
      builder.attribute("graphvizdot", graphvizDot);
    }

    Attributes attrs = builder.get();

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (imagesPath != null) {
      if (settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
        attrs.setAttribute("outdir", imagesPath.toAbsolutePath().normalize().toString());
      }
    }

    settings.getAsciiDocPreviewSettings().getAttributes().forEach(attrs::setAttribute);

    OptionsBuilder opts = OptionsBuilder.options().safe(SafeMode.UNSAFE).backend(backend).headerFooter(false)
      .attributes(attrs)
      .option("sourcemap", "true")
      .baseDir(fileBaseDir);

    return opts.asMap();
  }
}
