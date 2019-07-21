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
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
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
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
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
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author Julien Viet
 */
public class AsciiDocPreviewEditor extends UserDataHolderBase implements FileEditor {

  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("asciidoctor",
    NotificationDisplayType.NONE, true);

  private Logger log = Logger.getInstance(AsciiDocPreviewEditor.class);

  /**
   * single threaded with one task queue (one for each editor window).
   */
  private final LazyApplicationPoolExecutor lazyExecutor = new LazyApplicationPoolExecutor();

  /**
   * Indicates whether the HTML preview is obsolete and should regenerated from the AsciiDoc {@link #document}.
   */
  private transient String currentContent = null;

  private transient int targetLineNo = 0;
  private transient int offsetLineNo = 0;
  private transient int currentLineNo = 0;

  /**
   * The {@link Document} previewed in this editor.
   */
  private final Document document;
  private Project project;

  /**
   * The directory which holds the temporary images.
   */
  private final Path tempImagesPath;

  @NotNull
  private final JPanel myHtmlPanelWrapper;

  @NotNull
  private volatile AsciiDocHtmlPanel myPanel;

  @NotNull
  private final Alarm mySwingAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

  /**
   * .
   */
  private FutureTask<AsciiDoc> asciidoc = new FutureTask<>(new Callable<AsciiDoc>() {
    public AsciiDoc call() {
      File fileBaseDir = new File("");
      VirtualFile file = FileDocumentManager.getInstance().getFile(document);
      String name = "unkown";
      if (file != null) {
        name = file.getName();
        VirtualFile parent = file.getParent();
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
        }
      }
      return new AsciiDoc(project.getBasePath(), fileBaseDir, tempImagesPath, name);
    }
  });

  private void render() {
    final String contentWithConfig = AsciiDoc.prependConfig(document, project, o -> offsetLineNo = o);
    List<String> extensions = AsciiDoc.getExtensions(project);

    lazyExecutor.execute(() -> {
      try {
        if (!contentWithConfig.equals(currentContent)) {
          currentContent = contentWithConfig;

          String markup = asciidoc.get().render(contentWithConfig, extensions);
          if (markup != null) {
            myPanel.setHtml(markup);
          }
        }
        if (currentLineNo != targetLineNo) {
          currentLineNo = targetLineNo;
          myPanel.scrollToLine(targetLineNo, document.getLineCount(), offsetLineNo);
        }
        ApplicationManager.getApplication().invokeLater(myHtmlPanelWrapper::repaint);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        String message = "Error rendering preview: " + ex.getMessage();
        log.error(message, ex);
        Notification notification = NOTIFICATION_GROUP.createNotification("Error rendering asciidoctor", message,
          NotificationType.ERROR, null);
        // increase event log counter
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
      }
    });
  }

  void renderIfVisible() {
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
        settings.getAsciiDocPreviewSettings().getAttributes(), settings.getAsciiDocPreviewSettings().isVerticalSplit(),
        settings.getAsciiDocPreviewSettings().isEditorFirst(), settings.getAsciiDocPreviewSettings().isEnabledInjections(),
        settings.getAsciiDocPreviewSettings().getDisabledInjectionsByLanguage(),
        settings.getAsciiDocPreviewSettings().isShowAsciiDocWarningsAndErrorsInEditor(),
        settings.getAsciiDocPreviewSettings().isInplacePreviewRefresh()));

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

    this.tempImagesPath = AsciiDoc.tempImagesPath();

    myHtmlPanelWrapper = new JPanel(new BorderLayout());

    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();

    myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, null, retrievePanelProvider(settings));

    MessageBusConnection settingsConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
    AsciiDocApplicationSettings.SettingsChangedListener settingsChangedListener = new MyUpdatePanelOnSettingsChangedListener();
    settingsConnection.subscribe(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC, settingsChangedListener);

    MyEditorColorsListener editorColorsListener = new MyEditorColorsListener();
    settingsConnection.subscribe(EditorColorsManager.TOPIC, editorColorsListener);

    // Get asciidoc asynchronously
    new Thread(() -> asciidoc.run()).start();

    // Listen to the document modifications.
    this.document.addDocumentListener(new DocumentAdapter() {
      @Override
      public void documentChanged(DocumentEvent e) {
        renderIfVisible();
      }
    }, this);
  }

  @Contract("_, _, _, null, null -> fail")
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
    currentContent = null; // force a refresh of the preview by resetting the current memorized content
    reprocessAnnotations();
    renderIfVisible();
  }

  private void reprocessAnnotations() {
    PsiDocumentManager pm = PsiDocumentManager.getInstance(project);
    if (pm != null) {
      PsiFile psiFile = pm.getPsiFile(document);
      if (psiFile != null) {
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
      }
    }
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

  /**
   * Dispose the editor.
   */
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

  void scrollToLine(int line) {
    targetLineNo = line;
    renderIfVisible();
  }

  private class MyUpdatePanelOnSettingsChangedListener implements AsciiDocApplicationSettings.SettingsChangedListener {
    @Override
    public void onSettingsChange(@NotNull AsciiDocApplicationSettings settings) {
      final AsciiDocHtmlPanelProvider newPanelProvider = retrievePanelProvider(settings);

      mySwingAlarm.addRequest(() -> {
        synchronized (this) {
          myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, myPanel, newPanelProvider);
        }
        currentContent = null; // force a refresh of the preview by resetting the current memorized content
        reprocessAnnotations();
        renderIfVisible();
      }, 0, ModalityState.stateForComponent(getComponent()));
    }
  }

  public Editor getEditor() {
    return myPanel.getEditor();
  }

  public void setEditor(Editor editor) {
    myPanel.setEditor(editor);
  }

  private class MyEditorColorsListener implements EditorColorsListener {
    @Override
    public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
      final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
      // reset contents in preview with latest CSS headers
      if (settings.getAsciiDocPreviewSettings().getPreviewTheme() == AsciiDocHtmlPanel.PreviewTheme.INTELLIJ) {
        currentContent = null;
        myPanel.setHtml("");
        renderIfVisible();
      }
    }
  }
}
