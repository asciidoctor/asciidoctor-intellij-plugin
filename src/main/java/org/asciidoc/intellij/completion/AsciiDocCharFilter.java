package org.asciidoc.intellij.completion;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupFocusDegree;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.Nullable;

public class AsciiDocCharFilter extends CharFilter {

  @Nullable
  @Override
  public Result acceptChar(char c, int prefixLength, Lookup lookup) {
    if (!lookup.isCompletion()) {
      return null;
    }
    if (lookup.getPsiFile() == null || lookup.getPsiFile().getLanguage() != AsciiDocLanguage.INSTANCE) {
      return null;
    }
    // we're probably auto-completing a path in a BLOCK_MACRO, use "/" as completion char
    // can't check PSI element here, as block macro might not have brackets yet
    if (c == '/') {
      if (lookup instanceof LookupImpl) {
        // when opened by AutoPopupController, the lookup will only be SEMI_FOCUSED
        // set to FOCUSED, otherwise selection will not work in LookupTypeHandler
        ((LookupImpl) lookup).setLookupFocusDegree(LookupFocusDegree.FOCUSED);
      }
      return Result.SELECT_ITEM_AND_FINISH_LOOKUP;
    }
    // '.' is used in path completion;
    // '{' for attributes in path;
    // '-' is valid in file names and attribute names;
    // '%' is valid in file names
    if ('.' == c || c == '{' || c == '-' || c == '%') {
      return Result.ADD_TO_PREFIX;
    }
    return null;
  }
}
