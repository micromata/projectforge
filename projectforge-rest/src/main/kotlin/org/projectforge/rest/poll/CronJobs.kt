package org.projectforge.rest.poll

import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollDao
import org.projectforge.business.user.UserDao
import org.projectforge.mail.MailAttachment
import org.projectforge.rest.poll.Exel.ExcelExport
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
    private lateinit var userDao: UserDao


    /**
     * Cron job for daily stuff
     */
//    @Scheduled(cron = "0 0 1 * * *") // 1am everyday
    @Scheduled(cron = "0 * * * * *") // Every Minute
    fun dailyCronJobs() {
        cronDeletePolls()
        cronEndPolls()
    }

    /**
     * Method to end polls after deadline
     */
    fun cronEndPolls() {
        var mail = "";
        var header = "";

        val polls = pollDao.internalLoadAll()
        val list = ArrayList<MailAttachment>()
        // set State.FINISHED for all old polls
        polls.forEach {
            if (it.deadline?.isBefore(LocalDate.now().minusDays(1)) == true) {
                it.state = PollDO.State.FINISHED
                // check if state is open or closed

                val ihkExporter = ExcelExport()

                val poll = Poll()
                poll.copyFrom(it)

                val exel = ihkExporter
                    .getExcel(poll)

                val attachment = object : MailAttachment {
                    override fun getFilename(): String {
                        return it.title + "_" + LocalDateTime.now().year + "_Result" + ".xlsx"
                    }

                    override fun getContent(): ByteArray? {
                        return exel
                    }
                }
                list.add(attachment)

                header = "Umfrage ist abgelaufen"
                mail = """
                        Die Umfrage ist zu ende. Hier die ergebnisse.
                     """.trimMargin()

                pollDao.internalSaveOrUpdate(it)
            }

        }

        try {

            // erstell mir eine funktion, die alles deadlines mir gibt die in der zukunft liegen
            val pollsInFuture = polls.filter { it.deadline?.isAfter(LocalDate.now()) ?: false }
            pollsInFuture.forEach {
                val daysDifference = ChronoUnit.DAYS.between(LocalDate.now(), it.deadline)
                if (daysDifference == 1L || daysDifference == 7L) {
                    header = "Umfrage Endet in $daysDifference Tage"
                    mail = """
                    Sehr geehrter Teilnehmer,wir laden Sie herzlich dazu ein, an unserer Umfrage zum Thema ${it.title} teilzunehmen. 
                    Ihre Meinung ist uns sehr wichtig und wir würden uns freuen, wenn Sie uns dabei helfen könnten,
                    unsere Forschungsergebnisse zu verbessern. Für diese Umfrage ist ${
                        it ///owner
                    }zuständig.
                    Bei Fragen oder Anmerkungen können Sie sich gerne an ihn wenden.
                    Bitte beachten Sie, dass das Enddatum für die Teilnahme an dieser Umfrage der ${it.deadline.toString()} ist.
                    Wir würden uns freuen, wenn Sie sich die Zeit nehmen könnten, um diese Umfrage auszufüllen.
                    Vielen Dank im Voraus für Ihre Unterstützung.
                    
                    Mit freundlichen Grüßen,${
                        it  ///owmer
                    }
                     """.trimMargin()
                }
            }


            if (mail.isNotEmpty()) {
                pollMailService.sendMail(to = "test", subject = header, content = mail, mailAttachments = list)
            }
        } catch (e: Exception) {
            log.error(e.toString())
        }
    }


    /**
     * Method to delete old polls
     */
    private fun cronDeletePolls() {
        println("CRON JOB NOW!")
        // check if poll end in future
        val polls = pollDao.internalLoadAll()
        val pollsMoreThanOneYearPast = polls.filter {
            it.deadline?.isBefore(LocalDate.from(LocalDate.now().minusYears(1))) == true
        }

        pollsMoreThanOneYearPast.forEach { poll ->
            val pollDto = Poll()
            pollDto.copyFrom(poll)
            pollDto.fullAccessUsers?.forEach { user ->
                if (user.email != null) {
                    pollMailService.sendPollDeletedMail(user.email!!, pollDto)
                }
            }

            pollDao.internalMarkAsDeleted(poll)
        }
    }
}