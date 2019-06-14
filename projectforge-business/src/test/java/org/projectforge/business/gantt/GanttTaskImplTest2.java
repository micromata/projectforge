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

package org.projectforge.business.gantt;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.TestSetup;

import java.math.BigDecimal;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GanttTaskImplTest2 {
  @BeforeAll
  public static void setUp() {
    // Needed if this tests runs before the ConfigurationTest.
    TestSetup.init();
  }

  @Test
  public void hasDuration() {
    final GanttTask task = new GanttTaskImpl();
    assertFalse(task.hasDuration(), "duration should be 0.");
    task.setDuration(BigDecimal.TEN);
    assertTrue(task.hasDuration(), "duration should be 10.");
    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.AUGUST, 13);
    task.setStartDate(day.getDate());
    assertTrue(task.hasDuration(), "duration should be 10.");
    task.setDuration(null);
    assertFalse(task.hasDuration(), "duration should be null.");
    day.add(Calendar.DAY_OF_MONTH, 1);
    task.setEndDate(day.getDate());
    assertTrue(task.hasDuration(), "duration expected.");
  }
}
