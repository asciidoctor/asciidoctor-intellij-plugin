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
package org.asciidoc.intellij.file;

import com.intellij.ide.plugins.CannotUnloadPluginException;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.util.messages.MessageBusConnection;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Julien Viet
 */
public class AsciiDocFileType extends LanguageFileType {

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocFileType.class);

  /**
   * The {@link AsciiDocFileType} instance.
   */
  public static final AsciiDocFileType INSTANCE = new AsciiDocFileType();
  /**
   * .
   */
  public static final String[] DEFAULT_ASSOCIATED_EXTENSIONS = {"adoc", "asciidoc", "ad"};

  private AsciiDocFileType() {
    super(AsciiDocLanguage.INSTANCE);
  }

  @Override
  @NotNull
  public String getName() {
    return "AsciiDoc";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "AsciiDoc files";
  }

  @Override
  @NotNull
  public String getDefaultExtension() {
    return DEFAULT_ASSOCIATED_EXTENSIONS[0];
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return AsciiDocIcons.ASCIIDOC_ICON;
  }

  public static boolean hasAsciiDocExtension(String filename) {
    filename = filename.toLowerCase(Locale.US);
    for (String extension : DEFAULT_ASSOCIATED_EXTENSIONS) {
      if (filename.endsWith("." + extension)) {
        return true;
      }
    }
    return false;
  }

  /**
   * If the filename ends with a known AsciiDoc extension, return the file name without the extension (and the dot).
   */
  public static String removeAsciiDocExtension(String filename) {
    String filenameAsLowercase = filename.toLowerCase(Locale.US);
    for (String extension : DEFAULT_ASSOCIATED_EXTENSIONS) {
      if (filenameAsLowercase.endsWith("." + extension)) {
        return filename.substring(0, filename.length() - extension.length() - 1);
      }
    }
    return filename;
  }

  /*
   * This takes care of unloading the plugin on uninstalls or updates.
   * WARNING: A dynamic unload will usually only succeed when the application is NOT in debug mode;
   * classes might be marked as "JNI Global" due to this, and not reclaimed, and then unloading fails.
   */
  static {
    LOG.info("setup of subscription");
    MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
    busConnection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void checkUnloadPlugin(@NotNull IdeaPluginDescriptor pluginDescriptor) throws CannotUnloadPluginException {
        if (pluginDescriptor.getPluginId() != null
          && Objects.equals(pluginDescriptor.getPluginId().getIdString(), "org.asciidoctor.intellij.asciidoc")) {
          LOG.info("checkUnloadPlugin");
          // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/512
          // another reason: on windows even after unloading JAR file of the plugin still be locked and can't be deleted, making uninstall impossible
          // https://youtrack.jetbrains.com/issue/IDEA-244471
          throw new CannotUnloadPluginException("unloading mechanism is not safe, incomplete unloading might lead to strange exceptions");
          // AsciiDoc.checkUnloadPlugin();
        }
      }

      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (pluginDescriptor.getPluginId() != null
          && Objects.equals(pluginDescriptor.getPluginId().getIdString(), "org.asciidoctor.intellij.asciidoc")) {
          LOG.info("beforePluginUnload");
          AsciiDoc.beforePluginUnload();
          busConnection.dispose();
        }
      }
    });
  }

}
