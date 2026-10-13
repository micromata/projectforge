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

package org.projectforge.business.jobs

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.task.TaskDao
import org.projectforge.common.html.Html
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.time.DateHelper
import org.projectforge.jcr.JCRCheckSanityCheckJob
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext
import org.projectforge.jobs.JobListExecutionContext
import org.projectforge.mail.Mail
import org.projectforge.mail.MailAttachment
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Job should be scheduled nightly.
 * Lot of sanity checks will be done and a mail is sent to the administrator, if something is wrong.
 *
 * @author Kai Reinhard
 */
@Component
class CronSanityCheckJob {
    private val jobs = mutableListOf<AbstractJob>()

    @Autowired
    private lateinit var jcrCheckSanityJob: JCRCheckSanityCheckJob

    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::cron)
        registerJob(SystemSanityCheckJob(taskDao))
        registerJob(jcrCheckSanityJob) // JCRCheckSanityJob is a plugin job, and it is registered here, because CronSanityCheckJob is not known by JCR.
    }

    // For testing: @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
    @Scheduled(cron = CRON)
    fun cron() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
            val contextList = execute()
            if (contextList.status == JobExecutionContext.Status.ERRORS) {
                run.fail("Sanity checks found errors (see the mail to the administrators or the system integrity page).")
                val recipients = Configuration.instance.getStringValue(ConfigurationParam.SYSTEM_ADMIN_E_MAIL)
                if (!recipients.isNullOrBlank()) {
                    val msg = Mail()
                    msg.addTo(recipients)
                    msg.setProjectForgeSubject("Errors occurred on sanity check job.")
                    val intro = Html.Alert(Html.Alert.Type.DANGER).also {
                        it.add(
                            Html.P(
                                "Please refer the attached log file for more information or simply\n"
                                        + "re-run system check on page Administration -> System -> check system integrity."
                            )
                        ).add(Html.BR())
                            .add(Html.P("Your ProjectForge system"))
                    }
                    msg.content = contextList.getReportAsHtml(showAllMessages = false, intro)
                    msg.contentType = Mail.CONTENTTYPE_HTML
                    val attachments = listOf(MailAttachment(FILENAME, contextList.getReportAsHtml().toByteArray()))
                    sendMail.send(msg, null, attachments)
                }
            }
        }
    }

    fun execute(): JobListExecutionContext {
        val context = JobListExecutionContext()
        jobs.forEach { job ->
            val jobContext = context.add(job)
            try {
                log.info { "Executing sanity check job: ${job::class.simpleName}" }
                job.executeJob(jobContext)
                log.info { "Execution of sanity check job done: ${job::class.simpleName}" }
            } catch (ex: Throwable) {
                log.error(ex) { "While executing sanity job ${job::class.simpleName}: " + ex.message }
            }
        }
        return context
    }

    fun registerJob(job: AbstractJob) {
        log.info { "Registering sanity check job: ${job::class.simpleName}" }
        jobs.add(job)
    }

    companion object {
        private const val CRON = "\${projectforge.cron.sanityChecks}"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "cron.sanityChecks", SchedulerJobArea.MAINTENANCE, CronSanityCheckJob::class.java, "cron",
            SchedulerSchedule.Cron(CRON),
        )

        @JvmStatic
        val FILENAME: String
            get() {
                return "projectforge_sanity-check${DateHelper.getTimestampAsFilenameSuffix(Date())}.html"
            }
    }
}
