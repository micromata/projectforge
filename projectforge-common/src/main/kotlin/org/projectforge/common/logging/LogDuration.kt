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

package org.projectforge.common.logging

import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatMillis

/**
 * Measures the time since the constructor call. The duration is formatted ([formatMillis]).
 * For logging purposes.
 * Usage:
 *  val logDuration = LogDuration()
 *  // do something
 *  log.info("Duration: $logDuration")
 */
class LogDuration() {
    enum class TimeUnit { STANDARD, SECONDS }

    val start = System.currentTimeMillis()

    /**
     * Duration in formatted milliseconds since constructor call.
     * @see formatMillis
     */
    val duration: String
        get() = (System.currentTimeMillis() - start).formatMillis()


    /**
     * Duration in milliseconds since constructor call.
     * @see formatMillis
     */
    override fun toString(): String {
        return toString(TimeUnit.STANDARD)
    }

    /**
     * Duration since constructor call.
     * @param timeUnit The time unit to use for formatting. Default is [TimeUnit.STANDARD]. If [TimeUnit.SECONDS] is used, the duration is formatted in seconds, e.g. 17.3 s.
     * @see formatMillis
     */
    fun toString(timeUnit: TimeUnit): String {
        return if (timeUnit == TimeUnit.SECONDS) toSeconds(2) else duration
    }

    /**
     * Duration since constructor call in seconds, e.g. "17.3s".
     * @param scale The number of decimal places to use for formatting. Default is 1.
     */
    fun toSeconds(scale: Int = 1): String {
        val end = System.currentTimeMillis()
        val seconds = (end - start).toDouble() / 1000
        return "${seconds.format(scale = scale)}s"
    }
}
