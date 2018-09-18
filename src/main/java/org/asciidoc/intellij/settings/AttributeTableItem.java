package org.asciidoc.intellij.settings;

/**
 * @author Julian Ronge 2018
 */
public class AttributeTableItem {
  private String key;
  private String value;

  public AttributeTableItem(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public AttributeTableItem() {

  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
