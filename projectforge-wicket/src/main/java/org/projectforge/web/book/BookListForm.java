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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookFilter;
import org.projectforge.common.StringHelper;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;

public class BookListForm extends AbstractListForm<BookListFilter, BookListPage>
{
  private static final long serialVersionUID = 7055486163615227688L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookListForm.class);

  @SpringBean
  private BookDao bookDao;

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "present"), getString("book.status.present")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "missed"),
        getString("book.status.missed")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "disposed"), getString("book.status.disposed")));
  }

  public BookListForm(final BookListPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected TextField<?> createSearchTextField()
  {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    final PFAutoCompleteTextField<BookDO> searchField = new PFAutoCompleteTextField<BookDO>(InputPanel.WICKET_ID,
        new Model()
        {
          @Override
          public Serializable getObject()
          {
            // Pseudo object for storing search string (title field is used for this foreign purpose).
            return new BookDO().setTitle(getSearchFilter().getSearchString());
          }

          @Override
          public void setObject(final Serializable object)
          {
            if (object != null) {
              if (object instanceof String) {
                getSearchFilter().setSearchString((String) object);
              }
            } else {
              getSearchFilter().setSearchString("");
            }
          }
        })
    {
      @Override
      protected List<BookDO> getChoices(final String input)
      {
        final BookFilter filter = new BookFilter();
        filter.setSearchString(input);
        filter.setSearchFields("title", "authors");
        final List<BookDO> list = bookDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return parentPage.getRecentSearchTermsQueue().getRecents();
      }

      @Override
      protected String formatLabel(final BookDO book)
      {
        return StringHelper.listToString("; ", book.getAuthors(), book.getTitle());
      }

      @Override
      protected String formatValue(final BookDO book)
      {
        return "id:" + book.getId();
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            searchFilter.setSearchString(value);
            return null;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            return searchFilter.getSearchString();
          }
        };
      }
    };
    searchField.withLabelValue(true).withMatchContains(true).withMinChars(2).withFocus(true).withAutoSubmit(true);
    createSearchFieldTooltip(searchField);
    return searchField;
  }

  @Override
  protected BookListFilter newSearchFilterInstance()
  {
    return new BookListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
