package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * AsciiDoc declaration of a tag used for includes.
 * <p>
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
    return another instanceof AsciiDocTagDeclaration
      && ref == ((AsciiDocTagDeclaration) another).ref
      && ref.getKey().equals(((AsciiDocTagDeclaration) another).ref.getKey());
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
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    PsiElement element = ref.getElement();
    // handle the special case when the reference is a plaintext file, in this case IntelliJ will not rename references automatically
    if (element instanceof PsiFile && element.getFirstChild() instanceof LeafElement && element.getChildren().length == 1) {
      // replace the occurrences manually in the file with the given references
      StringBuilder sb = new StringBuilder(element.getText());
      PsiReference[] references = element.getReferences();
      // reverse order, so we start at the end
      Arrays.sort(references, (o1, o2) -> o2.getRangeInElement().getStartOffset() - o1.getRangeInElement().getStartOffset());
      for (PsiReference reference : references) {
        if (reference instanceof AsciiDocIncludeTagReferenceInComment
          && ((AsciiDocIncludeTagReferenceInComment) reference).getKey().equals(getName())) {
          sb.replace(reference.getRangeInElement().getStartOffset(), reference.getRangeInElement().getEndOffset(), name);
        }
      }
      ((LeafElement) element.getFirstChild()).replaceWithText(sb.toString());
    }
    return this;
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
