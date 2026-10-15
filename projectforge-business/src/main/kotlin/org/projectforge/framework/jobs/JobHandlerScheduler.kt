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

package org.projectforge.framework.jobs

import jakarta.annotation.PostConstruct
import org.projectforge.Constants
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobHandlerScheduler {
  @Autowired
  private lateinit var jobHandler: JobHandler

  @Autowired
  private lateinit var schedulerJobRunner: SchedulerJobRunner

  @PostConstruct
  private fun postConstruct() {
    schedulerJobRunner.register(SCHEDULER_JOB, ::execute)
  }

  // Runs every minute
  @Scheduled(fixedDelay = Constants.MILLIS_PER_MINUTE, initialDelay = Constants.MILLIS_PER_MINUTE)
  fun execute() {
    schedulerJobRunner.runAsync(SCHEDULER_JOB) { jobHandler.tidyUp() }
  }

  companion object {
    val SCHEDULER_JOB = SchedulerJobDefinition(
      "jobs.tidyUp", SchedulerJobArea.SYSTEM, JobHandlerScheduler::class.java, "execute",
      SchedulerSchedule.FixedDelay(Constants.MILLIS_PER_MINUTE, Constants.MILLIS_PER_MINUTE),
    )
  }
}
