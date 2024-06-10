package org.asciidoc.intellij.settings;

import com.intellij.ide.impl.TrustedProjects;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ThreeState;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import org.asciidoctor.SafeMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@State(
  name = "AsciidocApplicationSettings",
  storages = @Storage("asciidoc.xml")
)
@SuppressWarnings("SameNameButDifferent")
public class AsciiDocApplicationSettings implements PersistentStateComponent<AsciiDocApplicationSettings.State>,
  AsciiDocPreviewSettings.Holder {

  private final State myState = new State();

  /* this is a transient state, will be discarded on every restart
    as the setting is changed for each project, we keep a state for each project.
   */
  private final Map<String, Boolean> extensionsEnabled = new ConcurrentHashMap<>();
  private final Map<String, Boolean> extensionsPresent = new ConcurrentHashMap<>();

  @NotNull
  public static AsciiDocApplicationSettings getInstance() {
    return ApplicationManager.getApplication().getService(AsciiDocApplicationSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  @Override
  public void setAsciiDocPreviewSettings(@NotNull AsciiDocPreviewSettings settings) {
    myState.myPreviewSettings = settings;

    ApplicationManager.getApplication().getMessageBus().syncPublisher(SettingsChangedListener.TOPIC).onSettingsChange(this);
  }

  @NotNull
  @Override
  public AsciiDocPreviewSettings getAsciiDocPreviewSettings() {
    return myState.myPreviewSettings;
  }

  public void setExtensionsEnabled(String root, boolean extensionsEnabled) {
    this.extensionsEnabled.put(root, extensionsEnabled);
    ApplicationManager.getApplication().getMessageBus().syncPublisher(SettingsChangedListener.TOPIC).onSettingsChange(this);
  }

  public Boolean getExtensionsEnabled(Project project, String root) {
    if (TrustedProjects.getTrustedState(project) != ThreeState.YES) {
      return false;
    }
    return this.extensionsEnabled.get(root);
  }

  public void setExtensionsPresent(String projectBasePath, boolean extensionsPresent) {
    if (!Boolean.valueOf(extensionsPresent).equals(this.extensionsPresent.get(projectBasePath))) {
      this.extensionsPresent.put(projectBasePath, extensionsPresent);
      EditorNotifications.updateAll();
    }
  }

  public Boolean getExtensionsPresent(Project project, String projectBasePath) {
    if (TrustedProjects.getTrustedState(project) != ThreeState.YES) {
      return false;
    }
    return this.extensionsPresent.get(projectBasePath);
  }

  public SafeMode getSafe(Project project) {
    return myState.myPreviewSettings.getSafeMode(project);
  }

  public static class State {
    @Property(surroundWithTag = false)
    @NotNull
    private AsciiDocPreviewSettings myPreviewSettings = AsciiDocPreviewSettings.DEFAULT;
    private String pluginVersion;

    public String getPluginVersion() {
      return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
      this.pluginVersion = pluginVersion;
    }
  }

  public String getVersion() {
    return myState.getPluginVersion();
  }

  public void setVersion(String version) {
    myState.setPluginVersion(version);
  }

  public interface SettingsChangedListener {
    Topic<SettingsChangedListener> TOPIC = Topic.create("AsciiDocApplicationSettingsChanged", SettingsChangedListener.class);

    void onSettingsChange(@NotNull AsciiDocApplicationSettings settings);
  }
}
