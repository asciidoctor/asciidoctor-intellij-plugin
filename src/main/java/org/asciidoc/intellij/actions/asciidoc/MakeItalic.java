package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeItalic extends FormatAsciiDocAction {

  @Override
  public String updateSelection(String selection) {
    if(selection.startsWith("_") && selection.endsWith("_")) {
      return selection.substring(1, selection.length() - 1);
    }

    return "_" + selection +"_";
  }

  @Override
  public String getName() {
    return "MakeItalic";
  }


}
