package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.BinaryLightVirtualFile;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class ImageServiceTest {

  @Test
  public void returnsZeroWhenFileIsNull() throws ExecutionException, InterruptedException {
    int imageWidth = ImageService.getImageWidth(null).get();

    assertEquals(imageWidth, 0);
  }

  @Test
  public void returnsZeroForNonPngJpegFiles()  throws ExecutionException, InterruptedException, IOException {
    int imageWidth = ImageService.getImageWidth(createVirtualFile("testFiles/test-image.svg")).get();

    assertEquals(imageWidth, 0);
  }

  @Test
  public void returnsTheActualFileWidth() throws ExecutionException, InterruptedException, IOException {
    int imageWidth = ImageService.getImageWidth(createVirtualFile("testFiles/test-image-width-20px.png")).get();

    assertEquals(imageWidth, 20);
  }

  private VirtualFile createVirtualFile(final String path) throws IOException {
    URL resource = ImageServiceTest.class.getClassLoader().getResource(path);
    byte[] fileContent = IOUtils.toByteArray(Objects.requireNonNull(resource));

    return new BinaryLightVirtualFile("TestFile", fileContent);
  }
}
