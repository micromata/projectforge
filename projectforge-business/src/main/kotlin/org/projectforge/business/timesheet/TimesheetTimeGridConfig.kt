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

package org.projectforge.business.timesheet

import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.PFDateTime
import java.time.temporal.ChronoUnit

/**
 * The grid of the times of time sheets in minutes, chosen by the user (see [TimesheetTimeGridService]) (e.g. 15: the timer of the app
 * and the web pages and the presets of new time sheets round to full quarter hours). It is a rounding default only: the
 * fields "from" and "to" and the server ([TimesheetDao]) still accept any multiple of 5 minutes, so manual changes,
 * existing entries, imports and plugins keep working.
 */
object TimesheetTimeGridConfig {
    const val DEFAULT = 15

    /** The grids to choose from: divisors of 60 that are multiples of 5. */
    val ALLOWED = listOf(5, 10, 15, 20, 30)

    /** [minutes] if allowed, otherwise [DEFAULT] (also for null: the user has not chosen one). */
    fun parse(minutes: Int?): Int = if (minutes != null && minutes in ALLOWED) minutes else DEFAULT

    /** Rounds [dateTime] to the nearest full step of the grid of [minutes] (seconds are dropped). */
    fun roundToGrid(dateTime: PFDateTime, minutes: Int): PFDateTime {
        val minute = dateTime.withPrecision(DatePrecision.MINUTE).minute
        val rounded = ((minute + minutes / 2) / minutes) * minutes
        return dateTime.withPrecision(DatePrecision.MINUTE).withMinute(0).plus(rounded.toLong(), ChronoUnit.MINUTES)
    }
}
