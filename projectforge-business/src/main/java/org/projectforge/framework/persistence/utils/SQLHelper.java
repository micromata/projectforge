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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.projectforge.framework.i18n.InternalErrorException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class SQLHelper {
  /**
   * Usage:<br/>
   * <pre>
   *     final Object[] minMaxDate = getSession().createNamedQuery(ContractDO.SELECT_MIN_MAX_DATE, Object[].class)
   *             .getSingleResult();
   *     return SQLHelper.getYears((Date)minMaxDate[0], (Date)minMaxDate[1]);
   * </pre>
   *
   * @return Array of years in descendent order. If min or max is null, the current year is returned.
   */
  public static int[] getYears(Date min, Date max) {
    if (min == null || max == null) {
      return new int[]{Calendar.getInstance().get(Calendar.YEAR)};
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
      return new int[]{Calendar.getInstance().get(Calendar.YEAR)};
    }
    int[] res = new int[max - min + 1];
    int i = 0;
    for (int year = max; year >= min; year--) {
      res[i++] = year;
    }
    return res;
  }

  /**
   * Do a query.list() call and ensures that the result is either null/empty or the result list has only one element (size == 1).
   * If multiple entries were received, an Exception will be thrown
   * <br/>
   * Through this method ProjectForge ensures, that some entities are unique by their defined attributes (invoices with unique number etc.), especially
   * if the uniquness can't be guaranteed by a data base constraint.
   * <br/>
   * An internal error prevents the system on proceed with inconsistent (multiple) data entries.
   *
   * @throws InternalErrorException if the list is not empty and has more than one elements (size > 1).
   */
  public static <T> T ensureUniqueResult(Query<T> query) {
    return ensureUniqueResult(query, null);
  }

  /**
   * Do a query.list() call and ensures that the result is either null/empty or the result list has only one element (size == 1).
   * If multiple entries were received, an Exception will be thrown
   * <br/>
   * Through this method ProjectForge ensures, that some entities are unique by their defined attributes (invoices with unique number etc.), especially
   * if the uniquness can't be guaranteed by a data base constraint.
   * <br/>
   * An internal error prevents the system on proceed with inconsistent (multiple) data entries.
   *
   * @param errorMessage An optional error message to display.
   * @throws InternalErrorException if the list is not empty and has more than one elements (size > 1).
   */
  public static <T> T ensureUniqueResult(Query<T> query, String errorMessage) {
    List<T> list = query.list();
    if (list == null || list.size() == 0) {
      return null;
    }
    if (list.size() > 1) {
      throw new InternalErrorException("Internal error: ProjectForge detects an uniqueness violation in the database. Found multiple entries for: " + queryToString(query, errorMessage));
    }
    return list.get(0);
  }


  static String queryToString(Query<?> query, String errorMessage) {
    StringBuilder sb = new StringBuilder();
    sb.append("query='")
            .append(query.getQueryString())
            .append("', params=[");
    boolean first = true;
    for (String param : query.getParameterMetadata().getNamedParameterNames()) {
      if (!first) sb.append(",");
      else first = false;
      sb.append(param)
              .append("=[")
              .append(query.getParameterValue(param))
              .append("]");
    }
    sb.append("]");
    if (StringUtils.isNotBlank(errorMessage))
      sb.append(", msg=[")
              .append(errorMessage)
              .append("]");
    return sb.toString();
  }
}
