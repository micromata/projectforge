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

package org.projectforge.business.jobs

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.task.TaskDao
import org.projectforge.common.extensions.formatMillis
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

    @PostConstruct
    private fun postConstruct() {
        registerJob(SystemSanityCheckJob(taskDao))
        registerJob(jcrCheckSanityJob) // JCRCheckSanityJob is a plugin job, and it is registered here, because CronSanityCheckJob is not known by JCR.
    }

    // For testing: @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
    @Scheduled(cron = "\${projectforge.cron.sanityChecks}")
    fun cron() {
        log.info("Cronjob for executing sanity checks started...")

        Thread {
            val start = System.currentTimeMillis()
            try {
                val contextList = execute()
                if (contextList.status == JobExecutionContext.Status.ERRORS) {
                    val recipients = Configuration.instance.getStringValue(ConfigurationParam.SYSTEM_ADMIN_E_MAIL)
                    if (!recipients.isNullOrBlank()) {
                        val msg = Mail()
                        msg.addTo(recipients)
                        msg.setProjectForgeSubject("Errors occurred on sanity check job.")
                        val sb = StringBuilder()
                        sb.appendLine(
                            """
                            |Please refer the attached log file for more information or simply
                            |re-run system check on page Administration -> System -> check system integrity.
                            |
                            |Your ProjectForge system
                            |
                        """.trimMargin()
                        )
                        sb.appendLine(contextList.getReportAsText(showAllMessages = false))
                        msg.content = sb.toString()
                        msg.contentType = Mail.CONTENTTYPE_TEXT
                        val attachments = listOf(MailAttachment(FILENAME, contextList.getReportAsText().toByteArray()))
                        sendMail.send(msg, null, attachments)
                    }
                }
            } finally {
                log.info("Cronjob for executing sanity checks finished after ${(System.currentTimeMillis() - start).formatMillis()}")
            }
        }.start()
    }

    fun execute(): JobListExecutionContext {
        val context = JobListExecutionContext()
        jobs.forEach { job ->
            val jobContext = context.add(job)
            try {
                log.info("Executing sanity check job: ${job::class.simpleName}")
                job.executeJob(jobContext)
                log.info("Execution of sanity check job done: ${job::class.simpleName}")
            } catch (ex: Throwable) {
                log.error("While executing sanity job ${job::class.simpleName}: " + ex.message, ex)
            }
        }
        return context
    }

    fun registerJob(job: AbstractJob) {
        log.info { "Registering sanity check job: ${job::class.simpleName}" }
        jobs.add(job)
    }

    companion object {
        @JvmStatic
        val FILENAME: String
            get() {
                return "projectforge_sanity-check${DateHelper.getTimestampAsFilenameSuffix(Date())}.html"
            }
    }
}
