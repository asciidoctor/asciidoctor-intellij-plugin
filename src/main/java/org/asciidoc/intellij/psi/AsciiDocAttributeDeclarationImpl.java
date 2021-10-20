package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsciiDocAttributeDeclarationImpl
  extends AsciiDocAttributeDeclarationStubElementImpl<AsciiDocAttributeDeclarationStub>
  implements AsciiDocAttributeDeclaration {

  private static final Set<String> ATTRIBUTES_WITH_TEXT_CONTENT = new HashSet<>(Arrays.asList("description",
    "title", "doctitle", "navtitle", "reftext"));

  public AsciiDocAttributeDeclarationImpl(AsciiDocAttributeDeclarationStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public AsciiDocAttributeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  @Override
  public String getAttributeName() {
    AsciiDocAttributeDeclarationStub greenStub = getGreenStub();
    if (greenStub != null) {
      return greenStub.getAttributeName();
    }
    AsciiDocAttributeDeclarationName attributeName = findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
    if (attributeName != null) {
      return attributeName.getName();
    }
    return null;
  }

  /**
   * All attribute declarations within a document are soft by default.
   */
  @Override
  public boolean isSoft() {
    return true;
  }

  /**
   * Check if it is an unset attribute declaration (':attr!:').
   */
  public boolean isUnset() {
    return this.getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.ATTRIBUTE_UNSET)).length > 0;
  }

  private static final TokenSet CONTINUATION_TYPES = TokenSet.create(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION,
    AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY);

  @Nullable
  @Override
  public String getAttributeValue() {
    AsciiDocAttributeDeclarationStub greenStub = getGreenStub();
    if (greenStub != null) {
      String value = greenStub.getAttributeValue();
      // asciidoctorconfigdir might not be properly replaced during indexing, therefore don't use stub value
      if (value == null || !value.contains("{asciidoctorconfigdir}")) {
        return value;
      }
    }
    if (isUnset()) {
      return null;
    }
    ASTNode attributeValue = getNode().findChildByType(AsciiDocTokenTypes.ATTRIBUTE_NAME_END);
    if (attributeValue != null) {
      attributeValue = attributeValue.getTreeNext();
    }
    if (attributeValue != null) {
      StringBuilder sb = new StringBuilder();
      while (attributeValue != null) {
        if (CONTINUATION_TYPES.contains(attributeValue.getElementType())) {
          sb.append(" ");
        } else {
          sb.append(attributeValue.getText());
        }
        attributeValue = attributeValue.getTreeNext();
      }
      String val = sb.toString().trim();
      if (val.contains("{asciidoctorconfigdir}") &&
        (this.getContainingFile().getName().equals(".asciidoctorconfig") || this.getContainingFile().getName().equals(".asciidoctorconfig.adoc"))
        && this.getContainingFile().getVirtualFile() != null && this.getContainingFile().getVirtualFile().getParent() != null
        && this.getContainingFile().getVirtualFile().getParent().getCanonicalPath() != null) {
        val = val.replaceAll(
          Pattern.quote("{asciidoctorconfigdir}"),
          Matcher.quoteReplacement(this.getContainingFile().getVirtualFile().getParent().getCanonicalPath())
        );
      }
      return val;
    }
    // the attribute is defined, therefore its default value is an empty string
    return "";
  }

  @NotNull
  @Override
  public PsiElement getNavigationElement() {
    AsciiDocAttributeDeclarationName attributeName = getAttributeDeclarationName();
    if (attributeName != null) {
      return attributeName;
    }
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getNode().getElementType() + ")";
  }

  @Override
  public AsciiDocAttributeDeclarationName getAttributeDeclarationName() {
    return findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
  }

  public boolean hasSpellCheckableContent() {
    String name = getAttributeName();
    return ATTRIBUTES_WITH_TEXT_CONTENT.contains(name.toLowerCase(Locale.US));
  }
}
