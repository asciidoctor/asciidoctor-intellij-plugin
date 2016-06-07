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

import org.asciidoc.intellij.actions.AsciiDocAction;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;

import java.io.File;
import java.util.Map;

/** @author Julien Viet */
public class AsciiDoc {
  /** . */
  private final Asciidoctor asciidoctor;

  /** Base directory to look up includes. */
  private final File baseDir;

  public AsciiDoc(File path) {
    this.baseDir = path;
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
      asciidoctor = Asciidoctor.Factory.create();
    }
    finally {
      Thread.currentThread().setContextClassLoader(old);
    }
  }

  public String render(String text) {
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AsciiDocAction.class.getClassLoader());
      asciidoctor.requireLibrary("asciidoctor-diagram");
      return "<div id=\"content\">\n" + asciidoctor.render(text, getDefaultOptions()) + "\n</div>";
    }
    finally {
      Thread.currentThread().setContextClassLoader(old);
    }
  }

  public Map<String, Object> getDefaultOptions() {
    Attributes attrs = AttributesBuilder.attributes().showTitle(true)
        .sourceHighlighter("coderay").attribute("coderay-css", "style")
        .attribute("env", "idea").attribute("env-idea").get();
    OptionsBuilder opts = OptionsBuilder.options().safe(SafeMode.UNSAFE).backend("html5").headerFooter(false).attributes(attrs)
        .baseDir(baseDir);
    return opts.asMap();
  }
}
