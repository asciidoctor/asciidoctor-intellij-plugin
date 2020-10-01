package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class ImageService {

  private static final int FALLBACK_IMAGE_WIDTH = 0;

  // Currently it is only possible to return the width from PNG and JPEG files
  public static CompletableFuture<Integer> getImageWidth(final VirtualFile virtualFile) {
    return virtualFile == null
      ? CompletableFuture.completedFuture(FALLBACK_IMAGE_WIDTH)
      : CompletableFuture.supplyAsync(() -> extractImageWidth(virtualFile));
  }

  private static int extractImageWidth(final VirtualFile virtualFile) {
    BufferedImage image = readVirtualFile(virtualFile);

    return image != null
      ? image.getWidth()
      : FALLBACK_IMAGE_WIDTH;
  }

  private static BufferedImage readVirtualFile(final VirtualFile virtualFile) {
    try {
      return ImageIO.read(virtualFile.getInputStream());
    } catch (IOException exception) {
      pushErrorNotification(exception);
      return null;
    }
  }

  private static void pushErrorNotification(final IOException exception) {
    Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP.createNotification(
      "Error in plugin",
      "Failed to load image file: " + exception.getMessage(),
      NotificationType.ERROR,
      null
    );
    notification.setImportant(true);
    Notifications.Bus.notify(notification);
  }
}
