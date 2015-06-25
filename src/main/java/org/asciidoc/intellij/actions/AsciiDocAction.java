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
package org.asciidoc.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.file.AsciiDocFileType;

import java.io.File;

/** @author Julien Viet */
public class AsciiDocAction extends AnAction {

  public void actionPerformed(AnActionEvent event) {
    PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    new AsciiDoc(new File(file.getOriginalFile().getParent().getVirtualFile().getCanonicalPath())).render(file.getText());
  }

  @Override
  public void update(AnActionEvent e) {
    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
    boolean enabled = false;
    if (file != null) {
      for (String ext : AsciiDocFileType.DEFAULT_ASSOCIATED_EXTENSIONS) {
        if (file.getName().endsWith("." + ext)) {
          enabled = true;
          break;
        }
      }
    }
    e.getPresentation().setEnabled(enabled);
  }
}
