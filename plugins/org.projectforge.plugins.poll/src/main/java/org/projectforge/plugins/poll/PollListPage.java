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

package org.projectforge.plugins.poll;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@ListPage(editPage = PollEditPage.class)
public class PollListPage extends AbstractListPage<PollListForm, PollDao, PollDO>
    implements IListPageColumnsCreator<PollDO>
{
  private static final long serialVersionUID = 1749480610890950450L;

  @SpringBean
  private PollDao pollDao;

  /**
   * 
   */
  public PollListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.poll");
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<PollDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<PollDO, String>> columns = new ArrayList<>();

    final CellItemListener<PollDO> cellItemListener = new CellItemListener<PollDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<PollDO>> item, final String componentId,
          final IModel<PollDO> rowModel)
      {
        final PollDO poll = rowModel.getObject();
        appendCssClasses(item, poll.getId(), poll.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<PollDO>(getString("plugins.poll.new.title"),
        getSortable("title", sortable), "title",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<PollDO>> item, final String componentId,
          final IModel<PollDO> rowModel)
      {
        final PollDO poll = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, NewPollOverviewPage.class, poll.getId(), returnToPage,
            poll.getTitle()));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(getString("plugins.poll.new.description"),
        getSortable("description", sortable),
        "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(getString("plugins.poll.new.location"),
        getSortable("location", sortable),
        "location", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<>(getString("plugins.teamcal.owner"), getSortable("owner", sortable),
            "owner.username", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(getString("lastUpdate"), getSortable("lastUpdate", sortable),
        "lastUpdate",
        cellItemListener));
    return columns;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public PollDao getBaseDao()
  {
    return pollDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected PollListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new PollListForm(this);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#init()
   */
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "lastUpdate", SortOrder.DESCENDING);
    form.add(dataTable);
  }
}
