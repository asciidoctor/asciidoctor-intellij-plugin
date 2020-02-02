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
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagInDocument;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInComment;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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
    if (element != null) {
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
        if (parent.getNode().getElementType() == AsciiDocElementTypes.BLOCK_MACRO || parent.getNode().getElementType() == AsciiDocElementTypes.INLINE_MACRO
          || parent.getNode().getElementType() == AsciiDocElementTypes.ATTRIBUTE_IN_BRACKETS) {
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
      new CompletionProvider<CompletionParameters>() {
        @Override
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext,
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
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(AsciiDocTokenTypes.ATTR_VALUE).withLanguage(AsciiDocLanguage.INSTANCE),
      new CompletionProvider<CompletionParameters>() {
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
                    Collection<PsiComment> psiComments = PsiTreeUtil.findChildrenOfType(resolve, PsiComment.class);
                    for (PsiComment psiComment : psiComments) {
                      PsiReference[] commentReferences = psiComment.getReferences();
                      for (PsiReference commentReference : commentReferences) {
                        if (commentReference instanceof AsciiDocIncludeTagReferenceInComment) {
                          AsciiDocIncludeTagReferenceInComment reference = (AsciiDocIncludeTagReferenceInComment) commentReference;
                          if (!ids.contains(reference.getValue())) {
                            resultSet.addElement(LookupElementBuilder.create(reference.getValue())
                              .withPresentableText(reference.getValue())
                            );
                            ids.add(reference.getValue());
                          }
                        }
                      }
                    }
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
