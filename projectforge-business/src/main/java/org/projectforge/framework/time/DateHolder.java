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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

/**
 * Parse and formats dates.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DateHolder implements Serializable, Cloneable, Comparable<DateHolder>
{
  private static final long serialVersionUID = -5373883617915418698L;

  protected Calendar calendar;

  private DatePrecision precision;

  /**
   * Initializes calendar with current date and uses the time zone and the locale of the ContextUser if exists.
   * @see DateHelper#getCalendar()
   */
  public DateHolder()
  {
    this.calendar = DateHelper.getCalendar();
  }

  /**
   * Ensures the precision.
   * @param precision
   * @param timeZone
   * @param locale
   */
  public DateHolder(final DatePrecision precision, final TimeZone timeZone, final Locale locale)
  {
    calendar = Calendar.getInstance(timeZone, locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @param precision
   * @param locale
   * @see DateHelper#getCalendar(Locale)
   */
  public DateHolder(final DatePrecision precision, final Locale locale)
  {
    calendar = DateHelper.getCalendar(locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @see DateHelper#getCalendar()
   */
  public DateHolder(final DatePrecision precision)
  {
    this.calendar = DateHelper.getCalendar();
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @param date If null, the current date will be used.
   * @param precision
   * @see DateHelper#getCalendar()
   */
  public DateHolder(final Date date, final DatePrecision precision)
  {
    this.calendar = DateHelper.getCalendar();
    if (date != null) {
      this.calendar.setTime(date);
    }
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @param date
   * @see DateHelper#getCalendar(Locale)
   */
  public DateHolder(final Date date, final DatePrecision precision, final Locale locale)
  {
    this.calendar = DateHelper.getCalendar(locale);
    this.calendar.setTime(date);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @param date
   * @see DateHelper#getCalendar(TimeZone, Locale)
   */
  public DateHolder(final Date date, final DatePrecision precision, final TimeZone timeZone, final Locale locale)
  {
    this.calendar = DateHelper.getCalendar(timeZone, locale);
    this.calendar.setTime(date);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   * @param date
   * @see DateHelper#getCalendar()
   */
  public DateHolder(final Date date)
  {
    this.calendar = DateHelper.getCalendar();
    if (date != null) {
      calendar.setTime(date);
    }
    ensurePrecision();
  }

  /**
   * Initializes calendar with given date and uses the given time zone and the locale of the ContextUser if exists.
   */
  public DateHolder(final Date date, final TimeZone timeZone)
  {
    this(date);
    this.calendar.setTimeZone(timeZone);
  }

  /**
   * @see DateHelper#getCalendar(TimeZone, Locale)
   */
  public DateHolder(final Date date, final TimeZone timeZone, final Locale locale)
  {
    this.calendar = DateHelper.getCalendar(timeZone, locale);
    this.calendar.setTime(date);
  }

  /**
   * Initializes calendar with current date and uses the given time zone and the locale of the ContextUser if exists.
   * @see DateHelper#getCalendar(TimeZone)
   */
  public DateHolder(final TimeZone timeZone)
  {
    this();
    this.calendar.setTimeZone(timeZone);
  }

  /**
   * Ensures the precision.
   * @param date
   * @param locale
   * @see DateHelper#getCalendar(Locale)
   */
  public DateHolder(final Date date, final Locale locale)
  {
    this.calendar = DateHelper.getCalendar(locale);
    calendar.setTime(date);
    ensurePrecision();
  }

  /** Clones calendar. */
  public DateHolder(final Calendar cal, final DatePrecision precision)
  {
    this(cal);
    setPrecision(precision);
  }

  /** Clones calendar. */
  public DateHolder(final Calendar cal)
  {
    this.calendar = (Calendar) cal.clone();
  }

  public boolean before(final DateHolder date)
  {
    return this.getDate().before(date.getDate());
  }

  public boolean before(final Date date)
  {
    return this.getDate().before(date);
  }

  public boolean after(final DateHolder date)
  {
    return this.getDate().after(date.getDate());
  }

  public boolean after(final Date date)
  {
    return this.getDate().after(date);
  }

  public boolean isBetween(final Date from, final Date to)
  {
    final Date date = getDate();
    if (from == null) {
      if (to == null) {
        return false;
      }
      return date.after(to) == false;
    }
    if (to == null) {
      return date.before(from) == false;
    }
    return !(date.after(to) == true || date.before(from) == true);
  }

  public boolean isBetween(final DateHolder from, final DateHolder to)
  {
    final Date fromDate = from != null ? from.getDate() : null;
    final Date toDate = to != null ? to.getDate() : null;
    return isBetween(fromDate, toDate);
  }

  /** Clones the calendar. */
  public DateHolder setCalendar(final Calendar cal)
  {
    this.calendar = (Calendar) cal.clone();
    ensurePrecision();
    return this;
  }

  /**
   * Date will be set. Dependent on precision, also seconds etc. will be set to zero. Ensures the precision.
   */
  public DateHolder setDate(final Date date)
  {
    if (date == null) {
      return this;
    }
    this.calendar.setTime(date);
    ensurePrecision();
    return this;
  }

  /**
   * Date will be set. Dependent on precision, also seconds etc. will be set to zero. Ensures the precision.
   * @param millis UTC millis
   */
  public DateHolder setDate(final long millis)
  {
    this.calendar.setTimeInMillis(millis);
    ensurePrecision();
    return this;
  }

  /**
   * Sets the precision of the date represented by this object. Ensures the precision.
   * @param precision SECOND, MINUTE, MINUTE_15, HOUR_OF_DAY or DAY.
   */
  public DateHolder setPrecision(final DatePrecision precision)
  {
    this.precision = precision;
    ensurePrecision();
    return this;
  }

  /**
   * Ensure the given precision by setting / rounding fields such as minutes and seconds. If precision is MINUTE_15 then rounding the
   * minutes down: 00-14 -&gt; 00; 15-29 -&gt; 15, 30-44 -&gt; 30, 45-59 -&gt; 45.
   */
  public DateHolder ensurePrecision()
  {
    if (this.precision == null) {
      return this;
    }
    switch (this.precision) {
      case DAY:
        calendar.set(Calendar.HOUR_OF_DAY, 0);
      case HOUR_OF_DAY:
        calendar.set(Calendar.MINUTE, 0);
      case MINUTE_15:
      case MINUTE_5:
      case MINUTE:
        calendar.set(Calendar.SECOND, 0);
      case SECOND:
        calendar.set(Calendar.MILLISECOND, 0);
      default:
    }
    if (this.precision == DatePrecision.MINUTE_15) {
      final int minute = calendar.get(Calendar.MINUTE);
      if (minute < 8) {
        calendar.set(Calendar.MINUTE, 0);
      } else if (minute < 23) {
        calendar.set(Calendar.MINUTE, 15);
      } else if (minute < 38) {
        calendar.set(Calendar.MINUTE, 30);
      } else if (minute < 53) {
        calendar.set(Calendar.MINUTE, 45);
      } else {
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR, 1);
      }
    } else if (this.precision == DatePrecision.MINUTE_5) {
      final int minute = calendar.get(Calendar.MINUTE);
      for (int i = 3; i < 60; i += 5) {
        if (minute < i) {
          calendar.set(Calendar.MINUTE, i - 3);
          break;
        }
      }
      if (minute > 57) {
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.HOUR, 1);
      }
    }
    return this;
  }

  public DatePrecision getPrecision()
  {
    return precision;
  }

  /**
   * Considers the time zone of the user, for example, if date is 20.11.1970 23:00:00 UTC but the user's locale is Europe/Berlin then the
   * java.sql.Date should be 21.11.1970! <br/>
   * This methods transforms first the day into UTC and then into java.sql.Date.
   */

  public java.sql.Date getSQLDate()
  {
    final Calendar cal = Calendar.getInstance(DateHelper.UTC);
    cal.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
    cal.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR));
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    final java.sql.Date date = new java.sql.Date(cal.getTimeInMillis());
    return date;
  }

  /**
   * Has the given date the same day? The given date will be converted into a calendar (clone from this) with same time zone.
   * @param date
   * @return
   */
  public boolean isSameDay(final Date date)
  {
    final DateHolder other = new DateHolder(this.calendar);
    other.setDate(date);
    return isSameDay(other);
  }

  /**
   * Has the given date the same day? The given date will be converted into a calendar (clone from this) with same time zone.
   * @param date
   * @return
   */
  public boolean isSameDay(final DateHolder date)
  {
    return getYear() == date.getYear() && getDayOfYear() == date.getDayOfYear();
  }

  /**
   * Sets the date to the beginning of the year (first day of year) and calls setBeginOfDay.
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfYear()
  {
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    setBeginOfMonth();
    return this;
  }

  /**
   * Sets the date to the beginning of the year (first day of year) and calls setBeginOfDay.
   * @see #setBeginOfDay()
   */
  public DateHolder setEndOfYear()
  {
    calendar.set(Calendar.MONTH, Calendar.DECEMBER);
    setEndOfMonth();
    return this;
  }

  /**
   * Sets the date to the beginning of the month (first day of month) and calls setBeginOfDay.
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfMonth()
  {
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    setBeginOfDay();
    return this;
  }

  public DateHolder setEndOfMonth()
  {
    final int day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    calendar.set(Calendar.DAY_OF_MONTH, day);
    setEndOfDay();
    return this;
  }

  /**
   * Sets the date to the beginning of the week (first day of week) and calls setBeginOfDay.
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfWeek()
  {
    calendar.set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
    setBeginOfDay();
    return this;
  }

  private static int getFirstDayOfWeek()
  {
    return ThreadLocalUserContext.getCalendarFirstDayOfWeek();
  }

  /**
   * Checks on day equals first day of week and hour, minute, second and millisecond equals zero.
   */
  public boolean isBeginOfWeek()
  {
    return getDayOfWeek() == getFirstDayOfWeek() && getMilliSecond() == 0 && getSecond() == 0 && getMinute() == 0 && getHourOfDay() == 0;

  }

  /**
   * Sets the date to the ending of the week (last day of week) and calls setEndOfDay.
   * @see #setEndOfDay()
   */
  public DateHolder setEndOfWeek()
  {
    final int firstDayOfWeek = getFirstDayOfWeek();
    short endlessLoopDetection = 0;
    do {
      calendar.add(Calendar.DAY_OF_YEAR, 1);
      if (++endlessLoopDetection > 10) {
        throw new RuntimeException("Endless loop protection. Please contact developer!");
      }
    } while (calendar.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek);
    calendar.add(Calendar.DAY_OF_YEAR, -1); // Get one day before first day of next week.
    setEndOfDay();
    return this;
  }

  /**
   * Sets the hour, minutes and seconds to 0;
   */
  public DateHolder setBeginOfDay()
  {
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return this;
  }

  /**
   * Sets hour=23, minute=59, second=59
   */
  public DateHolder setEndOfDay()
  {
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return this;
  }

  /**
   * Setting the date from the given object (only year, month, day). Hours, minutes, seconds etc. will be preserved.
   * @param src The calendar from which to copy the values.
   * @see #ensurePrecision()
   */
  public DateHolder setDay(final Calendar src)
  {
    calendar.set(Calendar.YEAR, src.get(Calendar.YEAR));
    calendar.set(Calendar.MONTH, src.get(Calendar.MONTH));
    calendar.set(Calendar.DAY_OF_MONTH, src.get(Calendar.DAY_OF_MONTH));
    computeTime();
    return this;
  }

  public Date getDate()
  {
    return calendar.getTime();
  }

  /**
   * Gets the time of day in milliseconds since midnight. This method is used for comparing the times.
   * @return
   */
  public long getTimeOfDay()
  {
    return getHourOfDay() * 3600 + getMinute() * 60 + getSecond();
  }

  public Timestamp getTimestamp()
  {
    return new Timestamp(getDate().getTime());
  }

  public Calendar getCalendar()
  {
    return this.calendar;
  }

  /**
   * Compute time of all fields by calling by calling calendar.getTimeInMillis. Don't forget to call ensurePrecision if needed.
   * @see #ensurePrecision()
   * @see Calendar#getTimeInMillis()
   */
  public DateHolder computeTime()
  {
    calendar.getTimeInMillis();
    return this;
  }

  public int getYear()
  {
    return calendar.get(Calendar.YEAR);
  }

  public int getMonth()
  {
    return calendar.get(Calendar.MONTH);
  }

  /**
   * @return
   * @see DateHelper#getWeekOfYear(Date)
   */
  public int getWeekOfYear()
  {
    return DateHelper.getWeekOfYear(calendar);
  }

  public int getDayOfYear()
  {
    return calendar.get(Calendar.DAY_OF_YEAR);
  }

  public int getDayOfMonth()
  {
    return calendar.get(Calendar.DAY_OF_MONTH);
  }

  public int getDayOfWeek()
  {
    return calendar.get(Calendar.DAY_OF_WEEK);
  }

  /** Gets the hour of day (0-23). */
  public int getHourOfDay()
  {
    return calendar.get(Calendar.HOUR_OF_DAY);
  }

  public DateHolder setMonth(final int month)
  {
    calendar.set(Calendar.MONTH, month);
    return this;
  }

  public DateHolder setDayOfMonth(final int day)
  {
    calendar.set(Calendar.DAY_OF_MONTH, day);
    return this;
  }

  public DateHolder setHourOfDay(final int hour)
  {
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    return this;
  }

  /** Gets the minute (0-59) */
  public int getMinute()
  {
    return calendar.get(Calendar.MINUTE);
  }

  /**
   * Ensures the precision.
   * @param minute
   */
  public DateHolder setMinute(final int minute)
  {
    calendar.set(Calendar.MINUTE, minute);
    ensurePrecision();
    return this;
  }

  public int getSecond()
  {
    return calendar.get(Calendar.SECOND);
  }

  /**
   * Ensures the precision.
   * @param second
   */
  public DateHolder setSecond(final int second)
  {
    calendar.set(Calendar.SECOND, second);
    ensurePrecision();
    return this;
  }

  public int getMilliSecond()
  {
    return calendar.get(Calendar.MILLISECOND);
  }

  /**
   * Ensures the precision.
   * @param milliSecond
   */
  public DateHolder setMilliSecond(final int milliSecond)
  {
    calendar.set(Calendar.MILLISECOND, milliSecond);
    ensurePrecision();
    return this;
  }

  public long getTimeInMillis()
  {
    return calendar.getTimeInMillis();
  }

  /**
   * Stops calculation for more than 500 years.
   * @param other
   * @return other.days - this.days.
   */
  public int daysBetween(final Date other)
  {
    final DateHolder o = new DateHolder(calendar);
    o.setDate(other);
    return daysBetween(o);
  }

  /**
   * @param other
   * @return days between this and given date (other - this).
   */
  public int daysBetween(final DateHolder other)
  {
    final DateHolder from, to;
    if (this.getTimeInMillis() < other.getTimeInMillis()) {
      from = this;
      to = other;
    } else {
      from = other;
      to = this;
    }
    int result = 0;
    final int toYear = to.getYear();
    final DateHolder dh = new DateHolder(from.getDate());

    int endlessLoopProtection = 0;
    while (dh.getYear() < toYear) {
      final int fromDay = dh.getDayOfYear();
      dh.setMonth(Calendar.DECEMBER);
      dh.setDayOfMonth(31);
      result += dh.getDayOfYear() - fromDay + 1;
      dh.add(Calendar.DAY_OF_MONTH, 1);
      if (++endlessLoopProtection > 5000) {
        throw new IllegalArgumentException("Days between doesn's support more than 5000 years");
      }
    }
    result += to.getDayOfYear() - dh.getDayOfYear();
    if (this.getTimeInMillis() < other.getTimeInMillis()) {
      return result;
    } else {
      return -result;
    }
  }

  /**
   * Adds the given number of days.
   * @see Calendar#add(int, int)
   * @param field
   * @param amount
   */
  public DateHolder add(final int field, final int amount)
  {
    calendar.add(field, amount);
    return this;
  }

  /**
   * Adds the given number of days (non-working days will be skipped). Maximum allowed value is 10.000 (for avoiding end-less loops).
   * @param days Value can be positive or negative.
   */
  public DateHolder addWorkingDays(final int days)
  {
    Validate.isTrue(days <= 10000);
    short sign = 1;
    if (days < 0) {
      sign = -1;
    }
    int counter = 0;
    for (int i = 0; i < 10000; i++) {
      if (counter == days) {
        break;
      }
      do {
        calendar.add(Calendar.DAY_OF_MONTH, sign);
      } while (new DayHolder(this).isWorkingDay() == false);
      counter += sign;
    }
    return this;
  }

  @Override
  public String toString()
  {
    return DateHelper.formatAsUTC(getDate()) + ", time zone=" + calendar.getTimeZone().getID() + ", date=" + getDate().toString();
  }

  @Override
  public DateHolder clone()
  {
    final DateHolder res = new DateHolder();
    res.calendar = (Calendar) this.calendar.clone();
    // res.calendar.setTime(this.calendar.getTime());
    res.precision = this.precision;
    return res;
  }

  /**
   * Sets hour, minute, second and millisecond to zero.
   * @param year
   * @param month
   * @param day
   * @see #setDate(int, int, int, int, int, int, int)
   */
  public DateHolder setDate(final int year, final int month, final int day)
  {
    setDate(year, month, day, 0, 0, 0, 0);
    return this;
  }

  /**
   * Sets second and millisecond to zero.
   * @param year
   * @param month
   * @param day
   * @param hourOfDay
   * @param minute
   * @see #setDate(int, int, int, int, int, int, int)
   */
  public DateHolder setDate(final int year, final int month, final int day, final int hourOfDay, final int minute)
  {
    setDate(year, month, day, hourOfDay, minute, 0, 0);
    return this;
  }

  /**
   * Sets the date by giving all datefields and compute all fields. Set millisecond to zero.
   * @param year
   * @param month
   * @param date
   * @param hourOfDay
   * @param minute
   * @param second
   * @see #setDate(int, int, int, int, int, int, int)
   */
  public DateHolder setDate(final int year, final int month, final int date, final int hourOfDay, final int minute, final int second)
  {
    setDate(year, month, date, hourOfDay, minute, second, 0);
    return this;
  }

  /**
   * Sets the date by giving all datefields and compute all fields.
   * @param year
   * @param month
   * @param day
   * @param hour
   * @param minute
   * @param second
   * @param millisecond
   * @see #computeTime()
   * @see #ensurePrecision()
   */
  public DateHolder setDate(final int year, final int month, final int date, final int hourOfDay, final int minute, final int second,
      final int millisecond)
  {
    calendar.set(year, month, date, hourOfDay, minute, second);
    calendar.set(Calendar.MILLISECOND, millisecond);
    computeTime();
    ensurePrecision();
    return this;
  }

  @Override
  public boolean equals(final Object obj)
  {
    if (obj instanceof DateHolder) {
      final DateHolder other = (DateHolder) obj;
      if (other.getTimeInMillis() == getTimeInMillis() && other.getPrecision() == getPrecision()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getTimeInMillis()).append(getPrecision());
    return hcb.toHashCode();
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final DateHolder o)
  {
    return calendar.compareTo(o.calendar);
  }
}
