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

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class Holidays
{
  private static final Logger log = LoggerFactory.getLogger(Holidays.class);

  private static final Holidays instance = new Holidays();

  public static Holidays getInstance()
  {
    return instance;
  }

  private Holidays() {}

  /** Contains all holidays of a year. Key is the year. Value is a map of all holidays in the year with the day of the year as key. */
  private Map<Integer, Map<Integer, Holiday>> holidaysByYear = new HashMap<>();

  private Map<HolidayDefinition, ConfigureHoliday> reconfiguredHolidays = new HashMap<>();

  private ConfigXml xmlConfiguration;

  private Map<Integer, Holiday> computeHolidays(int year)
  {
    log.info("Compute holidays for year: " + year);
    final Map<Integer, Holiday> holidays = new HashMap<>();
    for (final HolidayDefinition holiday : HolidayDefinition.values()) {
      if (holiday.getEasterOffset() == null) {
        putHoliday(holidays, year, holiday);
      }
    }

    int g = year % 19; // "Golden Number" of year - 1
    int i; // # of days from 3/21 to the Paschal full moon
    int j; // Weekday (0-based) of Paschal full moon

    // We're past the Gregorian switchover, so use the Gregorian rules.
    int c = year / 100;
    int h = (c - c / 4 - (8 * c + 13) / 25 + 19 * g + 15) % 30;
    i = h - (h / 28) * (1 - (h / 28) * (29 / (h + 1)) * ((21 - g) / 11));
    j = (year + year / 4 + i + 2 - c + c / 4) % 7;
    /*
      Use otherwise the old Julian rules (not really yet needed ;-) i = (19*g + 15) % 30; j = (year + year/4 + i) % 7; }
     */
    int l = i - j;
    int m = 3 + (l + 40) / 44; // 1-based month in which Easter falls
    int d = l + 28 - 31 * (m / 4); // Date of Easter within that month

    PFDay day = PFDay.withDate(year, m, d);
    for (final HolidayDefinition holiday : HolidayDefinition.values()) {
      if (holiday.getEasterOffset() != null) {
        putEasterHoliday(holidays, day, holiday);
      }
    }
    if (xmlConfiguration.getHolidays() != null) {
      for (final ConfigureHoliday cfgHoliday : xmlConfiguration.getHolidays()) {
        if (cfgHoliday.getId() == null && !cfgHoliday.isIgnore()) {
          final Month month = PFDayUtils.getMonth(cfgHoliday.getMonth());
          // New Holiday.
          if (month == null || cfgHoliday.getDayOfMonth() == null || StringUtils.isBlank(cfgHoliday.getLabel())) {
            log.error("Holiday not full configured (month, dayOfMonth, label, ...) missed: " + cfgHoliday.toString());
            break;
          }
          if (cfgHoliday.getYear() != null && cfgHoliday.getYear() != year) {
            // Holiday affects not the current year.
            continue;
          }
          day = day.withMonth(month).withDayOfMonth(cfgHoliday.getDayOfMonth());
          final Holiday holiday = new Holiday(null, cfgHoliday.getLabel(), cfgHoliday.isWorkingDay(), cfgHoliday.getWorkFraction());
          putHoliday(holidays, day.getDayOfYear(), holiday);
          log.info("Configured holiday added: " + holiday);
        }
      }
    }
    return holidays;
  }

  private void putHoliday(final Map<Integer, Holiday> holidays, int year, final HolidayDefinition def)
  {
    if (def.getEasterOffset() != null) {
      return;
    }
    PFDay day = PFDay.withDate(year, def.getMonth(), def.getDayOfMonth());
    final Holiday holiday = createHoliday(def);
    if (holiday != null) {
      putHoliday(holidays, day.getDayOfYear(), holiday);
    }
  }

  private void putEasterHoliday(final Map<Integer, Holiday> holidays, final PFDay day, final HolidayDefinition def)
  {
    if (def.getEasterOffset() != null) {
      final Holiday holiday = createHoliday(def);
      if (holiday != null) {
        putHoliday(holidays, day.getDayOfYear() + def.getEasterOffset(), holiday);
      }
    }
  }

  private Holiday createHoliday(final HolidayDefinition def)
  {
    String i18nKey = def.getI18nKey();
    String label = null;
    BigDecimal workingFraction = null;
    boolean isWorkingDay = def.isWorkingDay();
    if (reconfiguredHolidays.containsKey(def)) {
      final ConfigureHoliday cfgHoliday = reconfiguredHolidays.get(def);
      if (cfgHoliday.isIgnore()) {
        // Ignore holiday.
        return null;
      }
      if (StringUtils.isNotBlank(cfgHoliday.getLabel())) {
        i18nKey = null;
        label = cfgHoliday.getLabel();
      }
      if (cfgHoliday.getWorkFraction() != null) {
        workingFraction = cfgHoliday.getWorkFraction();
      }
    }
    return new Holiday(i18nKey, label, isWorkingDay, workingFraction);
  }

  private void putHoliday(final Map<Integer, Holiday> holidays, final int dayOfYear, final Holiday holiday)
  {
    if (holidays.containsKey(dayOfYear)) {
      log.warn("Holiday does already exist (may-be use ignore in config.xml?): "
          + holidays.get(dayOfYear)
          + "! Overwriting it by new one: "
          + holiday);
    }
    holidays.put(dayOfYear, holiday);
  }

  private synchronized Map<Integer, Holiday> getHolidays(int year)
  {
    if (xmlConfiguration == null) {
      xmlConfiguration = ConfigXml.getInstance();
      if (xmlConfiguration.getHolidays() != null) {
        for (final ConfigureHoliday holiday : xmlConfiguration.getHolidays()) {
          if (holiday.getId() != null) {
            reconfiguredHolidays.put(holiday.getId(), holiday);
          }
        }
        holidaysByYear.clear();
      }
    }
    Map<Integer, Holiday> holidays = holidaysByYear.get(year);
    if (holidays == null) {
      holidays = computeHolidays(year);
      holidaysByYear.put(year, holidays);
    }
    return holidays;
  }

  public boolean isHoliday(IPFDate date)
  {
    return isHoliday(date.getYear(), date.getDayOfYear());
  }

  public boolean isHoliday(int year, int dayOfYear)
  {
    return getHolidays(year).containsKey(dayOfYear);
  }

  public boolean isWorkingDay(final DayHolder date)
  {
    if (date.isWeekend()) {
      return false;
    }
    final Holiday day = getHolidays(date.getYear()).get(date.getDayOfYear());
    return day == null || day.isWorkingDay();
  }

  public boolean isWorkingDay(final ZonedDateTime dateTime)
  {
    if (dateTime.getDayOfWeek() == DayOfWeek.SATURDAY || dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
      // Weeekend
      return false;
    }
    final Holiday day = getHolidays(dateTime.getYear()).get(dateTime.getDayOfYear());
    return day == null || day.isWorkingDay();
  }

  public boolean isWorkingDay(final IPFDate date)
  {
    if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
      // Weeekend
      return false;
    }
    final Holiday day = getHolidays(date.getYear()).get(date.getDayOfYear());
    return day == null || day.isWorkingDay();
  }

  public BigDecimal getWorkFraction(final DayHolder date)
  {
    if (date.isWeekend()) {
      return null;
    }
    final Holiday day = getHolidays(date.getYear()).get(date.getDayOfYear());
    if (day == null) {
      return null;
    }
    return day.getWorkFraction();
  }

  public BigDecimal getWorkFraction(final IPFDate date)
  {
    if (date.isWeekend()) {
      return null;
    }
    final Holiday day = getHolidays(date.getYear()).get(date.getDayOfYear());
    if (day == null) {
      return null;
    }
    return day.getWorkFraction();
  }

  public String getHolidayInfo(IPFDate date)
  {
    return getHolidayInfo(date.getYear(), date.getDayOfYear());
  }


  public String getHolidayInfo(int year, int dayOfYear)
  {
    final Holiday day = getHolidays(year).get(dayOfYear);
    if (day == null) {
      return "";
    }
    return StringUtils.isNotBlank(day.getLabel()) ? day.getLabel() : day.getI18nKey();
  }
}
