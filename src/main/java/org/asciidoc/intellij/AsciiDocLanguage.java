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

import com.intellij.diagnostic.ImplementationConflictException;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.activities.AsciiDocHandleUnloadActivity;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Julien Viet
 */
public class AsciiDocLanguage extends Language {

  public static final String LANGUAGE_NAME = "AsciiDoc";
  public static final Language INSTANCE = init();

  // if registration of language fails, log additional information that can be used to analyze the problem
  private static AsciiDocLanguage init() {
    try {
      Language prev = Language.findLanguageByID(LANGUAGE_NAME);
      if (prev != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("mycl=").append(AsciiDocLanguage.class.getClassLoader().toString());
        sb.append(", othercl=").append(prev.getClass().getClassLoader().toString());
        sb.append(", lang=").append(prev);
        sb.append(", langClass=").append(prev.getClass());
        AsciiDocLanguage myInstance = Language.findInstance(AsciiDocLanguage.class);
        sb.append(", foundmyself=").append(myInstance != null);
        Logger log = Logger.getInstance(AsciiDocLanguage.class);
        log.error("AsciiDoc Language already registered: " + sb);
        if (myInstance != null) {
          return myInstance;
        }
      }
      return new AsciiDocLanguage();
    } catch (ImplementationConflictException ex) {
      StringBuilder sb = new StringBuilder();
      sb.append("conflictingPluginIds[");
      for (PluginId conflictingPluginId : ex.getConflictingPluginIds()) {
        sb.append("id=").append(conflictingPluginId.getIdString());
      }
      sb.append("]");
      sb.append(", mycl=").append(AsciiDocLanguage.class.getClassLoader().toString());
      Logger log = Logger.getInstance(AsciiDocLanguage.class);
      log.error("Unable to register AsciiDoc Language, details about conflicting plugins and registrations: " + sb);
      throw ex;
    }
  }

  private AsciiDocLanguage() {
    super(LANGUAGE_NAME);
  }

  static {
    // startup activities ar running late, and might fail due to unloading. Therefore prevent unloading as early as possible.
    // see: https://youtrack.jetbrains.com/issue/IDEA-266736
    try {
      AsciiDocHandleUnloadActivity.setupListener();
    } catch (Throwable t) {
      // catch all exceptions and errors here, to prevent the class initialization from failing
      Logger log = Logger.getInstance(AsciiDocLanguage.class);
      log.error("unable to setup unload activity listener", t);
    }
  }

  public static boolean isAsciiDocFile(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory() || !file.exists()) {
      return false;
    }
    // when a project is already disposed due to a slow initialization, reject this file
    if (project.isDisposed()) {
      return false;
    }
    FileType fileType = file.getFileType();
    return fileType == AsciiDocFileType.INSTANCE ||
      (ScratchUtil.isScratch(file) && LanguageUtil.getLanguageForPsi(project, file) == AsciiDocLanguage.INSTANCE);
  }
}
