package org.asciidoc.intellij.activities;

import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.ide.plugins.CannotUnloadPluginException;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import org.asciidoc.intellij.AsciiDoc;
import org.asciidoc.intellij.AsciiDocPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This takes care of unloading the plugin on uninstalls or updates.
 * WARNING: A dynamic unload will usually only succeed when the application is NOT in debug mode;
 * classes might be marked as "JNI Global" due to this, and not reclaimed, and then unloading fails.
 */
public class AsciiDocHandleUnloadActivity implements StartupActivity, DumbAware, LightEditCompatible {

  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocHandleUnloadActivity.class);

  private static boolean setupComplete;

  @Override
  public void runActivity(@NotNull Project project) {
    setupListener();
  }

  public static synchronized void setupListener() {
    if (!setupComplete) {
      setupComplete = true;
      LOG.info("setup of subscription");
      Application application = ApplicationManager.getApplication();
      if (application != null) {
        // check application first to don't interfere with unit tests, as this is not initialized for Unit tests
        application.invokeLater(() -> {
          MessageBusConnection busConnection = application.getMessageBus().connect();
          Disposer.register(application, busConnection::disconnect);
          busConnection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
            @Override
            public void checkUnloadPlugin(@NotNull IdeaPluginDescriptor pluginDescriptor) throws CannotUnloadPluginException {
              if (Objects.equals(pluginDescriptor.getPluginId().getIdString(), AsciiDocPlugin.PLUGIN_ID)) {
                LOG.info("checkUnloadPlugin");
                // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/512
                // another reason: on Windows even after unloading JAR file of the plugin still be locked and can't be deleted, making uninstall impossible
                // https://youtrack.jetbrains.com/issue/IDEA-244471
                // Update: IDEA-244471 might not be relevant here as the plugin will have the version number in the JAR file, therefore a change file will have a new name

                // before trying to re-enable this for internal mode, try to unload plugin in development mode and analyze heap dumps.
                // if (!ApplicationManager.getApplication().isInternal()) {
                throw new CannotUnloadPluginException("unloading mechanism is not safe, incomplete unloading might lead to strange exceptions");
                // }
                // found that "IdeScriptEngineManagerImpl" will hold a reference to "org.jruby.embed.jsr223.JRubyEngineFactory"
                // https://youtrack.jetbrains.com/issue/IDEA-285933
                // AsciiDoc.checkUnloadPlugin();
              }
            }

            @Override
            public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
              if (Objects.equals(pluginDescriptor.getPluginId().getIdString(), AsciiDocPlugin.PLUGIN_ID)) {
                LOG.info("beforePluginUnload");
                AsciiDoc.beforePluginUnload();
                busConnection.dispose();
              }
            }
          });
        });
      }
    }
  }

}
