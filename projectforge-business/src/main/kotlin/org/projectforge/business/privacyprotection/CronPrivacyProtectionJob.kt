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

package org.projectforge.business.privacyprotection

import mu.KotlinLogging
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
  @Scheduled(cron = "\${projectforge.privacyProtection.cronDaily}")
  fun execute() {
    log.info("Daily privacy protection job started.")
    synchronized(jobs) {
      jobs.forEach {
        try {
          it.execute()
        } catch (ex: Exception) {
          log.error("Error while executing job '${it::class.java.name}: ${ex.message}", ex)
        }
      }
    }
    log.info("Daily privacy protection job finished.")
  }
}
