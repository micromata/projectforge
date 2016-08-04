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

package org.projectforge.plugins.crm;

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
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

/**
 * The controller of the list page. Most functionality such as search etc. is done by the super class.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
@ListPage(editPage = ContactEditPage.class)
public class ContactListPage extends AbstractListPage<ContactListForm, ContactDao, ContactDO> implements
    IListPageColumnsCreator<ContactDO>
{
  private static final long serialVersionUID = -7719281395937025724L;

  public static final String I18N_KEY_PREFIX = "contact";

  @SpringBean
  private ContactDao contactDao;

  public ContactListPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<ContactDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<ContactDO, String>> columns = new ArrayList<IColumn<ContactDO, String>>();
    final CellItemListener<ContactDO> cellItemListener = new CellItemListener<ContactDO>()
    {
      public void populateItem(final Item<ICellPopulator<ContactDO>> item, final String componentId,
          final IModel<ContactDO> rowModel)
      {
        final ContactDO contactDO = rowModel.getObject();
        appendCssClasses(item, contactDO.getId(), contactDO.isDeleted());
      }
    };

    columns.add(new CellItemListenerPropertyColumn<ContactDO>(ContactDO.class, getSortable("name", sortable), "name",
        cellItemListener));

    columns.add(
        new CellItemListenerPropertyColumn<ContactDO>(ContactDO.class, getSortable("firstName", sortable), "firstName",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ContactDO>(ContactDO.class, getSortable("title", sortable), "title",
        cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<ContactDO>(ContactDO.class, getSortable("birthday", sortable), "birthday",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<ContactDO>(new Model<String>(getString("modified")),
        getSortable("lastUpdate", sortable), "lastUpdate", cellItemListener));

    columns
        .add(new CellItemListenerPropertyColumn<ContactDO>(ContactDO.class, getSortable("created", sortable), "created",
            cellItemListener)
        {
          @SuppressWarnings({ "unchecked", "rawtypes" })
          @Override
          public void populateItem(final Item item, final String componentId, final IModel rowModel)
          {
            final ContactDO contactDO = (ContactDO) rowModel.getObject();
            item.add(
                new ListSelectActionPanel(componentId, rowModel, ContactEditPage.class, contactDO.getId(), returnToPage,
                    DateTimeFormatter.instance().getFormattedDateTime(contactDO.getCreated())));
            addRowClick(item);
            cellItemListener.populateItem(item, componentId, rowModel);
          }
        });
    return columns;
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

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#getBaseDao()
   */
  @Override
  public ContactDao getBaseDao()
  {
    return contactDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#newListForm(org.projectforge.web.wicket.AbstractListPage)
   */
  @Override
  protected ContactListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new ContactListForm(this);
  }

}
