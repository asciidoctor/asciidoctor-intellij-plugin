package org.asciidoc.intellij;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.macros.AsciiDocMacroAttributes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import javax.swing.*;

public class AsciiDocDocumentationProvider extends AbstractDocumentationProvider {
  @Nullable
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (object instanceof String string) {
      return new DummyElement(string, psiManager);
    }
    if (object instanceof AsciiDocMacroAttributes.AttributeRef ref) {
      return new DummyElement(ref, psiManager);
    }
    return null;
  }

  @Nullable
  @Override
  public PsiElement getCustomDocumentationElement(@NotNull final Editor editor,
                                                  @NotNull final PsiFile file,
                                                  @Nullable final PsiElement contextElement,
                                                  final int targetOffset) {
    if (contextElement != null && contextElement.getNode() != null
      && (contextElement.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_MACRO_ID
      || contextElement.getNode().getElementType() == AsciiDocTokenTypes.INLINE_MACRO_ID
      || contextElement.getNode().getElementType() == AsciiDocTokenTypes.LINKSTART)) {
      AsciiDocMacroAttributes.Macro macro = AsciiDocMacroAttributes.find(contextElement.getParent());
      if (macro != null) {
        return new DummyElement(macro, file.getManager());
      }
    }
    if (contextElement != null && contextElement.getNode() != null
      && (contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTR_NAME
      || contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTR_VALUE
      || contextElement.getNode().getElementType() == AsciiDocTokenTypes.ASSIGNMENT)
      && contextElement.getParent() instanceof AsciiDocAttributeInBrackets attributeInBrackets) {
      AsciiDocMacroAttributes.AttributeRef ref = AsciiDocMacroAttributes.getAttributeRef(attributeInBrackets);
      if (ref != null) {
        return new DummyElement(ref.withValue(contextElement.getText()), file.getManager());
      }
    }
    if (contextElement != null && contextElement.getNode() != null &&
      (contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_NAME ||
        contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_REF)) {
      String key = contextElement.getNode().getText();
      if (AsciiDocBundle.getBuiltInAttributesList().contains(key)) {
        return new DummyElement(key, file.getManager());
      }
    }
    PsiElement lookingForAttribute = contextElement;
    while (lookingForAttribute != null) {
      if (lookingForAttribute instanceof PsiWhiteSpace && lookingForAttribute.getPrevSibling() instanceof AsciiDocAttributeDeclaration) {
        lookingForAttribute = lookingForAttribute.getPrevSibling();
      }
      if (lookingForAttribute instanceof AsciiDocAttributeDeclaration) {
        String key = ((AsciiDocAttributeDeclaration) lookingForAttribute).getAttributeName();
        if (AsciiDocBundle.getBuiltInAttributesList().contains(key)) {
          return new DummyElement(key, file.getManager());
        } else {
          break;
        }
      }
      if (lookingForAttribute instanceof PsiFile) {
        break;
      }
      lookingForAttribute = lookingForAttribute.getParent();
    }
    return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
  }

  @Override
  public String generateDoc(final PsiElement element, @Nullable final PsiElement originalElement) {
    if (element instanceof DummyElement el && el.getMacro() != null) {
      return generateMacroDoc(el.getMacro());
    }
    if (element instanceof DummyElement el && el.getRef() != null) {
      return generateMacroAttributeDoc(el.getRef());
    }
    if (element instanceof DummyElement el) {
      if (AsciiDocBundle.containsAttribute(el.getKey())) {
        String defaultValue = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".default-value");
        defaultValue = StringEscapeUtils.escapeHtml4(defaultValue);
        String values = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".values");
        values = StringEscapeUtils.escapeHtml4(values);
        String html = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".text");
        html = StringEscapeUtils.escapeHtml4(html);
        if (values != null) {
          html += "<br/><b>" + AsciiDocBundle.message("asciidoc.attributes.values") + ":</b> " + values;
        }
        if (defaultValue != null) {
          html += "<br/><b>" + AsciiDocBundle.message("asciidoc.attributes.default-value") + ":</b> " + defaultValue;
        }
        return html;
      }
    }
    return null;
  }

  private static String generateMacroDoc(AsciiDocMacroAttributes.Macro macro) {
    StringBuilder html = new StringBuilder();
    html.append("<b>").append(StringEscapeUtils.escapeHtml4(macro.name() + (macro.block() ? "::" : ":"))).append("</b><br/>");
    html.append(StringEscapeUtils.escapeHtml4(macro.description()));
    if (!macro.positional().isEmpty()) {
      html.append("<br/><b>").append(AsciiDocBundle.message("asciidoc.macro.positional.attributes")).append(":</b> ")
        .append(StringEscapeUtils.escapeHtml4(String.join(", ", macro.positional())));
    }
    if (!macro.attributes().isEmpty()) {
      html.append("<br/><b>").append(AsciiDocBundle.message("asciidoc.macro.named.attributes")).append(":</b> ")
        .append(StringEscapeUtils.escapeHtml4(macro.attributes().stream()
          .map(AsciiDocMacroAttributes.Attribute::name).collect(Collectors.joining(", "))));
    }
    html.append("<br/><a href=\"").append(macro.docUrl()).append("\">")
      .append(AsciiDocBundle.message("asciidoc.macro.attribute.read.more")).append("</a>");
    return html.toString();
  }

  private static String generateMacroAttributeDoc(AsciiDocMacroAttributes.AttributeRef ref) {
    AsciiDocMacroAttributes.Macro macro = ref.macro();
    AsciiDocMacroAttributes.Attribute attribute = ref.attribute();
    String macroName = macro.name() + (macro.block() ? "::" : ":");
    StringBuilder html = new StringBuilder();
    if (ref.value() != null) {
      html.append("<b>").append(StringEscapeUtils.escapeHtml4(ref.value())).append("</b>");
      html.append(" (").append(StringEscapeUtils.escapeHtml4(attribute.name())).append(", ")
        .append(StringEscapeUtils.escapeHtml4(macroName)).append(")<br/>");
      String valueDescription = ref.getValueDescription();
      html.append(StringEscapeUtils.escapeHtml4(valueDescription != null ? valueDescription : attribute.description()));
      html.append("<br/><a href=\"").append(macro.docUrl()).append("\">")
        .append(AsciiDocBundle.message("asciidoc.macro.attribute.read.more")).append("</a>");
      return html.toString();
    }
    html.append("<b>").append(StringEscapeUtils.escapeHtml4(attribute.name())).append("</b>");
    html.append(" (").append(StringEscapeUtils.escapeHtml4(macroName)).append(")<br/>");
    html.append(StringEscapeUtils.escapeHtml4(attribute.description()));
    if (!attribute.values().isEmpty()) {
      html.append("<br/><b>").append(AsciiDocBundle.message("asciidoc.attributes.values")).append(":</b> ")
        .append(StringEscapeUtils.escapeHtml4(String.join(", ", attribute.suggestions())));
    }
    int position = macro.positionOf(attribute.name());
    if (position >= 0) {
      html.append("<br/>").append(StringEscapeUtils.escapeHtml4(
        AsciiDocBundle.message("asciidoc.macro.attribute.positional", position + 1, attribute.name())));
    }
    html.append("<br/><a href=\"").append(macro.docUrl()).append("\">")
      .append(AsciiDocBundle.message("asciidoc.macro.attribute.read.more")).append("</a>");
    return html.toString();
  }

  private static class DummyElement extends FakePsiElement {
    @NotNull
    private final PsiManager myPsiManager;
    @NotNull
    private final DummyHolder myDummyHolder;
    @NotNull
    private final String key;
    @Nullable
    private final AsciiDocMacroAttributes.AttributeRef ref;
    @Nullable
    private AsciiDocMacroAttributes.Macro macro;

    DummyElement(@NotNull AsciiDocMacroAttributes.Macro macro, @NotNull PsiManager psiManager) {
      this(macro.name(), psiManager, null);
      this.macro = macro;
    }

    DummyElement(@NotNull AsciiDocMacroAttributes.AttributeRef ref, @NotNull PsiManager psiManager) {
      this(ref.value() != null ? ref.value() : ref.attribute().name(), psiManager, ref);
    }

    DummyElement(@NotNull String key, @NotNull PsiManager psiManager) {
      this(key, psiManager, null);
    }

    private DummyElement(@NotNull String key, @NotNull PsiManager psiManager, @Nullable AsciiDocMacroAttributes.AttributeRef ref) {
      this.ref = ref;
      myPsiManager = psiManager;
      myDummyHolder = DummyHolderFactory.createHolder(myPsiManager, null);
      while (key.endsWith(":")) {
        key = key.substring(0, key.lastIndexOf(":"));
      }
      this.key = key;
    }

    @Nullable
    public AsciiDocMacroAttributes.Macro getMacro() {
      return macro;
    }

    @Nullable
    public AsciiDocMacroAttributes.AttributeRef getRef() {
      return ref;
    }

    @NotNull
    public String getKey() {
      return key;
    }

    @Override
    public PsiElement getParent() {
      return myDummyHolder;
    }

    @Override
    public ItemPresentation getPresentation() {
      return new ItemPresentation() {
        @Override
        public String getPresentableText() {
          return key;
        }

        @Nullable
        @Override
        public String getLocationString() {
          return null;
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
          return null;
        }
      };
    }

    @Override
    public PsiManager getManager() {
      return myPsiManager;
    }
  }
}
