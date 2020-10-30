package org.asciidoc.intellij.activities;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows update notification.
 */
public class AsciiDocPluginUpdateActivity implements StartupActivity, DumbAware {

  @Override
  public void runActivity(@NotNull Project project) {
    final AsciiDocApplicationSettings settings = AsciiDocApplicationSettings.getInstance();
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("org.asciidoctor.intellij.asciidoc"));
    if (plugin == null) {
      return;
    }
    String version = plugin.getVersion();
    String oldVersion = settings.getVersion();
    boolean updated = !version.equals(oldVersion);
    if (updated) {
      settings.setVersion(version);

      // collect the recent changes the user hasn't seen yet
      StringBuilder changes = new StringBuilder();
      Matcher matcher = Pattern.compile("(?ms)<h3[^>]*>(?<version>[0-9.]+).*?</div>").matcher(plugin.getChangeNotes());
      int count = 0;
      while (matcher.find()) {
        if (matcher.group("version").equals(oldVersion)) {
          break;
        }
        count++;
        if (count > 5) {
          break;
        }
        changes.append(matcher.group());
      }
      NotificationGroup group = new NotificationGroup(plugin.getName(), NotificationDisplayType.STICKY_BALLOON, true);
      Notification notification = group.createNotification(
        AsciiDocBundle.message("asciidocUpdateNotification.title", version),
        AsciiDocBundle.message("asciidocUpdateNotification.content") +
          changes.toString()
            // simplify HTML as not all tags are shown in event log
            .replaceAll("<[/]?(div|h3|p)[^>]*>", "")
            // avoid too many new lines as they will show as new lines in event log
            .replaceAll("(?ms)<ul>\\s*", "<ul>")
            // remove trailing blanks and empty lines
            .replaceAll("(?ms)\\n[\\s]+", "\n"),
        NotificationType.INFORMATION,
        new NotificationListener.UrlOpeningListener(false)
      );

      Notifications.Bus.notify(notification, project);
    }
  }

}
