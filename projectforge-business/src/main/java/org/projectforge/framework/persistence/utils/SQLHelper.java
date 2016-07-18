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

package org.projectforge.framework.persistence.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * Some helper methods ...
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SQLHelper
{
  /**
   * Usage:<br/>
   * 
   * <pre>
   * List&lt;Object[]&gt; list = () getSession().createQuery(&quot;select min(datum), max(datum) from AuftragDO t&quot;).list();
   * getYears(list);
   * </pre>
   * 
   * or
   * 
   * <pre>
   * List&lt;Object[]&gt; list = () getSession().createQuery(&quot;select min(year), max(year) from EmployeeSalaryDO t&quot;).list();
   * getYears(list);
   * </pre>
   * 
   * @param list
   * @return Array of years in descendent order.
   */
  public static int[] getYears(List<Object[]> list)
  {
    Validate.notNull(list);
    if (list.size() == 0 || list.get(0) == null || list.get(0)[0] == null) {
      return new int[] { Calendar.getInstance().get(Calendar.YEAR)};
    }
    int from, to;
    if (list.get(0)[0] instanceof Date) {
      Date min = (Date) list.get(0)[0];
      Date max = (Date) list.get(0)[1];
      Calendar cal = Calendar.getInstance();
      cal.setTime(min);
      from = cal.get(Calendar.YEAR);
      cal.setTime(max);
      to = cal.get(Calendar.YEAR);
    } else {
      from = (Integer) list.get(0)[0];
      to = (Integer) list.get(0)[1];
    }
    int[] res = new int[to - from + 1];
    int i = 0;
    for (int year = to; year >= from; year--) {
      res[i++] = year;
    }
    return res;
  }
}
