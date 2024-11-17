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

package org.projectforge.business.teamcal.event

import org.projectforge.framework.time.RecurrenceFrequency
import java.io.Serializable
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TeamEventRecurrenceData(
    /**
     * @return the timeZone
     */
    val timeZone: TimeZone?
) : Serializable {
    /**
     * @return the frequency
     */
    var frequency: RecurrenceFrequency? = RecurrenceFrequency.NONE
    var modeOneMonth: RecurrenceFrequencyModeOne? = RecurrenceFrequencyModeOne.FIRST
    var modeOneYear: RecurrenceFrequencyModeOne? = RecurrenceFrequencyModeOne.FIRST

    var modeTwoMonth: RecurrenceFrequencyModeTwo? = RecurrenceFrequencyModeTwo.MONDAY
    var modeTwoYear: RecurrenceFrequencyModeTwo? = RecurrenceFrequencyModeTwo.MONDAY

    var isCustomized: Boolean = false
        get() {
            if (field || interval > 1) return true
            else return false
        }

    var isYearMode: Boolean = false
    var monthMode: RecurrenceMonthMode? = RecurrenceMonthMode.NONE

    var weekdays = BooleanArray(7)
    var monthdays = BooleanArray(31)
    var months = BooleanArray(12)

    /**
     * @return the until, contains the possible last occurrence of an event
     */
    var until: Date? = null
    var untilDays: Int = 0

    /**
     * @return the interval
     */
    var interval: Int = 1

    companion object {
        private val serialVersionUID = -6258614682123676951L
    }
}
