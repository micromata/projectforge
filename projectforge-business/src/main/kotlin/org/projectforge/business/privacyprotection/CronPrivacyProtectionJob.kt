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

package org.projectforge.business.privacyprotection

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Daily job for deleting entities due to privacy protection.
 *
 * @author Kai Reinhard
 */
@Service
class CronPrivacyProtectionJob {
  private var jobs = mutableListOf<IPrivacyProtectionJob>()

  @Autowired
  private lateinit var schedulerJobRunner: SchedulerJobRunner

  @PostConstruct
  private fun postConstruct() {
    schedulerJobRunner.register(SCHEDULER_JOB, ::execute)
  }

  fun register(job: IPrivacyProtectionJob) {
    synchronized(jobs) {
      log.info { "Registering job ${job::class.java}." }
      jobs.add(job)
    }
  }

  /**
   * Starting nightly at 4 a.m.
   * second, minute, hour, day of month, month, day of week
   */
  //@Scheduled(cron = "0 0 4 * * *")
  @Scheduled(cron = CRON)
  fun execute() {
    schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
      val list = synchronized(jobs) { jobs.toList() }
      list.forEach {
        try {
          it.execute()
        } catch (ex: Exception) {
          log.error(ex) { "Error while executing job '${it::class.java.name}: ${ex.message}" }
          run.fail("Job '${it::class.java.simpleName}' failed: ${ex.message}", ex)
        }
      }
    }
  }

  companion object {
    private const val CRON = "\${projectforge.privacyProtection.cronDaily}"

    val SCHEDULER_JOB = SchedulerJobDefinition(
      "privacyProtection.daily", SchedulerJobArea.MAINTENANCE, CronPrivacyProtectionJob::class.java, "execute",
      SchedulerSchedule.Cron(CRON),
    )
  }
}
