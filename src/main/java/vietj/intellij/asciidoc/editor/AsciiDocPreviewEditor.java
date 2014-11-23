/*
 * Copyright 2013 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vietj.intellij.asciidoc.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vietj.intellij.asciidoc.AsciiDoc;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

/** @author Julien Viet */
public class AsciiDocPreviewEditor extends UserDataHolderBase implements FileEditor {

  /** The {@link java.awt.Component} used to render the HTML preview. */
  protected final JEditorPane jEditorPane = new JEditorPane();

  /** Indicates whether the HTML preview is obsolete and should regenerated from the AsciiDoc {@link #document}. */
  protected transient String currentContent = "";

  /** Indicate if view is selected = visible */
  protected transient boolean selected = false;

  /** The {@link Document} previewed in this editor. */
  protected final Document document;

  /** The {@link JBScrollPane} allowing to browse {@link #jEditorPane}. */
  protected final JBScrollPane scrollPane = new JBScrollPane(jEditorPane);

  /** . */
  private FutureTask<AsciiDoc> asciidoc = new FutureTask<AsciiDoc>(new Callable<AsciiDoc>() {
    public AsciiDoc call() throws Exception {
      return new AsciiDoc(new File(FileDocumentManager.getInstance().getFile(document).getParent().getCanonicalPath()));
    }
  });

  /** ensure that there is no concurrent asciidoc rendering to prevent excessive
   * CPU usage. */
  private Semaphore sem = new Semaphore(1);

  private void render() {
    /** use a swing worker to render asciidoc in the background, and call
    jEditorPane.setText in the EDT to avoid flicker
    @see http://en.wikipedia.org/wiki/Event_dispatching_thread)
    */
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
      public String doInBackground() {
        try {
          sem.acquire();
          AsciiDoc doc = asciidoc.get();
          /* allow a rendering once every 200ms to prevent
          excessive CPU usage. */
          Thread.sleep(200);
          if (!document.getText().equals(currentContent)) {
            currentContent = document.getText();
            return doc.render(currentContent);
          }
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        catch (ExecutionException ex) {
          throw new RuntimeException(ex.getCause());
        }
        return null;
      }

      public void done() {
        try {
          String markup = get();
          if(markup != null) {
            // markup = "<html><body>" + markup + "</body></html>";
            jEditorPane.setText(markup);
            Rectangle d = jEditorPane.getVisibleRect();

            jEditorPane.setSize((int)d.getWidth(), (int)jEditorPane.getSize().getHeight());
          }
          sem.release();
        } catch (Exception ex) {
          throw new RuntimeException(ex.getCause());
        }
      }
    };

    worker.execute();
  }

  public AsciiDocPreviewEditor(Project project, final Document document) {

    //
    this.document = document;

    // Get asciidoc asynchronously
    new Thread() {
      @Override
      public void run() {
        asciidoc.run();
      }
    }.start();

    // Listen to the document modifications.
    this.document.addDocumentListener(new DocumentAdapter() {
      @Override
      public void documentChanged(DocumentEvent e) {
        render();
      }
    });

    // Setup the editor pane for rendering HTML.
    final HTMLEditorKit kit = new AsciiDocEditorKit(document,
        new File(FileDocumentManager.getInstance().getFile(document).getParent().getCanonicalPath()));

    //
    URL previewURL = AsciiDocPreviewEditor.class.getResource("preview.css");
    if (previewURL != null) {
      final StyleSheet style = new StyleSheet();
      style.importStyleSheet(previewURL);
      kit.setStyleSheet(style);
    }

    //
    jEditorPane.setEditorKit(kit);
    jEditorPane.setEditable(false);
    // use this to prevent scrolling to the end of the pane on setText()
    ((DefaultCaret)jEditorPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    // initial rendering of content
    render();
  }

  /**
   * Get the {@link java.awt.Component} to display as this editor's UI.
   *
   * @return a scrollable {@link JEditorPane}.
   */
  @NotNull
  public JComponent getComponent() {
    return scrollPane;
  }

  /**
   * Get the component to be focused when the editor is opened.
   *
   * @return {@link #scrollPane}
   */
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return scrollPane;
  }

  /**
   * Get the editor displayable name.
   *
   * @return <code>AsciiDoc</code>
   */
  @NotNull
  @NonNls
  public String getName() {
    return "Preview";
  }

  /**
   * Get the state of the editor.
   * <p/>
   * Just returns {@link FileEditorState#INSTANCE} as {@link AsciiDocPreviewEditor} is stateless.
   *
   * @param level the level.
   * @return {@link FileEditorState#INSTANCE}
   * @see #setState(com.intellij.openapi.fileEditor.FileEditorState)
   */
  @NotNull
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    return FileEditorState.INSTANCE;
  }

  /**
   * Set the state of the editor.
   * <p/>
   * Does not do anything as {@link AsciiDocPreviewEditor} is stateless.
   *
   * @param state the new state.
   * @see #getState(com.intellij.openapi.fileEditor.FileEditorStateLevel)
   */
  public void setState(@NotNull FileEditorState state) {
  }

  /**
   * Indicates whether the document content is modified compared to its file.
   *
   * @return {@code false} as {@link AsciiDocPreviewEditor} is read-only.
   */
  public boolean isModified() {
    return false;
  }

  /**
   * Indicates whether the editor is valid.
   *
   * @return {@code true} if {@link #document} content is readable.
   */
  public boolean isValid() {
    return document.getText() != null;
  }

  /**
   * Invoked when the editor is selected.
   * <p/>
   * Refresh view on select (as dependent elements might have changed).
   */
  public void selectNotify() {
    selected = true;
    currentContent = "";
    render();
  }

  /**
   * Invoked when the editor is deselected.
   * <p/>
   * Does nothing.
   */
  public void deselectNotify() {
  }

  /**
   * Add specified listener.
   * <p/>
   * Does nothing.
   *
   * @param listener the listener.
   */
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  /**
   * Remove specified listener.
   * <p/>
   * Does nothing.
   *
   * @param listener the listener.
   */
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  /**
   * Get the background editor highlighter.
   *
   * @return {@code null} as {@link AsciiDocPreviewEditor} does not require highlighting.
   */
  @Nullable
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  /**
   * Get the current location.
   *
   * @return {@code null} as {@link AsciiDocPreviewEditor} is not navigable.
   */
  @Nullable
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  /**
   * Get the structure view builder.
   *
   * @return TODO {@code null} as parsing/PSI is not implemented.
   */
  @Nullable
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  /** Dispose the editor. */
  public void dispose() {
    Disposer.dispose(this);
  }
}
