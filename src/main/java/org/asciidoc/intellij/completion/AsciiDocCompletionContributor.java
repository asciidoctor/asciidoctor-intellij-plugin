package org.asciidoc.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AsciiDocCompletionContributor extends CompletionContributor {
  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    super.beforeCompletion(context);

    int offset = context.getStartOffset();
    PsiElement element = context.getFile().findElementAt(offset);
    if (element != null) {
      if (element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_NAME) {
        // the identifier end offset needs to be set as otherwise an id containing a "-" will not be replaced
        context.getOffsetMap().addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, offset + element.getTextLength());
      }
    }
  }

  public AsciiDocCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTRIBUTE_NAME).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<CompletionParameters>() {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext,
                                   @NotNull CompletionResultSet resultSet) {
          List<String> builtInAttributesList = AsciiDocBundle.getBuiltInAttributesList();
          for (String attribute : builtInAttributesList) {
            resultSet.addElement(LookupElementBuilder.create(attribute)
              .withTypeText(AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + attribute + ".values"))
              .withPresentableText(attribute)
              .withInsertHandler((insertionContext, item) -> {
                // the finalizing : hasn't been entered yet, autocomplete it here
                int offset = insertionContext.getStartOffset();
                PsiElement element = insertionContext.getFile().findElementAt(offset);
                if (element != null && element.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_NAME) {
                  offset += attribute.length();
                  insertionContext.getDocument().insertString(offset, ":");
                  offset += 1;
                  insertionContext.getEditor().getCaretModel().moveToOffset(offset);
                }
              })
            );
          }
        }
      });

  }

}
