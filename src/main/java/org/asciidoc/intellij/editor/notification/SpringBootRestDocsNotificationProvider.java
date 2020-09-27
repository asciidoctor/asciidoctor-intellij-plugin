package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Notify user that spring-restdocs support is available.
 * Notification is triggered by either an operation:xxx[] block macro in the current file or a generated-snippets folder
 * relative to the current file.
 * The condition needs to be fulfilled when opening the editor.
 * It will not be re-checked during typing or generating files.
 */
public class SpringBootRestDocsNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Spring REST Docs available");

  private static final String SPRING_REST_DOCS_AVAILABLE = "asciidoc.springrestdocs.available";

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor, @NotNull Project project) {
    // only in AsciiDoc files
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }

    // only if not previously disabled
    if (PropertiesComponent.getInstance().getBoolean(SPRING_REST_DOCS_AVAILABLE)) {
      return null;
    }

    // find out if operation block macro is used
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null) {
      return null;
    }

    Collection<AsciiDocBlockMacro> blockMacros = PsiTreeUtil.findChildrenOfType(psiFile, AsciiDocBlockMacro.class);
    boolean operationMacro = false;
    for (AsciiDocBlockMacro blockMacro : blockMacros) {
      if ("operation".equals(blockMacro.getMacroName())) {
        operationMacro = true;
        break;
      }
    }

    if (!operationMacro) {
      VirtualFile springRestDocSnippets = AsciiDocUtil.findSpringRestDocSnippets(psiFile);
      if (springRestDocSnippets == null) {
        return null;
      }
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("It seems you are editing a Spring REST Docs spec. Do you want to learn more how this plugin can support you?");
    panel.createActionLabel("Yes, tell me more!", ()
      -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/features/advanced/spring-rest-docs.html"));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(SPRING_REST_DOCS_AVAILABLE, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}
