package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.BinaryLightVirtualFile;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ImageServiceTest {

  @Test
  public void shouldReturnEmptyWhenFileIsNull() throws ExecutionException, InterruptedException {
    Optional<Integer> imageWidthOption = ImageService.getImageWidth(null).get();

    assertFalse(imageWidthOption.isPresent());
  }

  @Test
  public void shouldReturnWidthForNonPngJpegFiles() throws ExecutionException, InterruptedException, IOException {
    Optional<Integer> imageWidthOption = ImageService
      .getImageWidth(createVirtualFile("testFiles/test-image.svg"))
      .get();

    assertTrue(imageWidthOption.isPresent());
  }

  @Test
  public void shouldReturnOptionWithActualFileWidth() throws ExecutionException, InterruptedException, IOException {
    Optional<Integer> imageWidthOption = ImageService
      .getImageWidth(createVirtualFile("testFiles/test-image-width-20px.png"))
      .get();

    assertTrue(imageWidthOption.isPresent());
    assertThat(imageWidthOption.get(), is(20));
  }

  private VirtualFile createVirtualFile(final String path) throws IOException {
    URL resource = ImageServiceTest.class.getClassLoader().getResource(path);
    byte[] fileContent = IOUtils.toByteArray(Objects.requireNonNull(resource));

    return new BinaryLightVirtualFile("TestFile", fileContent);
  }
}
