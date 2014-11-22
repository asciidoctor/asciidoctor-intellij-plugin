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
package vietj.intellij.asciidoc.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vietj.intellij.asciidoc.AsciiDocIcons;
import vietj.intellij.asciidoc.AsciiDocLanguage;

import javax.swing.*;

/** @author Julien Viet */
public class AsciiDocFileType extends LanguageFileType {

  /** The {@link AsciiDocFileType} instance. */
  public static final AsciiDocFileType INSTANCE = new AsciiDocFileType();
  /** . */
  public static final String[] DEFAULT_ASSOCIATED_EXTENSIONS = {"adoc", "asciidoc", "ad", "asc"};

  public AsciiDocFileType() {
    super(new AsciiDocLanguage());
  }

  @NotNull
  public String getName() {
    return "AsciiDoc";
  }

  @NotNull
  public String getDescription() {
    return "The AsciiDoc markup language";
  }

  @NotNull
  public String getDefaultExtension() {
    return DEFAULT_ASSOCIATED_EXTENSIONS[0];
  }

  @Nullable
  public Icon getIcon() {
    return AsciiDocIcons.ASCIIDOC_ICON;
  }
}
