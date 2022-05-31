/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.excel.ContentProvider;
import org.projectforge.business.excel.ExportColumn;
import org.projectforge.business.excel.PropertyMapping;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.ldap.LdapUserDao;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.export.DOListExcelExporter;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.TimeAgo;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = UserEditPage.class)
public class UserListPage extends AbstractListPage<UserListForm, UserDao, PFUserDO>
    implements IListPageColumnsCreator<PFUserDO>
{
  private static final long serialVersionUID = 4408701323868106520L;

  @SpringBean
  private AccessChecker accessChecker;

  @SpringBean
  private UserDao userDao;

  @SpringBean
  private UserRightService userRightService;

  @SpringBean
  private LdapUserDao ldapUserDao;

  @SpringBean
  private GroupService groupService;

  public UserListPage(final PageParameters parameters)
  {
    super(parameters, "user");
  }

  public UserListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "user");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<PFUserDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final boolean updateAccess = userDao.hasLoggedInUserAccess(null, null, OperationType.UPDATE, false);
    final List<IColumn<PFUserDO, String>> columns = new ArrayList<IColumn<PFUserDO, String>>();
    final CellItemListener<PFUserDO> cellItemListener = new CellItemListener<PFUserDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<PFUserDO>> item, final String componentId,
          final IModel<PFUserDO> rowModel)
      {
        final PFUserDO user = rowModel.getObject();
        appendCssClasses(item, user.getId(), user.hasSystemAccess() == false);
      }
    };
    columns.add(new CellItemListenerPropertyColumn<PFUserDO>(getString("user.username"),
        getSortable("username", sortable), "username",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<PFUserDO>> item, final String componentId,
          final IModel<PFUserDO> rowModel)
      {
        final PFUserDO user = rowModel.getObject();
        if (isSelectMode() == true) {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, user.getId(),
              user.getUsername()));
          addRowClick(item);
        } else if (updateAccess == true) {
          item.add(new ListSelectActionPanel(componentId, rowModel, UserEditPage.class, user.getId(), returnToPage,
              user.getUsername()));
          addRowClick(item);
        } else {
          item.add(new Label(componentId, user.getUsername()));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<PFUserDO>(new Model<String>(getString("user.activated")),
        getSortable("deactivated",
            sortable),
        "deactivated", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<PFUserDO>> item, final String componentId,
                               final IModel<PFUserDO> rowModel)
      {
        final PFUserDO user = rowModel.getObject();
        if (user.getDeactivated() == false) {
          item.add(new IconPanel(componentId, IconType.ACCEPT));
        } else {
          item.add(new IconPanel(componentId, IconType.DENY));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    //columns.add(
    //    new CellItemListenerPropertyColumn<PFUserDO>(getString("login.lastLogin"), getSortable("lastLogin", sortable), "lastLogin",
    //        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<>(new Model<>(getString("login.lastLogin")),
        getSortable("lastLogin",
            sortable),
        "lastLogin", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<PFUserDO>> item, final String componentId,
                               final IModel<PFUserDO> rowModel)
      {
        final PFUserDO user = rowModel.getObject();
        final Label label = new Label(componentId, new Model<>(TimeAgo.getMessage(user.getLastLogin())));
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(
        new CellItemListenerPropertyColumn<PFUserDO>(getString("name"), getSortable("lastname", sortable), "lastname",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PFUserDO>(getString("firstName"), getSortable("firstname", sortable),
        "firstname",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PFUserDO>(getString("user.personalPhoneIdentifiers"), getSortable(
        "personalPhoneIdentifiers", sortable), "personalPhoneIdentifiers", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PFUserDO>(getString("description"),
        getSortable("description", sortable), "description",
        cellItemListener));
    if (updateAccess ) {
      // Show these columns only for admin users:
      columns.add(new AbstractColumn<PFUserDO, String>(new Model<String>(getString("user.assignedGroups")))
      {
        @Override
        public void populateItem(final Item<ICellPopulator<PFUserDO>> cellItem, final String componentId,
            final IModel<PFUserDO> rowModel)
        {
          final PFUserDO user = rowModel.getObject();
          final String groups = groupService.getGroupnames(user.getId());
          final Label label = new Label(componentId, new Model<String>(groups));
          cellItem.add(label);
          cellItemListener.populateItem(cellItem, componentId, rowModel);
        }
      });
      columns.add(new AbstractColumn<PFUserDO, String>(new Model<String>(getString("access.rights")))
      {
        @Override
        public void populateItem(final Item<ICellPopulator<PFUserDO>> cellItem, final String componentId,
            final IModel<PFUserDO> rowModel)
        {
          final PFUserDO user = rowModel.getObject();
          final List<UserRightDO> rights = userDao.getUserRights(user.getId());
          final StringBuffer buf = new StringBuffer();
          if (rights != null) {
            boolean first = true;
            for (final UserRightDO right : rights) {
              if (right.getValue() != null && right.getValue() != UserRightValue.FALSE) {
                if (first == true) {
                  first = false;
                } else {
                  buf.append(", ");
                }
                IUserRightId userRightId = userRightService.getRightId(right.getRightIdString());
                buf.append(getString(userRightId.getI18nKey()));
                if (right.getValue() == UserRightValue.READONLY) {
                  buf.append(" (ro)");
                } else if (right.getValue() == UserRightValue.PARTLYREADWRITE) {
                  buf.append(" (prw)");
                } else if (right.getValue() == UserRightValue.READWRITE) {
                  buf.append(" (rw)");
                }
              }
            }
          }
          final Label label = new Label(componentId, buf.toString());
          cellItem.add(label);
          cellItemListener.populateItem(cellItem, componentId, rowModel);
        }
      });
      if (ldapUserDao.isPosixAccountsConfigured() == true) {
        columns
            .add(new CellItemListenerPropertyColumn<PFUserDO>(getString("user.ldapValues"), "ldapValues", "ldapValues",
                cellItemListener));
      }
    } else {
      columns.add(new CellItemListenerPropertyColumn<PFUserDO>(getString("user.sshPublicKey"),
          getSortable("sshPublicKey", sortable), "sshPublicKey",
          cellItemListener));
    }
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "username", SortOrder.ASCENDING);
    form.add(dataTable);
    if (accessChecker.isLoggedInUserMemberOfAdminGroup()) {
      addExcelExport(getString("user.users"), getString("user.users"));
    }
  }

  @Override
  protected UserListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new UserListForm(this);
  }

  @Override
  public UserDao getBaseDao()
  {
    return userDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#createExcelExporter(java.lang.String)
   */
  @Override
  protected DOListExcelExporter createExcelExporter(final String filenameIdentifier)
  {
    return new DOListExcelExporter(filenameIdentifier)
    {
      @Override
      protected List<ExportColumn> onBeforeSettingColumns(final ContentProvider sheetProvider,
                                                          final List<ExportColumn> columns)
      {
        final List<ExportColumn> sortedColumns = reorderColumns(columns, "username", "jiraUsername", "localUser", "restrictedUser",
            "deactivated", "firstname", "nickname", "gender",
            "lastname","description", "email", "lastLogin", "locale", "timeZone", "organization", "lastPasswordChange", "lastWlanPasswordChange");
        return removeColumns(sortedColumns, "password", "rights", "sshPublicKey");
      }

      @Override
      public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
      {
        if ("deactivated".equals(field.getName())) {
          mapping.add(field.getName(), !((PFUserDO)entry).getDeactivated()); // Displayed as "activated", so negate value.
        } else {
          super.addMapping(mapping, entry, field);
        }
      }
    };
  }
}
