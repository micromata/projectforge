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
        Thread {
            cronDeletePolls()
            cronEndPolls()
        }.start()
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

                        val mailAttachment = MailAttachment("${pollDO.title}_${LocalDateTime.now().year}_Result.xlsx", excel)
                        val owner = userService.getUser(poll.owner?.id)
                        // Only send to Full Access Users and Attendees who haven't responded yet
                        val mailTo = pollMailService.getFilteredEmails(poll, pollDO.id!!)
                        val mailFrom = pollDO.owner?.email.toString()

                        // Group recipients by locale and send localized emails
                        val recipientsByLocale = pollMailService.groupRecipientsByLocale(mailTo)
                        recipientsByLocale.forEach { (locale, recipients) ->
                            val mailSubject = translateMsg(locale, "poll.mail.endedafterdeadline.subject", poll.title)
                            val mailContent = translateMsg(locale, "poll.mail.endedafterdeadline.content", pollDO.title, owner?.displayName)
                            
                            pollMailService.sendMail(mailFrom, recipients, mailSubject, mailContent, listOf(mailAttachment))
                            log.info("Sent end-of-poll mail for poll (${pollDO.id}) to ${recipients.size} users in locale $locale")
                        }
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
                // Only send to Full Access Users and Attendees who haven't responded yet
                val mailTo = pollMailService.getFilteredEmails(poll, pollDO.id!!)

                if (mailTo.isNotEmpty()) {
                    val mailFrom = pollDO.owner?.email.toString()
                    
                    // Group recipients by locale and send localized emails
                    val recipientsByLocale = pollMailService.groupRecipientsByLocale(mailTo)
                    recipientsByLocale.forEach { (locale, recipients) ->
                        // Create HTML link for placeholder {3}
                        val pollLink = "<p><a href=\"https://projectforge.micromata.de/react/pollResponse/dynamic/?pollId=${pollDO.id}\">" +
                                translateMsg(locale, "poll.mail.link.text") + "</a></p>"
                        
                        // Use custom reminder texts if available, otherwise use default i18n keys
                        // Placeholders: {0}=title, {1}=owner, {2}=deadline, {3}=link
                        val mailSubject = if (!pollDO.customReminderSubject.isNullOrEmpty()) {
                            translateMsg(
                                locale,
                                pollDO.customReminderSubject!!,
                                pollDO.title,
                                pollDO.owner?.displayName,
                                pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                                pollLink
                            )
                        } else {
                            translateMsg(
                                locale,
                                "poll.mail.endingSoon.subject.default",
                                pollDO.title,
                                pollDO.owner?.displayName,
                                pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                                pollLink
                            )
                        }
                        
                        val mailContent = if (!pollDO.customReminderContent.isNullOrEmpty()) {
                            translateMsg(
                                locale,
                                pollDO.customReminderContent!!,
                                pollDO.title,
                                pollDO.owner?.displayName,
                                pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                                pollLink
                            )
                        } else {
                            translateMsg(
                                locale,
                                "poll.mail.endingSoon.content.default",
                                pollDO.title,
                                pollDO.owner?.displayName,
                                pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                                pollLink
                            )
                        }
                        
                        pollMailService.sendMail(mailFrom, recipients, mailSubject, mailContent)
                        log.info("Sent reminder mail for poll (${pollDO.id}) to ${recipients.size} users in locale $locale (Full Access + not responded yet)")
                    }
                } else {
                    log.info("No reminder mail sent for poll (${pollDO.id}) - all users have already responded")
                }
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
