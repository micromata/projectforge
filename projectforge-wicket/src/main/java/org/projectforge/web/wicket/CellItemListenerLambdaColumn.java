/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.wicket;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.function.Function;

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
