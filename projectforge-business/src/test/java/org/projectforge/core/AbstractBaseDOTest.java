/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.core;

import org.junit.jupiter.api.Test;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookStatus;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractBaseDOTest extends AbstractTestBase {
  @Override
  protected void initDb() {
    init(false);
  }

  @Test
  public void determinePropertyName() throws NoSuchMethodException {
    BookDO obj = createBookDO(21, 22, false, BookStatus.DISPOSED, "Hurzel");
    final String created = DateHelper.getForTestCase(obj.getCreated());
    final String lastUpdate = DateHelper.getForTestCase(obj.getLastUpdate());

    BookDO src = createBookDO(19, 20, true, BookStatus.MISSED, "Test");

    obj.copyValuesFrom(src);

    assertEquals("Test", obj.getTitle());
    assertEquals(true, obj.isDeleted());
    assertEquals(BookStatus.MISSED, obj.getStatus());
    assertEquals(created, DateHelper.getForTestCase(obj.getCreated()));
    assertEquals(lastUpdate, DateHelper.getForTestCase(obj.getLastUpdate()));

    obj = createBookDO(21, 22, false, BookStatus.MISSED, "Hurzel");
    src = createBookDO(19, 20, false, BookStatus.MISSED, null);
    obj.copyValuesFrom(src);
    assertEquals(null, obj.getTitle(), "Expected, that the property will be overwritten by null");
    assertEquals(false, obj.isDeleted());
    assertEquals(BookStatus.MISSED, obj.getStatus());
    assertEquals(created, DateHelper.getForTestCase(obj.getCreated()));
    assertEquals(lastUpdate, DateHelper.getForTestCase(obj.getLastUpdate()));
  }

  private BookDO createBookDO(final int createdDayOfMonth, final int lastUpdateDateOfMonth, final boolean deleted,
                              final BookStatus bookStatus, final String testString) {
    BookDO obj = new BookDO();
    DateHolder dateHolder = new DateHolder(DatePrecision.SECOND, Locale.GERMAN);
    obj.setId(42);
    dateHolder.setDate(1970, Calendar.NOVEMBER, createdDayOfMonth, 4, 50, 0);
    obj.setCreated(dateHolder.getDate());
    dateHolder.setDate(1970, Calendar.NOVEMBER, lastUpdateDateOfMonth, 4, 50, 0);
    obj.setLastUpdate(dateHolder.getDate());
    obj.setDeleted(deleted);

    obj.setStatus(bookStatus);
    obj.setTitle(testString);
    return obj;
  }

  /**
   * does not work with entities, which are not in Emgr.
   */
  //  @Test
  public void copyValuesFrom() {
    final FooDO srcFoo = new FooDO();
    srcFoo.setManagedChildren(new ArrayList<BarDO>());
    srcFoo.setUnmanagedChildren1(new ArrayList<BarDO>());
    srcFoo.setUnmanagedChildren2(new ArrayList<BarDO>());
    srcFoo.getManagedChildren().add(new BarDO(1, "src1"));
    srcFoo.getManagedChildren().add(new BarDO(2, "src2"));
    srcFoo.getUnmanagedChildren1().add(new BarDO(3, "src3"));
    srcFoo.getUnmanagedChildren1().add(new BarDO(4, "src4"));
    srcFoo.getUnmanagedChildren2().add(new BarDO(5, "src5"));
    srcFoo.getUnmanagedChildren2().add(new BarDO(6, "src6"));
    final FooDO destFoo = new FooDO();
    destFoo.setManagedChildren(new ArrayList<BarDO>());
    destFoo.setUnmanagedChildren1(new ArrayList<BarDO>());
    destFoo.setUnmanagedChildren2(new ArrayList<BarDO>());
    destFoo.getManagedChildren().add(new BarDO(1, "dest1"));
    destFoo.getManagedChildren().add(new BarDO(2, "dest2"));
    destFoo.getUnmanagedChildren1().add(new BarDO(3, "dest3"));
    destFoo.getUnmanagedChildren1().add(new BarDO(4, "dest4"));
    destFoo.getUnmanagedChildren2().add(new BarDO(5, "dest5"));
    destFoo.getUnmanagedChildren2().add(new BarDO(6, "dest6"));
    destFoo.copyValuesFrom(srcFoo);
    ArrayList<BarDO> list = (ArrayList<BarDO>) destFoo.getManagedChildren();
    assertEquals("src1", list.get(0).getTestString());
    assertEquals("src2", list.get(1).getTestString());
    list = (ArrayList<BarDO>) destFoo.getUnmanagedChildren1();
    assertEquals("dest3", list.get(0).getTestString());
    assertEquals("dest4", list.get(1).getTestString());
    list = (ArrayList<BarDO>) destFoo.getUnmanagedChildren2();
    assertEquals("dest5", list.get(0).getTestString());
    assertEquals("dest6", list.get(1).getTestString());
  }
}
