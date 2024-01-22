package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class AsciiDocActionGroup extends DefaultActionGroup {

  {
    getTemplatePresentation().setHideGroupIfEmpty(true);
  }

}
