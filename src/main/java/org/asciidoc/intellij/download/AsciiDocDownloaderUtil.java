// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.asciidoc.intellij.download;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.DownloadableFileService;
import com.intellij.util.download.FileDownloader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Helps downloading additional resources from the Internet.
 * It automates the download and provides callbacks once the files are present.
 * It displays notifications when the download fails and allows the user to pick a file that they downloaded
 * manually and moves it to the target folder.
 */
public class AsciiDocDownloaderUtil {

  private static final Logger LOG = Logger.getInstance(AsciiDocDownloaderUtil.class);

  // when updating the version, also update the SHA1 hash!
  // https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-pdf
  public static final String ASCIIDOCTORJ_PDF_VERSION = "1.5.4";
  private static final String ASCIIDOCTORJ_PDF_HASH = "0f4dc4e8ac02ab6244fd0c769532ea6c2295519b";

  // when updating the version, also update the SHA1 hash!
  // https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-diagram
  public static final String ASCIIDOCTORJ_DIAGRAM_VERSION = "2.0.5";
  private static final String ASCIIDOCTORJ_DIAGRAM_HASH = "f4a408317f6c2e5bf42f26547185c66685b32957";

  private static final String DOWNLOAD_CACHE_DIRECTORY = "download-cache";
  // this is similar to the path where for example the grazie plugin places its dictionaries
  // content shouldn't be placed in the plugin's folder, as this will be replace upon plugin updates
  public static final String DOWNLOAD_PATH = PathManager.getSystemPath() + File.separator + DOWNLOAD_CACHE_DIRECTORY + File.separator + "asciidoctor-intellij-plugin";

  public static boolean downloadComplete() {
    return downloadCompleteAsciidoctorJPdf() && downloadCompleteAsciidoctorJDiagram();
  }

  public static boolean downloadCompleteAsciidoctorJDiagram() {
    File file = getAsciidoctorJDiagramFile();
    return file.exists();
  }

  public static boolean downloadCompleteAsciidoctorJPdf() {
    File file = getAsciidoctorJPdfFile();
    return file.exists();
  }

  public static File getAsciidoctorJPdfFile() {
    String fileName = DOWNLOAD_PATH + File.separator + "asciidoctorj-pdf-" + ASCIIDOCTORJ_PDF_VERSION + ".jar";
    return new File(fileName);
  }

  public static File getAsciidoctorJDiagramFile() {
    String fileName = DOWNLOAD_PATH + File.separator + "asciidoctorj-diagram-" + ASCIIDOCTORJ_DIAGRAM_VERSION + ".jar";
    return new File(fileName);
  }

  public static void download(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    downloadAsciidoctorJPdf(project, () -> downloadAsciidoctorJDiagram(project, onSuccess, onFailure), onFailure);
  }

  public static void downloadAsciidoctorJDiagram(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    String downloadName = "asciidoctorj-diagram-" + ASCIIDOCTORJ_DIAGRAM_VERSION + ".jar";
    String url = getAsciidoctorJDiagramUrl();
    download(downloadName, url, ASCIIDOCTORJ_DIAGRAM_HASH, project, onSuccess, onFailure);
  }

  public static void pickAsciidoctorJDiagram(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    String downloadName = "asciidoctorj-diagram-" + ASCIIDOCTORJ_DIAGRAM_VERSION + ".jar";
    try {
      pickFile(downloadName, project, ASCIIDOCTORJ_DIAGRAM_HASH, onSuccess);
    } catch (IOException ex) {
      LOG.warn("Can't pick file '" + downloadName + "'", ex);
      ApplicationManager.getApplication().invokeLater(() -> onFailure.consume(ex));
    }
  }

  public static void pickAsciidoctorJPdf(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    String downloadName = "asciidoctorj-pdf-" + ASCIIDOCTORJ_PDF_VERSION + ".jar";
    try {
      pickFile(downloadName, project, ASCIIDOCTORJ_PDF_HASH, onSuccess);
    } catch (IOException ex) {
      LOG.warn("Can't pick file '" + downloadName + "'", ex);
      ApplicationManager.getApplication().invokeLater(() -> onFailure.consume(ex));
    }
  }

  public static String getAsciidoctorJDiagramUrl() {
    return "https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-diagram/" +
      ASCIIDOCTORJ_DIAGRAM_VERSION +
      "/asciidoctorj-diagram-" +
      ASCIIDOCTORJ_DIAGRAM_VERSION +
      ".jar";
  }

  public static void downloadAsciidoctorJPdf(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    String downloadName = "asciidoctorj-pdf-" + ASCIIDOCTORJ_PDF_VERSION + ".jar";
    String url = getAsciidoctorJPdfUrl();
    download(downloadName, url, ASCIIDOCTORJ_PDF_HASH, project, onSuccess, onFailure);
  }

  public static String getAsciidoctorJPdfUrl() {
    return "https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-pdf/" +
      ASCIIDOCTORJ_PDF_VERSION +
      "/asciidoctorj-pdf-" +
      ASCIIDOCTORJ_PDF_VERSION +
      ".jar";
  }

  private static void download(String downloadName, String downloadUrl, String downloadHash, @Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    File directory = new File(DOWNLOAD_PATH);
    if (!directory.exists()) {
      //noinspection ResultOfMethodCallIgnored
      directory.mkdirs();
    }

    String fileName = DOWNLOAD_PATH + File.separator + downloadName;
    File file = new File(fileName);
    if (file.exists()) {
      ApplicationManager.getApplication().invokeLater(onSuccess);
      return;
    }

    DownloadableFileService service = DownloadableFileService.getInstance();
    DownloadableFileDescription description = service.createFileDescription(downloadUrl, downloadName + ".part");
    FileDownloader downloader = service.createDownloader(Collections.singletonList(description), downloadName + ".part");

    Task.Backgroundable task = new Task.Backgroundable(project, AsciiDocBundle.message("asciidoc.download.task")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          List<VirtualFile> files = downloader.downloadFilesWithProgress(DOWNLOAD_PATH, project, null);
          if (files == null || files.size() == 0) {
            throw new IOException("Download failed");
          }
          File file = files.get(0).toNioPath().toFile();
          try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            String hash = DigestUtils.sha1Hex(is);
            if (!downloadHash.equals(hash)) {
              throw new IOException("Hash of downloaded file doesn't match (expected: " + downloadHash + ", got: " + hash + ")");
            }
          }
          if (!file.renameTo(new File(fileName))) {
            throw new IOException("Unable to rename file '" + file.getAbsolutePath() + "' +  to + '" + downloadName + "'");
          }
          ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().getMessageBus()
              .syncPublisher(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC)
              .onSettingsChange(AsciiDocApplicationSettings.getInstance());
            onSuccess.run();
          });
        } catch (IOException e) {
          LOG.warn("Can't download content '" + downloadUrl + "' as '" + fileName + "'", e);
          createFailureNotification(e, true);
          ApplicationManager.getApplication().invokeLater(() -> onFailure.consume(e));
        }
      }

      private void createFailureNotification(IOException e, boolean download) {
        Notifications.Bus
          .notify(new Notification("asciidoc", AsciiDocBundle.message("asciidoc.download.title"),
              (download ? AsciiDocBundle.message("asciidoc.download.failed") + ": " + "Can't download <a href=\"" + downloadUrl + "\">" + downloadUrl + "</a>" : "Copy failed. Can't copy " + downloadName) + " to folder " + directory.getAbsolutePath() + ": " + e.getMessage() + "."
                + (download ? "" : " If you haven't downloaded the file yet, you can download it from <a href=\"" + downloadUrl + "\">" + downloadUrl + "</a>"),
              NotificationType.ERROR,
              new NotificationListener.UrlOpeningListener(false)
            )
              .addAction(NotificationAction.createSimpleExpiring(
                "Retry download", () -> download(downloadName, downloadUrl, downloadHash, project, onSuccess, onFailure)))
              .addAction(NotificationAction.createSimpleExpiring(
                "Pick local file...", () -> {
                  try {
                    pickFile(downloadName, project, downloadHash, onSuccess);
                  } catch (IOException ex) {
                    LOG.warn("Can't pick file '" + downloadUrl + "' as '" + fileName + "'", ex);
                    createFailureNotification(ex, false);
                    ApplicationManager.getApplication().invokeLater(() -> onFailure.consume(ex));
                  }
                }
              )),
            project);
      }
    };
    BackgroundableProcessIndicator processIndicator = new BackgroundableProcessIndicator(task);
    processIndicator.setIndeterminate(false);
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, processIndicator);
  }

  private static void pickFile(String downloadName, @Nullable Project project, String downloadHash, @NotNull Runnable onSuccess) throws IOException {
    File directory = new File(DOWNLOAD_PATH);
    if (!directory.exists()) {
      //noinspection ResultOfMethodCallIgnored
      directory.mkdirs();
    }
    @NotNull VirtualFile[] virtualFiles = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFileDescriptor()
        .withFileFilter(virtualFile -> virtualFile.getName().equals(downloadName))
        .withTitle("Pick Local File...")
        .withDescription("Please select file '" + downloadName + "' on your local disk."),
      project, null);
    if (virtualFiles.length == 1) {
      if (virtualFiles[0].isDirectory()) {
        throw new IOException("Directory selected. Please select a file named '" + downloadName + "'!");
      }
      if (!virtualFiles[0].getName().equals(downloadName)) {
        throw new IOException("Wrong file selected. Please select a file named '" + downloadName + "'!");
      }
      try (BufferedInputStream is = new BufferedInputStream(virtualFiles[0].getInputStream())) {
        String hash = DigestUtils.sha1Hex(is);
        if (!downloadHash.equals(hash)) {
          throw new IOException("Hash of selected file doesn't match (expected: " + ASCIIDOCTORJ_PDF_HASH + ", got: " + hash + ")");
        }
      }
      Path sourcePath = virtualFiles[0].getFileSystem().getNioPath(virtualFiles[0]);
      if (sourcePath == null) {
        throw new IOException("unable to so pick source path");
      }
      FileUtils.copyFile(sourcePath.toFile(), new File(directory, downloadName));
      ApplicationManager.getApplication().invokeLater(() -> {
        ApplicationManager.getApplication().getMessageBus()
          .syncPublisher(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC)
          .onSettingsChange(AsciiDocApplicationSettings.getInstance());
        onSuccess.run();
      });
    } else {
      throw new IOException("No file selected. Please select a file named '" + downloadName + "'!");
    }
  }

}
