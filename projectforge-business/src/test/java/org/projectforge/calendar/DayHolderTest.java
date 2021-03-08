/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.calendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.TestSetup;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deprecated
public class DayHolderTest {
  @BeforeAll
  static void beforeAll() {
    TestSetup.init();
  }

  @Test
  public void isToday() {
    final DayHolder day = new DayHolder();
    assertTrue(day.isToday());
  }

  @Test
  public void testGetNumberOfWorkingDays() {
    final DayHolder fromDay = new DayHolder();
    fromDay.setDate(2009, Month.JANUARY, 1);
    final DayHolder toDay = new DayHolder();
    toDay.setDate(2009, Month.JANUARY, 31);
    assertBigDecimal(21, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
    toDay.setDate(2009, Month.FEBRUARY, 28);
    assertBigDecimal(41, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
  }

  @Test
  public void testGetNumberOfWorkingDaysOneDay() {
    final DayHolder fromDay = new DayHolder();
    fromDay.setDate(2009, Month.JANUARY, 5);
    final DayHolder toDay = new DayHolder();
    toDay.setDate(2009, Month.JANUARY, 5);
    assertBigDecimal(1, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
  }

  //Test fertigstellen für Weihnachten/Silvester (config.xml)
  @Test
  @Disabled
  public void testGetNumberOfWorkingDaysChristmas() {
    final DayHolder fromDay = new DayHolder();
    fromDay.setDate(2009, Month.DECEMBER, 24);
    final DayHolder toDay = new DayHolder();
    toDay.setDate(2009, Month.DECEMBER, 24);
    assertBigDecimal(1.5, DayHolder.getNumberOfWorkingDays(fromDay, toDay));
  }

  @Test
  public void testAdd() {
    final DayHolder day = new DayHolder();
    day.setDate(2008, Month.JANUARY, 1);
    day.add(Calendar.DAY_OF_YEAR, -1);
    assertEquals(day.getYear(), 2007);
    assertEquals(day.getMonth(), Month.DECEMBER);
    assertEquals(day.getDayOfMonth(), 31);
  }

  private void assertBigDecimal(final double expected, final BigDecimal value) {
    assertEquals(expected, value.doubleValue(), 0.00001);
  }
}
