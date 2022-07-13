package org.asciidoc.intellij.findUsages;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;
import org.jetbrains.yaml.psi.impl.YAMLQuotedTextImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;
import static com.intellij.patterns.PlatformPatterns.virtualFile;

/**
 * Provide auto-complete popup for elements in an Antora component descriptor.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAntoraYamlReferenceContributor extends PsiReferenceContributor {
  private static final Logger LOG = Logger.getInstance(AsciiDocAntoraYamlReferenceContributor.class);

  private static volatile boolean notificationShown;

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    if (notificationShown) {
      return;
    }

    // support nav elements with a file reference

    try {

      final PsiElementPattern.Capture<YAMLScalar> navCapture =
        psiElement(YAMLScalar.class)
          .inside(psiElement(YAMLSequenceItem.class))
          .inFile(psiFile(YAMLFile.class))
          .inVirtualFile(virtualFile().withName("antora.yml"));
      registrar.registerReferenceProvider(navCapture,
        new PsiReferenceProvider() {
          @Override
          public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
            List<PsiReference> references = findNavReferencesElement((YAMLScalar) element);
            return references.toArray(new PsiReference[0]);
          }
        });

      // support start_page element with an Antora reference

      final PsiElementPattern.Capture<YAMLScalar> startPageCapture =
        psiElement(YAMLScalar.class)
          .inFile(psiFile(YAMLFile.class))
          .inVirtualFile(virtualFile().withName("antora.yml"));
      registrar.registerReferenceProvider(startPageCapture,
        new PsiReferenceProvider() {
          @Override
          public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
            List<PsiReference> references = findStartPageReferencesElement((YAMLScalar) element);
            return references.toArray(new PsiReference[0]);
          }
        });
    } catch (Throwable ex) {
      // this might fail due to the optional dependency being loaded after the AsciiDoc plugin in the wrong classloader
      // https://youtrack.jetbrains.com/issue/IDEA-287090/
      LOG.warn("Unable to register AsciiDocAntoraYamlReferenceContributor", ex);
      if (!notificationShown) {
        synchronized (AsciiDocAntoraYamlReferenceContributor.class) {
          Notification notification = AsciiDoc.getNotificationGroup().createNotification("Error initializing YAML support for Antora",
            "Please restart your IDE to finish the initialization of Antora YAML support in the AsciiDoc plugin.",
            NotificationType.WARNING);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
          notificationShown = true;
        }
      }
    }

  }

  private List<PsiReference> findNavReferencesElement(YAMLScalar element) {
    ArrayList<PsiReference> references = new ArrayList<>();
    String macroName = null;

    // find out if we are in an element as part 'nav'
    if (element.getParent() != null && element.getParent().getParent() != null) {
      PsiElement parent = element.getParent().getParent().getParent();
      if (parent instanceof YAMLKeyValueImpl) {
        PsiElement key = ((YAMLKeyValueImpl) parent).getKey();
        if (key != null && key.getText().equals("nav")) {
          macroName = "antora-nav";
        }
      }
    }

    if (macroName == null) {
      return references;
    }

    int i = 0;
    int start = 0;
    TextRange range = calcRange(element);
    if (range == null) {
      return references;
    }

    String file = range.substring(element.getText());
    for (; i < file.length(); ++i) {
      if (file.charAt(i) == '/') {
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
    );
    return references;
  }


  private List<PsiReference> findStartPageReferencesElement(YAMLScalar element) {
    ArrayList<PsiReference> references = new ArrayList<>();
    String macroName = null;

    // find out if we are in an element as part 'start_page'
    PsiElement parent = element.getParent();
    if (parent instanceof YAMLKeyValueImpl) {
      PsiElement key = ((YAMLKeyValueImpl) parent).getKey();
      if (key != null && key.getText().equals("start_page")) {
        macroName = "antora-startpage";
      }
    }

    if (macroName == null) {
      return references;
    }

    int i = 0;
    int start = 0;
    TextRange range = calcRange(element);
    if (range == null) {
      return references;
    }

    String file = range.substring(element.getText());
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
    for (; i < file.length(); ++i) {
      if (file.charAt(i) == '/') {
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
    );
    return references;
  }

  private TextRange calcRange(YAMLScalar element) {
    if (element instanceof YAMLPlainTextImpl) {
      List<TextRange> contentRanges = ((YAMLPlainTextImpl) element).getContentRanges();
      if (contentRanges.size() == 1) {
        return contentRanges.get(0);
      } else {
        return null;
      }
    } else if (element instanceof YAMLQuotedTextImpl) {
      List<TextRange> contentRanges = ((YAMLQuotedTextImpl) element).getContentRanges();
      if (contentRanges.size() == 1) {
        return contentRanges.get(0);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
