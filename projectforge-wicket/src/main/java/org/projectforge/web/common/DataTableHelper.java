/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.common;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.DataObjectSortableDataProvider;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.TablePanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DataTableHelper
{
  public static void createViewOnlyDataTable(GridBuilder gridBuilder, ExtendedBaseDO<Integer> dataObject,
      List<String> propertiesList, String sortProperty, SortOrder sortOrder,
      Supplier<List<ExtendedBaseDO<Integer>>> supplier)
  {
    DivPanel section = gridBuilder.getPanel();
    TablePanel tablePanel = new TablePanel(section.newChildId());
    section.add(tablePanel);
    final DataTable<ExtendedBaseDO<Integer>, String> dataTable = createDataTable(
        createColumns(propertiesList, dataObject), sortProperty, sortOrder, supplier);
    tablePanel.add(dataTable);
  }

  private static DataTable<ExtendedBaseDO<Integer>, String> createDataTable(
      final List<IColumn<ExtendedBaseDO<Integer>, String>> columns, final String sortProperty,
      final SortOrder sortOrder, Supplier<List<ExtendedBaseDO<Integer>>> supplier)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    return new DefaultDataTable<ExtendedBaseDO<Integer>, String>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam, supplier), 50);
  }

  private static ISortableDataProvider<ExtendedBaseDO<Integer>, String> createSortableDataProvider(
      final SortParam<String> sortParam, Supplier<List<ExtendedBaseDO<Integer>>> supplier)
  {
    return new DataObjectSortableDataProvider<ExtendedBaseDO<Integer>>(sortParam, supplier);
  }

  private static List<IColumn<ExtendedBaseDO<Integer>, String>> createColumns(List<String> propertyNames,
      ExtendedBaseDO<Integer> dataObject)
  {
    final List<IColumn<ExtendedBaseDO<Integer>, String>> columns = new ArrayList<IColumn<ExtendedBaseDO<Integer>, String>>();

    final CellItemListener<ExtendedBaseDO<Integer>> cellItemListener = new CellItemListener<ExtendedBaseDO<Integer>>()
    {
      private static final long serialVersionUID = 1L;

      @Override
      public void populateItem(final Item<ICellPopulator<ExtendedBaseDO<Integer>>> item, final String componentId,
          final IModel<ExtendedBaseDO<Integer>> rowModel)
      {
        //Nothing to do here
      }
    };
    propertyNames.forEach(property -> {
      columns.add(
          new CellItemListenerPropertyColumn<ExtendedBaseDO<Integer>>(dataObject.getClass(), property, property,
              cellItemListener));
    });
    return columns;
  }

}
