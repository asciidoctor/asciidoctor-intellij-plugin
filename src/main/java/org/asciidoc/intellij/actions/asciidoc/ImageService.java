package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDoc;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ImageService {

  // Currently it is only possible to return the width from PNG and JPEG files
  public static CompletableFuture<Optional<Integer>> getImageWidth(final VirtualFile virtualFile) {
    return virtualFile == null
      ? CompletableFuture.completedFuture(Optional.empty())
      : CompletableFuture.supplyAsync(() -> extractImageWidth(virtualFile));
  }

  private static Optional<Integer> extractImageWidth(final VirtualFile virtualFile) {
    return Optional.ofNullable(readVirtualFile(virtualFile))
      .map(BufferedImage::getWidth);
  }

  @Nullable
  private static BufferedImage readVirtualFile(final VirtualFile virtualFile) {
    try {
      return ImageIO.read(virtualFile.getInputStream());
    } catch (IOException exception) {
      pushErrorNotification(exception);
      return null;
    }
  }

  private static void pushErrorNotification(final IOException exception) {
    Notification notification = AsciiDoc.getNotificationGroup().createNotification(
      "Error in plugin",
      "Failed to load image file: " + exception.getMessage(),
      NotificationType.ERROR);
    notification.setImportant(true);
    Notifications.Bus.notify(notification);
  }
}
