package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

/**
 * Retrieve attribute as of after the document was rendered. For now only retrieve imagesdir.
 */
public class AttributesRetriever extends Postprocessor {
  private String imagesdir;

  @Override
  public String process(Document document, String output) {
    imagesdir = (String) document.getAttribute("imagesdir");
    return output;
  }

  public String getImagesdir() {
    return imagesdir;
  }
}
