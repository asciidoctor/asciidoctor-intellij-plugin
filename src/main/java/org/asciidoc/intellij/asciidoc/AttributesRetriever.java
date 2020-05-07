package org.asciidoc.intellij.asciidoc;

import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Retrieve attribute as of after the document was rendered. For now only retrieve imagesdir.
 */
public class AttributesRetriever extends Postprocessor {
  private volatile Map<String, String> attributes;

  @Override
  public String process(Document document, String output) {
    Map<String, String> result = new HashMap<String, String>() {
      @Override
      public String get(Object key) {
        String val = super.get(key);
        if (val != null) {
          Matcher matcher = AsciiDocUtil.ATTRIBUTES.matcher(val);
          // limit the maximum number of replacements to avoid an infinite loop
          int max = 20;
          while (matcher.find() && max > 0) {
            String attributeName = matcher.group(1);
            String attrVal = super.get(attributeName);
            if (attrVal != null) {
              val = matcher.replaceFirst(Matcher.quoteReplacement(attrVal));
              matcher = AsciiDocUtil.ATTRIBUTES.matcher(val);
              max--;
            }
          }
        }
        return val;
      }
    };
    document.getAttributes().forEach((s, o) -> result.put(s.toLowerCase(Locale.US), o.toString()));
    attributes = result;
    return output;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }
}
