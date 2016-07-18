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

package org.projectforge.framework.calendar;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DurationUtils
{
  /**
   * @param millis
   * @return hours:minutes (e. g. 2:30, 12:30, 127:15, ...)
   */
  public static String getFormattedHoursAndMinutes(final long millis)
  {
    final long seconds = millis / 60000;
    final int hours = (int) seconds / 60;
    final int minutes = (int) seconds % 60;
    final StringBuilder buf = new StringBuilder();
    buf.append(hours).append(":"); // hours
    formatNumber(buf, minutes); // minutes
    return buf.toString();
  }

  private static void formatNumber(final StringBuilder buf, final long number)
  {
    if (number < 10) {
      buf.append("0");
    }
    buf.append(number);
  }

}
