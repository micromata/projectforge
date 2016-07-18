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

package org.projectforge.web.fibu;

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
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = KontoEditPage.class)
public class KontoListPage extends AbstractListPage<KontoListForm, KontoDao, KontoDO> implements IListPageColumnsCreator<KontoDO>
{
  private static final long serialVersionUID = -8406452960003792763L;

  @SpringBean
  private KontoDao kontoDao;

  public KontoListPage(final PageParameters parameters)
  {
    super(parameters, "fibu.konto");
  }

  public KontoListPage(final ISelectCallerPage caller, final String selectProperty)
  {
    super(caller, selectProperty, "fibu.konto");
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<KontoDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<KontoDO, String>> columns = new ArrayList<IColumn<KontoDO, String>>();
    final CellItemListener<KontoDO> cellItemListener = new CellItemListener<KontoDO>() {
      public void populateItem(final Item<ICellPopulator<KontoDO>> item, final String componentId, final IModel<KontoDO> rowModel)
      {
        final KontoDO konto = rowModel.getObject();
        appendCssClasses(item, konto.getId(), konto.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<KontoDO>(new Model<String>(getString("fibu.konto.nummer")), getSortable("nummer",
        sortable), "nummer", cellItemListener) {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<KontoDO>> item, final String componentId, final IModel<KontoDO> rowModel)
      {
        final KontoDO konto = rowModel.getObject();
        if (isSelectMode() == false) {
          item.add(new ListSelectActionPanel(componentId, rowModel, KontoEditPage.class, konto.getId(), returnToPage, String.valueOf(konto
              .getNummer())));
        } else {
          item.add(new ListSelectActionPanel(componentId, rowModel, caller, selectProperty, konto.getId(),
              String.valueOf(konto.getNummer())));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<KontoDO>(new Model<String>(getString("status")), "status", "status", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<KontoDO>(new Model<String>(getString("fibu.konto.bezeichnung")), getSortable(
        "bezeichnung", sortable), "bezeichnung", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<KontoDO>(new Model<String>(getString("description")), getSortable("description",
        sortable), "description", cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "nummer", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  @Override
  protected KontoListForm newListForm(final AbstractListPage< ? , ? , ? > parentPage)
  {
    return new KontoListForm(this);
  }

  @Override
  protected KontoDao getBaseDao()
  {
    return kontoDao;
  }

  protected KontoDao getKontoDao()
  {
    return kontoDao;
  }
}
