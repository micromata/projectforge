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

package org.projectforge.framework.calendar;

import java.util.concurrent.TimeUnit;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DurationUtils {
  /**
   * @param millis
   * @return hours:minutes (e. g. 2:30, 12:30, 127:15, ...)
   */
  public static String getFormattedHoursAndMinutes(final long millis) {
    return getFormattedHoursAndMinutes(millis, false);
  }

  /**
   * @param millis
   * @return hours:minutes (e. g. 2:30, 12:30, 127:15, ...)
   */
  public static String getFormattedHoursAndMinutes(final long millis, boolean withSeconds) {
    final StringBuilder sb = new StringBuilder();
    addFormattedHoursAndMinutes(sb, millis, withSeconds);
    return sb.toString();
  }

  /**
   * @param millis
   */
  private static void addFormattedHoursAndMinutes(final StringBuilder sb, final long millis, boolean withSeconds) {
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    long leftMillis = millis - TimeUnit.HOURS.toMillis(hours);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(leftMillis);

    sb.append(hours).append(":"); // hours
    formatNumber(sb, minutes); // minutes

    if (withSeconds) {
      leftMillis -= TimeUnit.MINUTES.toMillis(minutes);
      sb.append(hours).append(":"); // hours
      formatNumber(sb, TimeUnit.MILLISECONDS.toSeconds(leftMillis)); // minutes
    }
  }

  /**
   * @param millis
   * @return days hours:minutes (e. g. 2d 2:30, 5d 12:30, 2:15, ...)
   */
  public static String getFormattedDaysHoursAndMinutes(final long millis) {
    long days = TimeUnit.MILLISECONDS.toDays(millis);
    if (days == 0) {
      return getFormattedHoursAndMinutes(millis, false);
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(days).append("d ");

    long leftMillis = millis - TimeUnit.DAYS.toMillis(days);
    addFormattedHoursAndMinutes(sb, leftMillis, false);
    return sb.toString();
  }

  private static void formatNumber(final StringBuilder buf, final long number) {
    if (number >= 0 && number < 10) {
      buf.append("0");
    }
    buf.append(number);
  }

  private static final int MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
}
