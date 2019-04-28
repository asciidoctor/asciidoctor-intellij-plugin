/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.asciidoc.intellij.braces;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocBraceMatcher extends PairedBraceMatcherAdapter {

  public AsciiDocBraceMatcher() {
    super(new MyPairedBraceMatcher(), AsciiDocLanguage.INSTANCE);
  }

  private static class MyPairedBraceMatcher implements PairedBraceMatcher {

    @NotNull
    @Override
    public BracePair[] getPairs() {
      return new BracePair[]{
        new BracePair(AsciiDocTokenTypes.BLOCK_ATTRS_START, AsciiDocTokenTypes.BLOCK_ATTRS_END, true),
        new BracePair(AsciiDocTokenTypes.LPAREN, AsciiDocTokenTypes.RPAREN, false),
        new BracePair(AsciiDocTokenTypes.LBRACKET, AsciiDocTokenTypes.RBRACKET, false),
        new BracePair(AsciiDocTokenTypes.LT, AsciiDocTokenTypes.GT, false),
        // TODO: doesn't work well as there are leading blanks in a line that are highlighted as well
//        new BracePair(AsciiDocTokenTypes.BOLD_START, AsciiDocTokenTypes.BOLD_END, true),
//        new BracePair(AsciiDocTokenTypes.ITALIC_START, AsciiDocTokenTypes.ITALIC_END, true),
//        new BracePair(AsciiDocTokenTypes.MONO_START, AsciiDocTokenTypes.MONO_END, true),
      };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, IElementType type) {
      return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
      return openingBraceOffset;
    }
  }
}
