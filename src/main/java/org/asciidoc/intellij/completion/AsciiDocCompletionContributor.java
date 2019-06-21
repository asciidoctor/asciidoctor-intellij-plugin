package org.asciidoc.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import java.util.List;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

public class AsciiDocCompletionContributor extends CompletionContributor {
  public AsciiDocCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().afterLeaf(":").withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<CompletionParameters>() {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
          @NotNull CompletionResultSet resultSet) {
          List<String> builtInAttributesList = AsciiDocBundle.getBuiltInAttributesList();
          for (String attribute : builtInAttributesList) {
            resultSet.addElement(LookupElementBuilder.create(attribute + ":")
              .withTypeText(AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + attribute + ".values"))
              .withPresentableText(attribute));
          }
        }
      });

  }

}
