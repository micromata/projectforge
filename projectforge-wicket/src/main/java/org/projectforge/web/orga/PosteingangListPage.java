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

package org.projectforge.web.orga;

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
import org.projectforge.business.orga.PosteingangDO;
import org.projectforge.business.orga.PosteingangDao;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = PosteingangEditPage.class)
public class PosteingangListPage extends AbstractListPage<PosteingangListForm, PosteingangDao, PosteingangDO> implements
    IListPageColumnsCreator<PosteingangDO>
{
  private static final long serialVersionUID = 6121734373079865758L;

  @SpringBean
  private PosteingangDao posteingangDao;

  public PosteingangListPage(final PageParameters parameters)
  {
    super(parameters, "orga.posteingang");
  }

  /**
   * @see org.projectforge.web.wicket.IListPageColumnsCreator#createColumns(org.apache.wicket.markup.html.WebPage,
   *      boolean)
   */
  @SuppressWarnings("serial")
  @Override
  public List<IColumn<PosteingangDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<PosteingangDO, String>> columns = new ArrayList<IColumn<PosteingangDO, String>>();

    final CellItemListener<PosteingangDO> cellItemListener = new CellItemListener<PosteingangDO>()
    {
      public void populateItem(final Item<ICellPopulator<PosteingangDO>> item, final String componentId,
          final IModel<PosteingangDO> rowModel)
      {
        final PosteingangDO posteingang = rowModel.getObject();
        appendCssClasses(item, posteingang.getId(), posteingang.isDeleted());
      }
    };
    columns
        .add(new CellItemListenerPropertyColumn<PosteingangDO>(new Model<String>(getString("date")), "datum", "datum",
            cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<PosteingangDO>> item, final String componentId,
              final IModel<PosteingangDO> rowModel)
          {
            final PosteingangDO posteingang = rowModel.getObject();
            item.add(new ListSelectActionPanel(componentId, rowModel, PosteingangEditPage.class, posteingang.getId(),
                returnToPage, DateTimeFormatter.instance().getFormattedDate(posteingang.getDatum())));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        });
    columns.add(new CellItemListenerPropertyColumn<PosteingangDO>(
        new Model<String>(getString("orga.posteingang.absender")), "absender",
        "absender", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PosteingangDO>(
        new Model<String>(getString("orga.posteingang.person")), "person",
        "person", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PosteingangDO>(new Model<String>(getString("orga.post.inhalt")),
        "inhalt", "inhalt",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PosteingangDO>(new Model<String>(getString("comment")), "bemerkung",
        "bemerkung",
        cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<PosteingangDO>(new Model<String>(getString("orga.post.type")),
        "type", "type",
        cellItemListener));
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "datum", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected PosteingangListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new PosteingangListForm(this);
  }

  @Override
  public PosteingangDao getBaseDao()
  {
    return posteingangDao;
  }
}
