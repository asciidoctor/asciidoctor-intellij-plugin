package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsciiDocPsiElementFactory {
  private AsciiDocPsiElementFactory() {
  }

  @NotNull
  public static AsciiDocFile createFile(@NotNull Project project, @NotNull String text) {
    final LightVirtualFile virtualFile = new LightVirtualFile("temp.rb", AsciiDocLanguage.INSTANCE, text);
    PsiFile psiFile = ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project))
      .trySetupPsiForFile(virtualFile, AsciiDocLanguage.INSTANCE, true, true);

    if (!(psiFile instanceof AsciiDocFile)) {
      throw new RuntimeException("Cannot create a new markdown file. Text: " + text);
    }

    return (AsciiDocFile) psiFile;
  }


  @NotNull
  public static AsciiDocListing createListing(@NotNull Project project,
                                              @NotNull String text) {
    final AsciiDocFile file = createFile(project, text);
    PsiElement firstChild = file.getFirstChild();
    if (!(firstChild instanceof AsciiDocListing)) {
      throw AsciiDocPsiImplUtil.getRuntimeException("unable to covert to one listing", text, null);
    }
    AsciiDocListing listing = (AsciiDocListing) firstChild;
    Objects.requireNonNull(listing, "listing should have been found");
    if (listing.getNextSibling() != null) {
      throw AsciiDocPsiImplUtil.getRuntimeException("unable to covert to one listing", text, null);
    }
    return listing;
  }

  @NotNull
  public static AsciiDocFrontmatter createFrontmatter(@NotNull Project project,
                                              @NotNull String text) {
    final AsciiDocFile file = createFile(project, text);
    AsciiDocFrontmatter listing = (AsciiDocFrontmatter) file.getFirstChild();
    Objects.requireNonNull(listing, "frontmatter should have been found");
    if (listing.getNextSibling() != null) {
      throw new RuntimeException("unable to covert to one frontmatter: " + text);
    }
    return listing;
  }

  @NotNull
  public static AsciiDocPassthrough createPassthrough(@NotNull Project project,
                                                      @NotNull String text) {
    // append a "\n" so that the terminating element is recognized correctly (currently required in the lexer)
    final AsciiDocFile file = createFile(project, text + "\n");
    AsciiDocPassthrough listing = (AsciiDocPassthrough) file.getFirstChild();
    Objects.requireNonNull(listing);
    return listing;
  }

}
