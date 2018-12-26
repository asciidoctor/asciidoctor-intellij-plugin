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
package org.asciidoc.intellij.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/** @author Julien Viet */
public class AsciiDocPreviewEditor extends UserDataHolderBase implements FileEditor {

  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("asciidoctor",
    NotificationDisplayType.NONE, true);

  /** single threaded with one task queue (one for each editor window) */
  private final LazyApplicationPoolExecutor LAZY_EXECUTOR = new LazyApplicationPoolExecutor();

  /** Indicates whether the HTML preview is obsolete and should regenerated from the AsciiDoc {@link #document}. */
  private transient String currentContent = "";

  private transient int targetLineNo = 0;
  private transient int offsetLineNo = 0;
  private transient int currentLineNo = 0;

  /** The {@link Document} previewed in this editor. */
  protected final Document document;
  private Project project;

  /** The directory which holds the temporary images. */
  protected final Path tempImagesPath;

  @NotNull
  private final JPanel myHtmlPanelWrapper;

  @NotNull
  private volatile AsciiDocHtmlPanel myPanel;

  @NotNull
  private final Alarm mySwingAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

  /** . */
  private FutureTask<AsciiDoc> asciidoc = new FutureTask<AsciiDoc>(new Callable<AsciiDoc>() {
    public AsciiDoc call() throws Exception {
      File fileBaseDir = new File("");
      VirtualFile parent = FileDocumentManager.getInstance().getFile(document).getParent();
      if (parent != null) {
        // parent will be null if we use Language Injection and Fragment Editor
        fileBaseDir = new File(parent.getCanonicalPath());
      }
      return new AsciiDoc(project.getBasePath(), fileBaseDir,
        tempImagesPath, FileDocumentManager.getInstance().getFile(document).getName());
    }
  });

  private void render() {
    VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
    VirtualFile folder = currentFile.getParent();
    String tempContent = "";
    offsetLineNo = 0;
    while (true) {
      VirtualFile configFile = folder.findChild(".asciidoctorconfig");
      if (configFile != null &&
        !currentFile.equals(configFile)) {
        Document config = FileDocumentManager.getInstance().getDocument(configFile);
        tempContent = config.getText() + "\n\n" + tempContent;
        offsetLineNo += config.getLineCount() + 1;
      }
      if (folder.getPath().equals(project.getBasePath())) {
        break;
      }
      folder = folder.getParent();
      if (folder == null) {
        break;
      }
    }
    tempContent += document.getText();
    final String newContent = tempContent;

    VirtualFile lib = project.getBaseDir().findChild(".asciidoctor");
    if (lib != null) {
      lib = lib.findChild("lib");
    }

    java.util.List<String> extensions = new ArrayList<>();
    if (lib != null) {
      for (VirtualFile vf : lib.getChildren()) {
        if ("rb".equals(vf.getExtension())) {
          Document extension = FileDocumentManager.getInstance().getDocument(vf);
          if (extension != null) {
            extensions.add(vf.getCanonicalPath());
          }
        }
      }
    }

    LAZY_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        try {
          if (!newContent.equals(currentContent)) {
            currentContent = newContent;

            String markup = asciidoc.get().render(currentContent, extensions);
            if (markup != null) {
              myPanel.setHtml(markup);
            }
          }
          if (currentLineNo != targetLineNo) {
            currentLineNo = targetLineNo;
            myPanel.scrollToLine(targetLineNo, document.getLineCount(), offsetLineNo);
          }
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              myHtmlPanelWrapper.repaint();
            }
          });
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        catch (Exception ex) {
          String message = "Error rendering asciidoctor: " + ex.getMessage();
          Notification notification = NOTIFICATION_GROUP.createNotification("Error rendering asciidoctor", message,
            NotificationType.ERROR, null);
          // increase event log counter
          notification.setImportant(true);
          Notifications.Bus.notify(notification);
        }
      }
    });
  }

  public void renderIfVisible() {
    if (getComponent().isVisible()) {
      render();
    }
  }

  @Nullable("Null means leave current panel")
  private AsciiDocHtmlPanelProvider retrievePanelProvider(@NotNull AsciiDocApplicationSettings settings) {
    final AsciiDocHtmlPanelProvider.ProviderInfo providerInfo = settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo();

    AsciiDocHtmlPanelProvider provider = AsciiDocHtmlPanelProvider.createFromInfo(providerInfo);

    if (provider.isAvailable() != AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      settings.setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(settings.getAsciiDocPreviewSettings().getSplitEditorLayout(),
        AsciiDocPreviewSettings.DEFAULT.getHtmlPanelProviderInfo(), settings.getAsciiDocPreviewSettings().getPreviewTheme(),
        settings.getAsciiDocPreviewSettings().getAttributes()));

      /* the following will not work, IntellIJ will show the error "parent must be showing" when this is
         tiggered during startup. */
      /*
      Messages.showMessageDialog(
          myHtmlPanelWrapper,
          "Tried to use preview panel provider (" + providerInfo.getName() + "), but it is unavailable. Reverting to default.",
          CommonBundle.getErrorTitle(),
          Messages.getErrorIcon()
      );
      */

      provider = AsciiDocHtmlPanelProvider.getProviders()[0];
    }

    return provider;
  }

  public AsciiDocPreviewEditor(final Document document, Project project) {

    this.document = document;
    this.project = project;

    // create temp dir for images. Will be used by JavaFX only!
    Path tempImagesPath = null;

    try {
      tempImagesPath = Files.createTempDirectory("asciidoctor-intellij");
    } catch (IOException _ex) {
      String message = "Can't create temp folder to render images: " + _ex.getMessage();
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Error rendering asciidoctor", message, NotificationType.ERROR, null);
      // increase event log counter
      notification.setImportant(true);
      Notifications.Bus.notify(notification);
    }
    this.tempImagesPath = tempImagesPath;

    myHtmlPanelWrapper = new JPanel(new BorderLayout());

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();

    myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, null, retrievePanelProvider(settings));

    MessageBusConnection settingsConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
    AsciiDocApplicationSettings.SettingsChangedListener settingsChangedListener = new MyUpdatePanelOnSettingsChangedListener();
    settingsConnection.subscribe(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC, settingsChangedListener);

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
        renderIfVisible();
      }
    }, this);
  }

  @Contract("_, null, null -> fail")
  @NotNull
  private static AsciiDocHtmlPanel detachOldPanelAndCreateAndAttachNewOne(Document document, Path imagesDir, @NotNull JPanel panelWrapper,
                                                                          @Nullable AsciiDocHtmlPanel oldPanel,
                                                                          @Nullable AsciiDocHtmlPanelProvider newPanelProvider) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (oldPanel == null && newPanelProvider == null) {
      throw new IllegalArgumentException("Either create new one or leave the old");
    }
    if (newPanelProvider == null) {
      return oldPanel;
    }
    if (oldPanel != null) {
      panelWrapper.remove(oldPanel.getComponent());
      Disposer.dispose(oldPanel);
    }

    final AsciiDocHtmlPanel newPanel = newPanelProvider.createHtmlPanel(document, imagesDir);
    if (oldPanel != null) {
      newPanel.setEditor(oldPanel.getEditor());
    }
    panelWrapper.add(newPanel.getComponent(), BorderLayout.CENTER);

    return newPanel;
  }

  /**
   * Get the {@link java.awt.Component} to display as this editor's UI.
   */
  @NotNull
  public JComponent getComponent() {
    return myHtmlPanelWrapper;
  }

  /**
   * Get the component to be focused when the editor is opened.
   */
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return myHtmlPanelWrapper;
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
    myHtmlPanelWrapper.repaint();
    currentContent = "";
    renderIfVisible();
  }

  /**
   * Invoked when the editor is deselected (it does not mean that it is not visible).
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
    if (tempImagesPath != null) {
      try {
        FileUtils.deleteDirectory(tempImagesPath.toFile());
      } catch (IOException _ex) {
        Logger.getInstance(AsciiDocPreviewEditor.class).warn("could not remove temp folder", _ex);
      }
    }
  }

  public void scrollToLine(int line) {
    targetLineNo = line - offsetLineNo;
    renderIfVisible();
  }

  private class MyUpdatePanelOnSettingsChangedListener implements AsciiDocApplicationSettings.SettingsChangedListener {
    @Override
    public void onSettingsChange(@NotNull AsciiDocApplicationSettings settings) {
      final AsciiDocHtmlPanelProvider newPanelProvider = retrievePanelProvider(settings);

      mySwingAlarm.addRequest(new Runnable() {
        @Override
        public void run() {
          myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, myPanel, newPanelProvider);
          currentContent = "";
          renderIfVisible();
        }
      }, 0, ModalityState.stateForComponent(getComponent()));
    }
  }

  public Editor getEditor() {
    return myPanel.getEditor();
  }

  public void setEditor(Editor editor) {
    myPanel.setEditor(editor);
  }
}
