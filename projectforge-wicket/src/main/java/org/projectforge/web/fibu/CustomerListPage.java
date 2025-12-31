/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.*;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.user.UserPrefListPage;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.util.ArrayList;
import java.util.List;

@ListPage(editPage = CustomerEditPage.class)
public class CustomerListPage extends AbstractListPage<CustomerListForm, KundeDao, KundeDO>
    implements IListPageColumnsCreator<KundeDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<KundeDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<KundeDO, String>> columns = new ArrayList<IColumn<KundeDO, String>>();

    final CellItemListener<KundeDO> cellItemListener = new CellItemListener<KundeDO>()
    {
      public void populateItem(final Item<ICellPopulator<KundeDO>> item, final String componentId,
          final IModel<KundeDO> rowModel)
      {
        final KundeDO kunde = rowModel.getObject();
        if (kunde.getStatus() == null) {
          // Should not occur:
          return;
        }
        appendCssClasses(item, kunde.getNummer(),
            kunde.getDeleted() == true || kunde.getStatus().isIn(KundeStatus.ENDED) == true);
      }
    };
    columns.add(
        new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("fibu.kunde.nummer")), "kost", "kost",
            cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<KundeDO>> item, final String componentId,
              final IModel<KundeDO> rowModel)
          {
            final KundeDO kunde = rowModel.getObject();
            if (isSelectMode() == false) {
              item.add(new ListSelectActionPanel(componentId, rowModel, CustomerEditPage.class, kunde.getNummer(),
                  returnToPage, String
                      .valueOf(kunde.getKost())));
            } else {
              item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, kunde.getNummer(),
                  String.valueOf(kunde.getKost())));
            }
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        });
    columns.add(
        new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("fibu.kunde.identifier")), "identifier",
            "identifier", cellItemListener));
    columns.add(
        new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("fibu.kunde.name")), "name", "name",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("fibu.kunde.division")),
        "division", "division",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("fibu.konto")), null, "konto",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<KundeDO>> item, final String componentId,
          final IModel<KundeDO> rowModel)
      {
        final KundeDO kunde = rowModel.getObject();
        final KontoDO konto = WicketSupport.get(KontoCache.class).getKontoIfNotInitialized(kunde.getKonto());
        item.add(new Label(componentId, konto != null ? konto.formatKonto() : ""));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("status")), "status", "status",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<KundeDO>(new Model<String>(getString("description")), "description",
        "description",
        cellItemListener));
    return columns;
  }

  public CustomerListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.kunde");
  }

  public CustomerListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.kunde");
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "kost", SortOrder.ASCENDING);
    form.add(dataTable);

    final BookmarkablePageLink<Void> addTemplatesLink = UserPrefListPage.createLink(ContentMenuEntryPanel.LINK_ID,
        UserPrefArea.KUNDE_FAVORITE);
    final ContentMenuEntryPanel menuEntry = new ContentMenuEntryPanel(getNewContentMenuChildId(), addTemplatesLink,
        getString("favorites"));
    addContentMenuEntry(menuEntry);
  }

  @Override
  protected CustomerListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new CustomerListForm(this);
  }

  @Override
  public KundeDao getBaseDao()
  {
    return WicketSupport.get(KundeDao.class);
  }
}
