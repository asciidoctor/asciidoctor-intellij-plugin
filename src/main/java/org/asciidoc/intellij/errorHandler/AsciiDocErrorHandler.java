package org.asciidoc.intellij.errorHandler;

import com.intellij.diagnostic.AbstractMessage;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.util.Consumer;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class AsciiDocErrorHandler extends ErrorReportSubmitter {
  @NotNull
  @Override
  public String getReportActionText() {
    return "Report to AsciiDoc Plugin Developers";
  }

  @Override
  public String getPrivacyNoticeText() {
    return AsciiDocBundle.message("asciidoc.error.dialog.notice");
  }

  @Override
  public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
    for (IdeaLoggingEvent event : events) {
      Throwable throwable = event.getThrowable();
      List<Attachment> attachments = null;
      if (event.getData() instanceof AbstractMessage) {
        throwable = ((AbstractMessage) event.getData()).getThrowable();
        attachments = ((AbstractMessage) event.getData()).getIncludedAttachments();
      }
      SentryErrorReporter.submitErrorReport(throwable, attachments, additionalInfo, consumer);
    }

    return true;
  }
}
