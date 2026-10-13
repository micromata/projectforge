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
 * Runs the scheduled jobs: measures and records each run (success, failure, duration), prevents overlapping runs of
 * the same job and catches and logs any exception. The base of the scheduler tab of the system dashboard.
 *
 * Usage, in the class of the `@Scheduled` method:
 * ```
 * @PostConstruct
 * private fun registerSchedulerJob() {
 *     schedulerJobRunner.register(SCHEDULER_JOB, ::execute) { if (!configured) "not configured" else null }
 * }
 *
 * @Scheduled(cron = CRON)
 * fun execute() = schedulerJobRunner.runAsync(SCHEDULER_JOB) { run -> ... }
 * ```
 * The implementation is the Spring bean `SchedulerJobRegistry` (projectforge-business).
 */
interface SchedulerJobRunner {
    /**
     * Registers the job, so it is listed even before its first run.
     * @param entry The `@Scheduled` method itself, called by "run now" of the dashboard.
     * @param activeCheck If given and it returns a reason, the job is inactive: its runs are skipped (not counted).
     */
    fun register(definition: SchedulerJobDefinition, entry: Runnable, activeCheck: SchedulerActiveCheck? = null)

    /** Runs the job in a new thread and returns immediately. A run is skipped while the previous one is running. */
    fun runAsync(definition: SchedulerJobDefinition, body: SchedulerJobBody)

    /** Runs the job in the calling thread (the scheduler's pool). Never throws. */
    fun run(definition: SchedulerJobDefinition, body: SchedulerJobBody)
}

/** The body of a job. An exception thrown marks the run as failed (and is logged). */
fun interface SchedulerJobBody {
    fun run(run: SchedulerRun)
}

/** Returns the reason, why the job is inactive (e.g. "plugin not activated"), or null, if it's active. */
fun interface SchedulerActiveCheck {
    fun inactiveReason(): String?
}

/** The current run of a job. */
interface SchedulerRun {
    val trigger: SchedulerTrigger

    /**
     * Marks the run as failed without logging anything: for errors the job catches and logs itself (e.g. with its
     * own log event) and for results considered as failure (e.g. a sanity check found errors).
     */
    fun fail(message: String, throwable: Throwable? = null)

    /** An info shown in the recent runs of the job (e.g. "holiday, nothing sent"). */
    fun note(text: String)
}
