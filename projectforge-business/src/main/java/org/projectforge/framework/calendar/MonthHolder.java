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

package org.projectforge.framework.calendar;

import org.projectforge.framework.time.*;

import java.math.BigDecimal;
import java.time.Month;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MonthHolder {
  /**
   * Keys of the month names (e. g. needed for I18n).
   */
  public static final String MONTH_KEYS[] = new String[]{"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};

  private List<WeekHolder> weeks;

  private int year = -1;

  private Month month = Month.JANUARY;

  private PFDate begin;

  private PFDate end;

  public MonthHolder() {

  }

  public MonthHolder(final PFDate date) {
    calculate(date);
  }

  public MonthHolder(final PFDateTime dateTime) {
    calculate(PFDate.from(dateTime));
  }


  /**
   * @param month      Can also be one month before or after if the day of the weeks of this month have an overlap to the nearby months.
   * @param dayOfMonth
   * @return null, if the demanded day is not member of the weeks of the MonthHolder.
   */
  public PFDate getDay(final Month month, final int dayOfMonth) {
    for (final WeekHolder week : weeks) {
      for (final PFDate day : week.getDays()) {
        if (day.getMonth() == month && day.getDayOfMonth() == dayOfMonth) {
          return day;
        }
      }
    }
    return null;
  }

  public MonthHolder(final Date date) {
    calculate(PFDate.from(date));
  }

  public MonthHolder(final int year, final Month month) {
    calculate(PFDate.withDate(year, month, 1));
  }

  private void calculate(PFDate date) {
    year = date.getYear();
    month = date.getMonth();
    begin = date.getBeginOfMonth(); // Storing begin of month.
    end = date.getEndOfMonth(); // Storing end of month.
    PFDate day = date.getBeginOfMonth().getBeginOfWeek(); // get first week (with days of previous month)

    weeks = new ArrayList<>();
    do {
      final WeekHolder week = new WeekHolder(day);
      weeks.add(week);
      day = day.plusWeeks(1);
    } while (day.getMonth() == month);
  }

  public int getYear() {
    return year;
  }

  public Month getMonth() {
    return month;
  }

  public List<PFDate> getDays() {
    final List<PFDate> list = new LinkedList<>();
    PFDate day = begin;
    int paranoiaCounter = 40;
    while (!day.isAfter(end) && --paranoiaCounter > 0) {
      list.add(day);
      day = day.plusDays(1);
    }
    return list;
  }

  public String getMonthKey() {
    if (month == null) {
      return "unknown";
    }
    return MONTH_KEYS[month.ordinal()];
  }

  /**
   * @return i18n key of the month name.
   */
  public String getI18nKey() {
    return "dateTime.month." + getMonthKey();
  }

  public WeekHolder getFirstWeek() {
    return getWeeks().get(0);
  }

  public WeekHolder getLastWeek() {
    return weeks.get(weeks.size() - 1);
  }

  public List<WeekHolder> getWeeks() {
    return weeks;
  }

  public Date getBegin() {
    return begin.getUtilDate();
  }

  public Date getEnd() {
    return end.getUtilDate();
  }

  /**
   * Is the given day member of the current month?
   *
   * @param day
   * @return
   */
  public boolean containsDay(final PFDate day) {
    return (!day.isBefore(begin) && !day.isAfter(end));
  }

  public BigDecimal getNumberOfWorkingDays() {
    return PFDateTimeUtils.getNumberOfWorkingDays(this.begin, this.end);
  }
}
