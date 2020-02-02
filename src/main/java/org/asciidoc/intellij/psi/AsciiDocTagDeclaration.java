package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * AsciiDoc declaration of a tag used for includes.
 *
 * This is FakePsiElement as it doesn't really exist: the thing that exists once is the comment in the source file
 * with the tag::name[] -- this is a PsiComment, and no real PSI element exists here.
 * The start and end tag and several include statements in AsciiDoc files might reference it.
 */
public class AsciiDocTagDeclaration extends FakePsiElement implements PsiNameIdentifierOwner {

  private final AsciiDocIncludeTagReferenceInComment ref;

  public AsciiDocTagDeclaration(AsciiDocIncludeTagReferenceInComment ref) {
    this.ref = ref;
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return another instanceof AsciiDocTagDeclaration && ref == ((AsciiDocTagDeclaration) another).ref;
  }

  @Override
  public String getName() {
    return ref.getRangeInElement().substring(ref.getElement().getText());
  }

  @Override
  public int getTextOffset() {
    return ref.getElement().getTextOffset() + ref.getRangeInElement().getStartOffset();
  }

  @Override
  public PsiElement getParent() {
    return ref.getElement();
  }

  @Nullable
  @Override
  public PsiElement getNameIdentifier() {
    return this;
  }
}
