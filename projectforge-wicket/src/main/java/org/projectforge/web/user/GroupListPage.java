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

package org.projectforge.web.user;

import java.util.ArrayList;
import java.util.List;

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
import org.projectforge.business.ldap.LdapUserDao;
import org.projectforge.business.user.GroupDao;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = GroupEditPage.class)
public class GroupListPage extends AbstractListPage<GroupListForm, GroupDao, GroupDO>
    implements IListPageColumnsCreator<GroupDO>
{
  private static final long serialVersionUID = 3124148202828889250L;

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  LdapUserDao ldapUserDao;

  public GroupListPage(final PageParameters parameters)
  {
    super(parameters, "group");
  }

  public GroupListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "group");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<GroupDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<GroupDO, String>> columns = new ArrayList<IColumn<GroupDO, String>>();
    final CellItemListener<GroupDO> cellItemListener = new CellItemListener<GroupDO>()
    {
      public void populateItem(final Item<ICellPopulator<GroupDO>> item, final String componentId,
          final IModel<GroupDO> rowModel)
      {
        final GroupDO group = rowModel.getObject();
        appendCssClasses(item, group.getId(), group.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<GroupDO>(new Model<String>(getString("name")),
        getSortable("name", sortable), "name",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<GroupDO>> item, final String componentId,
          final IModel<GroupDO> rowModel)
      {
        final boolean updateAccess = groupDao.hasLoggedInUserAccess(null, null, OperationType.UPDATE, false);
        final GroupDO group = rowModel.getObject();
        if (isSelectMode() == true) {
          item.add(
              new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, group.getId(), group.getName()));
          addRowClick(item);
        } else if (updateAccess == true) {
          item.add(new ListSelectActionPanel(componentId, rowModel, GroupEditPage.class, group.getId(), returnToPage,
              group.getName()));
          addRowClick(item);
        } else {
          item.add(new Label(componentId, group.getName()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupDO>(new Model<String>(getString("organization")),
        getSortable("organization",
            sortable),
        "organization", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<GroupDO>(new Model<String>(getString("description")),
        getSortable("description",
            sortable),
        "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<GroupDO>(new Model<String>(getString("group.assignedUsers")),
        getSortable("usernames",
            sortable),
        "usernames", cellItemListener));
    if (ldapUserDao.isPosixAccountsConfigured() == true) {
      columns
          .add(new CellItemListenerPropertyColumn<GroupDO>(getString("group.ldapValues"), "ldapValues", "ldapValues",
              cellItemListener));
    }
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "name", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected GroupListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new GroupListForm(this);
  }

  @Override
  protected GroupDao getBaseDao()
  {
    return groupDao;
  }

  protected GroupDao getGroupDao()
  {
    return groupDao;
  }
}
