package org.asciidoc.intellij.errorHandler;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import io.sentry.DuplicateEventDetectionEventProcessor;
import io.sentry.ILogger;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Contexts;
import io.sentry.protocol.Message;
import io.sentry.protocol.OperatingSystem;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.SentryRuntime;
import org.asciidoc.intellij.AsciiDocPlugin;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.jetbrains.annotations.Nullable;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


class SentryErrorReporter {
  private static final Logger LOG = Logger.getInstance(SentryErrorReporter.class);

  private static boolean initialized;

  static synchronized void initialize() {
    if (!initialized) {
      Sentry.init(options -> {
        options.setDsn("https://ad6d66fcd1704192b283e054a3e68ba8@o502663.ingest.sentry.io/5585225");
        options.setRelease(getPluginVersion(AsciiDocPlugin.PLUGIN_ID));
        options.getEventProcessors().removeIf(
          // this duplicate event processor might remove messages based on the exception only;
          // users might have written different error messages that might then be discarded
          // therefore remove this handler
          eventProcessor -> eventProcessor instanceof DuplicateEventDetectionEventProcessor
        );
        options.setLogger(new ILogger() {
          @Override
          public void log(SentryLevel level, String message, Object... args) {
            if (!isEnabled(level)) {
              return;
            }
            message = new Formatter().format(message, args).toString();
            logInternal(level, null, message);
          }

          @Override
          public void log(SentryLevel level, String message, Throwable throwable) {
            if (!isEnabled(level)) {
              return;
            }
            logInternal(level, throwable, message);
          }

          @Override
          public void log(SentryLevel level, Throwable throwable, String message, Object... args) {
            if (!isEnabled(level)) {
              return;
            }
            message = new Formatter().format(message, args).toString();
            logInternal(level, throwable, message);
          }

          @Override
          public boolean isEnabled(@Nullable SentryLevel level) {
            if (level == SentryLevel.DEBUG) {
              return LOG.isDebugEnabled();
            }
            return true;
          }

        });
        options.setBeforeSend((event, hint) -> {

          final OperatingSystem os = new OperatingSystem();
          os.setName(SystemInfo.OS_NAME);
          os.setVersion(SystemInfo.OS_VERSION + "-" + SystemInfo.OS_ARCH);

          final ApplicationInfoImpl applicationInfo = (ApplicationInfoImpl) ApplicationInfo.getInstance();
          final SentryRuntime runtime = new SentryRuntime();
          runtime.setName(applicationInfo.getBuild().getProductCode());
          runtime.setVersion(applicationInfo.getFullVersion());

          event.getContexts().setOperatingSystem(os);
          event.getContexts().setRuntime(runtime);

          event.setTag("java_vendor", SystemInfo.JAVA_VENDOR);
          event.setTag("java_version", SystemInfo.JAVA_VERSION);

          SentryErrorReporter.fillActivePlugins(event.getContexts());

          return event;
        });
      });
      initialized = true;
    }
  }

  private static void logInternal(SentryLevel level, Throwable throwable, String message) {
    switch (level) {
      case DEBUG:
        LOG.debug(message, throwable);
        break;
      case INFO:
        LOG.info(message, throwable);
        break;
      case WARNING:
        LOG.warn(message, throwable);
        break;
      case ERROR:
        LOG.debug(message, throwable);
        break;
      case FATAL:
        LOG.debug(message, throwable);
        break;
      default:
        throw new IllegalStateException("unknown value for level: " + level);
    }
  }

  static void submitErrorReport(Throwable error, List<Attachment> attachments, String description, Consumer<? super SubmittedReportInfo> consumer) {
    initialize();
    SentryEvent event = new SentryEvent();
    Message message = new Message();

    if (!StringUtil.isEmptyOrSpaces(description)) {
      message.setMessage(description);
      event.setTag("with-description", "true");
    }
    event.setMessage(message);
    event.setLevel(SentryLevel.ERROR);
    event.setThrowable(error);


    // Local Scope
    Sentry.withScope(
      scope -> {
        if (attachments != null) {
          for (Attachment attachment : attachments) {
            io.sentry.Attachment fileAttachment = new io.sentry.Attachment(attachment.getBytes(), attachment.getPath());
            scope.addAttachment(fileAttachment);
          }
        }

        SentryId sentryId = Sentry.captureEvent(event);

        if (!Objects.equals(sentryId, SentryId.EMPTY_ID)) {
          LOG.info("Sentry event reported: " + sentryId);
          consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
            .createNotification("Error report submitted", "Error report with ID " + sentryId + " submitted.", NotificationType.INFORMATION, null);
          notification.setImportant(false);
          Notifications.Bus.notify(notification);
        } else {
          Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
            .createNotification("Unable to send error report", "Unable to send error report to Sentry server", NotificationType.WARNING, null);
          notification.setImportant(false);
          Notifications.Bus.notify(notification);
          LOG.warn("Unable to submit Sentry error information");
          consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED));
        }
      });

  }

  @Nullable
  private static String getPluginVersion(String pluginKey) {
    final PluginId pluginId = PluginId.getId(pluginKey);
    final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
    if (plugin == null) {
      return null;
    }

    return plugin.getVersion();
  }

  private static void fillActivePlugins(Contexts contexts) {
    final Map<String, Object> bundledPlugins = new HashMap<>();
    final Map<String, Object> activePlugins = new HashMap<>();
    for (IdeaPluginDescriptor plugin : PluginManager.getPlugins()) {
      if (!plugin.isEnabled()) {
        continue;
      }

      if (plugin.isBundled()) {
        bundledPlugins.put(plugin.getName(), plugin.getVersion());
      } else {
        activePlugins.put(plugin.getName(), plugin.getVersion());
      }
    }

    if (!bundledPlugins.isEmpty()) {
      contexts.put("bundled plugins", bundledPlugins);
    }

    if (!activePlugins.isEmpty()) {
      contexts.put("active plugins", activePlugins);
    }
  }
}
