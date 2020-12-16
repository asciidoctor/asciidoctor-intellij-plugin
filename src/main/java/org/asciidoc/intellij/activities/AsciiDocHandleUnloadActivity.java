package org.asciidoc.intellij.activities;

import com.intellij.ide.plugins.CannotUnloadPluginException;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.AsciiDoc;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This takes care of unloading the plugin on uninstalls or updates.
 * WARNING: A dynamic unload will usually only succeed when the application is NOT in debug mode;
 * classes might be marked as "JNI Global" due to this, and not reclaimed, and then unloading fails.
 */
public class AsciiDocHandleUnloadActivity implements StartupActivity, DumbAware {

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocHandleUnloadActivity.class);

  @Override
  public void runActivity(@NotNull Project project) {
    LOG.info("setup of subscription");
    MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
    busConnection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void checkUnloadPlugin(@NotNull IdeaPluginDescriptor pluginDescriptor) throws CannotUnloadPluginException {
        if (pluginDescriptor.getPluginId() != null
          && Objects.equals(pluginDescriptor.getPluginId().getIdString(), "org.asciidoctor.intellij.asciidoc")) {
          LOG.info("checkUnloadPlugin");
          // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/512
          // another reason: on windows even after unloading JAR file of the plugin still be locked and can't be deleted, making uninstall impossible
          // https://youtrack.jetbrains.com/issue/IDEA-244471
          throw new CannotUnloadPluginException("unloading mechanism is not safe, incomplete unloading might lead to strange exceptions");
          // AsciiDoc.checkUnloadPlugin();
        }
      }

      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (pluginDescriptor.getPluginId() != null
          && Objects.equals(pluginDescriptor.getPluginId().getIdString(), "org.asciidoctor.intellij.asciidoc")) {
          LOG.info("beforePluginUnload");
          AsciiDoc.beforePluginUnload();
          busConnection.dispose();
        }
      }
    });
  }

}
