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

package org.projectforge.common.scheduling

/**
 * A scheduled job (a `@Scheduled` method) as shown on the system dashboard (tab scheduler): its runs are tracked by
 * the [SchedulerJobRunner]. The title and description are the i18n keys [titleKey] and [descriptionKey].
 *
 * @param id Unique and stable (the statistics are stored by it), e.g. `cron.nightly`.
 * @param owner The class of the `@Scheduled` method (checked on start-up, see `SchedulerJobRegistry`).
 * @param method The name of the `@Scheduled` method.
 * @param schedule The same values as given in the annotation.
 * @param slowThresholdMillis A run taking longer is slow (stored and shown). Default: derived from the average.
 */
class SchedulerJobDefinition @JvmOverloads constructor(
    val id: String,
    val area: SchedulerJobArea,
    val owner: Class<*>,
    val method: String,
    val schedule: SchedulerSchedule,
    val slowThresholdMillis: Long? = null,
) {
    val titleKey: String
        get() = "system.scheduler.job.$id.title"

    val descriptionKey: String
        get() = "system.scheduler.job.$id.description"

    override fun toString(): String = id
}

/** The area of a job, for grouping and filtering (i18n `system.scheduler.area.<name in lower case>`). */
enum class SchedulerJobArea {
    SYSTEM, MAINTENANCE, BACKUP, INTEGRATION, BUSINESS, FILES;

    val i18nKey: String
        get() = "system.scheduler.area.${name.lowercase()}"
}

/**
 * The schedule of a job, as given in its `@Scheduled` annotation, placeholders (`${...}`) included: they are
 * resolved by the runner. Best given by a constant used by both, the annotation and the definition.
 */
sealed class SchedulerSchedule {
    /** A cron expression (Spring format, second first), `-` for disabled. */
    class Cron(val expression: String) : SchedulerSchedule()

    /** A fixed delay in millis between the end of a run and the start of the next one. */
    class FixedDelay(val delay: String, val initialDelay: String? = null) : SchedulerSchedule() {
        @JvmOverloads
        constructor(delayMillis: Long, initialDelayMillis: Long? = null) :
                this(delayMillis.toString(), initialDelayMillis?.toString())
    }
}

/** Who started a run. */
enum class SchedulerTrigger { SCHEDULED, MANUAL }
