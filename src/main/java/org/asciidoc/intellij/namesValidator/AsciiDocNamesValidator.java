package org.asciidoc.intellij.namesValidator;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Declare all well known attribute names as keywords, thereby preventing them to be reported as misspelled.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocNamesValidator implements NamesValidator {

  @Override
  public boolean isKeyword(@NotNull String name, Project project) {
    return AsciiDocBundle.containsAttribute(name);
  }

  @Override
  public boolean isIdentifier(@NotNull String name, Project project) {
    return false;
  }
}
