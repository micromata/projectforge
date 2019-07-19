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

package org.projectforge.framework.persistence.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SQLHelper {
  /**
   * Usage:<br/>
   * <pre>
   *     final java.sql.Date[] minMaxDate = getSession().createNamedQuery(ContractDO.SELECT_MIN_MAX_DATE, java.sql.Date[].class)
   *             .getSingleResult();
   *     return SQLHelper.getYears(minMaxDate[0], minMaxDate[1]);
   * </pre>
   *
   * @return Array of years in descendent order. If min or max is null, the current year is returned.
   */
  public static int[] getYears(Date min, Date max) {
    if (min == null || max == null) {
      return new int[] { Calendar.getInstance().get(Calendar.YEAR)};
    }
    int from, to;
    Calendar cal = Calendar.getInstance();
    cal.setTime(min);
    from = cal.get(Calendar.YEAR);
    cal.setTime(max);
    to = cal.get(Calendar.YEAR);
    return getYears(from, to);
  }

  public static int[] getYears(Integer min, Integer max) {
    if (min == null || max == null) {
      return new int[] { Calendar.getInstance().get(Calendar.YEAR)};
    }
    int[] res = new int[max - min + 1];
    int i = 0;
    for (int year = max; year >= min; year--) {
      res[i++] = year;
    }
    return res;
  }
}
