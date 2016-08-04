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

package org.projectforge.web.meb;

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
import org.projectforge.business.meb.MebDao;
import org.projectforge.business.meb.MebEntryDO;
import org.projectforge.business.meb.MebEntryStatus;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.RowCssClass;

@ListPage(editPage = MebEditPage.class)
public class MebListPage extends AbstractListPage<MebListForm, MebDao, MebEntryDO>
    implements IListPageColumnsCreator<MebEntryDO>
{
  private static final long serialVersionUID = -3852280776436565963L;

  @SpringBean
  private MebDao mebDao;

  @SpringBean
  private UserFormatter userFormatter;

  public MebListPage(final PageParameters parameters)
  {
    super(parameters, "meb");
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<MebEntryDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<MebEntryDO, String>> columns = new ArrayList<IColumn<MebEntryDO, String>>();
    final CellItemListener<MebEntryDO> cellItemListener = new CellItemListener<MebEntryDO>()
    {
      public void populateItem(final Item<ICellPopulator<MebEntryDO>> item, final String componentId,
          final IModel<MebEntryDO> rowModel)
      {
        final MebEntryDO meb = rowModel.getObject();
        appendCssClasses(item, meb.getId(), meb.isDeleted());
        if (meb.isDeleted() == true) {
          // Should not occur
        } else if (meb.getStatus() == MebEntryStatus.RECENT) {
          appendCssClasses(item, RowCssClass.RECENT);
        } else if (meb.getStatus() == MebEntryStatus.IMPORTANT) {
          appendCssClasses(item, RowCssClass.IMPORTANT_ROW);
        } else if (meb.getStatus() == MebEntryStatus.DONE) {
          appendCssClasses(item, RowCssClass.SUCCESS_ROW);
        }
      }
    };
    columns.add(new CellItemListenerPropertyColumn<MebEntryDO>(new Model<String>(getString("date")), "date", "date",
        cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<MebEntryDO>> item, final String componentId,
          final IModel<MebEntryDO> rowModel)
      {
        final MebEntryDO meb = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, MebEditPage.class, meb.getId(), MebListPage.this,
            DateTimeFormatter
                .instance().getFormattedDateTime(meb.getDate())));
        cellItemListener.populateItem(item, componentId, rowModel);
        cellItemListener.populateItem(item, componentId, rowModel);
        addRowClick(item);
      }
    });
    columns
        .add(new UserPropertyColumn<MebEntryDO>(getUserGroupCache(), getString("meb.owner"), "owner", "owner",
            cellItemListener)
                .withUserFormatter(userFormatter));
    columns.add(
        new CellItemListenerPropertyColumn<MebEntryDO>(new Model<String>(getString("meb.sender")), "sender", "sender",
            cellItemListener));
    columns
        .add(new CellItemListenerPropertyColumn<MebEntryDO>(new Model<String>(getString("status")), "status", "status",
            cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<MebEntryDO>(new Model<String>(getString("meb.message")), "message",
        "message",
        cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "date", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected MebListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new MebListForm(this);
  }

  @Override
  public MebDao getBaseDao()
  {
    return mebDao;
  }

  protected MebDao getMebDao()
  {
    return mebDao;
  }
}
