package org.asciidoc.intellij.grazie;

import com.intellij.grazie.text.ProblemFilter;
import com.intellij.grazie.text.TextProblem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

public class AsciiDocGrazieProblemFilter extends ProblemFilter {

  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  @Override
  public boolean shouldIgnore(@NotNull TextProblem problem) {
    // start with the character just before the problem
    int start = problem.getHighlightRange().getStartOffset() - 1;
    while (start > 0 && problem.getText().charAt(start) == ' ') {
      // If we're at a whitespace, and the previous element is UNKNOWN, ignore this problem message.
      // This handles problems when parsing a text like "Foo bar. <<id>> should be modelled."
      // would ask for "should" to be capitalized as it is at the start of a sentence.
      PsiElement element = problem.getText().findPsiElementAt(start);
      if (element instanceof PsiWhiteSpace && element.getPrevSibling() != null) {
        if (languageSupport.getElementBehavior(element.getParent(), element) == AsciiDocLanguageSupport.Behavior.UNKNOWN) {
          return true;
        }
      }
      // repeat while we're at a whitespace
      start--;
    }
    return false;
  }
}
