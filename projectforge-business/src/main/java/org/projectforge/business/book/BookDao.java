/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.book;

import kotlin.Pair;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class BookDao extends BaseDao<BookDO> {

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"lendOutBy.username", "lendOutBy.firstname",
          "lendOutBy.lastname"};

  @Autowired
  private UserDao userDao;

  public BookDao() {
    super(BookDO.class);
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * Does the book's signature already exists? If signature is null, then return always false.
   *
   * @param book
   * @return
   */
  public boolean doesSignatureAlreadyExist(final BookDO book) {
    Validate.notNull(book);
    return doesSignatureAlreadyExist(book.getSignature(), book.getId());
  }

  public boolean doesSignatureAlreadyExist(final String signature, final Long id) {
    if (signature == null) {
      return false;
    }
    BookDO other = null;
    if (id == null) {
      // New book
      other = persistenceService.selectNamedSingleResult(
              BookDO.FIND_BY_SIGNATURE,
                      BookDO.class,
          new Pair<>("signature", signature));
    } else {
      // Book already exists. Check maybe changed signature:
      other = persistenceService.selectNamedSingleResult(
              BookDO.FIND_OTHER_BY_SIGNATURE,
                      BookDO.class,
           new Pair<>("signature", signature),
              new Pair<>("id", id));

    }
    return other != null;
  }

  public void setLendOutBy(final BookDO book, final Long lendOutById) {
    final PFUserDO user = userDao.getOrLoad(lendOutById);
    book.setLendOutBy(user);
  }

  /**
   * @return Always true, no generic select access needed for book objects.
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  /**
   * @return Always true.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final BookDO obj, final BookDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return true;
  }

  @Override
  public BookDO newInstance() {
    return new BookDO();
  }
}
