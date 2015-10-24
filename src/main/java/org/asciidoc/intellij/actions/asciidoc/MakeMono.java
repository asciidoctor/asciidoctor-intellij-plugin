package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeMono extends FormatAsciiDocAction {

  @Override
  public String updateSelection(String selection) {
    if(selection.startsWith("`") && selection.endsWith("`")) {
      return selection.substring(1, selection.length() - 1);
    }

    return "`" + selection +"`";
  }

  @Override
  public String getName() {
    return "MakeMono";
  }


}
