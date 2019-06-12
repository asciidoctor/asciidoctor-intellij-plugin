package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Michael Krausse, Raffael Krebs, Ulrich Etter
 */
public class MakeLink extends FormatAsciiDocAction {

  @Override
  public String getName() {
    return "MakeLink";
  }

  @Override
  public String updateSelection(String selection, boolean isWord) {
    if (isLink(selection)) {
      return selection + "[]";
    } else {
      return "http://" + selection + "[" + selection + "]";
    }

  }

  boolean isLink(String selection) {
    return selection.startsWith("http://") || selection.startsWith("https://");
  }

}
