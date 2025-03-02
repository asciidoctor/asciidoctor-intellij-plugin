package org.asciidoc.intellij.activities;

import com.intellij.ide.plugins.DynamicPluginVetoer;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import org.asciidoc.intellij.AsciiDocPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AsciiDocHandleUnloadVetoer implements DynamicPluginVetoer {

  @Override
  public @Nullable String vetoPluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    if (Objects.equals(pluginDescriptor.getPluginId().getIdString(), AsciiDocPlugin.PLUGIN_ID)) {
      // before trying to re-enable this for internal mode, try to unload plugin in development mode and analyze heap dumps.
      // if (!ApplicationManager.getApplication().isInternal()) {
      return "The AsciiDoc plugin can't be dynamically unloaded. Please restart the IDE.";
      // }
      // Pending: https://youtrack.jetbrains.com/issue/IJPL-18535/, https://youtrack.jetbrains.com/issue/IJPL-166041/, https://youtrack.jetbrains.com/issue/IJPL-166040/
      // AsciiDoc.checkUnloadPlugin();
    } else {
      return null;
    }
  }
}
