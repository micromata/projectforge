package org.projectforge.web.wicket;

import java.io.Serializable;
import java.util.function.Function;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

public class CellItemListenerLambdaColumn<T, S> extends AbstractColumn<T, S>
{
  private final CellItemListener<T> cellItemListener;

  private final Function<IModel<T>, Serializable> cellValueGenerator;

  public CellItemListenerLambdaColumn(final IModel<String> headerModel, final Function<IModel<T>, Serializable> cellValueGenerator,
      final CellItemListener<T> cellItemListener)
  {
    super(headerModel);
    this.cellItemListener = cellItemListener;
    this.cellValueGenerator = cellValueGenerator;
  }

  @Override
  public void populateItem(final Item<ICellPopulator<T>> cellItem, final String componentId, final IModel<T> rowModel)
  {
    final Serializable result = cellValueGenerator.apply(rowModel);
    cellItem.add(new Label(componentId, result));

    if (cellItemListener != null) {
      cellItemListener.populateItem(cellItem, componentId, rowModel);
    }
  }
}
