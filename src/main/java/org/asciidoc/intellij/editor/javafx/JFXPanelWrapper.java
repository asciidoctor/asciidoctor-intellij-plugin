// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.asciidoc.intellij.editor.javafx;

import com.intellij.ui.JreHiDpiUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.FieldAccessor;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import java.awt.*;

/**
 * This is a backport of a class that will be removed from IntelliJ 2020.3.
 */
public class JFXPanelWrapper extends JFXPanel {
  private static final FieldAccessor<JFXPanel, Integer> MY_SCALE_FACTOR_ACCESSOR = new FieldAccessor<>(JFXPanel.class, "scaleFactor", Integer.TYPE);

  public JFXPanelWrapper() {
    Platform.setImplicitExit(false);
  }

  /**
   * This override fixes the situation of using multiple JFXPanels
   * with jbtabs/splitters when some of them are not showing.
   * On getMinimumSize there is no layout manager nor peer so
   * the result could be #size() which is incorrect.
   * @return zero size
   */
  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }

  @Override
  public void addNotify() {
    if (MY_SCALE_FACTOR_ACCESSOR.isAvailable()) {
      if (JreHiDpiUtil.isJreHiDPIEnabled()) {
        // JFXPanel is scaled asynchronously after first repaint, what may lead
        // to showing unscaled content. To work it around, set "scaleFactor" ahead.
        int scale = Math.round(JBUIScale.sysScale(this));
        MY_SCALE_FACTOR_ACCESSOR.set(this, scale);
      }
    }
    // change scale factor before component will be resized in super
    super.addNotify();
  }
}
