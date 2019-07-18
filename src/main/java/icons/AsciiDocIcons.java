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
package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/** @author Julien Viet */
public class AsciiDocIcons {

  private static Icon load(String path) {
    Icon icon;
    try {
      icon = IconLoader.getIcon(path, AsciiDocIcons.class);
      icon.getIconHeight(); // NoClassDefFoundError
    } catch (NoClassDefFoundError e) {
      icon = null;
    }
    if ((icon == null || icon.getIconHeight() == 1) && path.endsWith(".svg")) {
      // when trying to load SVG icons on an older version, they will be returned as 1x1 icon
      // if this is the case, fallback to PNG icons
      path = path.substring(0, path.length() - 4) + ".png";
      icon = IconLoader.getIcon(path, AsciiDocIcons.class);
    }
    return icon;
  }

  /** The AsciiDoc {@link Icon}. */
  public static final Icon ASCIIDOC_ICON = load("/icons/asciidoc.svg");

  public static class Layout {
    public static final Icon EDITOR_ONLY = load("/icons/layout/Editor_only.png"); // 16x16
    public static final Icon EDITOR_PREVIEW = load("/icons/layout/Editor_preview.png"); // 16x16
    public static final Icon PREVIEW_ONLY = load("/icons/layout/Preview_only.png"); // 16x16
  }

  public static class EditorActions {
    public static final Icon BOLD = load("/icons/editor_actions/Bold.svg"); // 16x16
    public static final Icon ITALIC = load("/icons/editor_actions/Italic.svg"); // 16x16
    public static final Icon TABLE = load("/icons/editor_actions/table.png"); // 16x16
    public static final Icon LINK = load("/icons/editor_actions/Link.svg"); // 16x16
    public static final Icon STRIKE_THROUGH = load("/icons/editor_actions/Strike_through.svg"); // 16x16
    public static final Icon CODE_SPAN = load("/icons/editor_actions/Code_span.svg"); // 16x16
    public static final Icon IMAGE = load("/icons/editor_actions/image.png"); // 16x16
  }

  public static class Structure {
    public static final Icon SECTION = load("/icons/structure/section.svg"); // 16x16
    public static final Icon BLOCK = load("/icons/structure/block.svg"); // 16x16
    public static final Icon LISTING = load("/icons/structure/listing.svg"); // 16x16
    public static final Icon MACRO = load("/icons/structure/macro.svg"); // 16x16
  }

}
