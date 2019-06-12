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
package org.asciidoc.intellij.editor.jeditor;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.net.MalformedURLException;

/** @author Julien Viet */
public class AsciiDocEditorKit extends HTMLEditorKit {

  /** Base directory used to show images. */
  private File baseDir;

  /**
   * When the document is created, the base is set to the current file's folder so
   * that images will displayed in the preview.
   */
  @Override
  public javax.swing.text.Document createDefaultDocument() {
    HTMLDocument document = (HTMLDocument) super.createDefaultDocument();
    try {
      document.setBase(baseDir.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return document;
  }

  public AsciiDocEditorKit(File baseDir) {
    this.baseDir = baseDir;
  }
}
