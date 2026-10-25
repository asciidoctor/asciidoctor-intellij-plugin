package org.asciidoc.intellij.errorHandler;

import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.util.Consumer;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

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
      if (throwable == null || throwable.getStackTrace().length == 0) {
        // errors from the IDE's error dialog come as a text-only throwable
        throwable = StackTraceParser.parse(event.getThrowableText());
      }
      SentryErrorReporter.submitErrorReport(throwable, event.getAttachments(), additionalInfo, consumer);
    }

    return true;
  }
}
