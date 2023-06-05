/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.calendar.DurationUtils;

public class DurationUtilsTest {
  @Test
  public void formattedHoursAndMinutes() {
    Assertions.assertEquals("0:00", getFormattedHoursAndMinutes(0, 0));
    Assertions.assertEquals("0:01", getFormattedHoursAndMinutes(0, 1));
    Assertions.assertEquals("0:59", getFormattedHoursAndMinutes(0, 59));
    Assertions.assertEquals("1:00", getFormattedHoursAndMinutes(0, 60));
    Assertions.assertEquals("9:59", getFormattedHoursAndMinutes(9, 59));
    Assertions.assertEquals("10:59", getFormattedHoursAndMinutes(10, 59));
  }

  @Test
  public void formattedDaysHoursAndMinutes() {
    assertFormattedDaysHoursAndMinutes("", 0);
    assertFormattedDaysHoursAndMinutes("1d ", 1);
    assertFormattedDaysHoursAndMinutes("2d ", 2);
    assertFormattedDaysHoursAndMinutes("5d ", 5);

    Assertions.assertEquals("1d 0:00", getFormattedDaysHoursAndMinutes(0, 24, 0));
    Assertions.assertEquals("1d 0:59", getFormattedDaysHoursAndMinutes(0, 24, 59));
    Assertions.assertEquals("1d 1:59", getFormattedDaysHoursAndMinutes(0, 25, 59));

    Assertions.assertEquals("-1d -3:-5", DurationUtils.getFormattedDaysHoursAndMinutes(-3600000 * 24 - 3 * 3600000 - 5 * 60000));
  }

  private String getFormattedHoursAndMinutes(final int hours, final int minutes) {
    return DurationUtils.getFormattedHoursAndMinutes(hours * 3600000 + minutes * 60000);
  }

  private void assertFormattedDaysHoursAndMinutes(final String dayString, final int days) {
    Assertions.assertEquals(dayString + "0:00", getFormattedDaysHoursAndMinutes(days, 0, 0));
    Assertions.assertEquals(dayString + "0:01", getFormattedDaysHoursAndMinutes(days, 0, 1));
    Assertions.assertEquals(dayString + "0:59", getFormattedDaysHoursAndMinutes(days, 0, 59));
    Assertions.assertEquals(dayString + "1:00", getFormattedDaysHoursAndMinutes(days, 0, 60));
    Assertions.assertEquals(dayString + "9:59", getFormattedDaysHoursAndMinutes(days, 9, 59));
    Assertions.assertEquals(dayString + "10:59", getFormattedDaysHoursAndMinutes(days, 10, 59));
    Assertions.assertEquals(dayString + "23:59", getFormattedDaysHoursAndMinutes(days, 23, 59));
  }

  private String getFormattedDaysHoursAndMinutes(final int days, final int hours, final int minutes) {
    return DurationUtils.getFormattedDaysHoursAndMinutes(days * 3600000 * 24 + hours * 3600000 + minutes * 60000);
  }
}
