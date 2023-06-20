package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.poll.PollResponseDao
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.mail.MailAttachment
import org.projectforge.rest.poll.excel.ExcelExport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

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

    /**
     * Cron job for daily stuff
     */
    @Scheduled(cron = "0 0 1 * * *") // 1am everyday
    fun dailyCronJobs() {
        cronDeletePolls()
        cronEndPolls()
    }

    /**
     * Method to end polls after deadline
     */
    private fun cronEndPolls() {
        val pollDOs = pollDao.internalLoadAll()
        // set State.FINISHED for all old polls and export excel
        pollDOs.forEach { pollDO ->
            if (pollDO.deadline?.isBefore(LocalDate.now()) == true) {
                pollDO.state = PollDO.State.FINISHED

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
                // add all attendees mails
                val mailTo: ArrayList<String> = ArrayList(poll.attendees?.map { it.email }?.mapNotNull { it } ?: emptyList())
                val mailFrom = pollDO.owner?.email.toString()
                val mailSubject = translateMsg("poll.mail.ended.subject")
                val mailContent = translateMsg("poll.mail.ended.content", pollDO.title, pollDO.owner?.displayName)

                pollDao.internalSaveOrUpdate(pollDO)
                pollMailService.sendMail(mailFrom, mailTo, mailContent, mailSubject, listOf(mailAttachment))
            }
        }

        val pollsInFuture = pollDOs.filter { it.deadline?.isAfter(LocalDate.now()) ?: false }
        pollsInFuture.forEach { pollDO ->
            val poll = Poll()
            poll.copyFrom(pollDO)
            val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), pollDO.deadline)
            if (daysDifference == 1L || daysDifference == 7L) {
                // add all attendees mails
                val mailTo: ArrayList<String> = ArrayList(poll.attendees?.map { it.email }?.mapNotNull { it } ?: emptyList())
                val mailFrom = pollDO.owner?.email.toString()
                val mailSubject = translateMsg("poll.mail.endingSoon.subject", daysDifference)
                val mailContent = translateMsg(
                    "poll.mail.endingSoon.content",
                    pollDO.title,
                    pollDO.owner?.displayName,
                    pollDO.deadline?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString(),
                    "https://projectforge.micromata.de/react/response/dynamic/${pollDO.id}"
                )
                pollMailService.sendMail(mailFrom, mailTo, mailSubject, mailContent)
            }
        }
    }

    /**
     * Method to delete old polls
     */
    private fun cronDeletePolls() {
        val polls = pollDao.internalLoadAll()
        val pollsMoreThanOneYearPast = polls.filter {
            it.deadline!!.isBefore(LocalDate.now().minusYears(1))
        }
        pollsMoreThanOneYearPast.forEach { poll ->
            val pollResponses = pollResponseDao.internalLoadAll().filter { response ->
                response.poll!!.id == poll.id
            }
            pollResponses.forEach {
                pollResponseDao.internalMarkAsDeleted(it)
            }
            pollDao.internalMarkAsDeleted(poll)
        }
    }
}