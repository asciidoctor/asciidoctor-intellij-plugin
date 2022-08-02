package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
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
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagInDocument;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagReferenceInDocument;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocSimpleFileReference;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class AsciiDocReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    final PsiElementPattern.Capture<AsciiDocAttributeReference> attributeReferenceCapture =
      psiElement(AsciiDocAttributeReference.class);

    registrar.registerReferenceProvider(attributeReferenceCapture, new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
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
      psiElement(AsciiDocLink.class);

    registrar.registerReferenceProvider(linkCapture,
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
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
      psiElement(AsciiDocAttributeDeclaration.class);

    registrar.registerReferenceProvider(attributeDeclaration,
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
          List<PsiReference> references = findUrlReferencesInAttributeDefinition(element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocIncludeTagInDocument> tagInInclude =
      psiElement(AsciiDocIncludeTagInDocument.class);

    registrar.registerReferenceProvider(tagInInclude,
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
          List<PsiReference> references = findTagInDocument((AsciiDocIncludeTagInDocument) element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocTextQuoted> quotedCapture =
      psiElement(AsciiDocTextQuoted.class);

    registrar.registerReferenceProvider(quotedCapture,
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                               @NotNull ProcessingContext context) {
          List<PsiReference> references = findFilesInProject((AsciiDocTextQuoted) element);
          return references.toArray(new PsiReference[0]);
        }
      });

  }

  private List<PsiReference> findFilesInProject(AsciiDocTextQuoted element) {
    ArrayList<PsiReference> references = new ArrayList<>();
    if (!element.hasNestedQuotedText()) {
      references.add(new AsciiDocSimpleFileReference(element, AsciiDocTextQuoted.getBodyRange(element).shiftLeft(element.getNode().getStartOffset())));
    }
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
              || ("xref".equals(macroName) && start == 0
              // an xref can be only a block ID, then it is an anchor even without the # prefix
              && (AsciiDocRenameInputValidator.BLOCK_ID_PATTERN.matcher(file).matches()
              // or it is just a section heading, then it contains at least one blank and some uppercase letters
              || (file.contains(" ") && !file.toLowerCase().equals(file)
            ))
              && !file.contains(".")
            )
          )
      );
      return Collections.unmodifiableList(references);
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
