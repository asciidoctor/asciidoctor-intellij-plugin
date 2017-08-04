package org.asciidoc.intellij.psi;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocChooseByNameContributor implements ChooseByNameContributor {
  @NotNull
  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    List<AsciiDocSection> properties = AsciiDocFileUtil.findProperties(project);
    List<String> names = new ArrayList<>(properties.size());
    for (AsciiDocSection property : properties) {
      if (property.getTitle() != null && property.getTitle().length() > 0) {
        names.add(property.getTitle());
      }
    }
    return names.toArray(new String[names.size()]);
  }

  @NotNull
  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    // todo include non project items
    List<AsciiDocSection> properties = AsciiDocFileUtil.findProperties(project, name);
    return properties.toArray(new NavigationItem[properties.size()]);
  }

}
