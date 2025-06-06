/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.ical

import net.fortuna.ical4j.model.property.RRule
import org.projectforge.framework.time.DateParser
import java.time.temporal.Temporal

/**
 */
object RRuleUtils {
    @JvmStatic
    fun <T : Temporal> getRecurUntil(rRule: RRule<T>): T? {
        return rRule.getRecur()?.getUntil()
    }

    fun parseExcludeDates(datesAsCsv: String?): List<Temporal>? {
        datesAsCsv ?: return null
        val dateStrings = datesAsCsv.removePrefix("EXDATE:").split(",", ";", "|")
        if (dateStrings.isEmpty()) {
            return null
        }
        val exDates = mutableListOf<Temporal>()
        dateStrings.forEach { dateString ->
            val date = DateParser.parse(dateString)
            date?.let { exDates.add(it) }
        }
        return exDates;
    }

    fun isEventExcluded(eventDate: Temporal, exDates: List<Temporal>?): Boolean {
        exDates ?: return false
        return exDates.any { exDate ->
            ICalDateUtils.isSameDay(eventDate, exDate)
        }
    }
}
