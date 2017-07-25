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

package org.projectforge.web.address;

import java.util.ArrayList;
import java.util.List;

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
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.address.AddressbookFilter;
import org.projectforge.business.address.AddressbookRight;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

/**
 * @author Florian Blumenstein
 */
@ListPage(editPage = AddressbookEditPage.class)
public class AddressbookListPage extends AbstractListPage<AddressbookListForm, AddressbookDao, AddressbookDO>
    implements IListPageColumnsCreator<AddressbookDO>
{
  private static final long serialVersionUID = 1749480610890950450L;

  @SpringBean
  private AddressbookDao addressbookDao;

  private final boolean isAdminUser;

  /**
   *
   */
  public AddressbookListPage(final PageParameters parameters)
  {
    super(parameters, "addressbook");
    isAdminUser = accessChecker.isLoggedInUserMemberOfAdminGroup();
  }

  /**
   * @see IListPageColumnsCreator#createColumns(WebPage,
   * boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<AddressbookDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<AddressbookDO, String>> columns = new ArrayList<IColumn<AddressbookDO, String>>();

    final CellItemListener<AddressbookDO> cellItemListener = new CellItemListener<AddressbookDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<AddressbookDO>> item, final String componentId,
          final IModel<AddressbookDO> rowModel)
      {
        final AddressbookDO teamCal = rowModel.getObject();
        appendCssClasses(item, teamCal.getId(), teamCal.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<AddressbookDO>(getString("addressbook.title"),
        getSortable("title", sortable), "title",
        cellItemListener)
    {
      /**
       * @see CellItemListenerPropertyColumn#populateItem(Item,
       *      String, IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressbookDO>> item, final String componentId,
          final IModel<AddressbookDO> rowModel)
      {
        final AddressbookDO teamCal = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, AddressbookEditPage.class, teamCal.getId(), returnToPage,
            teamCal.getTitle()));
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });

    columns.add(new CellItemListenerPropertyColumn<AddressbookDO>(getString("addressbook.description"),
        getSortable("description", sortable), "description", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<AddressbookDO>(getString("addressbook.owner"),
        getSortable("owner", sortable),
        "owner.username", cellItemListener));
    columns.add(new AbstractColumn<AddressbookDO, String>(new Model<String>(getString("access")))
    {
      /**
       * @see CellItemListenerPropertyColumn#populateItem(Item,
       *      String, IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<AddressbookDO>> item, final String componentId,
          final IModel<AddressbookDO> rowModel)
      {
        final AddressbookDO ab = rowModel.getObject();
        final AddressbookRight right = (AddressbookRight) addressbookDao.getUserRight();
        String label;
        if (right.isOwner(getUser(), ab) == true) {
          label = getString("addressbook.owner");
        } else if (right.hasFullAccess(ab, getUserId()) == true) {
          label = getString("addressbook.fullAccess");
        } else if (right.hasReadonlyAccess(ab, getUserId()) == true) {
          label = getString("addressbook.readonlyAccess");
        } else if (isAdminUser == true) {
          label = getString("addressbook.adminAccess");
        } else {
          label = "???";
        }
        item.add(new Label(componentId, label));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<AddressbookDO>(getString("lastUpdate"),
        getSortable("lastUpdate", sortable), "lastUpdate",
        cellItemListener));
    return columns;
  }

  protected AddressbookFilter getFilter()
  {
    return form.getFilter();
  }

  /**
   * @see AbstractListPage#getBaseDao()
   */
  @Override
  public AddressbookDao getBaseDao()
  {
    return addressbookDao;
  }

  /**
   * @see AbstractListPage#newListForm(AbstractListPage)
   */
  @Override
  protected AddressbookListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new AddressbookListForm(this);
  }

  /**
   * @see AbstractListPage#init()
   */
  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "title", SortOrder.ASCENDING);
    form.add(dataTable);
  }
}
