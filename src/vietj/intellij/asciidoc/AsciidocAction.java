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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.psi.PsiFile;
import org.asciidoctor.Asciidoctor;

import java.util.Collections;

/** @author Julien Viet */
public class AsciidocAction extends AnAction {
  public void actionPerformed(AnActionEvent event) {

    PsiFile file = event.getData(DataKeys.PSI_FILE);
    String text = file.getText();

    ClassLoader old = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(AsciidocAction.class.getClassLoader());
      Asciidoctor doctor = Asciidoctor.Factory.create();
      String result = doctor.render(text, Collections.<String, Object>emptyMap());
      System.out.println("Rendered as " + result);
    }
    catch (Exception e
        ) {
      e.printStackTrace();
    }
    finally {
      Thread.currentThread().setContextClassLoader(old);
    }


  }

  @Override
  public void update(AnActionEvent e) {
    PsiFile file = e.getData(DataKeys.PSI_FILE);
    boolean enabled = file != null && file.getName().endsWith(".asciidoc");
    e.getPresentation().setEnabled(enabled);
  }
}
