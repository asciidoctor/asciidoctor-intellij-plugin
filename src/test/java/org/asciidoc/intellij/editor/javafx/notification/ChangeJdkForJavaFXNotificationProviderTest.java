package org.asciidoc.intellij.editor.javafx.notification;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeJdkForJavaFXNotificationProviderTest {

  private VirtualFile file;
  private FileEditor fileEditor;
  private JavaFx javaFxHelper;
  private ChangeJdkForJavaFXNotificationProvider notificationProvider;
  private EditorNotificationPanel notificationPanel;
  private PreviewNotificationRepository previewNotificationRepository;

  @Before
  public void setup() {
    file = mock(VirtualFile.class);
    fileEditor = mock(FileEditor.class);
    javaFxHelper = mock(JavaFx.class);
    previewNotificationRepository = mock(PreviewNotificationRepository.class);
  }

  @Test
  public void noNotificationPanelWhenFileIsNoAsciiDoc() {
    givenNotificationProvider();
    givenFileTypeIsNotAsciidoc();

    whenNotificationPanelCreationIsAttempted();

    thenNoPanelIsCreated();
  }

  @Test
  public void noNotificationPanelWhenShowJavaFxPreviewInstructionsIsDisabled() {
    givenNoPreviewInstructions();
    givenNotificationProvider();
    givenFileTypeIsAsciidoc();

    whenNotificationPanelCreationIsAttempted();

    thenNoPanelIsCreated();
  }



  @Test
  public void noNotificationPanelWhenHtmlProviderIsOtherThanJavaFX() {
    givenShowPreviewInstructions();
    givenNotificationProvider();
    givenFileTypeIsAsciidoc();
    givenJavaFxIsNotCurrentProvider();

    whenNotificationPanelCreationIsAttempted();

    thenNoPanelIsCreated();
  }


  @Test
  public void noNotificationPanelWhenJavaFXIsAvailable() {
    givenShowPreviewInstructions();
    givenNotificationProvider();
    givenFileTypeIsAsciidoc();
    givenJavaFxIsCurrentProvider();
    givenJavaFxIsAvailable();

    whenNotificationPanelCreationIsAttempted();

    thenNoPanelIsCreated();
  }

  @Test
  public void noNotificationPanelWhenJavaFXIsStuck() {
    givenShowPreviewInstructions();
    givenNotificationProvider();
    givenFileTypeIsAsciidoc();
    givenJavaFxIsCurrentProvider();
    givenJavaFxIsNotAvailable();
    givenJavaFxIsStuck();

    whenNotificationPanelCreationIsAttempted();

    thenNoPanelIsCreated();
  }


  @Test
  public void notificationPanelIsCreatedOtherwise() {
    givenShowPreviewInstructions();
    givenNotificationProvider();
    givenFileTypeIsAsciidoc();
    givenJavaFxIsCurrentProvider();
    givenJavaFxIsNotAvailable();
    givenJavaFxIsNotStuck();

    whenNotificationPanelCreationIsAttempted();

    thenPanelIsCreated();
  }

  private void thenPanelIsCreated() {
    assertNotNull(notificationPanel);
  }

  private void givenShowPreviewInstructions() {
    when(previewNotificationRepository.isShown()).thenReturn(true);
  }

  private void givenNoPreviewInstructions() {
    when(previewNotificationRepository.isShown()).thenReturn(false);
  }

  private void givenJavaFxIsStuck() {
    when(javaFxHelper.isStuck()).thenReturn(true);
  }
  private void givenJavaFxIsNotStuck() {
    when(javaFxHelper.isStuck()).thenReturn(false);
  }
  private void givenJavaFxIsAvailable() {
    when(javaFxHelper.isAvailable()).thenReturn(true);
  }
  private void givenJavaFxIsNotAvailable() {
    when(javaFxHelper.isAvailable()).thenReturn(false);
  }

  private void givenJavaFxIsNotCurrentProvider() {
    when(javaFxHelper.isCurrentHtmlProvider(any())).thenReturn(false);
  }

  private void givenJavaFxIsCurrentProvider() {
    when(javaFxHelper.isCurrentHtmlProvider(any())).thenReturn(true);
  }

  private void givenFileTypeIsAsciidoc() {
    when(file.getFileType()).thenReturn(AsciiDocFileType.INSTANCE);
  }

  private void givenFileTypeIsNotAsciidoc() {
    when(file.getFileType()).thenReturn(null);
  }

  private void givenNotificationProvider() {
    notificationProvider = new FakeChangeJdkForJavaFXNotificationProvider(javaFxHelper, previewNotificationRepository);
  }

  private void whenNotificationPanelCreationIsAttempted() {
    notificationPanel = notificationProvider.createNotificationPanel(file, fileEditor);
  }

  private void thenNoPanelIsCreated() {
    assertNull(notificationPanel);
  }

  private static class FakeChangeJdkForJavaFXNotificationProvider extends ChangeJdkForJavaFXNotificationProvider {

    private FakeChangeJdkForJavaFXNotificationProvider(JavaFx javaFxHelper, PreviewNotificationRepository previewNotificationRepository) {
      super(javaFxHelper, previewNotificationRepository);
    }

    @NotNull
    @Override
    protected EditorNotificationPanel notificationPanelFactory(AsciiDocApplicationSettings asciiDocApplicationSettings) {
      return mock(EditorNotificationPanel.class);
    }

    protected AsciiDocApplicationSettings getAsciiDocApplicationSettings() {
      return mock(AsciiDocApplicationSettings.class);
    }
  }
}
