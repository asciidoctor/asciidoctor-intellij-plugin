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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.geronimo.gshell.io.SystemOutputHijacker;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogHandler;

/**
 * @author Julien Viet
 */
public class AsciiDoc {

  private static Asciidoctor asciidoctor;

  static {
    SystemOutputHijacker.install();
  }

  private static String hash = "";

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

  private void initWithExtensions(List<String> extensions) {
    synchronized (AsciiDoc.class) {
      boolean extensionsEnabled;
      AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
      if(extensions.size() > 0) {
        asciiDocApplicationSettings.setExtensionsPresent(projectBasePath, true);
      }
      String md;
      if (Boolean.TRUE.equals(asciiDocApplicationSettings.getExtensionsEnabled(projectBasePath))) {
        extensionsEnabled = true;
        md = calcMd(projectBasePath, extensions);
      } else {
        extensionsEnabled = false;
        md = calcMd(projectBasePath, Collections.EMPTY_LIST);
      }
      if (!md.equals(hash)) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
        ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
        SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
        LogHandler logHandler = new IntellijLogHandler("initialize");
        try {
          Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
          asciidoctor = Asciidoctor.Factory.create();
          asciidoctor.registerLogHandler(logHandler);
          // disable JUL logging of captured messages
          // https://github.com/asciidoctor/asciidoctorj/issues/669
          Logger.getLogger("asciidoctor").setUseParentHandlers(false);
          asciidoctor.requireLibrary("asciidoctor-diagram");
          InputStream is = this.getClass().getResourceAsStream("/sourceline-treeprocessor.rb");
          if (is == null) {
            throw new RuntimeException("unable to load script sourceline-treeprocessor.rb");
          }
          asciidoctor.rubyExtensionRegistry().loadClass(is).treeprocessor("SourceLineTreeProcessor");
          is = this.getClass().getResourceAsStream("/plantuml-png-patch.rb");
          if (is == null) {
            throw new RuntimeException("unable to load script plantuml-png-patch.rb");
          }
          if(extensionsEnabled) {
            for (String extension : extensions) {
              asciidoctor.rubyExtensionRegistry().requireLibrary(extension);
            }
          }
          asciidoctor.rubyExtensionRegistry().loadClass(is);
          hash = md;
        }
        finally {
          if (asciidoctor != null) {
            asciidoctor.unregisterLogHandler(logHandler);
          }
          SystemOutputHijacker.deregister();
          notify(boasOut, boasErr);
          Thread.currentThread().setContextClassLoader(old);
        }
      }
    }
  }

  /** Calculate a hash for the extensions.
   * Hash will change if the project has been changed, of the contents of files have changed.
   * TODO: hash will not change if files referenced in extensions changed.
   */
  private String calcMd(String projectBasePath, List<String> extensions) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(projectBasePath.getBytes(StandardCharsets.UTF_8));
      for (String s : extensions) {
        try {
          InputStream is = new FileInputStream(s);
          try {
            md.update(IOUtils.toByteArray(is));
          } finally {
            IOUtils.closeQuietly(is);
          }
        }
        catch (IOException e) {
          throw new RuntimeException("unable to read file", e);
        }
      }
      byte[] mdbytes = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte mdbyte : mdbytes) {
        sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("unknown hash", e);
    }
  }

  private void notify(ByteArrayOutputStream boasOut, ByteArrayOutputStream boasErr) {
    String out = boasOut.toString();
    String err = boasErr.toString();
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

  public String render(String text, List<String> extensions) {
    LogHandler logHandler = new IntellijLogHandler(name);
    synchronized (this) {
      initWithExtensions(extensions);
      ClassLoader old = Thread.currentThread().getContextClassLoader();
      ByteArrayOutputStream boasOut = new ByteArrayOutputStream();
      ByteArrayOutputStream boasErr = new ByteArrayOutputStream();
      SystemOutputHijacker.register(new PrintStream(boasOut), new PrintStream(boasErr));
      asciidoctor.registerLogHandler(logHandler);
      try {
        Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
        return "<div id=\"content\">\n" + asciidoctor.render(text, getDefaultOptions()) + "\n</div>";
      } finally {
        asciidoctor.unregisterLogHandler(logHandler);
        SystemOutputHijacker.deregister();
        notify(boasOut, boasErr);
        Thread.currentThread().setContextClassLoader(old);
      }
    }
  }

  private Map<String, Object> getDefaultOptions() {
    Attributes attrs = AttributesBuilder.attributes()
      .showTitle(true)
      .sourceHighlighter("coderay")
      .attribute("coderay-css", "style")
      .attribute("env", "idea")
      .attribute("env-idea")
      .get();

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    if (imagesPath != null) {
      if (settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
        attrs.setAttribute("outdir", imagesPath.toAbsolutePath().normalize().toString());
      }
    }

    settings.getAsciiDocPreviewSettings().getAttributes().forEach(attrs::setAttribute);

    OptionsBuilder opts = OptionsBuilder.options().safe(SafeMode.UNSAFE).backend("html5").headerFooter(false)
      .attributes(attrs)
      .option("sourcemap", "true")
      .baseDir(fileBaseDir);

    return opts.asMap();
  }
}
