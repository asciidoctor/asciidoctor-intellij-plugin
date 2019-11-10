package org.asciidoc.intellij;

import com.intellij.spellchecker.BundledDictionaryProvider;

public class AsciiDocBundledDictionaryProvider implements BundledDictionaryProvider {
  @Override
  public String[] getBundledDictionaries() {
    return new String[]{"asciidoc.dic"};
  }
}
