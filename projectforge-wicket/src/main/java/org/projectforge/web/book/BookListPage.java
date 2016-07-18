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

package org.projectforge.web.book;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = BookEditPage.class)
public class BookListPage extends AbstractListPage<BookListForm, BookDao, BookDO>
    implements IListPageColumnsCreator<BookDO>
{
  private static final long serialVersionUID = 7227240465661485515L;

  @SpringBean
  private BookDao bookDao;

  @SpringBean
  private UserFormatter userFormatter;

  public BookListPage(final PageParameters parameters)
  {
    super(parameters, "book");
  }

  @Override
  protected void setup()
  {
    super.setup();
    this.recentSearchTermsUserPrefKey = "bookSearchTerms";
  }

  @SuppressWarnings("serial")
  public List<IColumn<BookDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<BookDO, String>> columns = new ArrayList<IColumn<BookDO, String>>();
    final CellItemListener<BookDO> cellItemListener = new CellItemListener<BookDO>()
    {
      public void populateItem(final Item<ICellPopulator<BookDO>> item, final String componentId,
          final IModel<BookDO> rowModel)
      {
        final BookDO book = rowModel.getObject();
        appendCssClasses(item, book.getId(), book.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("created")),
        getSortable("created", sortable),
        "created", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<BookDO>> item, final String componentId,
          final IModel<BookDO> rowModel)
      {
        final BookDO book = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, BookEditPage.class, book.getId(), returnToPage,
            DateTimeFormatter
                .instance().getFormattedDate(book.getCreated())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.yearOfPublishing.short")),
        getSortable(
            "yearOfPublishing", sortable),
        "yearOfPublishing", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.signature")),
        getSortable("signature4Sort",
            sortable),
        "signature", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.authors")),
        getSortable("authors", sortable),
        "authors", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.title")),
        getSortable("title", sortable),
        "title", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.keywords")),
        getSortable("keywords", sortable), "keywords", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<BookDO>(new Model<String>(getString("book.lendOutBy")),
        getSortable("authors", sortable), "authors", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<BookDO>> item, final String componentId,
          final IModel<BookDO> rowModel)
      {
        final BookDO book = rowModel.getObject();
        final StringBuffer buf = new StringBuffer();
        if (book.getLendOutBy() != null) {
          buf.append(userFormatter.formatUser(book.getLendOutBy()));
          buf.append(" ");
          DateTimeFormatter.instance().getFormattedDate(book.getLendOutDate());
        }
        if (StringUtils.isNotEmpty(book.getLendOutComment()) == true) {
          buf.append(" ").append(book.getLendOutComment());
        }
        final String htmlString = HtmlHelper.escapeXml(buf.toString());
        final Label label = new Label(componentId, new Model<String>(htmlString));
        label.setEscapeModelStrings(false);
        item.add(label);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "created", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected BookListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new BookListForm(this);
  }

  @Override
  protected BookDao getBaseDao()
  {
    return bookDao;
  }

  protected BookDao getBookDao()
  {
    return bookDao;
  }
}
