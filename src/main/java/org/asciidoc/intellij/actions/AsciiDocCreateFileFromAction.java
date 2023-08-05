package org.asciidoc.intellij.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.psi.PsiDirectory;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;

public class AsciiDocCreateFileFromAction extends CreateFileFromTemplateAction implements DumbAware {

  public AsciiDocCreateFileFromAction() {
    super("AsciiDoc File", "Creates new AsciiDoc file", AsciiDocIcons.ASCIIDOC_ICON);
  }

  @Override
  protected String getDefaultTemplateProperty() {
    return "DefaultPlantUmlFileTemplate";
  }

  @Override
  protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder
      .setTitle("New AsciiDoc File")
      // add templates to filer src/main/resources/fileTemplates.internal
      .addKind("Empty", AsciiDocIcons.ASCIIDOC_ICON, "Empty")
      .addKind("Article", AsciiDocIcons.ASCIIDOC_ICON, "Article")
      .setValidator(new NonEmptyInputValidator());
  }

  @Override
  protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
    return "AsciiDoc File";
  }
}
