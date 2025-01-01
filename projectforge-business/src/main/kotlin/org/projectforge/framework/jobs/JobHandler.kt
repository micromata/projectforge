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

package org.projectforge.framework.jobs

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.ShutdownListener
import org.projectforge.ShutdownService
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.calendar.DurationUtils
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

@Service
class JobHandler : ShutdownListener {
    @Autowired
    private lateinit var sendMail: SendMail

    @Autowired
    private lateinit var configService: ConfigurationService

    @Autowired
    private lateinit var shutdownService: ShutdownService

    private val jobs = mutableListOf<AbstractJob>()

    @PostConstruct
    private fun postConstruct() {
        shutdownService.registerListener(this)
    }

    /**
     * Will be called by JobHandlerScheduler.
     */
    fun tidyUp() {
        lastRun = System.currentTimeMillis()
        runningJobs.forEach { job ->
            if (job.timeoutReached) {
                internalCancelJob(job)
            }
        }
        terminatedJobs.forEach { job ->
            val time = job.terminatedTimeMillis ?: job.startTimeMillis ?: job.createdTimeMillis
            if (System.currentTimeMillis() - time > KEEP_TERMINATED_JOBS_INTERVALL_MS) {
                synchronized(jobs) {
                    jobs.remove(job)
                }
            }
        }
    }

    /**
     * @return Given job for chaining.
     */
    fun addJob(job: AbstractJob): AbstractJob {
        synchronized(jobs) {
            jobs.add(job)
        }
        val userContext = ThreadLocalUserContext.userContext!!
        val locale = ThreadLocalUserContext.locale
        val mdcContext = MDCContext() // For MDC context of logger.
        thread {
            var start = true
            runBlocking {
                job.coroutinesJob = launch(
                    Dispatchers.Default + ThreadLocalUserContext.getUserAsContextElement(userContext) + ThreadLocalUserContext.getLocaleAsContextElement(
                        locale
                    ) + mdcContext
                ) {
                    for (i in 0..10000) {// Paranoia counter (wait time max 10.000s)
                        var blocking: AbstractJob.Status? = null
                        synchronized(jobs) {
                            for (other in jobs) {
                                if (other != job) {
                                    blocking = other.isBlocking(job)
                                    if (blocking != null) {
                                        break
                                    }
                                }
                            }
                            if (blocking == null) {
                                // Must be set here, otherwise two waiting jobs may run simultaneously
                                job.status = AbstractJob.Status.RUNNING
                            }
                        }
                        val status = blocking
                        if (status != null) {
                            job.status = status
                            start = false
                            if (status == AbstractJob.Status.REFUSED) {
                                break
                            }
                            delay(1000)
                        } else {
                            start = true
                            break
                        }
                    }
                    if (start) {
                        yield()
                        job.onBeforeStart()
                        job.start()
                        job.onFinish()
                    } else {
                        if (job.status == AbstractJob.Status.REFUSED) {
                            log.error { "Couldn't start job, because another job is already running: ${job.logInfo}" }
                            job.markJobAsRefused(errorMessage = "jobs.error.refusedByAnotherRunningJob")
                            job.onAfterFailure(error = AbstractJob.ErrorCode.REFUSED_BY_ANOTHER_RUNNING_JOB)
                            job.onAfterTermination()
                        } else {
                            log.error { "Couldn't start job due to long running job(s) blocking this job: ${job.logInfo}" }
                            job.markJobAsFailed(errorMessage = "jobs.error.waitingTimeExceeded")
                            job.onAfterFailure(error = AbstractJob.ErrorCode.TIMEOUT_WHILE_WAITING)
                            job.onAfterTermination()
                        }
                    }
                }
                if (start) {
                    job.coroutinesJob.join()
                }
            }
        }
        return job
    }

    fun getJobById(id: Int): AbstractJob? {
        val job = synchronized(jobs) {
            jobs.find { it.id == id }
        } ?: return null
        if (job.readAccess()) {
            return job
        }
        log.warn { "Logged-in user ${ThreadLocalUserContext.loggedInUser?.username} has no read access to requested job." }
        return null
    }

    fun cancelJob(job: AbstractJob) {
        if (job.writeAccess()) {
            internalCancelJob(job)
        } else {
            log.warn { "Logged-in user ${ThreadLocalUserContext.loggedInUser?.username} has no write access to requested job. So job will not be cancelled." }
        }
    }

    /**
     * @param jobId If given, this job will be the first of the result list (if the user has read-access.
     * @param all If true, all jobs of the user will be part of the result list. The list is ordered by status and timestamps.
     */
    fun getJobsOfUser(jobId: Int?, all: Boolean? = true): List<AbstractJob> {
        val list = mutableListOf<AbstractJob>()
        if (all == true) {
            synchronized(jobs) {
                list.addAll(jobs.filter { it.readAccess() })
            }
        }
        list.sort()
        if (jobId != null) {
            val job = getJobById(jobId)
            if (job != null) {
                list.removeIf { it.id == job.id }
                list.add(0, job)
            }
        }
        return list
    }

    private fun internalCancelJob(job: AbstractJob) {
        log.warn { "Job ${job.logInfo} is going to be cancelled." }
        job.cancel()
    }

    private val runningJobs: List<AbstractJob>
        get() {
            synchronized(jobs) {
                return jobs.filter { it.status == AbstractJob.Status.RUNNING }
            }
        }

    /**
     * Finished and cancelled jobs.
     */
    private val terminatedJobs: List<AbstractJob>
        get() {
            synchronized(jobs) {
                return jobs.filter { it.terminated }
            }
        }

    override fun shutdown() {
        runBlocking {
            runningJobs.forEach { job ->
                if (job.status == AbstractJob.Status.RUNNING) {
                    job.cancel()
                    job.coroutinesJob.join()
                }
            }
        }
    }

    private var lastRun: Long? = null

    fun checkStatus() {
        lastRun.let {
            if (it == null) {
                // Job wasn't executed yet. It's OK, if ProjectForge isn't running longer than a few minutes.
                val processUptime = ManagementFactory.getRuntimeMXBean().uptime
                val processUptimeFormatted = DurationUtils.getFormattedDaysHoursAndMinutes(processUptime)
                if (processUptime > 5 * Constants.MILLIS_PER_MINUTE) {
                    reportErrorMail("ProjectForge is running since $processUptimeFormatted but no scheduled job of JobHandler was running.")
                } else {
                    log.info { "Checking status of Spring's job scheduler: ProjectForge was started $processUptimeFormatted ago, no job was scheduled yet (OK)." }
                }
            } else {
                val durationSinceLastRun = System.currentTimeMillis() - it
                val durationSinceLastRunFormatted = DurationUtils.getFormattedDaysHoursAndMinutes(durationSinceLastRun)
                if (durationSinceLastRun > 5 * Constants.MILLIS_PER_MINUTE) {
                    reportErrorMail(
                        "The last scheduled job of JobHandler was running $durationSinceLastRunFormatted ago but should be started every minute."
                    )
                } else {
                    log.info { "Checking status of Spring's job scheduler: lastRun was $durationSinceLastRunFormatted ago (OK)." }
                }
            }
        }
    }

    private fun reportErrorMail(message: String) {
        val fullMessage =
            "Job scheduling might not be running (no backups, no e-mail notification etc.) You should restart ProjectForge. Reason: $message"
        log.error { fullMessage }
        configService.pfSupportMailAddress?.let { receiver ->
            val data = mutableMapOf("description" to fullMessage)
            val params = mutableMapOf<String, Any?>("data" to data)
            val msg = Mail()
            msg.addTo(receiver)
            val subject = "Jobs not running? Restart required (no backups, no mail notification etc.)"
            msg.setProjectForgeSubject(subject)
            params.put("subject", subject)
            val content = sendMail.renderGroovyTemplate(
                msg,
                "mail/feedback.txt",
                params,
                getLocalizedMessage("administration.configuration.param.feedbackEMail.label"),
                null
            )
            msg.content = content
            msg.contentType = Mail.CONTENTTYPE_TEXT
            sendMail.send(msg, null, null)

        }
    }

    companion object {
        /**
         * Keep terminated jobs not older than this given intervall in ms.
         */
        internal const val KEEP_TERMINATED_JOBS_INTERVALL_MS = Constants.MILLIS_PER_HOUR
    }
}
