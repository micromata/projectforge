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

package org.projectforge.files

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatBytes
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.files.store.FileStore
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Checks the sanity of the file store (checksums and sizes of all files).
 * Runs nightly as part of CronSanityCheckJob (projectforge.cron.sanityChecks). Its own schedule
 * projectforge.files.cron.sanityCheck is disabled by default.
 */
@Component
open class FileStoreSanityCheckJob : AbstractJob("File store sanity check") {
    @Autowired
    internal lateinit var fileStore: FileStore

    /** Not set in tests without Spring. */
    @Autowired(required = false)
    internal var schedulerJobRunner: SchedulerJobRunner? = null

    @PostConstruct
    internal fun postConstruct() {
        schedulerJobRunner?.register(SCHEDULER_JOB, ::cron)
    }

    class CheckResult(
        val errors: List<String>,
        val warnings: List<String>,
        val numberOfVisitedFiles: Int,
    )

    // Disabled by default (projectforge.files.cron.sanityCheck=-), CronSanityCheckJob runs this job nightly.
    @Scheduled(cron = CRON)
    open fun cron() {
        val job = this
        // Duration and exceptions are logged by the scheduler.
        schedulerJobRunner?.runAsync(SCHEDULER_JOB) { run ->
            val jobContext = JobExecutionContext(job)
            executeJob(jobContext)
            val numberOfVisitedFiles = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_FILES)
            val msg =
                "${numberOfVisitedFiles.format()} files checked: errors=${jobContext.errors.size}, warnings=${jobContext.warnings.size}."
            if (jobContext.status == JobExecutionContext.Status.ERRORS) {
                log.error { "File store sanity check job finished with errors. $msg" }
                run.fail("File store sanity check found errors. $msg")
            } else {
                log.info { "File store sanity check job finished. $msg" }
            }
        }
    }

    override fun executeJob() {
        var storeFiles = 0
        var storeFailed = 0
        var storeSize = 0L
        fileStore.checkSanity { result ->
            ++storeFiles
            storeSize += result.fileObject.size ?: 0
            result.error?.let { error ->
                ++storeFailed
                val msg =
                    "File '${result.fileObject.fileName}' (${result.storage}): $error ['${result.fileObject.location}/${result.fileObject.fileId}']"
                jobExecutionContext.addError(msg)
                log.error { msg }
            }
        }
        jobExecutionContext.setAttribute(NUMBER_OF_VISITED_FILES, storeFiles)
        jobExecutionContext.addMessage(
            "Checksums of ${storeFiles.format()} files (${storeSize.formatBytes()}) of the file store checked."
        )
        if (storeFailed > 0) {
            jobExecutionContext.addError(
                "Checks of $storeFailed/${storeFiles.format()} files of the file store failed."
            )
        }
    }

    open fun execute(): CheckResult {
        val jobContext = JobExecutionContext(this)
        executeJob(jobContext)
        val errors = jobContext.errors.map { it.message }.toMutableList()
        val warnings = jobContext.warnings.map { it.message }.toMutableList()
        val numberOfVisitedFiles = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_FILES)
        return CheckResult(errors, warnings, numberOfVisitedFiles ?: -1)
    }

    companion object {
        const val NUMBER_OF_VISITED_FILES = "numberOfVisitedFiles"

        private const val CRON = "\${projectforge.files.cron.sanityCheck}"

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "files.sanityCheck", SchedulerJobArea.FILES, FileStoreSanityCheckJob::class.java, "cron",
            SchedulerSchedule.Cron(CRON),
        )
    }
}
