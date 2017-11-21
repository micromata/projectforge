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

package org.projectforge.framework.time;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.Validate;
import org.projectforge.framework.calendar.Holidays;
import org.projectforge.framework.i18n.UserException;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DayHolder extends DateHolder
{
  private static final long serialVersionUID = 2646871164508930568L;

  /**
   * I18n keys of the day names (e. g. needed for I18n).
   */
  public static final String DAY_KEYS[] = new String[] { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };

  private transient Holidays holidays = Holidays.getInstance();

  private Map<String, Object> objects;

  /**
   * Only set, if day is holiday.
   */
  private String holidayInfo = null;

  private boolean marker = false;

  public static String getDayKey(final int dayOfWeek)
  {
    return DAY_KEYS[dayOfWeek - 1];
  }

  public static BigDecimal getNumberOfWorkingDays(final Date from, final Date to)
  {
    Validate.notNull(from);
    Validate.notNull(to);
    final DayHolder fromDay = new DayHolder(from);
    final DayHolder toDay = new DayHolder(to);
    return getNumberOfWorkingDays(fromDay, toDay);
  }

  public static BigDecimal getNumberOfWorkingDays(final DateHolder from, final DateHolder to)
  {
    Validate.notNull(from);
    Validate.notNull(to);
    if (to.before(from) == true) {
      return BigDecimal.ZERO;
    }
    if (from.isSameDay(to) == true) {
      final DayHolder day = new DayHolder(from);
      if (day.isWorkingDay() == true) {
        final BigDecimal workFraction = day.getWorkFraction();
        if (workFraction != null) {
          return day.getWorkFraction();
        } else {
          return BigDecimal.ONE;
        }
      } else {
        return BigDecimal.ZERO;
      }
    }
    final DayHolder day = new DayHolder(from);
    BigDecimal numberOfWorkingDays = BigDecimal.ZERO;
    int numberOfFullWorkingDays = 0;
    int dayCounter = 1;
    if (day.isWorkingDay() == true) {
      final BigDecimal workFraction = day.getWorkFraction();
      if (workFraction != null) {
        numberOfWorkingDays = numberOfWorkingDays.add(day.getWorkFraction());
      } else {
        numberOfFullWorkingDays++;
      }
    }
    do {
      day.add(Calendar.DAY_OF_MONTH, 1);
      if (dayCounter++ > 740) { // Endless loop protection, time period greater 2 years.
        throw new UserException(
            "getNumberOfWorkingDays does not support calculation of working days for a time period greater than two years!");
      }
      if (day.isWorkingDay() == true) {
        final BigDecimal workFraction = day.getWorkFraction();
        if (workFraction != null) {
          numberOfWorkingDays = numberOfWorkingDays.add(day.getWorkFraction());
        } else {
          numberOfFullWorkingDays++;
        }
      }
    } while (day.isSameDay(to) == false);
    numberOfWorkingDays = numberOfWorkingDays.add(new BigDecimal(numberOfFullWorkingDays));
    return numberOfWorkingDays;
  }

  /**
   * Initializes with current day (with time zone UTC!).
   */
  public DayHolder()
  {
    super(DatePrecision.DAY);
  }

  /**
   * @param date
   */
  public DayHolder(final Date date)
  {
    super(date, DatePrecision.DAY);
  }

  public DayHolder(final Date date, final TimeZone timeZone, final Locale locale)
  {
    super(date, DatePrecision.DAY, timeZone, locale);
  }

  public DayHolder(final DateHolder dateHolder)
  {
    this.calendar = (Calendar) dateHolder.getCalendar().clone();
    setPrecision(DatePrecision.DAY);
  }

  /**
   * Multipurpose marker, e. g. used by select date for marking days as days not from the current month.
   *
   * @return
   */
  public boolean isMarker()
  {
    return marker;
  }

  public DayHolder setMarker(final boolean marker)
  {
    this.marker = marker;
    return this;
  }

  public String getDayKey()
  {
    return getDayKey(getDayOfWeek());
  }

  public boolean isToday()
  {
    return isSameDay(new Date());
  }

  public boolean isSunday()
  {
    return Calendar.SUNDAY == getDayOfWeek();
  }

  public boolean isWeekend()
  {
    final int dayOfWeek = getDayOfWeek();
    return Calendar.SUNDAY == dayOfWeek || Calendar.SATURDAY == dayOfWeek;
  }

  public boolean isHoliday()
  {
    return holidays.isHoliday(getYear(), getDayOfYear());
  }

  public boolean isSunOrHoliday()
  {
    if (isSunday() == true || isHoliday() == true)
      return true;
    else return false;
  }

  public boolean isWorkingDay()
  {
    return holidays.isWorkingDay(this);
  }

  /**
   * Weekend days have always no work fraction!
   */
  public BigDecimal getWorkFraction()
  {
    return holidays.getWorkFraction(this);
  }

  public String getHolidayInfo()
  {
    if (holidayInfo == null) {
      holidayInfo = holidays.getHolidayInfo(getYear(), getDayOfYear());
    }
    return holidayInfo;
  }

  public DayHolder setHolidays(final Holidays holidays)
  {
    this.holidays = holidays;
    return this;
  }

  /**
   * Does not set end of day as DateHolder.
   */
  @Override
  public DayHolder setEndOfWeek()
  {
    calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() + 6);
    return this;
  }

  /**
   * Does not set end of day as DateHolder.
   */
  @Override
  public DayHolder setEndOfMonth()
  {
    final int day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    calendar.set(Calendar.DAY_OF_MONTH, day);
    return this;
  }

  /**
   * @return The time in millis for the day represented by this object but with current time of day (now).
   * @see DateHelper#getCalendar()
   */
  public long getMillisForCurrentTimeOfDay()
  {
    final Calendar cal = (Calendar) calendar.clone();
    cal.set(Calendar.YEAR, this.getYear());
    cal.set(Calendar.MONTH, this.getMonth());
    cal.set(Calendar.DAY_OF_MONTH, this.getDayOfMonth());
    return cal.getTimeInMillis();
  }

  @Override
  public String toString()
  {
    return isoFormat();
  }

  public String isoFormat()
  {
    return DateHelper.formatIsoDate(getDate());
  }

  /**
   * For storing additional objects to a day. This is used by the date selector for showing the user's timesheets at this day.
   *
   * @param key
   * @param value
   */
  public DayHolder addObject(final String key, final Object value)
  {
    if (this.objects == null) {
      this.objects = new HashMap<String, Object>();
    }
    this.objects.put(key, value);
    return this;
  }

  /**
   * Used for getting e. g. the user time sheets at this day for showing the calendar in ical like format.
   *
   * @return the stored objects to this day or null, if not exist.
   */
  public Object getObject(final String key)
  {
    if (this.objects == null) {
      return null;
    }
    return this.objects.get(key);
  }

  public Map<String, Object> getObjects()
  {
    return this.objects;
  }

  @Override
  public DayHolder clone()
  {
    final DayHolder res = new DayHolder();
    res.calendar = (Calendar) this.calendar.clone();
    return res;
  }
}
