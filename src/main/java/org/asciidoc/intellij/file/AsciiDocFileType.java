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

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.AsciiDocLanguage;

import javax.swing.*;
import java.util.Locale;

/** @author Julien Viet */
public class AsciiDocFileType extends LanguageFileType {

  /** The {@link AsciiDocFileType} instance. */
  public static final AsciiDocFileType INSTANCE = new AsciiDocFileType();
  /** . */
  public static final String[] DEFAULT_ASSOCIATED_EXTENSIONS = {"adoc", "asciidoc", "ad" };

  private AsciiDocFileType() {
    super(AsciiDocLanguage.INSTANCE);
  }

  @NotNull
  public String getName() {
    return "AsciiDoc";
  }

  @NotNull
  public String getDescription() {
    return "AsciiDoc files";
  }

  @NotNull
  public String getDefaultExtension() {
    return DEFAULT_ASSOCIATED_EXTENSIONS[0];
  }

  @Nullable
  public Icon getIcon() {
    return AsciiDocIcons.Asciidoc_Icon;
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
}
