package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiNamedElement;
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
  private final TextRange myRangeInElement;

  public AsciiDocJavaReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    myRangeInElement = textRange;
  }

  public boolean matches(PsiElement psiElement) {
    if (psiElement instanceof PsiClass psiClass) {
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
    AsciiDocUtil.swallowIndexNotReadyExceptionIfInsideDumbAware(() -> {
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
        ResolveResult resolveResult = new PsiElementResolveResult(aClass);
        if (!results.contains(resolveResult)) {
          // avoid having the same class in the list twice. Would happen if class doesn't have a package name,
          // and short name and fully qualified name are the same.
          results.add(resolveResult);
        }
      }
    });
    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement otherElement) {
    if (!(otherElement instanceof PsiNamedElement otherClass)) {
      return otherElement;
    }
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(this.getElement());
    String oldName = myRangeInElement.substring(myElement.getText());
    boolean isAnnotation = oldName.startsWith("@");
    boolean isFullyQualifiedName = oldName.contains(".");
    manipulator.handleContentChange(this.getElement(), getRangeInElement(),
      (isAnnotation ? "@" : "") +
        (isFullyQualifiedName ? ((PsiJavaFile) otherClass.getContainingFile()).getPackageName() + "." : "")
        + otherClass.getName()
    );
    return otherElement;
  }

}
