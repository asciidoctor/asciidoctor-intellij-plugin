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

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import org.asciidoc.intellij.activities.AsciiDocHandleUnloadActivity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Julien Viet
 */
public class AsciiDocLanguage extends Language {

  public static final Language INSTANCE = new AsciiDocLanguage();

  /**
   * .
   */
  public static final String LANGUAGE_NAME = "AsciiDoc";

  private AsciiDocLanguage() {
    super(LANGUAGE_NAME);
  }

  static {
    // loading this as a startup activity is too late as a user can manage plugins before they open a project and the listener will only fire within a project
    // reloading a plugin before opening a project has problems when loading resources from JAR files (like properties or images)
    // see: https://youtrack.jetbrains.com/issue/IDEA-244471
    AsciiDocHandleUnloadActivity.setupListener();
  }

  public static boolean isAsciiDocFile(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory() || !file.exists()) {
      return false;
    }
    // when a project is already disposed due to a slow initialization, reject this file
    if (project.isDisposed()) {
      return false;
    }
    final FileViewProvider provider = PsiManager.getInstance(project).findViewProvider(file);
    return provider != null && provider.getBaseLanguage().isKindOf(INSTANCE);
  }
}
