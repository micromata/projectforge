/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.calendar

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.calendar.HolidayDefinition
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.time.IPFDate
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDay.Companion.withDate
import org.projectforge.framework.time.PFDayUtils.Companion.getMonth
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class Holidays private constructor() {
    /**
     * Contains all holidays of a year. Key is the year. Value is a map of all holidays in the year with the day of the year as key.
     */
    private val holidaysByYear: MutableMap<Int, Map<Int, Holiday?>?> = HashMap()
    private val reconfiguredHolidays: MutableMap<HolidayDefinition, ConfigureHoliday?> = HashMap()
    private var xmlConfiguration: ConfigXml? = null
    private fun computeHolidays(year: Int): Map<Int, Holiday?> {
        log.info("Compute holidays for year: $year")
        val holidays: MutableMap<Int, Holiday?> = HashMap()
        for (holiday in HolidayDefinition.values()) {
            if (holiday.easterOffset == null) {
                putHoliday(holidays, year, holiday)
            }
        }
        val g = year % 19 // "Golden Number" of year - 1
        val i: Int // # of days from 3/21 to the Paschal full moon
        val j: Int // Weekday (0-based) of Paschal full moon
        // We're past the Gregorian switchover, so use the Gregorian rules.
        val c = year / 100
        val h = (c - c / 4 - (8 * c + 13) / 25 + 19 * g + 15) % 30
        i = h - h / 28 * (1 - h / 28 * (29 / (h + 1)) * ((21 - g) / 11))
        j = (year + year / 4 + i + 2 - c + c / 4) % 7
        /*
      Use otherwise the old Julian rules (not really yet needed ;-) i = (19*g + 15) % 30; j = (year + year/4 + i) % 7; }
     */
        val l = i - j
        val m = 3 + (l + 40) / 44 // 1-based month in which Easter falls
        val d = l + 28 - 31 * (m / 4) // Date of Easter within that month
        var day = withDate(year, m, d)
        for (holiday in HolidayDefinition.values()) {
            if (holiday.easterOffset != null) {
                putEasterHoliday(holidays, day, holiday)
            }
        }
        if (xmlConfiguration!!.holidays != null) {
            for (cfgHoliday in xmlConfiguration!!.holidays) {
                if (cfgHoliday.id == null && !cfgHoliday.isIgnore) {
                    val month = getMonth(cfgHoliday.month)
                    // New Holiday.
                    if (month == null || cfgHoliday.dayOfMonth == null || StringUtils.isBlank(cfgHoliday.label)) {
                        log.error("Holiday not full configured (month, dayOfMonth, label, ...) missed: $cfgHoliday")
                        break
                    }
                    if (cfgHoliday.year != null && cfgHoliday.year != year) { // Holiday affects not the current year.
                        continue
                    }
                    day = day.withMonth(month).withDayOfMonth(cfgHoliday.dayOfMonth)
                    val holiday = Holiday(null, cfgHoliday.label, cfgHoliday.isWorkingDay, cfgHoliday.workFraction)
                    putHoliday(holidays, day.dayOfYear, holiday)
                    log.info("Configured holiday added: $holiday")
                }
            }
        }
        return holidays
    }

    private fun putHoliday(holidays: MutableMap<Int, Holiday?>, year: Int, def: HolidayDefinition) {
        if (def.easterOffset != null) {
            return
        }
        val day = withDate(year, def.month, def.dayOfMonth)
        val holiday = createHoliday(def)
        if (holiday != null) {
            putHoliday(holidays, day.dayOfYear, holiday)
        }
    }

    private fun putEasterHoliday(holidays: MutableMap<Int, Holiday?>, day: PFDay, def: HolidayDefinition) {
        if (def.easterOffset != null) {
            val holiday = createHoliday(def)
            if (holiday != null) {
                putHoliday(holidays, day.dayOfYear + def.easterOffset, holiday)
            }
        }
    }

    private fun createHoliday(def: HolidayDefinition): Holiday? {
        var i18nKey = def.i18nKey
        var label: String? = null
        var workingFraction: BigDecimal? = null
        val isWorkingDay = def.isWorkingDay
        if (reconfiguredHolidays.containsKey(def)) {
            val cfgHoliday = reconfiguredHolidays[def]
            if (cfgHoliday!!.isIgnore) { // Ignore holiday.
                return null
            }
            if (StringUtils.isNotBlank(cfgHoliday.label)) {
                i18nKey = null
                label = cfgHoliday.label
            }
            if (cfgHoliday.workFraction != null) {
                workingFraction = cfgHoliday.workFraction
            }
        }
        return Holiday(i18nKey, label, isWorkingDay, workingFraction)
    }

    private fun putHoliday(holidays: MutableMap<Int, Holiday?>, dayOfYear: Int, holiday: Holiday) {
        if (holidays.containsKey(dayOfYear)) {
            log.warn("Holiday does already exist (may-be use ignore in config.xml?): "
                    + holidays[dayOfYear]
                    + "! Overwriting it by new one: "
                    + holiday)
        }
        holidays[dayOfYear] = holiday
    }

    @Synchronized
    private fun getHolidays(year: Int): Map<Int, Holiday?>? {
        if (xmlConfiguration == null) {
            xmlConfiguration = ConfigXml.getInstance()
            xmlConfiguration!!.let { xmlConfiguration ->
                for (holiday in xmlConfiguration.holidays) {
                    if (holiday.id != null) {
                        reconfiguredHolidays[holiday.id] = holiday
                    }
                }
                holidaysByYear.clear()
            }
        }
        var holidays = holidaysByYear[year]
        if (holidays == null) {
            holidays = computeHolidays(year)
            holidaysByYear[year] = holidays
        }
        return holidays
    }

    fun isHoliday(date: IPFDate<*>): Boolean {
        return isHoliday(date.year, date.dayOfYear)
    }

    fun isHoliday(year: Int, dayOfYear: Int): Boolean {
        return getHolidays(year)!!.containsKey(dayOfYear)
    }

    fun isWorkingDay(dateTime: ZonedDateTime): Boolean {
        return isWorkingDay(dateTime.year, dateTime.dayOfYear, dateTime.dayOfWeek)
    }

    fun isWorkingDay(date: IPFDate<*>): Boolean {
        return isWorkingDay(date.year, date.dayOfYear, date.dayOfWeek)
    }

    fun isWorkingDay(date: LocalDate): Boolean {
        return isWorkingDay(date.year, date.dayOfYear, date.dayOfWeek)
    }

    private fun isWorkingDay(year: Int, dayOfYear: Int, dayOfWeek: DayOfWeek): Boolean {
        if (WEEKEND_DAYS.contains(dayOfWeek)) {
            return false
        }
        return getHolidays(year)?.get(dayOfYear)?.isWorkingDay ?: true
    }

    fun getWorkFraction(date: IPFDate<*>): BigDecimal? {
        if (date.isWeekend()) {
            return null
        }
        val day = getHolidays(date.year)!![date.dayOfYear] ?: return null
        return day.workFraction
    }

    fun getHolidayInfo(date: IPFDate<*>): String {
        return getHolidayInfo(date.year, date.dayOfYear)
    }

    fun getHolidayInfo(year: Int, dayOfYear: Int): String {
        val day = getHolidays(year)!![dayOfYear] ?: return ""
        return if (StringUtils.isNotBlank(day.label)) day.label else day.i18nKey
    }

    companion object {
        private val log = LoggerFactory.getLogger(Holidays::class.java)
        @JvmStatic
        val instance = Holidays()

        val WEEKEND_DAYS = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}
