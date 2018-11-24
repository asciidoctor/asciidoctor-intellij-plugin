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
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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

  /**
   * Base directory to look up includes.
   */
  private final File baseDir;

  /**
   * Images directory.
   */
  private final Path imagesPath;
  private final String name;

  public AsciiDoc(File baseDir, Path imagesPath, String name) {
    this.baseDir = baseDir;
    this.imagesPath = imagesPath;
    this.name = name;

    synchronized (AsciiDoc.class) {
      if (asciidoctor == null) {
        SystemOutputHijacker.install();
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
          asciidoctor.rubyExtensionRegistry().loadClass(is);
        } finally {
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

  public String render(String text) {
    LogHandler logHandler = new IntellijLogHandler(name);
    synchronized (asciidoctor) {
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
      .baseDir(baseDir);

    return opts.asMap();
  }
}
