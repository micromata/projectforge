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

package org.projectforge.framework.time;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Parse and formats dates. Holds a PFDateTime object, which handles all operations. You may use PFDateTime directly.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DateHolder implements Serializable, Cloneable, Comparable<DateHolder> {
  private static final long serialVersionUID = -5373883617915418698L;

  protected PFDateTime dateTime;

  private DatePrecision precision;

  /**
   * Initializes calendar with current date and uses the time zone and the locale of the ContextUser if exists.
   */
  public DateHolder() {
    this.dateTime = PFDateTime.now();
  }

  /**
   * Ensures the precision.
   */
  public DateHolder(final DateHolder dateHolder) {
    this.dateTime = dateHolder.dateTime;
    setPrecision(dateHolder.precision);
  }

  /**
   * Ensures the precision.
   *
   * @param precision
   * @param timeZone
   * @param locale
   */
  public DateHolder(final DatePrecision precision, final TimeZone timeZone, final Locale locale) {
    this.dateTime = PFDateTime.now(asZone(timeZone), locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   *
   * @param precision
   * @param locale
   */
  public DateHolder(final DatePrecision precision, final Locale locale) {
    this.dateTime = PFDateTime.now(ThreadLocalUserContext.getTimeZone().toZoneId(), locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   */
  public DateHolder(final DatePrecision precision) {
    this.dateTime = PFDateTime.now();
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   *
   * @param date      If null, the current date will be used.
   * @param precision
   */
  public DateHolder(final Date date, final DatePrecision precision) {
    this.dateTime = PFDateTime.from(date, true);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   *
   * @param date
   */
  public DateHolder(final Date date, final DatePrecision precision, final Locale locale) {
    this.dateTime = PFDateTime.from(date, true, ThreadLocalUserContext.getTimeZone(), locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   */
  public DateHolder(final Date date, final DatePrecision precision, final TimeZone timeZone, final Locale locale) {
    this.dateTime = PFDateTime.from(date, true, timeZone, locale);
    setPrecision(precision);
  }

  /**
   * Ensures the precision.
   */
  public DateHolder(final Date date) {
    this.dateTime = PFDateTime.from(date, true);
    ensurePrecision();
  }

  /**
   * Initializes calendar with given date and uses the given time zone and the locale of the ContextUser if exists.
   */
  public DateHolder(final Date date, final TimeZone timeZone) {
    this.dateTime = PFDateTime.from(date, true, timeZone);
    ensurePrecision();
  }

  public DateHolder(final Date date, final TimeZone timeZone, final Locale locale) {
    this.dateTime = PFDateTime.from(date, true, timeZone, locale);
    ensurePrecision();
  }

  /**
   * Initializes calendar with current date and uses the given time zone and the locale of the ContextUser if exists.
   */
  public DateHolder(final TimeZone timeZone) {
    this.dateTime = PFDateTime.now(asZone(timeZone));
    ensurePrecision();
  }

  /**
   * Ensures the precision.
   */
  public DateHolder(final Date date, final Locale locale) {
    this.dateTime = PFDateTime.from(date, true, ThreadLocalUserContext.getTimeZone(), locale);
    ensurePrecision();
  }

  public boolean before(final DateHolder date) {
    return this.getDate().before(date.getDate());
  }

  public boolean before(final Date date) {
    return this.getDate().before(date);
  }

  public boolean after(final DateHolder date) {
    return this.getDate().after(date.getDate());
  }

  public boolean after(final Date date) {
    return this.getDate().after(date);
  }

  public boolean isBetween(final Date from, final Date to) {
    final Date date = getDate();
    if (from == null) {
      if (to == null) {
        return false;
      }
      return !date.after(to);
    }
    if (to == null) {
      return !date.before(from);
    }
    return !(date.after(to) || date.before(from));
  }

  public boolean isBetween(final DateHolder from, final DateHolder to) {
    final Date fromDate = from != null ? from.getDate() : null;
    final Date toDate = to != null ? to.getDate() : null;
    return isBetween(fromDate, toDate);
  }

  /**
   * Date will be set. Dependent on precision, also seconds etc. will be set to zero. Ensures the precision.
   */
  public DateHolder setDate(final Date date) {
    if (date == null) {
      return this;
    }
    this.dateTime = PFDateTime.from(date, false, dateTime.getTimeZone(), dateTime.getLocale());
    ensurePrecision();
    return this;
  }

  /**
   * Date will be set. Dependent on precision, also seconds etc. will be set to zero. Ensures the precision.
   *
   * @param millis UTC millis
   */
  public DateHolder setDate(final long millis) {
    this.dateTime = PFDateTime.from(millis, false, dateTime.getZone(), dateTime.getLocale());
    ensurePrecision();
    return this;
  }

  /**
   * Sets the precision of the date represented by this object. Ensures the precision.
   *
   * @param precision SECOND, MINUTE, MINUTE_15, HOUR_OF_DAY or DAY.
   */
  public DateHolder setPrecision(final DatePrecision precision) {
    this.precision = precision;
    ensurePrecision();
    return this;
  }

  /**
   * Ensure the given precision by setting / rounding fields such as minutes and seconds. If precision is MINUTE_15 then rounding the
   * minutes down: 00-14 -&gt; 00; 15-29 -&gt; 15, 30-44 -&gt; 30, 45-59 -&gt; 45.
   */
  public DateHolder ensurePrecision() {
    if (this.precision == null) {
      return this;
    }
    dateTime = dateTime.withPrecision(this.precision);
    return this;
  }

  public DatePrecision getPrecision() {
    return precision;
  }

  /**
   * Considers the time zone of the user, for example, if date is 20.11.1970 23:00:00 UTC but the user's locale is Europe/Berlin then the
   * java.sql.Date should be 21.11.1970! <br/>
   * This methods transforms first the day into UTC and then into java.sql.Date.
   */

  public java.sql.Date getSQLDate() {
    return this.dateTime.getSqlDate();
  }

  /**
   * Has the given date the same day? The given date will be converted into a calendar (clone from this) with same time zone.
   *
   * @param date
   * @return
   */
  public boolean isSameDay(final Date date) {
    final PFDateTime other = PFDateTime.from(date, false, this.dateTime.getTimeZone(), this.dateTime.getLocale());
    return this.dateTime.isSameDay(other);
  }

  /**
   * Has the given date the same day? The given date will be converted into a calendar (clone from this) with same time zone.
   *
   * @param date
   * @return
   */
  public boolean isSameDay(final DateHolder date) {
    return this.dateTime.isSameDay(date.dateTime);
  }

  /**
   * Sets the date to the beginning of the year (first day of year) and calls setBeginOfDay.
   *
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfYear() {
    this.dateTime = this.dateTime.getBeginOfYear();
    return this;
  }

  /**
   * Sets the date to the beginning of the year (first day of year) and calls setBeginOfDay.
   *
   * @see #setBeginOfDay()
   */
  public DateHolder setEndOfYear() {
    this.dateTime = this.dateTime.getEndOfYear();
    return this;
  }

  /**
   * Sets the date to the beginning of the month (first day of month) and calls setBeginOfDay.
   *
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfMonth() {
    this.dateTime = this.dateTime.getBeginOfMonth();
    return this;
  }

  public DateHolder setEndOfMonth() {
    this.dateTime = this.dateTime.getEndOfMonth();
    return this;
  }

  /**
   * Sets the date to the beginning of the week (first day of week) and calls setBeginOfDay.
   *
   * @see #setBeginOfDay()
   */
  public DateHolder setBeginOfWeek() {
    this.dateTime = this.dateTime.getBeginOfWeek();
    return this;
  }

  private static DayOfWeek getFirstDayOfWeek() {
    return ThreadLocalUserContext.getFirstDayOfWeek();
  }

  /**
   * Checks on day equals first day of week and hour, minute, second and millisecond equals zero.
   */
  public boolean isBeginOfWeek() {
    return this.dateTime.isBeginOfWeek();
  }

  /**
   * Sets the date to the ending of the week (last day of week) and calls setEndOfDay.
   *
   * @see #setEndOfDay()
   */
  public DateHolder setEndOfWeek() {
    this.dateTime = this.dateTime.getEndOfWeek();
    return this;
  }

  /**
   * Sets the hour, minutes and seconds to 0;
   */
  public DateHolder setBeginOfDay() {
    this.dateTime = this.dateTime.getBeginOfDay();
    return this;
  }

  /**
   * Sets hour=23, minute=59, second=59
   */
  public DateHolder setEndOfDay() {
    this.dateTime = this.dateTime.getEndOfDay();
    return this;
  }

  /**
   * Setting the date from the given object (only year, month, day). Hours, minutes, seconds etc. will be preserved.
   *
   * @param src The calendar from which to copy the values.
   * @see #ensurePrecision()
   */
  public DateHolder setDay(final DateHolder src) {
    this.dateTime = this.dateTime.withYear(src.getYear()).withMonth(src.getMonth()).withDayOfMonth(src.getDayOfMonth());
    return this;
  }

  public Date getDate() {
    return this.dateTime.getUtilDate();
  }

  /**
   * Gets the time of day in milliseconds since midnight. This method is used for comparing the times.
   *
   * @return
   */
  public long getTimeOfDay() {
    return getHourOfDay() * 3600 + getMinute() * 60 + getSecond();
  }

  public Timestamp getTimestamp() {
    return new Timestamp(getDate().getTime());
  }

  public int getYear() {
    return this.dateTime.getYear();
  }

  /**
   * @return 0 - January, 11 - December (0-based)
   */
  public Month getMonth() {
    return this.dateTime.getMonth();
  }

  /**
   * @return
   * @see DateHelper#getWeekOfYear(Date)
   */
  public int getWeekOfYear() {
    return this.dateTime.getWeekOfYear();
  }

  public int getDayOfYear() {
    return this.dateTime.getDayOfYear();
  }

  public int getDayOfMonth() {
    return this.dateTime.getDayOfMonth();
  }

  public DayOfWeek getDayOfWeek() {
    return this.dateTime.getDayOfWeek();
  }

  /**
   * Gets the hour of day (0-23).
   */
  public int getHourOfDay() {
    return this.dateTime.getHour();
  }

  public DateHolder setMonth(final Month month) {
    this.dateTime = this.dateTime.withMonth(month);
    return this;
  }

  public DateHolder setDayOfMonth(final int day) {
    this.dateTime = this.dateTime.withDayOfMonth(day);
    return this;
  }

  public DateHolder setHourOfDay(final int hour) {
    this.dateTime = this.dateTime.withHour(hour);
    return this;
  }

  /**
   * Gets the minute (0-59)
   */
  public int getMinute() {
    return this.dateTime.getMinute();
  }

  /**
   * Ensures the precision.
   *
   * @param minute
   */
  public DateHolder setMinute(final int minute) {
    this.dateTime = this.dateTime.withMinute(minute);
    ensurePrecision();
    return this;
  }

  public int getSecond() {
    return this.dateTime.getSecond();
  }

  /**
   * Ensures the precision.
   *
   * @param second
   */
  public DateHolder setSecond(final int second) {
    this.dateTime = this.dateTime.withSecond(second);
    ensurePrecision();
    return this;
  }

  /**
   * @return Milli seconds inside second (0..9999).
   */
  public int getMilliSecond() {
    return this.dateTime.getMilliSecond();
  }

  /**
   * Ensures the precision.
   *
   * @param milliSecond
   */
  public DateHolder setMilliSecond(final int milliSecond) {
    this.dateTime = this.dateTime.withMilliSecond(milliSecond);
    ensurePrecision();
    return this;
  }

  public long getTimeInMillis() {
    return this.dateTime.getEpochMilli();
  }

  /**
   * @param other
   * @return other.days - this.days.
   */
  public long daysBetween(final Date other) {
    PFDateTime otherDay = PFDateTime.from(other, false, this.dateTime.getTimeZone(), this.dateTime.getLocale());
    return this.dateTime.daysBetween(otherDay);
  }

  /**
   * @param other
   * @return days between this and given date (other - this).
   */
  public long daysBetween(final DateHolder other) {
    return this.dateTime.daysBetween(other.dateTime);
  }

  /**
   * Adds the given number of units.
   *
   * @param field
   * @param amount
   */
  public DateHolder add(final int field, final int amount) {
    this.dateTime = this.dateTime.plus(amount, PFDateCompabilityUtils.getCompabilityFields(field));
    return this;
  }

  /**
   * Adds the given number of days (non-working days will be skipped). Maximum allowed value is 10.000 (for avoiding end-less loops).
   *
   * @param days Value can be positive or negative.
   */
  public DateHolder addWorkingDays(final int days) {
    Validate.isTrue(days <= 10000);
    this.dateTime = PFDayUtils.addWorkingDays(this.dateTime, days);
    return this;
  }

  @Override
  public String toString() {
    return DateHelper.formatAsUTC(getDate()) + ", time zone=" + dateTime.getZone() + ", date=" + this.dateTime;
  }

  @Override
  public DateHolder clone() {
    return new DateHolder(this);
  }

  /**
   * Sets hour, minute, second and millisecond to zero.
   *
   * @param year
   * @param month
   * @param day
   */
  public DateHolder setDate(final int year, final Month month, final int day) {
    setDate(year, month, day, 0, 0, 0, 0);
    return this;
  }

  /**
   * Sets second and millisecond to zero.
   *
   * @param year
   * @param month
   * @param day
   * @param hourOfDay
   * @param minute
   */
  public DateHolder setDate(final int year, final Month month, final int day, final int hourOfDay, final int minute) {
    setDate(year, month, day, hourOfDay, minute, 0, 0);
    return this;
  }

  /**
   * Sets the date by giving all datefields and compute all fields. Set millisecond to zero.
   *
   * @param year
   * @param month
   * @param date
   * @param hourOfDay
   * @param minute
   * @param second
   */
  public DateHolder setDate(final int year, final Month month, final int date, final int hourOfDay, final int minute, final int second) {
    setDate(year, month, date, hourOfDay, minute, second, 0);
    return this;
  }

  /**
   * Sets the date by giving all datefields and compute all fields.
   *
   * @param year
   * @param month
   * @param minute
   * @param second
   * @param millisecond
   * @see #ensurePrecision()
   */
  public DateHolder setDate(final int year, final Month month, final int date, final int hourOfDay, final int minute, final int second,
                            final int millisecond) {
    dateTime = PFDateTime.withDate(year, month, date, hourOfDay, minute, second, millisecond, this.dateTime.getZone(), this.dateTime.getLocale());
    ensurePrecision();
    return this;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof DateHolder) {
      final DateHolder other = (DateHolder) obj;
      if (other.getTimeInMillis() == getTimeInMillis() && other.getPrecision() == getPrecision()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getTimeInMillis()).append(getPrecision());
    return hcb.toHashCode();
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final DateHolder o) {
    return this.dateTime.compareTo(o.dateTime);
  }

  /**
   * @param timeZone might be null.
   * @return Given timeZone as {@link ZoneId} or null, if timeZone is null.
   */
  public static ZoneId asZone(TimeZone timeZone) {
    if (timeZone != null)
      return timeZone.toZoneId();
    return null;
  }
}
