package org.asciidoc.intellij.findUsages;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.psi.AsciiDocJavaReference;

import java.util.Collection;

public class AsciiDocJavaReferencesSearchTest extends BasePlatformTestCase {

  public void testFindJavaClassReferenceInAsciiDocFileWhenHighlighting() {
    PsiClass clazz = addClass();
    PsiFile asciidoc = myFixture.configureByText("test.adoc", "Use `MyService` here.");
    Collection<PsiReference> references = ReferencesSearch.search(clazz, new LocalSearchScope(asciidoc)).findAll();
    assertTrue(references.stream().anyMatch(r -> r instanceof AsciiDocJavaReference));
  }

  public void testSkipJavaFileWhenHighlighting() {
    PsiClass clazz = addClass();
    Collection<PsiReference> references = ReferencesSearch.search(clazz, new LocalSearchScope(clazz.getContainingFile())).findAll();
    assertFalse(references.stream().anyMatch(r -> r instanceof AsciiDocJavaReference));
  }

  private PsiClass addClass() {
    PsiFile file = myFixture.addFileToProject("org/example/MyService.java", "package org.example; public class MyService {}");
    return ((PsiJavaFile) file).getClasses()[0];
  }
}
