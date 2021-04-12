package org.asciidoc.intellij.psi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Define an AsciiDoc specific search scope.
 * For AsciiDoc content, always exclude libraries and excluded content.
 * All other content should show up in the search for references.
 * Excluded content will never show up as it is not indexed.
 */
public class AsciiDocSearchScope extends GlobalSearchScope {

  private final FileIndexFacade myFileIndexFacade;

  public AsciiDocSearchScope(Project project) {
    super(project);
    myFileIndexFacade = FileIndexFacade.getInstance(project);
  }

  public GlobalSearchScope restrictedByAsciiDocFileType() {
    return GlobalSearchScope.getScopeRestrictedByFileTypes(this, AsciiDocFileType.INSTANCE);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    // even if isSearchInLibraries returns false, the check for isInLibraryXXX is still needed
    return !myFileIndexFacade.isExcludedFile(file) &&
      !myFileIndexFacade.isInLibraryClasses(file) &&
      !myFileIndexFacade.isInLibrarySource(file);
  }

  @Override
  public boolean isSearchInModuleContent(@NotNull Module aModule) {
    return true;
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @NotNull
  @Override
  public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
    return myFileIndexFacade.getUnloadedModuleDescriptions();
  }
}

