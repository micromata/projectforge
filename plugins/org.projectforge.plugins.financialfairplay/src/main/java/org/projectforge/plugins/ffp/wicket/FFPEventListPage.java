/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.ffp.wicket;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = FFPEventEditPage.class)
public class FFPEventListPage extends AbstractListPage<FFPEventListForm, FFPEventService, FFPEventDO> implements
    IListPageColumnsCreator<FFPEventDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private FFPEventService eventService;

  public FFPEventListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.ffp");
  }

  @Override
  @SuppressWarnings("serial")
  public List<IColumn<FFPEventDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<FFPEventDO, String>> columns = new ArrayList<>();

    final CellItemListener<FFPEventDO> cellItemListener = new CellItemListener<FFPEventDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<FFPEventDO>> item, final String componentId,
          final IModel<FFPEventDO> rowModel)
      {
        final FFPEventDO event = rowModel.getObject();
        appendCssClasses(item, event.getId(), event.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<FFPEventDO>(new ResourceModel("plugins.ffp.eventDate"),
        getSortable("eventDate", sortable),
        "eventDate", cellItemListener));

    columns.add(new CellItemListenerPropertyColumn<FFPEventDO>(new ResourceModel("title"),
        getSortable("title", sortable),
        "title", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<FFPEventDO>> item, final String componentId,
          final IModel<FFPEventDO> rowModel)
      {
        final FFPEventDO event = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, FFPEventEditPage.class, event.getId(),
              returnToPage, event.getTitle()));
        } else {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, event.getId(),
                  event.getTitle()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(AttributeModifier.replace("style", "width: 50%"));
        addRowClick(item);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<FFPEventDO>(new ResourceModel("plugins.ffp.status"),
        getSortable("status", sortable), "status", cellItemListener));

    return columns;
  }

  @Override
  protected void init()
  {
    final List<IColumn<FFPEventDO, String>> columns = createColumns(this, true);
    dataTable = createDataTable(columns, "title", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected FFPEventListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new FFPEventListForm(this);
  }

  @Override
  public FFPEventService getBaseDao()
  {
    return eventService;
  }

}
