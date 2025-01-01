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

package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.mail.MailAttachment
import org.projectforge.rest.poll.excel.ExcelExport
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RestController
class PollCronJobs {

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollResponseDao: PollResponseDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var exporter: ExcelExport

    @Autowired
    private lateinit var userService: UserService

    private val log: Logger = LoggerFactory.getLogger(PollCronJobs::class.java)

    /**
     * Cron job for daily stuff
     */

    @Scheduled(cron = "0 5 6 * * *") //Immer um 00:05
    fun dailyCronJobs() {
        log.info("Start daily cron jobs")
        cronDeletePolls()
        cronEndPolls()
    }


    /**
     * Method to end polls after deadline
     */
    private fun cronEndPolls() {
        val pollDOs = pollDao.selectAllNotDeleted(checkAccess = false)
        // set State.FINISHED for all old polls and export excel
        pollDOs.forEach { pollDO ->
            // try to send mail until successfully changed to FINISHED_AND_MAIL_SENT
            if (pollDO.state != PollDO.State.FINISHED_AND_MAIL_SENT) {
                if (pollDO.deadline?.isBefore(LocalDate.now()) == true) {
                    pollDO.state = PollDO.State.FINISHED

                    try {
                        val poll = Poll()
                        poll.copyFrom(pollDO)

                        val excel = exporter.getExcel(poll)

                        val mailAttachment = object : MailAttachment {
                            override fun getFilename(): String {
                                return "${pollDO.title}_${LocalDateTime.now().year}_Result.xlsx"
                            }

                            override fun getContent(): ByteArray? {
                                return excel
                            }
                        }
                        val owner = userService.getUser(poll.owner?.id)
                        val mailTo = pollMailService.getAllAttendeesEmails(poll)
                        val mailFrom = pollDO.owner?.email.toString()
                        val mailSubject = translateMsg("poll.mail.endedafterdeadline.subject", poll.title)
                        val mailContent = translateMsg("poll.mail.endedafterdeadline.content", pollDO.title, owner?.displayName, )

                        pollMailService.sendMail(mailFrom, mailTo, mailContent, mailSubject, listOf(mailAttachment))
                        pollDO.state = PollDO.State.FINISHED_AND_MAIL_SENT
                        log.info("Set state of poll (${pollDO.id}) ${pollDO.title} to FINISHED_AND_MAIL_SENT")
                        pollDao.insertOrUpdate(pollDO, checkAccess = false)
                    } catch (e: Exception) {
                        log.error(e.message, e)
                    }
                }
            }
        }

        val pollsInFuture = pollDOs.filter { it.deadline?.isAfter(LocalDate.now()) ?: false }
        pollsInFuture.forEach { pollDO ->
            val poll = Poll()
            poll.copyFrom(pollDO)
            val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), pollDO.deadline)
            if (daysDifference == 1L || daysDifference == 7L) {
                // add all attendees mails
                val mailTo = pollMailService.getAllAttendeesEmails(poll)
                val mailFrom = pollDO.owner?.email.toString()
                val mailSubject = translateMsg("poll.mail.endingSoon.subject", daysDifference)
                val mailContent = translateMsg(
                    "poll.mail.endingSoon.content",
                    pollDO.title,
                    pollDO.owner?.displayName,
                    pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                    pollDO.id.toString(),
                )
                pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)
            }
        }
    }

    /**
     * Method to delete old polls
     */
    private fun cronDeletePolls() {
        val polls = pollDao.selectAll(checkAccess = false)
        val pollsMoreThanOneYearPast = polls.filter {
            it.deadline?.isBefore(LocalDate.now().minusYears(1)) == true
        }
        pollsMoreThanOneYearPast.forEach { poll ->
            val pollResponses = pollResponseDao.selectAll(checkAccess = false).filter { response ->
                response.poll?.id == poll.id
            }
            pollResponses.forEach {
                pollResponseDao.markAsDeleted(it, checkAccess = false)
            }
            pollDao.markAsDeleted(poll, checkAccess = false)
        }
    }
}
