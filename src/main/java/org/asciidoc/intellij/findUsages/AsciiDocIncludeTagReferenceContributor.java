package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.ValuePatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.patterns.StringPattern.newBombedCharSequence;

public class AsciiDocIncludeTagReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    final PsiElementPattern.Capture<PsiElement> tagInPlaintext =
      PlatformPatterns.psiElement().with(new PatternCondition<>("onlyLeafs") {
        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
          // for plain text files, only the file appears here, not its element
          if (psiElement instanceof PsiFile) {
            PsiFile file = (PsiFile) psiElement;
            VirtualFile virtualFile = ((PsiFile) psiElement).getVirtualFile();
            if (virtualFile == null) {
              virtualFile = file.getOriginalFile().getVirtualFile();
            }
            // offer refactoring of includes only for plain text files smaller than 200k to avoid performance problems when editing
            if (virtualFile != null && virtualFile.getLength() > 200L * 1024L) {
              return false;
            }
          }
          // restricting this to leaf elements only avoid parsing the same text multiple times
          // all comments where these texts usually occur are LeafElements
          return psiElement instanceof LeafElement || psiElement instanceof PsiFile;
        }
      }).withText(StandardPatterns.string().with(new ValuePatternCondition<>("find") {
        @Override
        public boolean accepts(@NotNull final String str, final ProcessingContext context) {
          return TAG_PATTERN.matcher(newBombedCharSequence(str)).find();
        }

        @Override
        public Collection<String> getValues() {
          return Collections.singleton("(?s).*" + TAG_PATTERN_STR + ".*");
        }
      }));

    registrar.registerReferenceProvider(tagInPlaintext,
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
          List<PsiReference> references = findTagInElement(element);
          return references.toArray(new PsiReference[0]);
        }
      });

  }

  public static final String TAG_PATTERN_STR = "\\b(tag|end)::([a-zA-Z0-9_-]*)\\[](?=$|[ \\n])";
  public static final Pattern TAG_PATTERN = Pattern.compile(TAG_PATTERN_STR);

  private List<PsiReference> findTagInElement(PsiElement element) {
    String text = element.getText();
    Matcher matcher = TAG_PATTERN.matcher(text);
    List<PsiReference> result = null;
    while (matcher.find()) {
      if (result == null) {
        result = new ArrayList<>();
      }
      result.add(new AsciiDocIncludeTagReferenceInElement(
        element,
        TextRange.create(matcher.start(2), matcher.end(2)),
        matcher.group(1))
      );
    }
    if (result == null) {
      return Collections.emptyList();
    } else {
      return result;
    }
  }

}
