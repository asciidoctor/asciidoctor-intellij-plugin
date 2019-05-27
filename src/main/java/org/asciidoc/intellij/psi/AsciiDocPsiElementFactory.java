package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.Project;
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
    // append a "\n" so that the terminating element is recognized correctly (currently required in the lexer)
    final AsciiDocFile file = createFile(project, text + "\n");
    AsciiDocListing listing = (AsciiDocListing) file.getFirstChild();
    Objects.requireNonNull(listing);
    return listing;
  }

}
