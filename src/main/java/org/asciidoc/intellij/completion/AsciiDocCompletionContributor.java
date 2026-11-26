package org.asciidoc.intellij.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.OffsetKey;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.macros.AsciiDocMacroAttributes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagInDocument;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInElement;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsciiDocCompletionContributor extends CompletionContributor {
  public static final OffsetKey IDENTIFIER_FILE_REFERENCE = OffsetKey.create("fileReferenceEnd");

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    super.beforeCompletion(context);

    int offset = context.getStartOffset();
    PsiElement element = context.getFile().findElementAt(offset);
    if (element != null && element.getNode() != null) {
      if (element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_NAME) {
        // the identifier end offset needs to be set as otherwise an id containing a "-" will not be replaced
        context.getOffsetMap().addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, element.getTextOffset() + element.getTextLength());
      }
      if (element.getNode().getElementType() == AsciiDocTokenTypes.ATTR_VALUE) {
        // the identifier end offset needs to be set as otherwise an id containing a "-" will not be replaced
        context.getOffsetMap().addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, element.getTextOffset() + element.getTextLength());
      }
      if (element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_REF) {
        PsiElement parent = element.getParent().getParent();
        // help the autocomplete in AsciiDocFileReference to replace the full reference if it contains a nested attribute
        if (parent.getNode() != null && (parent.getNode().getElementType() == AsciiDocElementTypes.BLOCK_MACRO || parent.getNode().getElementType() == AsciiDocElementTypes.INLINE_MACRO
          || parent.getNode().getElementType() == AsciiDocElementTypes.ATTRIBUTE_IN_BRACKETS)) {
          for (PsiReference reference : parent.getReferences()) {
            if (reference.getRangeInElement().shiftRight(parent.getTextOffset()).contains(element.getTextRange())) {
              context.getOffsetMap().addOffset(IDENTIFIER_FILE_REFERENCE, parent.getTextOffset() + reference.getRangeInElement().getEndOffset());
            }
          }
        }
      }
    }
  }

  public AsciiDocCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTRIBUTE_NAME).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<>() {
        @Override
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext,
                                   @NotNull CompletionResultSet resultSet) {
          List<String> builtInAttributesList = AsciiDocBundle.getBuiltInAttributesList();
          for (String attribute : builtInAttributesList) {
            resultSet.addElement(LookupElementBuilder.create(attribute)
              .withTypeText(AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + attribute + ".values"))
              .withPresentableText(attribute)
              .withCaseSensitivity(false)
              .withInsertHandler((insertionContext, item) -> {
                int offset = insertionContext.getStartOffset();
                PsiElement element = insertionContext.getFile().findElementAt(offset);
                if (element != null) {
                  if (element.getNode() != null && element.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_NAME) {
                    // the finalizing : hasn't been entered yet, autocomplete it here
                    offset += attribute.length();
                    insertionContext.getDocument().insertString(offset, ":");
                    offset += 1;
                    insertionContext.getEditor().getCaretModel().moveToOffset(offset);
                  } else {
                    // the finalizing : is already there
                    offset += attribute.length();
                    offset += 1;
                    insertionContext.getEditor().getCaretModel().moveToOffset(offset);
                  }
                }
              })
            );
          }
        }
      });
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTR_NAME).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<>() {
        @Override
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext,
                                   @NotNull CompletionResultSet resultSet) {
          PsiElement position = parameters.getPosition();
          PsiElement macroElement = AsciiDocMacroAttributes.getMacroElement(position);
          AsciiDocMacroAttributes.Macro macro = AsciiDocMacroAttributes.find(macroElement);
          if (macroElement == null || macro == null) {
            return;
          }
          if (position.getParent() instanceof AsciiDocAttributeInBrackets attribute
            && AsciiDocMacroAttributes.isNamed(attribute)) {
            // completing the value of a named attribute
            return;
          }
          if (position.getParent() instanceof AsciiDocAttributeInBrackets attribute) {
            // a positional attribute can be a value of the attribute at this position
            int index = AsciiDocMacroAttributes.getPosition(attribute);
            if (index >= 0 && index < macro.positional().size()) {
              AsciiDocMacroAttributes.Attribute positional = macro.find(macro.positional().get(index));
              if (positional != null) {
                for (String value : positional.suggestions()) {
                  resultSet.addElement(LookupElementBuilder.create(
                      new AsciiDocMacroAttributes.AttributeRef(macro, positional, value), value).withCaseSensitivity(false)
                    .withTypeText(positional.name(), true));
                }
              }
            }
          }
          Set<String> used = AsciiDocMacroAttributes.getUsedNames(macroElement, position.getParent() instanceof AsciiDocAttributeInBrackets ? position.getParent() : null);
          for (AsciiDocMacroAttributes.Attribute attribute : macro.attributes()) {
            if (used.contains(attribute.name())) {
              continue;
            }
            resultSet.addElement(LookupElementBuilder.create(new AsciiDocMacroAttributes.AttributeRef(macro, attribute), attribute.name())
              .withTypeText(String.join(", ", attribute.suggestions()), true)
              .withCaseSensitivity(false)
              .withInsertHandler((insertionContext, item) -> {
                int end = insertionContext.getTailOffset();
                CharSequence text = insertionContext.getDocument().getCharsSequence();
                if (end >= text.length() || text.charAt(end) != '=') {
                  insertionContext.getDocument().insertString(end, "=");
                }
                insertionContext.getEditor().getCaretModel().moveToOffset(end + 1);
              }));
          }
        }
      });
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTR_VALUE).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<>() {
        @Override
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext,
                                   @NotNull CompletionResultSet resultSet) {
          PsiElement position = parameters.getPosition();
          if (!(position.getParent() instanceof AsciiDocAttributeInBrackets attribute)) {
            return;
          }
          PsiElement macroElement = AsciiDocMacroAttributes.getMacroElement(position);
          AsciiDocMacroAttributes.Macro macro = AsciiDocMacroAttributes.find(macroElement);
          String name = AsciiDocMacroAttributes.getName(attribute);
          if (macro == null || name == null) {
            return;
          }
          AsciiDocMacroAttributes.Attribute documented = macro.find(name);
          if (documented == null) {
            return;
          }
          for (String value : documented.suggestions()) {
            resultSet.addElement(LookupElementBuilder.create(
              new AsciiDocMacroAttributes.AttributeRef(macro, documented, value), value).withCaseSensitivity(false));
          }
        }
      });
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTR_VALUE).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<>() {
        @Override
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext,
                                   @NotNull CompletionResultSet resultSet) {
          PsiElement parent = parameters.getPosition().getParent();
          if (!(parent instanceof AsciiDocIncludeTagInDocument)) {
            return;
          }
          parent = parent.getParent();
          Set<String> ids = new HashSet<>();
          if (parent != null) {
            PsiElement blockMacro = parent.getParent();
            if (blockMacro != null) {
              PsiReference[] references = blockMacro.getReferences();
              for (int i = references.length - 1; i >= 0; i--) {
                if (references[i] instanceof AsciiDocFileReference) {
                  PsiElement resolve = references[i].resolve();
                  if (resolve != null) {
                    PsiTreeUtil.processElements(resolve.getContainingFile(), element -> {
                      for (PsiReference reference : element.getReferences()) {
                        if (reference instanceof AsciiDocIncludeTagReferenceInElement tagReference) {
                          if (!ids.contains(tagReference.getValue())) {
                            resultSet.addElement(LookupElementBuilder.create(tagReference.getValue())
                              .withCaseSensitivity(false)
                              .withPresentableText(tagReference.getValue())
                            );
                            ids.add(tagReference.getValue());
                          }
                        }
                      }
                      return true;
                    });
                  }
                  // only the last file reference is the one with the file
                  // any preceding will be a directory that could contain many children with comments
                  break;
                }
              }
            }
          }
        }
      });

  }

}
