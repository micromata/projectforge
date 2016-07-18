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

package org.projectforge.book;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.Serializable;

import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookStatus;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.Test;

public class BookTest extends AbstractTestBase
{
  @Autowired
  private BookDao bookDao;

  @Autowired
  private TransactionTemplate txTemplate;

  @Test
  public void testUniqueSignatureDO()
  {
    final Serializable[] ids = new Integer[3];
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      public Object doInTransaction(TransactionStatus status)
      {
        BookDO book = createTestBook("42");
        ids[0] = bookDao.internalSave(book);
        book = createTestBook(null);
        ids[1] = bookDao.internalSave(book);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      public Object doInTransaction(TransactionStatus status)
      {
        BookDO book = createTestBook("42");
        assertTrue("Signature should already exist.", bookDao.doesSignatureAlreadyExist(book));
        book.setSignature("5");
        assertFalse("Signature should not exist.", bookDao.doesSignatureAlreadyExist(book));
        bookDao.internalSave(book);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      public Object doInTransaction(TransactionStatus status)
      {
        BookDO dbBook = bookDao.internalGetById(ids[1]);
        BookDO book = new BookDO();
        book.copyValuesFrom(dbBook);
        assertFalse("Signature should not exist.", bookDao.doesSignatureAlreadyExist(book));
        book.setSignature("42");
        assertTrue("Signature should already exist.", bookDao.doesSignatureAlreadyExist(book));
        book.setSignature("4711");
        assertFalse("Signature should not exist.", bookDao.doesSignatureAlreadyExist(book));
        bookDao.internalUpdate(book);
        return null;
      }
    });
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      public Object doInTransaction(TransactionStatus status)
      {
        BookDO book = bookDao.internalGetById(ids[1]);
        assertFalse("Signature should not exist.", bookDao.doesSignatureAlreadyExist(book));
        book.setSignature(null);
        assertFalse("Signature should not exist.", bookDao.doesSignatureAlreadyExist(book));
        bookDao.internalUpdate(book);
        return null;
      }
    });
  }

  @Test
  public void testGetSignature4Sort()
  {
    final BookDO book = new BookDO();
    book.setSignature(null);
    assertNull(book.getSignature4Sort());
    book.setSignature("");
    assertEquals("", book.getSignature4Sort());
    book.setSignature("W");
    assertEquals("W", book.getSignature4Sort());
    book.setSignature("WT-");
    assertEquals("WT-", book.getSignature4Sort());
    book.setSignature("WT-23a");
    assertEquals("WT-00023a", book.getSignature4Sort());
    book.setSignature("WT-2-5");
    assertEquals("WT-00002-00005", book.getSignature4Sort());
  }

  private BookDO createTestBook(String signature)
  {
    BookDO book = new BookDO();
    book.setTask(getTask("root"));
    book.setSignature(signature);
    book.setStatus(BookStatus.PRESENT);
    return book;
  }
}
