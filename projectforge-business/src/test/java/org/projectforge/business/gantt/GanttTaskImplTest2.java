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

package org.projectforge.business.gantt;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;

import org.projectforge.business.gantt.GanttTask;
import org.projectforge.business.gantt.GanttTaskImpl;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.time.DayHolder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GanttTaskImplTest2
{
  @BeforeClass
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void hasDuration()
  {
    final GanttTask task = new GanttTaskImpl();
    assertFalse("duration should be 0.", task.hasDuration());
    task.setDuration(BigDecimal.TEN);
    assertTrue("duration should be 10.", task.hasDuration());
    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.AUGUST, 13);
    task.setStartDate(day.getDate());
    assertTrue("duration should be 10.", task.hasDuration());
    task.setDuration(null);
    assertFalse("duration should be null.", task.hasDuration());
    day.add(Calendar.DAY_OF_MONTH, 1);
    task.setEndDate(day.getDate());
    assertTrue("duration expected.", task.hasDuration());
  }
}
