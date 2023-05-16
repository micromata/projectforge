package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.mail.MailAttachment
import org.projectforge.rest.poll.excel.ExcelExport
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.collections.ArrayList
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RestController
class CronJobs {

    private val log: Logger = LoggerFactory.getLogger(CronJobs::class.java)

    @Autowired
    private lateinit var pollDao: PollDao

    @Autowired
    private lateinit var pollMailService: PollMailService

    @Autowired
    private lateinit var exporter: ExcelExport

    /**
     * Cron job for daily stuff
     */
//    @Scheduled(cron = "0 0 1 * * *") // 1am everyday
    @Scheduled(cron = "0 * * * * *") // 1am everyday
    fun dailyCronJobs() {
        cronDeletePolls()
        cronEndPolls()
    }


    /**
     * Method to end polls after deadline
     */
    private fun cronEndPolls() {
        var mailContent = ""
        var mailHeader = ""
        val mailAttachments = ArrayList<MailAttachment>()
        val mailTo = "l.spohr@micromata.de"

        val polls = pollDao.internalLoadAll()
        // set State.FINISHED for all old polls and export excel
        polls.forEach {
            if (it.deadline?.isBefore(LocalDate.now()) == true) {
                it.state = PollDO.State.FINISHED

                val poll = Poll()
                poll.copyFrom(it)

                val excel = exporter.getExcel(poll)

                val mailAttachment = object : MailAttachment {
                    override fun getFilename(): String {
                        return "${it.title}_${LocalDateTime.now().year}_Result.xlsx"
                    }

                    override fun getContent(): ByteArray? {
                        return excel
                    }
                }
                mailAttachments.add(mailAttachment)

                mailHeader = translateMsg("poll.mail.ended.header")
                mailContent = translateMsg(
                    "poll.mail.ended.content", it.title, it.owner?.displayName
                )

                pollDao.internalSaveOrUpdate(it)

                sendMail(mailTo, mailContent, mailHeader, mailAttachments)
            }
        }

        val pollsInFuture = polls.filter { it.deadline?.isAfter(LocalDate.now()) ?: false }
        pollsInFuture.forEach {
            val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), it.deadline)
            if (daysDifference == 1L || daysDifference == 7L) {
                mailHeader = translateMsg("poll.mail.endingSoon.header", daysDifference)
                mailContent = translateMsg(
                    "poll.mail.endingSoon.content",
                    it.title,
                    it.owner?.displayName,
                    it.deadline.toString(),
                    "https://projectforge.micromata.de/react/response/dynamic/${it.id}"
                )
            }
        }
    }

    private fun sendMail(to: String, subject: String, content: String, mailAttachments: List<MailAttachment>? = null) {
        try {
            if (content.isNotEmpty()) {
                pollMailService.sendMail(subject = subject, content = content, mailAttachments = mailAttachments, to = to)
            }
        } catch (e: Exception) {
            log.error(e.toString())
        }
    }

    /**
     * Method to delete old polls
     */
    private fun cronDeletePolls() {
        // check if poll end in future
        val polls = pollDao.internalLoadAll()
        val pollsMoreThanOneYearPast = polls.filter {
            it.created?.before(
                Date.from(
                    LocalDate.now().minusYears(1).atStartOfDay(
                        ZoneId.systemDefault()
                    ).toInstant()
                )
            ) ?: false
        }
        pollsMoreThanOneYearPast.forEach {
            pollDao.delete(it)
        }
    }

}