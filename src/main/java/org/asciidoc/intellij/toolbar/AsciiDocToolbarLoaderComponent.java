package org.asciidoc.intellij.toolbar;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

public class AsciiDocToolbarLoaderComponent implements ProjectComponent {

  public static final Key<AsciiDocToolbarPanel> ASCII_DOC_TOOLBAR = Key.create("AsciiDocToolbar");

  private Project myProject;

  public AsciiDocToolbarLoaderComponent(Project project) {
    this.myProject = project;
  }

  @Override
  public void initComponent() {
    myProject.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new AsciiDocFileEditorManagerListener());
  }

  @Override
  public void disposeComponent() {
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "AsciiDocToolbarLoaderComponent";
  }

  @Override
  public void projectOpened() {
    // called when project is opened
  }

  @Override
  public void projectClosed() {
    // called when project is being closed
  }


  /**
   * inspired by com.intellij.xml.breadcrumbs.BreadcrumbsLoaderComponent.MyFileEditorManagerListener.
   */
  private static class AsciiDocFileEditorManagerListener extends FileEditorManagerAdapter {

    @Override
    /** called on EDT */
    public void fileOpened(@NotNull final FileEditorManager manager, @NotNull final VirtualFile file) {
      if (AsciiDocLanguage.isAsciiDocFile(manager.getProject(), file)) {
        final FileEditor[] fileEditors = manager.getAllEditors(file);
        for (final FileEditor fileEditor : fileEditors) {
          if (fileEditor instanceof TextEditor) {
            Editor editor = ((TextEditor) fileEditor).getEditor();
            if (editor.getUserData(ASCII_DOC_TOOLBAR) != null) {
              continue;
            }

            final AsciiDocToolbarPanel toolbarPanel = new AsciiDocToolbarPanel(editor, fileEditor.getComponent());

            manager.addTopComponent(fileEditor, toolbarPanel);
            Disposer.register(fileEditor, toolbarPanel);
            Disposer.register(fileEditor, new Disposable() {
              @Override
              public void dispose() {
                manager.removeTopComponent(fileEditor, toolbarPanel);
              }
            });
          }
        }
      }
    }

  }
}
