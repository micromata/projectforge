/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.memo;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@ListPage(editPage = MemoEditPage.class)
public class MemoListPage extends AbstractListPage<MemoListForm, MemoDao, MemoDO>
    implements IListPageColumnsCreator<MemoDO>
{
  private static final long serialVersionUID = 4228569581764015696L;

  @SpringBean
  private MemoDao memoDao;

  public MemoListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.memo");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<MemoDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<MemoDO, String>> columns = new ArrayList<>();
    final CellItemListener<MemoDO> cellItemListener = new CellItemListener<MemoDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<MemoDO>> item, final String componentId,
          final IModel<MemoDO> rowModel)
      {
        final MemoDO memo = rowModel.getObject();
        appendCssClasses(item, memo.getId(), memo.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<MemoDO>(MemoDO.class, getSortable("created", sortable), "created",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<MemoDO>> item, final String componentId,
          final IModel<MemoDO> rowModel)
      {
        final MemoDO memo = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, MemoEditPage.class, memo.getId(), returnToPage,
            DateTimeFormatter
                .instance().getFormattedDateTime(memo.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns
        .add(new CellItemListenerPropertyColumn<>(MemoDO.class, getSortable("lastUpdate", sortable), "lastUpdate",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(MemoDO.class, getSortable("subject", sortable), "subject",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<MemoDO>(MemoDO.class, getSortable("memo", sortable), "memo",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<MemoDO>> item, final String componentId,
          final IModel<MemoDO> rowModel)
      {
        final MemoDO memo = rowModel.getObject();
        final Label label = new Label(componentId, new Model<>(StringUtils.abbreviate(memo.getMemo(), 100)));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected MemoListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new MemoListForm(this);
  }

  @Override
  public MemoDao getBaseDao()
  {
    return memoDao;
  }

  protected MemoDao getMemoDao()
  {
    return memoDao;
  }
}
