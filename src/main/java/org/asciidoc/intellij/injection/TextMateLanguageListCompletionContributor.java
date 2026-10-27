package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.textmate.TextMateService;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Offers the languages of the TextMate bundles for the language of a source block,
 * in addition to the languages the IDE provides in {@link LanguageListCompletionContributor}.
 * It is only registered when the TextMate plugin is available.
 */
public class TextMateLanguageListCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (!LanguageListCompletionContributor.isSourceLanguagePosition(parameters)) {
      return;
    }
    Set<String> ideLanguages = LanguageGuesser.getLangToLanguageMap().keySet();
    for (String name : findTextMateLanguageNames()) {
      if (!ideLanguages.contains(name)) {
        result.addElement(LookupElementBuilder.create(name).withTypeText("TextMate", true));
      }
    }
  }

  /**
   * Language names as used in the scope names of the bundles, for example "go" for "source.go".
   * Only names that the {@link TextMateCodeFenceInjector} can resolve are returned.
   */
  private static Set<String> findTextMateLanguageNames() {
    Set<String> names = new TreeSet<>();
    for (var scope : TextMateService.getInstance().getFileNameMatcherToScopeNameMapping().values()) {
      String scopeName = scope.toString().toLowerCase(Locale.US);
      for (String prefix : new String[]{"source.", "text."}) {
        if (scopeName.startsWith(prefix)) {
          String name = scopeName.substring(prefix.length());
          if (!name.isEmpty() && name.indexOf('.') < 0 && TextMateCodeFenceInjector.findTextMateExtension(name) != null) {
            names.add(name);
          }
        }
      }
    }
    return names;
  }
}
