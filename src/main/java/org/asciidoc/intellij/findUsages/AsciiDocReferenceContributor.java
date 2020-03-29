package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.namesValidator.AsciiDocRenameInputValidator;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationReference;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagInDocument;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInComment;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInDocument;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocSimpleFileReference;
import org.asciidoc.intellij.psi.AsciiDocTextItalic;
import org.asciidoc.intellij.psi.AsciiDocTextMono;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;

public class AsciiDocReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    final PsiElementPattern.Capture<AsciiDocAttributeReference> attributeReferenceCapture =
      psiElement(AsciiDocAttributeReference.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(attributeReferenceCapture, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                   @NotNull ProcessingContext
                                                     context) {
        int start = 0;
        PsiElement child = element.getFirstChild();
        while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_REF) {
          start += child.getTextLength();
          child = child.getNextSibling();
        }
        if (child != null) {
          return new PsiReference[]{
            new AsciiDocAttributeDeclarationReference(element, TextRange.create(start, start + child.getTextLength()))
          };
        } else {
          return new PsiReference[]{};
        }
      }
    });

    final PsiElementPattern.Capture<AsciiDocLink> linkCapture =
      psiElement(AsciiDocLink.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(linkCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                       context) {

          List<PsiReference> fileReferences = findFileReferences(element);
          List<PsiReference> references = new ArrayList<>(fileReferences);

          List<PsiReference> urlReferences = findUrlReferencesInLinks(element);
          references.addAll(urlReferences);

          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocAttributeDeclaration> attributeDeclaration =
      psiElement(AsciiDocAttributeDeclaration.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(attributeDeclaration,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findUrlReferencesInAttributeDefinition(element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<PsiElement> tagInPlaintext =
      PlatformPatterns.psiElement().withText(StandardPatterns.string().matches("(?s).*" + TAG_PATTERN_STR + ".*"));

    registrar.registerReferenceProvider(tagInPlaintext,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findTagInElement(element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocIncludeTagInDocument> tagInInclude =
      psiElement(AsciiDocIncludeTagInDocument.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(tagInInclude,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findTagInDocument((AsciiDocIncludeTagInDocument) element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocTextMono> monoCapture =
      psiElement(AsciiDocTextMono.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(monoCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findFilesInProject(element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocTextItalic> italicCapture =
      psiElement(AsciiDocTextItalic.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(italicCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findFilesInProject(element);
          return references.toArray(new PsiReference[0]);
        }
      });
  }

  private List<PsiReference> findFilesInProject(PsiElement element) {
    ArrayList<PsiReference> references = new ArrayList<>();
    references.add(new AsciiDocSimpleFileReference(element, TextRange.create(0, element.getTextLength())));
    return references;
  }

  private List<PsiReference> findFileReferences(PsiElement element) {
    if (element.getNode().findChildByType(AsciiDocTokenTypes.URL_LINK) != null) {
      return Collections.emptyList();
    }
    if (element.getChildren().length > 0 && element.getChildren()[0] instanceof AsciiDocAttributeReference) {
      AsciiDocAttributeReference ref = (AsciiDocAttributeReference) element.getChildren()[0];
      if (ref.getText().endsWith("-url}")) {
        return Collections.emptyList();
      }
      for (PsiReference reference : ref.getReferences()) {
        PsiElement resolve = reference.resolve();
        if (resolve instanceof AsciiDocAttributeDeclaration) {
          String attributeValue = ((AsciiDocAttributeDeclaration) resolve).getAttributeValue();
          if (attributeValue != null) {
            if (URL_IN_ATTRIBUTE_VAL.matcher(attributeValue).find()) {
              return Collections.emptyList();
            }
          }
        }
      }
    }
    TextRange range = AsciiDocLink.getBodyRange((AsciiDocLink) element);
    if (!range.isEmpty()) {
      String file = element.getText().substring(range.getStartOffset(), range.getEndOffset());
      String macroName = ((AsciiDocLink) element).getMacroName();
      int start = 0;
      int i = 0;
      if ("xref".equals(macroName) &&
        file.contains("@")) {
        return Collections.emptyList(); // Antora cross-references not supported at the moment
      }
      ArrayList<PsiReference> references = new ArrayList<>();
      if ("xref".equals(macroName)) {
        Matcher matcher = AsciiDocUtil.ANTORA_PREFIX_PATTERN.matcher(file);
        if (matcher.find()) {
          i += matcher.end();
          references.add(
            new AsciiDocFileReference(element, macroName, file.substring(0, start),
              TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i - 1),
              true, true, 1)
          );
          start = i;
        }
        matcher = AsciiDocUtil.ANTORA_FAMILY_PATTERN.matcher(file.substring(start));
        if (matcher.find()) {
          i += matcher.end();
          references.add(
            new AsciiDocFileReference(element, macroName, file.substring(0, start),
              TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i - 1),
              true, true, 1)
          );
          start = i;
        }
      }
      for (; i < file.length(); ++i) {
        if (file.charAt(i) == '/' || file.charAt(i) == '#') {
          references.add(
            new AsciiDocFileReference(element, macroName, file.substring(0, start),
              TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i),
              file.charAt(i) == '/')
          );
          start = i + 1;
        }
      }
      references.add(
        new AsciiDocFileReference(element, macroName, file.substring(0, start),
          TextRange.create(range.getStartOffset() + start, range.getStartOffset() + file.length()),
          false)
          .withAnchor((start > 0 && file.charAt(start - 1) == '#')
            // an xref can be only a block ID, then it is an anchor even without the # prefix
            || ("xref".equals(macroName) && start == 0
            && AsciiDocRenameInputValidator.BLOCK_ID_PATTERN.matcher(file).matches()
            && !file.contains(".")
            )
          )
      );
      return references;
    } else {
      return Collections.emptyList();
    }
  }

  private List<PsiReference> findUrlReferencesInLinks(PsiElement element) {
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.URL_LINK) {
      child = child.getNextSibling();
    }
    if (child != null) {
      String url = child.getText();
      url = url.replaceAll("\\+\\+\\+(.*)\\+\\+\\+", "$1");
      url = url.replaceAll("\\+\\+(.*)\\+\\+", "$1");
      return Collections.singletonList(new WebReference(
        element,
        TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength()),
        StringEscapeUtils.unescapeHtml4(url))
      );
    } else {
      return Collections.emptyList();
    }
  }

  public static final String TAG_PATTERN_STR = "\\b(tag|end)::([a-zA-Z0-9_-]*)\\[](?=$|[ \\r])";
  private static final Pattern TAG_PATTERN = Pattern.compile(TAG_PATTERN_STR);

  private List<PsiReference> findTagInElement(PsiElement element) {
    String text = element.getText();
    Matcher matcher = TAG_PATTERN.matcher(text);
    List<PsiReference> result = null;
    while (matcher.find()) {
      if (result == null) {
        result = new ArrayList<>();
      }
      result.add(new AsciiDocIncludeTagReferenceInComment(
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

  private List<PsiReference> findTagInDocument(AsciiDocIncludeTagInDocument element) {
    PsiElement child = element.getFirstChild();
    if (child == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new AsciiDocIncludeTagReferenceInDocument(
      element,
      TextRange.from(0, element.getTextLength()))
    );
  }

  public static final Pattern URL_IN_ATTRIBUTE_VAL = Pattern.compile("(https?|file|ftp|irc)://[^\\s\\[\\]<]*([^\\s\\[\\]])");

  private List<PsiReference> findUrlReferencesInAttributeDefinition(PsiElement element) {
    PsiElement child = element.getFirstChild();
    List<PsiReference> result = new ArrayList<>();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_VAL) {
      child = child.getNextSibling();
    }
    if (child != null) {
      Matcher urls = URL_IN_ATTRIBUTE_VAL.matcher(child.getText());
      while (urls.find()) {
        result.add(new WebReference(
          element,
          TextRange.create(child.getStartOffsetInParent() + urls.start(),
            child.getStartOffsetInParent() + urls.end()),
          StringEscapeUtils.unescapeHtml4(urls.group())
        ));
      }
    }
    return result;
  }

}
