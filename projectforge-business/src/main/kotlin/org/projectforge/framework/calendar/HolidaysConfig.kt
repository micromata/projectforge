/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.core.type.TypeReference
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.json.JsonUtils
import java.math.BigDecimal
import java.time.YearMonth

private val log = KotlinLogging.logger {}

/**
 * The (re-)definitions of holidays, stored as JSON array of [ConfigureHoliday] in the configuration parameter
 * `ConfigurationParam.HOLIDAYS`, e.g.
 * `[{"label":"Erster Mai","month":5,"dayOfMonth":1}, {"id":"XMAS_EVE","workingDay":true,"workFraction":0.5},
 * {"id":"SHROVE_TUESDAY","ignore":true}]`.
 */
object HolidaysConfig {
    val typeReference = object : TypeReference<List<ConfigureHoliday>>() {}

    /** The current JSON value (configuration parameter `holidays`). */
    val json: String?
        get() = ConfigurationJsonValues.get(ConfigurationParam.HOLIDAYS)

    /**
     * The translated errors of the given holidays (configuration page): an own holiday needs label, month and
     * day of month; a pre-defined one may be configured once only; a work fraction is between 0 and 1.
     */
    fun validate(holidays: List<ConfigureHoliday>): List<String> {
        val errors = mutableListOf<String>()
        val ids = mutableSetOf<HolidayDefinition>()
        holidays.forEachIndexed { index, holiday ->
            val name = holiday.label?.trim()?.ifEmpty { null } ?: holiday.id?.name ?: "${index + 1}"
            val id = holiday.id
            if (id != null) {
                if (!ids.add(id)) {
                    errors.add(translateMsg("administration.configuration.holidays.error.duplicate", name))
                }
            } else {
                if (holiday.label.isNullOrBlank()) {
                    errors.add(translateMsg("administration.configuration.holidays.error.label", index + 1))
                }
                val month = holiday.month
                val day = holiday.dayOfMonth
                // A leap year, so the 29th of February is accepted.
                if (month == null || month !in 1..12 || day == null || day < 1 ||
                    day > YearMonth.of(holiday.year ?: 2024, month).lengthOfMonth()
                ) {
                    errors.add(translateMsg("administration.configuration.holidays.error.date", name))
                }
                holiday.year?.let { year ->
                    if (year !in 1900..2999) {
                        errors.add(translateMsg("administration.configuration.holidays.error.year", name))
                    }
                }
            }
            holiday.workFraction?.let { fraction ->
                if (fraction < BigDecimal.ZERO || fraction > BigDecimal.ONE) {
                    errors.add(translateMsg("administration.configuration.holidays.error.workFraction", name))
                }
            }
        }
        return errors
    }

    /** The holidays of the given JSON value, none if it is blank or not readable. */
    fun parse(json: String?): List<ConfigureHoliday> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            JsonUtils.fromJson(json, typeReference, failOnUnknownProps = false) ?: emptyList()
        } catch (ex: Exception) {
            log.error { "Holidays: configuration (holidays) not readable, using the pre-defined holidays only: ${ex.message}" }
            emptyList()
        }
    }
}
