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

package org.projectforge.rest.poll

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerRun
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.persistence.api.QueryFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * The daily work of the polls: ends the running polls whose deadline has passed (and mails it), reminds the
 * attendees who haven't answered yet 7 days and 1 day before the deadline, and deletes the polls a year after their
 * deadline. Templates are left alone.
 */
@Service
class PollCronJobs {
    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var pollEntityRest: PollEntityRest

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::dailyCronJobs)
    }

    /** Daily at 06:05 (server time). */
    @Scheduled(cron = CRON)
    fun dailyCronJobs() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
            run(run, LocalDate.now())
        }
    }

    internal fun run(run: SchedulerRun?, today: LocalDate) {
        val polls = pollDao.select(QueryFilter().also { it.add(QueryFilter.eq("template", false)) }, checkAccess = false)
            .filter { !it.deleted }
        polls.filter { it.state == PollDO.State.RUNNING && it.deadline?.isBefore(today) == true }.forEach { poll ->
            try {
                pollEntityRest.finishAndMail(poll)
            } catch (ex: Exception) {
                log.error(ex) { "Finishing poll ${poll.id} failed: ${ex.message}" }
                run?.fail("Finishing poll ${poll.id} failed: ${ex.message}", ex)
            }
        }
        polls.filter { it.state == PollDO.State.RUNNING }.forEach { poll ->
            val days = ChronoUnit.DAYS.between(today, poll.deadline ?: return@forEach)
            if (days == 1L || days == 7L) {
                val answered = pollResponseDao.getResponses(poll.id!!).mapNotNull { it.owner?.id }.toSet()
                val missing = pollDao.getAttendeeIds(poll) - answered
                pollMailService.sendReminder(poll, missing, days)
                log.info { "Reminded ${missing.size} attendees of poll ${poll.id}." }
            }
        }
        polls.filter { it.deadline?.isBefore(today.minusYears(1)) == true }.forEach { poll ->
            pollResponseDao.getResponses(poll.id!!).forEach { pollResponseDao.markAsDeleted(it, checkAccess = false) }
            pollDao.markAsDeleted(poll, checkAccess = false)
            log.info { "Deleted poll ${poll.id}, its deadline is more than a year ago." }
        }
    }

    companion object {
        private const val CRON = "0 5 6 * * *"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "poll.daily", SchedulerJobArea.BUSINESS, PollCronJobs::class.java, "dailyCronJobs",
            SchedulerSchedule.Cron(CRON),
        )
    }
}
