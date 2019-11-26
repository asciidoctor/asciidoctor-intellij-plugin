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
    List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(project);
    List<String> names = new ArrayList<>(sections.size());
    for (AsciiDocSection section : sections) {
      if (section.getTitle() != null && section.getTitle().length() > 0) {
        names.add(section.getTitle());
      }
    }
    List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(project);
    for (AsciiDocBlockId property : ids) {
      if (property.getName() != null && property.getName().length() > 0) {
        names.add(property.getName());
      }
    }
    return names.toArray(new String[0]);
  }

  @NotNull
  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    // todo include non project items
    List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(project, name);
    List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(project, name);
    List<? super NavigationItem> list = new ArrayList<>();
    list.addAll(sections);
    list.addAll(ids);
    return list.toArray(new NavigationItem[0]);
  }

}
