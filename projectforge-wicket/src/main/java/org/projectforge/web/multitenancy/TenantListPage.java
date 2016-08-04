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

package org.projectforge.web.multitenancy;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;

@ListPage(editPage = TenantEditPage.class)
public class TenantListPage extends AbstractListPage<TenantListForm, TenantDao, TenantDO>
    implements IListPageColumnsCreator<TenantDO>
{
  private static final long serialVersionUID = 7227240465661485515L;

  @SpringBean
  private TenantDao tenantDao;

  public TenantListPage(final PageParameters parameters)
  {
    super(parameters, "multitenancy");
  }

  @SuppressWarnings("serial")
  public List<IColumn<TenantDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<TenantDO, String>> columns = new ArrayList<IColumn<TenantDO, String>>();
    final CellItemListener<TenantDO> cellItemListener = new CellItemListener<TenantDO>()
    {
      public void populateItem(final Item<ICellPopulator<TenantDO>> item, final String componentId,
          final IModel<TenantDO> rowModel)
      {
        final TenantDO tenant = rowModel.getObject();
        appendCssClasses(item, tenant.getId(), tenant.isDeleted());
        if (tenant.isDefault() == true) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<TenantDO>(new Model<String>(getString("created")),
        getSortable("created", sortable),
        "created", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<TenantDO>> item, final String componentId,
          final IModel<TenantDO> rowModel)
      {
        final TenantDO Tenant = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, TenantEditPage.class, Tenant.getId(), returnToPage,
            DateTimeFormatter
                .instance().getFormattedDate(Tenant.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<TenantDO>(getString("id"), getSortable("id", sortable), "id",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TenantDO>(getString("multitenancy.tenant.shortName"),
        getSortable("shortName", sortable), "shortName",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TenantDO>(getString("name"), getSortable("name", sortable), "name",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<TenantDO>(getString("description"),
        getSortable("description", sortable), "description",
        cellItemListener));

    columns.add(
        new CellItemListenerPropertyColumn<TenantDO>(getString("multitenancy.assignedUsers"), getSortable("usernames",
            sortable), "usernames", cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "created", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected TenantListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new TenantListForm(this);
  }

  @Override
  public TenantDao getBaseDao()
  {
    return tenantDao;
  }

  protected TenantDao getTenantDao()
  {
    return tenantDao;
  }
}
