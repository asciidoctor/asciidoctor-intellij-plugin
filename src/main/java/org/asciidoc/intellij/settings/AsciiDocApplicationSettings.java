package org.asciidoc.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@State(
  name = "AsciidocApplicationSettings",
  storages = @Storage("asciidoc.xml")
)
public class AsciiDocApplicationSettings implements PersistentStateComponent<AsciiDocApplicationSettings.State>,
  AsciiDocPreviewSettings.Holder {

  private State myState = new State();

  /* this is a transient state, will be discarded on every restart
    as the setting is changed for each project, we keep a state for each project.
   */
  private Map<String, Boolean> extensionsEnabled = new ConcurrentHashMap<>();
  private Map<String, Boolean> extensionsPresent = new ConcurrentHashMap<>();

  @NotNull
  public static AsciiDocApplicationSettings getInstance() {
    return ServiceManager.getService(AsciiDocApplicationSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
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

  public void setExtensionsEnabled(String projectBasePath, boolean extensionsEnabled) {
    this.extensionsEnabled.put(projectBasePath, extensionsEnabled);
    ApplicationManager.getApplication().getMessageBus().syncPublisher(SettingsChangedListener.TOPIC).onSettingsChange(this);
  }

  public Boolean getExtensionsEnabled(String projectBasePath) {
    return this.extensionsEnabled.get(projectBasePath);
  }

  public void setExtensionsPresent(String projectBasePath, boolean extensionsPresent) {
    if (!Boolean.valueOf(extensionsPresent).equals(this.extensionsPresent.get(projectBasePath))) {
      this.extensionsPresent.put(projectBasePath, extensionsPresent);
      EditorNotifications.updateAll();
    }
  }

  public Boolean getExtensionsPresent(String projectBasePath) {
    return this.extensionsPresent.get(projectBasePath);
  }

  public static class State {
    @Property(surroundWithTag = false)
    @NotNull
    private AsciiDocPreviewSettings myPreviewSettings = AsciiDocPreviewSettings.DEFAULT;
  }

  public interface SettingsChangedListener {
    Topic<SettingsChangedListener> TOPIC = Topic.create("AsciiDocApplicationSettingsChanged", SettingsChangedListener.class);

    void onSettingsChange(@NotNull AsciiDocApplicationSettings settings);
  }
}
