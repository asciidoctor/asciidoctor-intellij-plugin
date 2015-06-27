package org.asciidoc.intellij.toolbar;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.AsciiDocLanguage;

public class AsciiDocToolbarProvider extends EditorNotifications.Provider<AsciiDocToolbarPanel> {

  private static final Key<AsciiDocToolbarPanel> KEY = Key.create("AsciiDocToolbarProvider");

  public Key<AsciiDocToolbarPanel> getKey() {
    return KEY;
  }

  public AsciiDocToolbarPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
    if (fileEditor instanceof TextEditor) {
      DataProvider component = (DataProvider)fileEditor.getComponent();
      PsiFile data = LangDataKeys.PSI_FILE.getData(component);
      if (data != null && data.getViewProvider().getBaseLanguage().isKindOf(AsciiDocLanguage.INSTANCE)) {
        return new AsciiDocToolbarPanel();
      }
    }
    return null;
  }
}