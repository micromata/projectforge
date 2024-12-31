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

package org.projectforge.business.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.projectforge.common.extensions.formatMillis
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Job should be scheduled nightly.
 * Lot of sanity checks will be done and a mail is sent to the administrator, if something is wrong.
 *
 * @author Kai Reinhard
 */
@Component
class CronSanityCheckJob {
    private val jobs = mutableListOf<SanityCheckJob>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Scheduled(cron = "\${projectforge.cron.sanityChecks}")
    fun execute() {
        log.info("Cronjob for executing sanity checks started...")

        coroutineScope.launch {
            val start = System.currentTimeMillis()
            try {
                val contextList = executeNow()
            } finally {
                log.info("Cronjob for executing sanity checks finished after ${(System.currentTimeMillis() - start).formatMillis()}")
            }
        }
    }

    fun executeNow(): SanityCheckContext {
        val context = SanityCheckContext()
        jobs.forEach { job ->
            val jobContext = context.add(job)
            try {
                log.info("Executing sanity check job: ${job::class.simpleName}")
                job.execute(jobContext)
                log.info("Execution of sanity check job done: ${job::class.simpleName}")
            } catch (ex: Throwable) {
                log.error("While executing sanity job ${job::class.simpleName}: " + ex.message, ex)
            }
        }
        return context
    }

    fun registerJob(job: SanityCheckJob) {
        log.info { "Registering sanity check job: ${job::class.simpleName}" }
        jobs.add(job)
    }
}
