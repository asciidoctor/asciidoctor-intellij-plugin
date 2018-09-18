package org.asciidoc.intellij.settings;

import com.intellij.execution.util.ListTableWithButtons;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.Nullable;

/**
 * @author Julian Ronge 2018
 */
public class AttributeTable extends ListTableWithButtons<AttributeTableItem> {

  private static final ColumnInfo<AttributeTableItem, String> NAME_COLUMN = new ColumnInfo<AttributeTableItem, String>("Key") {
    @Nullable
    @Override
    public String valueOf(AttributeTableItem item) {
      return item.getKey();
    }

    @Override
    public boolean isCellEditable(AttributeTableItem item) {
      return true;
    }

    @Override
    public void setValue(AttributeTableItem item, String value) {
      item.setKey(value);
    }
  };

  private static final ColumnInfo<AttributeTableItem, String> SCOPE_COLUMN = new ColumnInfo<AttributeTableItem, String>("Value") {
    @Nullable
    @Override
    public String valueOf(AttributeTableItem item) {
      return item.getValue();
    }

    @Override
    public boolean isCellEditable(AttributeTableItem item) {
      return true;
    }

    @Override
    public void setValue(AttributeTableItem item, String value) {
      item.setValue(value);
    }
  };

  @Override
  protected ListTableModel createListModel() {
    return new ListTableModel<AttributeTableItem>(NAME_COLUMN, SCOPE_COLUMN);
  }

  @Override
  protected AttributeTableItem createElement() {
    return new AttributeTableItem();
  }

  @Override
  protected boolean isEmpty(AttributeTableItem element) {
    return element.getKey() == null;
  }

  @Override
  protected AttributeTableItem cloneElement(AttributeTableItem attributeTableItem) {
    return new AttributeTableItem(attributeTableItem.getKey(), attributeTableItem.getValue());
  }

  @Override
  protected boolean canDeleteElement(AttributeTableItem selection) {
    return true;
  }
}
