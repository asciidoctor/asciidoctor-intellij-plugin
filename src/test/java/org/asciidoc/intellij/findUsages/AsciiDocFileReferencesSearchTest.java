package org.asciidoc.intellij.findUsages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import org.asciidoc.intellij.psi.AsciiDocFileReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AsciiDocFileReferencesSearchTest extends BasePlatformTestCase {
  public void testSafeDeleteFindsImageUsageThroughAttribute() {
    assertSafeDeleteUsage("images/example.png", ":resource: images/example.png\n\nimage::{resource}[]\n");
  }

  public void testSafeDeleteFindsPageUsageThroughPartialAttribute() {
    assertSafeDeleteUsage("pages/example.adoc", ":name: exam\n\nxref:pages/{name}ple.adoc[]\n");
  }

  public void testImagePathInAttribute() {
    assertUsage("images/example.png", ":resource: images/example.png\n\nimage::{resource}[]\n");
  }

  public void testInlineImageWithPartialFileName() {
    assertUsage("images/example.png", ":name: exam\n\nimage:images/{name}ple.png[]\n");
  }

  public void testImageWithAttributeForDirectory() {
    assertUsage("images/example.png", ":dir: images\n\nimage::{dir}/example.png[]\n");
  }

  public void testImageWithImplicitImagesDirectory() {
    assertUsage("images/example.png", ":imagesdir: images\n:name: example\n\nimage::{name}.png[]\n");
  }

  public void testXrefPathInAttribute() {
    assertUsage("pages/example.adoc", ":resource: pages/example.adoc\n\nxref:{resource}[]\n");
  }

  public void testLinkWithPartialFileName() {
    assertUsage("pages/example.adoc", ":name: example\n\nlink:pages/{name}.adoc[]\n");
  }

  public void testIncludeWithNestedAttributes() {
    assertUsage("pages/example.adoc",
      ":dir: pages\n:name: example\n:resource: {dir}/{name}.adoc\n\ninclude::{resource}[]\n");
  }

  public void testAntoraXrefPathInAttribute() {
    myFixture.addFileToProject("antora.yml", "name: test\nversion: '1.0'\n");
    PsiFile target = myFixture.addFileToProject("modules/ROOT/pages/example.adoc", "Content\n");
    PsiFile source = myFixture.addFileToProject("modules/ROOT/pages/index.adoc",
      ":resource: page$example.adoc\n\nxref:{resource}[]\n");
    assertReference(target, source, GlobalSearchScope.projectScope(getProject()));
  }

  public void testLocalScope() {
    PsiFile target = myFixture.addFileToProject("example.adoc", "Content\n");
    PsiFile source = myFixture.addFileToProject("index.adoc", ":resource: example.adoc\n\nxref:{resource}[]\n");
    myFixture.addFileToProject("other.adoc", ":resource: example.adoc\n\nxref:{resource}[]\n");
    assertReference(target, source, new LocalSearchScope(source));
  }

  public void testDirectoryInAttribute() {
    PsiFile target = myFixture.addFileToProject("images/example.png", "Content\n");
    PsiFile source = myFixture.addFileToProject("index.adoc", ":dir: images\n\nimage::{dir}/example.png[]\n");
    Collection<PsiReference> references = ReferencesSearch.search(target.getContainingDirectory()).findAll();
    assertSize(1, references);
    PsiReference reference = references.iterator().next();
    assertEquals(source, reference.getElement().getContainingFile());
    assertEquals(target.getContainingDirectory(), reference.resolve());
  }

  public void testGlobalScopeExcludesOtherFiles() {
    PsiFile target = myFixture.addFileToProject("example.adoc", "Content\n");
    PsiFile source = myFixture.addFileToProject("index.adoc", ":resource: example.adoc\n\nxref:{resource}[]\n");
    myFixture.addFileToProject("other.adoc", ":resource: example.adoc\n\nxref:{resource}[]\n");
    assertReference(target, source, GlobalSearchScope.fileScope(source));
  }

  public void testUnrelatedAndUnresolvedAttributes() {
    PsiFile target = myFixture.addFileToProject("example.adoc", "Content\n");
    myFixture.addFileToProject("other.adoc", "Content\n");
    myFixture.addFileToProject("index.adoc",
      ":resource: other.adoc\n:unused: example.adoc\n\nxref:{resource}[]\nxref:{missing}[]\n");
    assertEmpty(ReferencesSearch.search(target).findAll());
  }

  public void testSameFileNameInDifferentDirectory() {
    PsiFile target = myFixture.addFileToProject("pages/example.adoc", "Content\n");
    myFixture.addFileToProject("other/example.adoc", "Content\n");
    myFixture.addFileToProject("index.adoc", ":resource: other/example.adoc\n\nxref:{resource}[]\n");
    assertEmpty(ReferencesSearch.search(target).findAll());
  }

  public void testPlainReferenceIsNotDuplicated() {
    assertUsage("example.adoc", "xref:example.adoc[]\n");
  }

  public void testConsumerCanStopSearch() {
    PsiFile target = myFixture.addFileToProject("example.adoc", "Content\n");
    myFixture.addFileToProject("index.adoc",
      ":resource: example.adoc\n\nxref:{resource}[]\nxref:{resource}[]\n");
    int[] count = {0};
    boolean completed = ReferencesSearch.search(target).forEach(reference -> {
      count[0]++;
      return false;
    });
    assertFalse(completed);
    assertEquals(1, count[0]);
  }

  private void assertUsage(String path, String text) {
    PsiFile target = myFixture.addFileToProject(path, "Content\n");
    PsiFile source = myFixture.addFileToProject("index.adoc", text);
    assertReference(target, source, GlobalSearchScope.projectScope(getProject()));
  }

  private void assertSafeDeleteUsage(String path, String text) {
    PsiFile target = myFixture.addFileToProject(path, "Content\n");
    PsiFile source = myFixture.addFileToProject("index.adoc", text);
    List<UsageInfo> usages = new ArrayList<>();
    SafeDeleteProcessor.findGenericElementUsages(target, usages, new PsiElement[]{target});
    assertSize(1, usages);
    UsageInfo usage = usages.get(0);
    assertInstanceOf(usage, SafeDeleteReferenceUsageInfo.class);
    assertFalse(((SafeDeleteReferenceUsageInfo) usage).isSafeDelete());
    assertEquals(source, usage.getElement().getContainingFile());
    assertTrue(target.isValid());
  }

  private void assertReference(PsiFile target, PsiFile source, SearchScope scope) {
    Collection<PsiReference> references = ReferencesSearch.search(target, scope).findAll();
    assertSize(1, references);
    PsiReference reference = references.iterator().next();
    assertInstanceOf(reference, AsciiDocFileReference.class);
    assertEquals(source, reference.getElement().getContainingFile());
    assertEquals(target, reference.resolve());
  }
}
