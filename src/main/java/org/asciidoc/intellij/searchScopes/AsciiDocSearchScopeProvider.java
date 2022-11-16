package org.asciidoc.intellij.searchScopes;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.SearchScopeProvider;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocSearchScopeProvider implements SearchScopeProvider {
  @Override
  public @Nullable String getDisplayName() {
    return "AsciiDoc";
  }

  @Override
  public @NotNull List<SearchScope> getSearchScopes(@NotNull Project project, @NotNull DataContext dataContext) {
    ArrayList<SearchScope> result = new ArrayList<>();
    result.add(new AsciiDocSearchScopeWithName(new AsciiDocSearchScope(project).excludeSymlinks(), "All files without Symlinks"));
    result.add(new AsciiDocSearchScopeWithName(new AsciiDocSearchScope(project).restrictedByAsciiDocFileType(), "AsciiDoc files"));
    result.add(new AsciiDocSearchScopeWithName(new AsciiDocSearchScope(project).excludeSymlinks().restrictedByAsciiDocFileType(), "AsciiDoc files without Symlinks"));
    return result;
  }
}
