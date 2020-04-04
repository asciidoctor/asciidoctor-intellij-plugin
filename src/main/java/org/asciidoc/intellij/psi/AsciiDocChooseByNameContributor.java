package org.asciidoc.intellij.psi;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AsciiDocChooseByNameContributor implements ChooseByNameContributor {
  @NotNull
  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    List<AsciiDocSection> sections = AsciiDocFileUtil.findSections(project);
    List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(project);
    List<AsciiDocAttributeDeclaration> attributes = AsciiDocUtil.findAttributes(project);
    Set<String> names = new HashSet<>(sections.size() + ids.size() + attributes.size());
    for (AsciiDocSection section : sections) {
      String title = section.getTitle();
      if (title.length() > 0) {
        names.add(title);
      }
    }
    for (AsciiDocBlockId property : ids) {
      String name = property.getName();
      if (name != null && name.length() > 0) {
        names.add(name);
      }
    }
    for (AsciiDocAttributeDeclaration attribute : attributes) {
      String name = attribute.getAttributeName();
      if (name != null && name.length() > 0) {
        names.add(name);
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
    List<AsciiDocAttributeDeclaration> attributes = AsciiDocUtil.findAttributes(project, name);
    List<? super NavigationItem> list = new ArrayList<>();
    list.addAll(sections);
    list.addAll(ids);
    attributes.forEach(asciiDocAttributeDeclaration -> list.add(asciiDocAttributeDeclaration.getAttributeDeclarationName()));
    return list.toArray(new NavigationItem[0]);
  }

}
