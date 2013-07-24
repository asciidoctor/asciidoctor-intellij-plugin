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
package vietj.intellij.asciidoc;

import org.asciidoctor.Asciidoctor;

import java.util.Collections;

/** @author Julien Viet */
public class Asciidoc {

  /** . */
  private final Asciidoctor doctor;

  public Asciidoc() {
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AsciidocAction.class.getClassLoader());
      doctor = Asciidoctor.Factory.create();
    }
    finally {
      Thread.currentThread().setContextClassLoader(old);
    }
  }

  public String render(String text) {
    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AsciidocAction.class.getClassLoader());
      return doctor.render(text, Collections.<String, Object>emptyMap());
    }
    finally {
      Thread.currentThread().setContextClassLoader(old);
    }
  }
}
