package org.asciidoc.intellij.editor.javafx.notification;

import com.intellij.ide.util.PropertiesComponent;

public class PreviewNotificationRepository {
  private static final String DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX = "asciidoc.do.not.ask.to.change.jdk.for.javafx";

  public boolean isShown() {
    return PropertiesComponent.getInstance().getBoolean(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX, false);
  }

  public void reset() {
    PropertiesComponent.getInstance().setValue(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX, true);
  }
}
