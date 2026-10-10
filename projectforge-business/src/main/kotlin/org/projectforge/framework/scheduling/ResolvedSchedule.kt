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

package org.projectforge.framework.scheduling

import org.projectforge.common.scheduling.SchedulerSchedule
import org.springframework.scheduling.support.CronExpression
import java.time.Instant
import java.time.ZoneId

/**
 * The schedule of a job with its placeholders resolved. The cron expressions are evaluated in the system time zone,
 * as by Spring (`@Scheduled` without zone).
 */
sealed class ResolvedSchedule {
    /** The next run after the given time, or null, if not known. */
    abstract fun nextAfter(millis: Long): Long?

    /** The time between two runs around the given one, or null, if not known. */
    abstract fun periodAround(millis: Long): Long?

    class Cron(val expression: String, private val cron: CronExpression, private val zone: ZoneId) : ResolvedSchedule() {
        override fun nextAfter(millis: Long): Long? =
            cron.next(Instant.ofEpochMilli(millis).atZone(zone))?.toInstant()?.toEpochMilli()

        override fun periodAround(millis: Long): Long? {
            val next = nextAfter(millis) ?: return null
            val afterNext = nextAfter(next) ?: return null
            return afterNext - next
        }
    }

    class FixedDelay(val delayMillis: Long, val initialDelayMillis: Long) : ResolvedSchedule() {
        override fun nextAfter(millis: Long): Long = millis + delayMillis
        override fun periodAround(millis: Long): Long = delayMillis
    }

    /** Cron expression `-`: never run by the scheduler. */
    object Disabled : ResolvedSchedule() {
        override fun nextAfter(millis: Long): Long? = null
        override fun periodAround(millis: Long): Long? = null
    }

    class Invalid(val value: String, val error: String) : ResolvedSchedule() {
        override fun nextAfter(millis: Long): Long? = null
        override fun periodAround(millis: Long): Long? = null
    }

    companion object {
        /**
         * @param resolver Resolves the placeholders (`${...}`), e.g. `Environment::resolveRequiredPlaceholders`.
         */
        fun resolve(
            schedule: SchedulerSchedule,
            resolver: (String) -> String,
            zone: ZoneId = ZoneId.systemDefault(),
        ): ResolvedSchedule {
            return when (schedule) {
                is SchedulerSchedule.Cron -> {
                    val expression = runCatching { resolver(schedule.expression).trim() }
                        .getOrElse { return Invalid(schedule.expression, it.message ?: it.javaClass.simpleName) }
                    if (expression == "-") {
                        Disabled
                    } else {
                        runCatching { Cron(expression, CronExpression.parse(expression), zone) }
                            .getOrElse { Invalid(expression, it.message ?: it.javaClass.simpleName) }
                    }
                }

                is SchedulerSchedule.FixedDelay -> runCatching {
                    FixedDelay(
                        resolver(schedule.delay).trim().toLong(),
                        schedule.initialDelay?.let { resolver(it).trim().toLong() } ?: 0L,
                    )
                }.getOrElse { Invalid(schedule.delay, it.message ?: it.javaClass.simpleName) }
            }
        }
    }
}
