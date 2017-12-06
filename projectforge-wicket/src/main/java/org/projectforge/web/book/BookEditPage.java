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

import java.util.Date;

import org.slf4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = BookListPage.class)
public class BookEditPage extends AbstractEditPage<BookDO, BookEditForm, BookDao> implements ISelectCallerPage
{
  private static final long serialVersionUID = 7091721062661400435L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookEditPage.class);

  @SpringBean
  private BookDao bookDao;

  public BookEditPage(final PageParameters parameters)
  {
    super(parameters, "book");
    init();
  }

  protected void lendOut()
  {
    getData().setLendOutDate(new Date());
    bookDao.setLendOutBy(getData(), getUserId());
    bookDao.update(getData());
    setResponsePage();
  }

  protected void returnBook()
  {
    getData().setLendOutDate(null);
    getData().setLendOutBy(null);
    getData().setLendOutComment(null);
    bookDao.update(getData());
    setResponsePage();
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("lendOutById".equals(property) == true) {
      bookDao.setLendOutBy(getData(), (Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("lendOutById".equals(property) == true) {
      getData().setLendOutBy(null);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  protected BookDao getBaseDao()
  {
    return bookDao;
  }

  @Override
  protected BookEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final BookDO data)
  {
    return new BookEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
