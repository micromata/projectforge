/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.calendar.Holidays;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.*;

/**
 * Please use PFDate instead.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DayHolder {
  private static final long serialVersionUID = 2646871164508930568L;

  private PFDay date;

  /**
   * I18n keys of the day names (e. g. needed for I18n).
   */
  public static final String DAY_KEYS[] = new String[]{"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};

  private transient Holidays holidays = Holidays.getInstance();

  private Map<String, Object> objects;

  /**
   * Only set, if day is holiday.
   */
  private String holidayInfo = null;

  private boolean marker = false;

  public static String getDayKey(final DayOfWeek dayOfWeek) {
    return DAY_KEYS[dayOfWeek.ordinal()];
  }

  public static BigDecimal getNumberOfWorkingDays(final Date from, final Date to) {
    Validate.notNull(from);
    Validate.notNull(to);
    final DayHolder fromDay = new DayHolder(from);
    final DayHolder toDay = new DayHolder(to);
    return getNumberOfWorkingDays(fromDay, toDay);
  }

  public static BigDecimal getNumberOfWorkingDays(final DayHolder from, final DayHolder to) {
    return PFDayUtils.getNumberOfWorkingDays(from.date, to.date);
  }

  /**
   * Initializes with current day (with time zone UTC!).
   */
  public DayHolder() {
    this.date = PFDay.now();
  }

  /**
   * @param date
   */
  public DayHolder(final Date date) {
    this.date = PFDay.from(date, true);
  }

  public DayHolder(final Date date, final TimeZone timeZone) {
    this.date = PFDay.from(date, true, timeZone);
  }

  public DayHolder(final DayHolder dateHolder) {
    this.date = dateHolder.date;
  }

  public int getYear() {
    return this.date.getYear();
  }

  public Month getMonth() {
    return this.date.getMonth();
  }

  public int getDayOfYear() {
    return this.date.getDayOfYear();
  }

  public int getDayOfMonth() {
    return this.date.getDayOfMonth();
  }

  public boolean before(final DayHolder date) {
    return this.date.isBefore(date.date);
  }

  public boolean after(final DayHolder date) {
    return this.date.isAfter(date.date);
  }

  public boolean isBetween(final Date from, final Date to) {
    final Date utilDate = date.getUtilDate();
    if (from == null) {
      if (to == null) {
        return false;
      }
      return !utilDate.after(to);
    }
    if (to == null) {
      return !utilDate.before(from);
    }
    return !(utilDate.after(to) || utilDate.before(from));
  }

  /**
   * @param other
   * @return other.days - this.days.
   */
  public long daysBetween(final Date other) {
    PFDay otherDay = PFDay.from(other);
    return this.date.daysBetween(otherDay);
  }

  /**
   * @param other
   * @return other.days - this.days.
   */
  public long daysBetween(final DayHolder other) {
    return this.date.daysBetween(other.date);
  }

  /**
   * Multipurpose marker, e. g. used by select date for marking days as days not from the current month.
   *
   * @return
   */
  public boolean isMarker() {
    return marker;
  }

  public DayHolder setMarker(final boolean marker) {
    this.marker = marker;
    return this;
  }

  public String getDayKey() {
    return getDayKey(getDayOfWeek());
  }

  public boolean isToday() {
    return date.isSameDay(PFDay.now());
  }

  public boolean isSameDay(Date other) {
    return date.isSameDay(PFDay.from(other));
  }

  public boolean isSameDay(DayHolder other) {
    return date.isSameDay(other.date);
  }

  public boolean isSunday() {
    return DayOfWeek.SUNDAY == getDayOfWeek();
  }

  public boolean isWeekend() {
    final DayOfWeek dayOfWeek = getDayOfWeek();
    return DayOfWeek.SUNDAY == dayOfWeek || DayOfWeek.SATURDAY == dayOfWeek;
  }

  public boolean isHoliday() {
    return holidays.isHoliday(date);
  }

  public boolean isSunOrHoliday() {
    return isSunday() || isHoliday();
  }

  public boolean isWorkingDay() {
    return holidays.isWorkingDay(this.date);
  }

  /**
   * Weekend days have always no work fraction!
   */
  public BigDecimal getWorkFraction() {
    return holidays.getWorkFraction(this.date);
  }

  public String getHolidayInfo() {
    if (holidayInfo == null) {
      holidayInfo = holidays.getHolidayInfo(date);
    }
    return holidayInfo;
  }

  public DayHolder setHolidays(final Holidays holidays) {
    this.holidays = holidays;
    return this;
  }

  public DayHolder setBeginOfWeek() {
    this.date = this.date.getBeginOfWeek();
    return this;
  }

  public DayHolder setEndOfWeek() {
    this.date = this.date.getEndOfWeek();
    return this;
  }

  /**
   * Does not set end of day as DateHolder.
   */
  public DayHolder setEndOfMonth() {
    this.date = this.date.getEndOfMonth();
    return this;
  }

  public DayHolder setDate(int year, Month month, int dayOfMonth) {
    this.date = PFDay.withDate(year, month, dayOfMonth);
    return this;
  }

  /**
   * Adds the given number of units.
   *
   * @param field
   * @param amount
   */
  public DayHolder add(final int field, final int amount) {
    this.date = this.date.plus(amount, PFDateCompabilityUtils.getCompabilityFields(field));
    return this;
  }

  /**
   * Adds the given number of days (non-working days will be skipped). Maximum allowed value is 10.000 (for avoiding end-less loops).
   *
   * @param days Value can be positive or negative.
   */
  public DayHolder addWorkingDays(final int days) {
    Validate.isTrue(days <= 10000);
    this.date = PFDayUtils.addWorkingDays(this.date, days);
    return this;
  }

  @Override
  public String toString() {
    return isoFormat();
  }

  public String isoFormat() {
    return date.getIsoString();
  }

  /**
   * For storing additional objects to a day. This is used by the date selector for showing the user's timesheets at this day.
   *
   * @param key
   * @param value
   */
  public DayHolder addObject(final String key, final Object value) {
    if (this.objects == null) {
      this.objects = new HashMap<>();
    }
    this.objects.put(key, value);
    return this;
  }

  /**
   * Used for getting e. g. the user time sheets at this day for showing the calendar in ical like format.
   *
   * @return the stored objects to this day or null, if not exist.
   */
  public Object getObject(final String key) {
    if (this.objects == null) {
      return null;
    }
    return this.objects.get(key);
  }

  public Map<String, Object> getObjects() {
    return this.objects;
  }

  public DayOfWeek getDayOfWeek() {
    return this.date.getDayOfWeek();
  }

  public java.util.Date getUtilDate() {
    return date.getUtilDate();
  }

  public java.sql.Date getSqlDate() {
    return date.getSqlDate();
  }

  @Override
  public DayHolder clone() {
    return new DayHolder(this);
  }
}
