package org.asciidoc.intellij.searchScopes;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class AsciiDocSearchScopeWithName extends GlobalSearchScope {

  private final GlobalSearchScope searchScope;
  private final String name;

  public AsciiDocSearchScopeWithName(GlobalSearchScope searchScope, String name) {
    this.searchScope = searchScope;
    this.name = name;
  }

  @Override
  protected int calcHashCode() {
    return Objects.hash(name);
  }

  @SuppressWarnings("checkstyle:EqualsHashCode")
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AsciiDocSearchScopeWithName)) {
      return false;
    }
    return Objects.equals(name, ((AsciiDocSearchScopeWithName) obj).name);
  }

  @Override
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  public String getDisplayName() {
    return name;
  }

  @Override
  public @Nullable Icon getIcon() {
    return searchScope.getIcon();
  }

  @Override
  public boolean isSearchInModuleContent(@NotNull Module aModule) {
    return searchScope.isSearchInModuleContent(aModule);
  }

  @Override
  public boolean isSearchInLibraries() {
    return searchScope.isSearchInLibraries();
  }

  @Override
  @Contract(pure = true)
  @NotNull
  public SearchScope intersectWith(@NotNull SearchScope scope2) {
    return searchScope.intersectWith(scope2);
  }

  @Override
  @Contract(pure = true)
  @NotNull
  public GlobalSearchScope union(@NotNull SearchScope scope) {
    return searchScope.union(scope);
  }

  @Override
  @Contract(pure = true)
  public boolean contains(@NotNull VirtualFile file) {
    return searchScope.contains(file);
  }

}
