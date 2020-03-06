package org.asciidoc.intellij.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationName;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationReference;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocSelfDescribe;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AsciiDocFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
  private static final Map<String, String> COLLAPSABLE_ATTRIBUTES = new HashMap<>();

  // https://asciidoctor.org/docs/user-manual/#charref-attributes
  // only fold those pre-defined attributes that have visible characters
  // that is: leaving out blank, zwsp and wj
  static {
    COLLAPSABLE_ATTRIBUTES.put("sp", " ");
    COLLAPSABLE_ATTRIBUTES.put("nbsp", " ");
    COLLAPSABLE_ATTRIBUTES.put("apos", "'");
    COLLAPSABLE_ATTRIBUTES.put("quot", "\"");
    COLLAPSABLE_ATTRIBUTES.put("lsquo", "\u2018");
    COLLAPSABLE_ATTRIBUTES.put("rsquo", "\u2019");
    COLLAPSABLE_ATTRIBUTES.put("ldquo", "\u201c");
    COLLAPSABLE_ATTRIBUTES.put("rdquo", "\u2018");
    COLLAPSABLE_ATTRIBUTES.put("deg", "°");
    COLLAPSABLE_ATTRIBUTES.put("plus", "+");
    COLLAPSABLE_ATTRIBUTES.put("brvbar", "¦");
    COLLAPSABLE_ATTRIBUTES.put("vbar", "|");
    COLLAPSABLE_ATTRIBUTES.put("amp", "&");
    COLLAPSABLE_ATTRIBUTES.put("lt", "<");
    COLLAPSABLE_ATTRIBUTES.put("gt", ">");
    COLLAPSABLE_ATTRIBUTES.put("startsb", "[");
    COLLAPSABLE_ATTRIBUTES.put("endsb", "]");
    COLLAPSABLE_ATTRIBUTES.put("caret", "^");
    COLLAPSABLE_ATTRIBUTES.put("asterisk", "*");
    COLLAPSABLE_ATTRIBUTES.put("tilde", "~");
    COLLAPSABLE_ATTRIBUTES.put("backslash", "\\");
    COLLAPSABLE_ATTRIBUTES.put("backtick", "`");
    COLLAPSABLE_ATTRIBUTES.put("two-colons", "::");
    COLLAPSABLE_ATTRIBUTES.put("two-semicolons", ";;");
    COLLAPSABLE_ATTRIBUTES.put("cpp", "C++");
  }

  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    boolean attributeFoldingEnabled = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isAttributeFoldingEnabled();
    root.accept(new AsciiDocVisitor() {

      @Override
      public void visitElement(PsiElement element) {
        if (attributeFoldingEnabled && element instanceof AsciiDocAttributeReference) {
          if (!element.getText().toLowerCase().endsWith("dir}")) {
            // avoid replacing imagesdir, partialsdir, attachmentdir, etc. as this would be too verbose
            addDescriptors(element);
          }
        }
        super.visitElement(element);
        element.acceptChildren(this);
      }

      @Override
      public void visitSections(@NotNull AsciiDocSection section) {
        addDescriptors(section);
        super.visitSections(section);
      }

      @Override
      public void visitBlocks(@NotNull AsciiDocBlock block) {
        addDescriptors(block);
        super.visitBlocks(block);
      }

      private void addDescriptors(@NotNull PsiElement element) {
        AsciiDocFoldingBuilder.addDescriptors(element, element.getTextRange(), descriptors, document);
      }
    });

    root.accept(new AsciiDocVisitor() {
      @Override
      public void visitSections(@NotNull AsciiDocSection header) {
        super.visitSections(header);
      }

      @Override
      public void visitBlocks(@NotNull AsciiDocBlock block) {
        super.visitBlocks(block);
      }
    });
  }

  private static void addDescriptors(@NotNull PsiElement element,
                                     @NotNull TextRange range,
                                     @NotNull List<? super FoldingDescriptor> descriptors,
                                     @NotNull Document document) {
    if (document.getLineNumber(range.getStartOffset()) != document.getLineNumber(range.getEndOffset() - 1)) {
      descriptors.add(new FoldingDescriptor(element, range));
    } else if (element instanceof AsciiDocAttributeReference) {
      descriptors.add(new FoldingDescriptor(element, range));
    }
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    String title;
    if (node.getPsi() instanceof AsciiDocSelfDescribe) {
      title = ((AsciiDocSelfDescribe) node.getPsi()).getFoldedSummary();
      title = StringUtil.shortenTextWithEllipsis(title, 50, 5);
      title += " ...";
    } else if (node.getPsi() instanceof AsciiDocAttributeReference) {
      String text = node.getText();
      if (text.startsWith("{") && text.endsWith("}")) {
        title = COLLAPSABLE_ATTRIBUTES.get(text.substring(1, text.length() - 1));
        if (title == null && !DumbService.isDumb(node.getPsi().getProject())) {
          // checking dumb mode to avoid IndexNotReadyException
          Set<String> values = new HashSet<>();
          iterateReferences:
          for (PsiReference reference : node.getPsi().getReferences()) {
            if (reference instanceof AsciiDocAttributeDeclarationReference) {
              for (ResolveResult resolveResult : ((AsciiDocAttributeDeclarationReference) reference).multiResolve(false)) {
                PsiElement element = resolveResult.getElement();
                if (element instanceof AsciiDocAttributeDeclarationName) {
                  PsiElement parent = element.getParent();
                  if (parent instanceof AsciiDocAttributeDeclaration) {
                    values.add(((AsciiDocAttributeDeclaration) parent).getAttributeValue());
                    if (values.size() > 1) {
                      break iterateReferences;
                    }
                  }
                }
              }
            }
          }
          if (values.size() == 1) {
            title = values.iterator().next();
          } else {
            title = text;
          }
        }
      } else {
        title = text;
      }
    } else {
      title = StringUtil.shortenTextWithEllipsis(node.getText(), 50, 5);
    }
    return title;
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return node.getPsi() instanceof AsciiDocAttributeReference;
  }
}
