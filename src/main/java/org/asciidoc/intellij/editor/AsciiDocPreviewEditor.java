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
import com.intellij.ide.impl.TrustStateListener;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.notification.Notification;
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
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.Alarm;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.AsciiDocExtensionService;
import org.asciidoc.intellij.AsciiDocWrapper;
import org.asciidoc.intellij.download.AsciiDocDownloadNotificationProvider;
import org.asciidoc.intellij.editor.jeditor.JeditorHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author Julien Viet
 */
public class AsciiDocPreviewEditor extends UserDataHolderBase implements FileEditor {

  private static final Logger LOG = Logger.getInstance(AsciiDocPreviewEditor.class);
  private final AsciiDocExtensionService extensionService = ApplicationManager.getApplication().getService(AsciiDocExtensionService.class);
  /**
   * single threaded with one task queue (one for each editor window).
   */
  private final LazyApplicationPoolExecutor lazyExecutor = new LazyApplicationPoolExecutor(this);

  /**
   * Indicates whether the HTML preview is obsolete and should regenerated from the AsciiDoc {@link #document}.
   */
  private transient String currentContent = null;
  private transient int lastRenderCycle = 0;
  private final AtomicInteger forcedRenderCycle = new AtomicInteger(1);

  private transient int targetLineNo = 0;
  private transient int currentLineNo = 0;

  /**
   * The {@link Document} previewed in this editor.
   */
  private final Document document;
  private final Project project;
  private Editor editor;

  /**
   * The directory which holds the temporary images.
   */
  private final Path tempImagesPath;

  @NotNull
  private final JPanel myHtmlPanelWrapper;

  private volatile AsciiDocHtmlPanel myPanel;

  @NotNull
  private final Alarm mySwingAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

  private AsciiDocWrapper asciidoc;

  private void render() {
    lazyExecutor.execute(() -> {
      try {
        if (project.isDisposed()) {
          // due to lazy execution, this project have been already closed, do nothing then to avoid exceptions
          return;
        }
        final @Language("asciidoc") String content = document.getText();
        final String config = AsciiDocWrapper.config(document, project);
        List<String> extensions = extensionService.getExtensions(project);
        int currentRenderCycle = forcedRenderCycle.get();
        if (!(config + content).equals(currentContent) || currentRenderCycle != lastRenderCycle) {
          AsciiDocWrapper instance = getAsciiDocInstance();
          VirtualFile file = FileDocumentManager.getInstance().getFile(document);
          if (file != null) {
            String name = file.getName();
            File fileBaseDir = new File("");
            VirtualFile parent = file.getParent();
            if (parent != null && parent.getCanonicalPath() != null) {
              // parent will be null if we use Language Injection and Fragment Editor
              fileBaseDir = new File(parent.getCanonicalPath());
            }
            instance.updateFileName(fileBaseDir, name);
          }
          String markup = instance.render(content, config, extensions);
          if (Objects.equals("true", instance.getAttributes().get("asciidoctor-diagram-missing-diagram-extension"))) {
            if (getComponent().isVisible() && getComponent().isDisplayable()) {
              AsciiDocDownloadNotificationProvider.showNotification();
            }
          }
          if (markup != null) {
            AsciiDocHtmlPanel localPanel = myPanel;
            if (localPanel != null) {
              localPanel.setHtml(markup, instance.getAttributes());
              synchronized (this) {
                if (myPanel == localPanel) {
                  // only set the content if the panel hasn't been updated (due to settings changed)
                  currentContent = config + content;
                  lastRenderCycle = currentRenderCycle;
                }
              }
            }
          }
        }
        if (currentLineNo != targetLineNo) {
          currentLineNo = targetLineNo;
          synchronized (this) {
            if (myPanel != null) {
              myPanel.scrollToLine(targetLineNo, document.getLineCount());
            }
          }
        }
      } catch (ProcessCanceledException e) {
        renderIfVisible();
      } catch (AlreadyDisposedException e) {
        // noop - content hasn't rendered, project has been closed already
      } catch (Exception ex) {
        String message = "Error rendering preview: " + ex.getMessage();
        LOG.error(message, ex);
        Notification notification = AsciiDocWrapper.getNotificationGroup()
          .createNotification("Error rendering asciidoctor", message,
            NotificationType.ERROR);
        // increase event log counter
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
      }
    });
  }

  private AsciiDocWrapper getAsciiDocInstance() {
    if (asciidoc == null) {
      File fileBaseDir = new File("");
      VirtualFile file = FileDocumentManager.getInstance().getFile(document);
      String name = "unknown";
      if (file != null) {
        name = file.getName();
        VirtualFile parent = file.getParent();
        if (parent != null && parent.getCanonicalPath() != null) {
          // parent will be null if we use Language Injection and Fragment Editor
          fileBaseDir = new File(parent.getCanonicalPath());
        }
      }
      asciidoc = new AsciiDocWrapper(project, fileBaseDir, tempImagesPath, name);
    }
    return asciidoc;
  }

  void renderIfVisible() {
    // visible = preview is enabled
    // displayable = editor window is visible as it is the active editor in a group
    if (myPanel != null && getComponent().isVisible() && getComponent().isDisplayable()) {
      render();
    }
  }

  private AsciiDocHtmlPanelProvider retrievePanelProvider(@NotNull AsciiDocApplicationSettings settings) {
    final AsciiDocHtmlPanelProvider.ProviderInfo providerInfo = settings.getAsciiDocPreviewSettings().getHtmlPanelProviderInfo();

    AsciiDocHtmlPanelProvider provider = AsciiDocHtmlPanelProvider.createFromInfo(providerInfo);

    if (provider.isAvailable() != AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      settings.setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(settings.getAsciiDocPreviewSettings().getSplitEditorLayout(),
        AsciiDocPreviewSettings.DEFAULT.getHtmlPanelProviderInfo(), settings.getAsciiDocPreviewSettings().getPreviewTheme(),
        settings.getSafe(project), settings.getAsciiDocPreviewSettings().getAttributes(), settings.getAsciiDocPreviewSettings().isVerticalSplit(),
        settings.getAsciiDocPreviewSettings().isEditorFirst(), settings.getAsciiDocPreviewSettings().isEnabledInjections(),
        settings.getAsciiDocPreviewSettings().getLanguageForPassthrough(),
        settings.getAsciiDocPreviewSettings().getDisabledInjectionsByLanguage(),
        settings.getAsciiDocPreviewSettings().isShowAsciiDocWarningsAndErrorsInEditor(),
        settings.getAsciiDocPreviewSettings().isInplacePreviewRefresh(),
        settings.getAsciiDocPreviewSettings().isKrokiEnabled(),
        settings.getAsciiDocPreviewSettings().getKrokiUrl(),
        settings.getAsciiDocPreviewSettings().isAttributeFoldingEnabled(),
        settings.getAsciiDocPreviewSettings().isConversionOfClipboardTextEnabled(),
        settings.getAsciiDocPreviewSettings().getZoom(),
        settings.getAsciiDocPreviewSettings().isHideErrorsInSourceBlocks(),
        settings.getAsciiDocPreviewSettings().getHideErrorsByLanguage()));

      /* the following will not work, IntelliJ will show the error "parent must be showing" when this is
         triggered during startup. */
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

  public void printToPdf(String canonicalPath, Consumer<Boolean> success) {
    if (myPanel != null) {
      myPanel.printToPdf(canonicalPath, success);
    }
  }

  public boolean isPrintingSupported() {
    return myPanel != null && myPanel.isPrintingSupported();
  }

  /**
   * Force re-rendering of the preview independent of content changes.
   * Using an integer that increments replace the previous logic where a currently rendering preview
   * reset the flag to re-render the preview.
   */
  private void forceRenderCycle() {
    forcedRenderCycle.incrementAndGet();
  }

  public AsciiDocPreviewEditor(final Document document, Project project) {

    this.document = document;
    this.project = project;

    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    Path parent = null;
    if (file != null && file.getParent() != null) {
      parent = Path.of(file.getParent().getPath());
    }
    this.tempImagesPath = AsciiDocWrapper.tempImagesPath(parent, project);

    myHtmlPanelWrapper = new JPanel(new BorderLayout());

    myHtmlPanelWrapper.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        setupPanel();
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        if (!mySwingAlarm.isDisposed()) {
          mySwingAlarm.addRequest(() -> {
            try {
              if (!mySwingAlarm.isDisposed()) {
                synchronized (this) {
                  if (myPanel != null) {
                    myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, myPanel, null);
                  }
                }
              }
            } catch (Exception ex) {
              LOG.error("unhandled exception when preparing the preview", ex);
            }
          }, 0, ModalityState.stateForComponent(getComponent()));
        }
      }
    });

    // Set if to false by default, so that it will be enabled only when needed later.
    myHtmlPanelWrapper.setVisible(false);

    MessageBusConnection settingsConnection = ApplicationManager.getApplication().getMessageBus().connect(this);

    settingsConnection.subscribe(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC, new MyUpdatePanelOnSettingsChangedListener());
    settingsConnection.subscribe(EditorColorsManager.TOPIC, new MyEditorColorsListener());
    settingsConnection.subscribe(TrustStateListener.TOPIC, new MyTrustChangedListener());

    // Listen to the document modifications.
    this.document.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        renderIfVisible();
      }
    }, this);

    // Listen to any file modification in the project.
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void exitDumbMode() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          forceRenderCycle();
          renderIfVisible();
        });
      }
    });
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        // any modification of a file within the project refreshes the preview
        for (VFileEvent event : events) {
          if (event.getFile() != null) {
            if (!project.isDisposed()) {
              try {
                if (ProjectFileIndex.getInstance(project).getModuleForFile(event.getFile()) != null) {
                  // As an include might have been modified, force the refresh of the preview
                  forceRenderCycle();
                  renderIfVisible();
                  break;
                }
              } catch (AlreadyDisposedException ex) {
                // nop
              }
            }
          }
        }
      }
    });

    // some references (for example Antora references) might not have been resolved in dumb mode
    // therefore re-render the preview when the project is no longer in dumb mode.
    class RenderPreviewOnDumbModeChangeListener implements DumbService.DumbModeListener {
      @Override
      public void enteredDumbMode() {
        renderIfVisible();
      }

      @Override
      public void exitDumbMode() {
        renderIfVisible();
      }
    }

    connection.subscribe(DumbService.DUMB_MODE, new RenderPreviewOnDumbModeChangeListener());

  }

  private void setupPanel() {
    if (!mySwingAlarm.isDisposed()) {
      mySwingAlarm.addRequest(() -> {
        try {
          if (!mySwingAlarm.isDisposed()) {
            synchronized (this) {
              if (myPanel == null) {
                final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
                myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, null, retrievePanelProvider(settings));
                myPanel.setEditor(editor);
                forceRenderCycle();
                renderIfVisible();
              }
            }
          }
        } catch (Exception ex) {
          LOG.error("unhandled exception when preparing the preview", ex);
        }
      }, 0, ModalityState.stateForComponent(getComponent()));
    }
  }

  @Contract("_, _, _, null, null -> fail")
  private static AsciiDocHtmlPanel detachOldPanelAndCreateAndAttachNewOne(Document document, Path imagesDir, @NotNull JPanel panelWrapper,
                                                                          @Nullable AsciiDocHtmlPanel oldPanel,
                                                                          @Nullable AsciiDocHtmlPanelProvider newPanelProvider) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (oldPanel == null && newPanelProvider == null) {
      throw new IllegalArgumentException("Either create new one or leave the old");
    }
    if (oldPanel != null) {
      panelWrapper.remove(oldPanel.getComponent());
      Disposer.dispose(oldPanel);
    }
    if (newPanelProvider == null) {
      return null;
    }

    AsciiDocHtmlPanel newPanel;
    try {
      newPanel = newPanelProvider.createHtmlPanel(document, imagesDir);
    } catch (IllegalStateException ex) {
      if (ex.getMessage() != null && ex.getMessage().startsWith("JCEF is not supported")) {
        LOG.warn("JCEF panel couldn't be initialized", ex);
        Notification notification = AsciiDocWrapper.getNotificationGroup()
          .createNotification("Error creating JCEF preview", ex.getMessage(), NotificationType.ERROR);
        // increase event log counter
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
        newPanel = new JeditorHtmlPanelProvider().createHtmlPanel(document, imagesDir);
      } else {
        throw ex;
      }
    }
    if (oldPanel != null) {
      newPanel.setEditor(oldPanel.getEditor());
    }
    panelWrapper.add(newPanel.getComponent(), BorderLayout.CENTER);

    return newPanel;
  }

  /**
   * Get the {@link java.awt.Component} to display as this editor's UI.
   */
  @Override
  @NotNull
  public JComponent getComponent() {
    return myHtmlPanelWrapper;
  }

  /**
   * Get the component to be focused when the editor is opened.
   */
  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    JComponent preferredFocusedComponent = null;
    if (myPanel != null) {
      preferredFocusedComponent = myPanel.getPreferredFocusedComponent();
    }
    if (preferredFocusedComponent == null) {
      preferredFocusedComponent = myHtmlPanelWrapper;
    }
    return preferredFocusedComponent;
  }

  public AsciiDocHtmlPanel getPanel() {
    return myPanel;
  }

  /**
   * Get the editor displayable name.
   *
   * @return <code>AsciiDoc</code>
   */
  @Override
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
  @Override
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
  @Override
  public void setState(@NotNull FileEditorState state) {
  }

  /**
   * Indicates whether the document content is modified compared to its file.
   *
   * @return {@code false} as {@link AsciiDocPreviewEditor} is read-only.
   */
  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  /**
   * Invoked when the editor is selected.
   * <p/>
   * Refresh view on select (as dependent elements might have changed).
   */
  @Override
  public void selectNotify() {
    if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
      ApplicationManager.getApplication().invokeLater(() -> {
        // don't try to run save-all in parallel, therefore synchronize
        ApplicationManager.getApplication().runWriteAction(() -> {
          // project might be already closed (yes, this really happens when you work in multiple projects opened in separate windows)
          if (!project.isDisposed()) {
            // save the content in all other editors as their content might be referenced in preview
            // don't use ApplicationManager.getApplication().saveAll() as it will save in the background and will save settings as well
            for (Document unsavedDocument : FileDocumentManager.getInstance().getUnsavedDocuments()) {
              FileDocumentManager.getInstance().saveDocument(unsavedDocument);
            }
            reprocessAnnotations();
            forceRenderCycle(); // force a refresh of the preview by resetting the current memorized content
            renderIfVisible();
          }
        });
      });
    } else {
      if (!project.isDisposed()) {
        reprocessAnnotations();
        forceRenderCycle(); // force a refresh of the preview by resetting the current memorized content
        renderIfVisible();
      }
    }
  }

  private void reprocessAnnotations() {
    PsiDocumentManager pm = PsiDocumentManager.getInstance(project);
    if (pm != null) {
      PsiFile psiFile = pm.getPsiFile(document);
      if (psiFile != null && psiFile.isValid()) {
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
      }
    }
  }

  /**
   * Invoked when the editor is deselected (it does not mean that it is not visible).
   * <p/>
   * Does nothing.
   */
  @Override
  public void deselectNotify() {
  }

  /**
   * Add specified listener.
   * <p/>
   * Does nothing.
   *
   * @param listener the listener.
   */
  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  /**
   * Remove specified listener.
   * <p/>
   * Does nothing.
   *
   * @param listener the listener.
   */
  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  /**
   * Get the background editor highlighter.
   *
   * @return {@code null} as {@link AsciiDocPreviewEditor} does not require highlighting.
   */
  @Override
  @Nullable
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  /**
   * Get the current location.
   *
   * @return {@code null} as {@link AsciiDocPreviewEditor} is not navigable.
   */
  @Override
  @Nullable
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  /**
   * Get the structure view builder.
   *
   * @return TODO {@code null} as parsing/PSI is not implemented.
   */
  @Override
  @Nullable
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  /**
   * Dispose the editor.
   */
  @Override
  public void dispose() {
    if (myPanel != null) {
      Disposer.dispose(myPanel);
    }
    AsciiDocWrapper.cleanupImagesPath(tempImagesPath);
  }

  void scrollToLine(int line) {
    targetLineNo = line;
    boolean executed = false;
    synchronized (lazyExecutor) {
      if (lazyExecutor.isIdle()) {
        lazyExecutor.execute(() -> {
          if (myPanel != null) {
            myPanel.scrollToLine(line, document.getLineCount());
          }
        });
        executed = true;
      }
    }
    if (!executed) {
      renderIfVisible();
    }
  }

  private class MyUpdatePanelOnSettingsChangedListener implements AsciiDocApplicationSettings.SettingsChangedListener {
    @Override
    public void onSettingsChange(@NotNull AsciiDocApplicationSettings settings) {
      ApplicationManager.getApplication().invokeLater(() -> {
        reprocessAnnotations();

        // trigger re-parsing of content as language injection might have changed
        // TODO - doesn't work reliably yet when switching back-and-forth
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
          FileContentUtilCore.reparseFiles(file);
        }

        final AsciiDocHtmlPanelProvider newPanelProvider = retrievePanelProvider(settings);
        if (!mySwingAlarm.isDisposed()) {
          mySwingAlarm.addRequest(() -> {
            try {
              if (!mySwingAlarm.isDisposed()) {
                synchronized (this) {
                  myPanel = detachOldPanelAndCreateAndAttachNewOne(document, tempImagesPath, myHtmlPanelWrapper, myPanel, newPanelProvider);
                  forceRenderCycle(); // force a refresh of the preview by resetting the current memorized content
                }
                renderIfVisible();
              }
            } catch (Exception ex) {
              LOG.error("unhandled exception when preparing the preview", ex);
            }
          }, 0, ModalityState.stateForComponent(getComponent()));
        }
      });
    }
  }

  public Editor getEditor() {
    return editor;
  }

  public void setEditor(Editor editor) {
    this.editor = editor;
  }

  private class MyEditorColorsListener implements EditorColorsListener {
    @Override
    public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
      final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
      // reset contents in preview with latest CSS headers
      if (settings.getAsciiDocPreviewSettings().getPreviewTheme() == AsciiDocHtmlPanel.PreviewTheme.INTELLIJ) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          forceRenderCycle();
          if (myPanel != null) {
            myPanel.setHtml("", Collections.emptyMap());
            renderIfVisible();
          }
        });
      }
    }
  }

  private class MyTrustChangedListener implements TrustStateListener {
    @Override
    public void onProjectTrusted(@NotNull Project project) {
      // opening a project in non-trusted mode forces the SECURE mode on preview rendering
      // making the project trusted should therefore force re-rendering of the preview.
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        forceRenderCycle();
        AsciiDocHtmlPanel localPanel = myPanel;
        if (localPanel != null) {
          localPanel.setHtml("", Collections.emptyMap());
        }
        renderIfVisible();
      });
    }
  }

}
