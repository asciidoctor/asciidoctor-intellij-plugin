package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.ClassUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocJavaReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private TextRange myRangeInElement;

  public AsciiDocJavaReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    myRangeInElement = textRange;
  }

  public boolean matches(PsiElement psiElement) {
    if (psiElement instanceof PsiClass) {
      PsiClass psiClass = (PsiClass) psiElement;
      String otherName = psiClass.getName();
      if (otherName == null) {
        return false;
      }
      boolean annotation = false;
      String myName = getValue();
      if (myName.startsWith("@")) {
        annotation = true;
        myName = myName.substring(1);
      }
      if (annotation && !psiClass.isAnnotationType()) {
        return false;
      }
      if (myName.equals(otherName)) {
        return true;
      }
      final String jvmClassName = ClassUtil.getJVMClassName(psiClass);
      if (myName.equals(jvmClassName)) {
        return true;
      }
      return false;
    } else if (psiElement instanceof PsiPackage) {
      if (getValue().equals(((PsiPackage) psiElement).getName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    String name = myRangeInElement.substring(myElement.getText());
    boolean annotation = false;
    if (name.startsWith("@")) {
      annotation = true;
      name = name.substring(1);
    }
    PsiClass[] fullQualifiedClasses = JavaPsiFacade.getInstance(myElement.getProject()).findClasses(
      name,
      new AsciiDocSearchScope(myElement.getProject())
    );
    for (PsiClass aClass : fullQualifiedClasses) {
      if (annotation && !aClass.isAnnotationType()) {
        continue;
      }
      results.add(new PsiElementResolveResult(aClass));
    }
    PsiPackage thePackage = JavaPsiFacade.getInstance(myElement.getProject()).findPackage(name);
    if (thePackage != null) {
      results.add(new PsiElementResolveResult(thePackage));
    }
    PsiClass[] shortNamedClasses = PsiShortNamesCache.getInstance(myElement.getProject()).getClassesByName(
      name,
      new AsciiDocSearchScope(myElement.getProject())
    );
    for (PsiClass aClass : shortNamedClasses) {
      if (annotation && !aClass.isAnnotationType()) {
        continue;
      }
      results.add(new PsiElementResolveResult(aClass));
    }
    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

}
