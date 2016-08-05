package org.asciidoc.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
  name = "AsciidocApplicationSettings",
  storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/asciidoc.xml")
    // keep the line above to be compatible with IntellJ 15.x editions
    // use the line below with IntellJ 2016.2.x
    // @Storage("asciidoc.xml")
)
public class AsciiDocApplicationSettings implements PersistentStateComponent<AsciiDocApplicationSettings.State>,
                                                    AsciiDocPreviewSettings.Holder {

  private State myState = new State();

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
