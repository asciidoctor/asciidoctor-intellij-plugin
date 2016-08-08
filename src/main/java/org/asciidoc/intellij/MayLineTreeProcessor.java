package org.asciidoc.intellij;

import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Treeprocessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Schwartz (msg systems ag) 2016
 */
public class MayLineTreeProcessor extends Treeprocessor {
  @Override
  public Document process(Document document) {
    Map map = new HashMap();
    List<AbstractBlock> items = document.findBy(map);
    for(AbstractBlock block : items) {
      block.setAttr("role", "data-line-", true);
    }
    return document;
  }
}
