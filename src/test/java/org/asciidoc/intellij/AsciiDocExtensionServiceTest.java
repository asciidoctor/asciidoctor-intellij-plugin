package org.asciidoc.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.compress.utils.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AsciiDocExtensionServiceTest {

  private static final String ASCIIDOCTOR_CONFIG_FILE_NAME = ".asciidoctor";
  private static final String LIB_DIRECTORY_NAME = "lib";

  @Mock
  private Project project;

  @Mock
  private VirtualFile virtualFile;

  private MockedStatic<ProjectUtil> mockedProjectUtil;
  private AsciiDocExtensionService service;

  @Before
  public void setup() {
    mockedProjectUtil = Mockito.mockStatic(ProjectUtil.class);

    service = new AsciiDocExtensionService();
  }

  @After
  public void teardown() {
    mockedProjectUtil.close();
  }

  @Test
  public void shouldReturnEmptyListIfProjectDirCannotBeGuessed() {
    mockProjectDir(null);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
  }

  @Test
  public void shouldReturnEmptyListIfUnableToFindAsciidoctorConfig() {
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(null);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
  }

  @Test
  public void shouldReturnEmptyListIfUnableToFindLibDirectory() {
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(null);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
  }

  @Test
  public void shouldReturnEmptyListIfLibIsNotADirectory() {
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(virtualFile);
    when(virtualFile.getChildren()).thenReturn(null);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
    verify(virtualFile).getChildren();
  }

  @Test
  public void shouldReturnEmptyListIfLibDirectoryContainsNoFiles() {
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(virtualFile);
    when(virtualFile.getChildren()).thenReturn(new VirtualFile[0]);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
    verify(virtualFile).getChildren();
  }

  @Test
  public void shouldReturnEmptyListIfLibDirDoesNotContainExtensionFiles() {
    final VirtualFile fileWithNoExtension = createVirtualFileMock(null);
    final VirtualFile nonAsciiDocExtensionFile = createVirtualFileMock("xml");
    final VirtualFile[] libFiles = new VirtualFile[]{fileWithNoExtension, nonAsciiDocExtensionFile};
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(virtualFile);
    when(virtualFile.getChildren()).thenReturn(libFiles);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
    verify(virtualFile).getChildren();
    verify(fileWithNoExtension).getExtension();
    verify(nonAsciiDocExtensionFile).getExtension();
  }

  @Test
  public void shouldReturnEmptyListIfCanonicalPathExtensionFilesCannotBeResolved() {
    final VirtualFile jarExtensionFile = createVirtualFileMock("jar", null);
    final VirtualFile rubyExtensionFile = createVirtualFileMock("rb", null);
    final VirtualFile[] libFiles = new VirtualFile[]{jarExtensionFile, rubyExtensionFile};
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(virtualFile);
    when(virtualFile.getChildren()).thenReturn(libFiles);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(newArrayList(), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
    verify(virtualFile).getChildren();
    verify(jarExtensionFile).getExtension();
    verify(jarExtensionFile).getCanonicalPath();
    verify(rubyExtensionFile).getExtension();
    verify(rubyExtensionFile).getCanonicalPath();
  }

  @Test
  public void shouldReturnListOfCanonicalPathsOfExtensionFiles() {
    final String path1 = "path/to/jar";
    final String path2 = "path/To/rb";
    final VirtualFile jarExtensionFile = createVirtualFileMock("jar", path1);
    final VirtualFile rubyExtensionFile = createVirtualFileMock("rb", path2);
    final VirtualFile[] libFiles = new VirtualFile[]{jarExtensionFile, rubyExtensionFile};
    mockProjectDir(virtualFile);
    when(virtualFile.findChild(ASCIIDOCTOR_CONFIG_FILE_NAME)).thenReturn(virtualFile);
    when(virtualFile.findChild(LIB_DIRECTORY_NAME)).thenReturn(virtualFile);
    when(virtualFile.getChildren()).thenReturn(libFiles);

    final List<String> extensions = service.getExtensions(project);

    assertEquals(Arrays.asList(path1, path2), extensions);
    verify(virtualFile).findChild(ASCIIDOCTOR_CONFIG_FILE_NAME);
    verify(virtualFile).findChild(LIB_DIRECTORY_NAME);
    verify(virtualFile).getChildren();
    verify(jarExtensionFile).getExtension();
    verify(jarExtensionFile).getCanonicalPath();
    verify(rubyExtensionFile).getExtension();
    verify(rubyExtensionFile).getCanonicalPath();
  }

  private void mockProjectDir(final VirtualFile projectDirFile) {
    mockedProjectUtil
      .when(() -> ProjectUtil.guessProjectDir(project))
      .thenReturn(projectDirFile);
  }

  private VirtualFile createVirtualFileMock(final @Nullable String extension) {
    final VirtualFile file = mock(VirtualFile.class);

    when(file.getExtension()).thenReturn(extension);

    return file;
  }

  private VirtualFile createVirtualFileMock(final @Nullable String extension, final @Nullable String path) {
    final VirtualFile file = createVirtualFileMock(extension);

    when(file.getCanonicalPath()).thenReturn(path);

    return file;
  }
}
