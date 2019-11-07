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

package org.projectforge.plugins.marketing;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
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
@ListPage(editPage = AddressCampaignEditPage.class)
public class AddressCampaignListPage
    extends AbstractListPage<AddressCampaignListForm, AddressCampaignDao, AddressCampaignDO> implements
    IListPageColumnsCreator<AddressCampaignDO>
{
  private static final long serialVersionUID = -4070838758263185222L;

  @SpringBean
  private AddressCampaignDao addressCampaignDao;

  public AddressCampaignListPage(final PageParameters parameters)
  {
    super(parameters, "plugins.marketing.addressCampaign");
  }

  @SuppressWarnings("serial")
  public List<IColumn<AddressCampaignDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<AddressCampaignDO, String>> columns = new ArrayList<>();
    final CellItemListener<AddressCampaignDO> cellItemListener = new CellItemListener<AddressCampaignDO>()
    {
      public void populateItem(final Item<ICellPopulator<AddressCampaignDO>> item, final String componentId,
          final IModel<AddressCampaignDO> rowModel)
      {
        final AddressCampaignDO campaign = rowModel.getObject();
        appendCssClasses(item, campaign.getId(), campaign.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<AddressCampaignDO>(new Model<>(getString("created")),
        getSortable("created",
            sortable),
        "created", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressCampaignDO>> item, final String componentId,
          final IModel<AddressCampaignDO> rowModel)
      {
        final AddressCampaignDO campaign = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, AddressCampaignEditPage.class, campaign.getId(),
            returnToPage,
            DateTimeFormatter.instance().getFormattedDateTime(campaign.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<>(getString("modified"),
        getSortable("lastUpdate", sortable),
        "lastUpdate", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(new Model<>(getString("title")),
        getSortable("title", sortable), "title", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(new Model<>(getString("values")),
        getSortable("values",
            sortable),
        "values", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<>(new Model<>(getString("comment")), null, "comment",
            cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "title", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected AddressCampaignListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new AddressCampaignListForm(this);
  }

  @Override
  public AddressCampaignDao getBaseDao()
  {
    return addressCampaignDao;
  }

  protected AddressCampaignDao getCampaignDao()
  {
    return addressCampaignDao;
  }
}
