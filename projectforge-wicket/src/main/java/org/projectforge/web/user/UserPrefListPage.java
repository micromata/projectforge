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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.web.wicket.*;

import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = UserPrefEditPage.class)
public class UserPrefListPage extends AbstractListPage<UserPrefListForm, UserPrefDao, UserPrefDO>
{
  private static final long serialVersionUID = 6121734373079865758L;

  @SpringBean
  private UserPrefDao userPrefDao;

  @SpringBean
  private UserFormatter userFormatter;

  public static BookmarkablePageLink<Void> createLink(final String id, final UserPrefArea area)
  {
    final PageParameters params = new PageParameters();
    params.add("area", area.getId());
    final BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, UserPrefListPage.class, params);
    return link;
  }

  public UserPrefListPage(final PageParameters parameters)
  {
    super(parameters, "userPref");
    final String area = WicketUtils.getAsString(parameters, "area");
    if (area != null) {
      final UserPrefArea userPrefArea = UserPrefAreaRegistry.instance().getEntry(area);
      form.getSearchFilter().setArea(userPrefArea);
    }
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final List<IColumn<UserPrefDO, String>> columns = new ArrayList<IColumn<UserPrefDO, String>>();

    final CellItemListener<UserPrefDO> cellItemListener = new CellItemListener<UserPrefDO>()
    {
      public void populateItem(final Item<ICellPopulator<UserPrefDO>> item, final String componentId,
          final IModel<UserPrefDO> rowModel)
      {
      }
    };
    columns.add(
        new CellItemListenerPropertyColumn<UserPrefDO>(new Model<String>(getString("userPref.area")), "area", "area",
            cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<UserPrefDO>> item, final String componentId,
              final IModel<UserPrefDO> rowModel)
          {
            final UserPrefDO userPref = rowModel.getObject();
            final String label;
            if (userPref.getAreaObject() != null) {
              label = getString(userPref.getAreaObject().getI18nKey());
            } else {
              label = "";
            }
            item.add(new ListSelectActionPanel(componentId, rowModel, UserPrefEditPage.class, userPref.getId(),
                UserPrefListPage.this, label));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        });
    columns.add(
        new CellItemListenerPropertyColumn<UserPrefDO>(new Model<String>(getString("userPref.name")), "name", "name",
            cellItemListener));
    columns
        .add(new UserPropertyColumn<UserPrefDO>(getUserGroupCache(), getString("user"), "user.fullname", "user",
            cellItemListener)
                .withUserFormatter(userFormatter));
    columns.add(new CellItemListenerPropertyColumn<UserPrefDO>(new Model<String>(getString("lastUpdate")), "lastUpdate",
        "lastUpdate", cellItemListener));
    dataTable = createDataTable(columns, null, SortOrder.DESCENDING);
    form.add(dataTable);
  }

  /**
   * Gets the current areaObject and preset this areaObject for the edit page.
   *
   * @see org.projectforge.web.wicket.AbstractListPage#onNewEntryClick(org.apache.wicket.PageParameters)
   */
  @Override
  protected AbstractEditPage<?, ?, ?> redirectToEditPage(PageParameters params)
  {
    if (params == null) {
      params = new PageParameters();
    }
    final UserPrefArea area = form.getSearchFilter().getArea();
    if (area != null) {
      params.add(UserPrefEditPage.PARAMETER_AREA, area.getId());
    }
    return super.redirectToEditPage(params);
  }

  @Override
  protected UserPrefListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new UserPrefListForm(this);
  }

  @Override
  public UserPrefDao getBaseDao()
  {
    return userPrefDao;
  }
}
