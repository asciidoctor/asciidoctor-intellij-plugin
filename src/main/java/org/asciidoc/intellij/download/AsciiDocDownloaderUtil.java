// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.asciidoc.intellij.download;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.DownloadableFileService;
import com.intellij.util.download.FileDownloader;
import org.apache.commons.codec.digest.DigestUtils;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AsciiDocDownloaderUtil {

  private static final Logger LOG = Logger.getInstance(AsciiDocDownloaderUtil.class);

  public static final String ASCIIDOCTORJ_PDF_VERSION = "1.5.3";
  private static final String ASCIIDOCTORJ_PDF_HASH = "ccaccbef0af5b5e836ff983f5ed682ff3e063851";

  public static final String ASCIIDOCTORJ_DIAGRAM_VERSION = "2.0.2";
  private static final String ASCIIDOCTORJ_DIAGRAM_HASH = "f0b7b9bcecc20a7aeece8733996d3fe75eb7ed5b";

  private static final String DOWNLOAD_CACHE_DIRECTORY = "download-cache";
  // this is similar to the path where for example the grazie plugin places its dictionaries
  // content shouldn't be placed in the plugin's folder, as this will be replace upon plugin updates
  private static final String DOWNLOAD_PATH = PathManager.getSystemPath() + File.separator + DOWNLOAD_CACHE_DIRECTORY + File.separator + "asciidoctor-intellij-plugin";

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
    String fileName = DOWNLOAD_PATH + File.separator + "asciidoctorj-pdf-" + ASCIIDOCTORJ_DIAGRAM_VERSION + ".jar";
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
    String url = "https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-diagram/" +
      ASCIIDOCTORJ_DIAGRAM_VERSION +
      "/asciidoctorj-diagram-" +
      ASCIIDOCTORJ_DIAGRAM_VERSION +
      ".jar";
    download(downloadName, url, ASCIIDOCTORJ_DIAGRAM_HASH, project, onSuccess, onFailure);
  }

  public static void downloadAsciidoctorJPdf(@Nullable Project project, @NotNull Runnable onSuccess, @NotNull Consumer<Throwable> onFailure) {
    String downloadName = "asciidoctorj-pdf-" + ASCIIDOCTORJ_DIAGRAM_VERSION + ".jar";
    String url = "https://repo1.maven.org/maven2/org/asciidoctor/asciidoctorj-pdf/" +
      ASCIIDOCTORJ_PDF_VERSION +
      "/asciidoctorj-pdf-" +
      ASCIIDOCTORJ_PDF_VERSION +
      ".jar";
    download(downloadName, url, ASCIIDOCTORJ_PDF_HASH, project, onSuccess, onFailure);
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
          List<Pair<File, DownloadableFileDescription>> pairs = downloader.download(new File(DOWNLOAD_PATH));
          Pair<File, DownloadableFileDescription> first = ContainerUtil.getFirstItem(pairs);
          File file = first != null ? first.first : null;
          if (file != null) {
            try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
              String hash = DigestUtils.sha1Hex(is);
              if (!downloadHash.equals(hash)) {
                throw new IOException("Hash of downloaded file '" + file.getAbsolutePath() + "' doesn't match (expected: " + ASCIIDOCTORJ_PDF_HASH + ", got: " + hash);
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
          }
        } catch (IOException e) {
          LOG.warn("Can't download content '" + downloadUrl + "' as '" + fileName + "'", e);
          ApplicationManager.getApplication().invokeLater(() -> onFailure.consume(e));
        }
      }
    };
    BackgroundableProcessIndicator processIndicator = new BackgroundableProcessIndicator(task);
    processIndicator.setIndeterminate(false);
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, processIndicator);
  }

}
