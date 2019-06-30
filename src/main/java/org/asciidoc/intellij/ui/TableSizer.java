package org.asciidoc.intellij.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Small component to choose a table size (in terms of columns and rows).
 * @author bbrenne
 */
public class TableSizer extends AbstractButton {
  private final int rowHeight = 20;
  private final int colWidth = 20;
  private final int rows = 10;
  private final int cols = 10;

  private Point position;

  private JLabel sizeDisplay = null;

  public TableSizer() {

    this.addMouseMotionListener(new MouseAdapter() {

      @Override
      public void mouseMoved(MouseEvent e) {
        position = e.getPoint();
        sizeDisplay.setText(getSelectedWidth() + " x " + getSelectedHeight());
        TableSizer.this.repaint();
      }
    });

    this.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseExited(MouseEvent e) {
        position = null;
        sizeDisplay.setText("");
        TableSizer.this.repaint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        position = e.getPoint();
        if (position != null) {
          Object[] listeners = listenerList.getListenerList();
          ActionEvent event = new CreateTableActionEvent(this, ActionEvent.ACTION_PERFORMED, "click", getSelectedWidth(), getSelectedHeight());
          // We cannot use fireActionEvent so we do it manually
          for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
              ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
          }
        }
      }
    });
  }

  public void setSizeDisplay(JLabel sizeDisplay) {
    this.sizeDisplay = sizeDisplay;
  }

  private int getGridWidth() {
    return cols * colWidth;
  }

  private int getGridHeight() {
    return rows * rowHeight;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(getGridWidth(), getGridHeight());
  }

  @Override
  public void paint(Graphics g) {
    g.setColor(UIManager.getColor("Menu.background"));
    g.fillRect(0, 0, getGridWidth(), getGridHeight());

    if (position != null) {
      g.setColor(UIManager.getColor("Menu.selectionBackground"));
      g.fillRect(0, 0, getSelectedWidth() * colWidth, getSelectedHeight() * rowHeight);
    }

    g.setColor(UIManager.getColor("Separator.foreground"));

    g.drawRect(0, 0, getGridWidth(), getGridHeight());
    for (int i = 1; i < cols; i++) {
      g.drawLine(i * colWidth, 0, i * colWidth, getGridHeight());
    }
    for (int i = 1; i < rows; i++) {
      g.drawLine(0, i * rowHeight, getGridWidth(), i * rowHeight);
    }
  }

  private int getSelectedWidth() {
    return (position.x / colWidth) + 1;
  }

  private int getSelectedHeight() {
    return (position.y / rowHeight) + 1;
  }

  @Override
  public void updateUI() {
    super.updateUI();
  }

  public class CreateTableActionEvent extends ActionEvent {
    private int width;
    private int height;

    public CreateTableActionEvent(Object source, int id, String command, int width, int height) {
      super(source, id, command);
      this.width = width;
      this.height = height;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }
}
