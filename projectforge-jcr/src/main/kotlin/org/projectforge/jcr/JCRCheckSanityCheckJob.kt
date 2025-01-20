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

package org.projectforge.jcr

import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.common.extensions.format
import org.projectforge.common.extensions.formatBytes
import org.projectforge.common.extensions.formatMillis
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * Checks the sanity of the JCR repository.
 * This job is scheduled by the cron expression in the application.properties file.
 */
@Component
open class JCRCheckSanityCheckJob : AbstractJob("JCR Check Sanity") {
    @Autowired
    internal lateinit var repoService: RepoService

    class CheckResult(
        val errors: List<String>,
        val warnings: List<String>,
        val numberOfVisitedFiles: Int,
        val numberOfVisitedNodes: Int
    )

    // For testing: @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 10 * 1000)
    // projectforge.jcr.cron.backup=0 30 0 * * *
    @Scheduled(cron = "\${projectforge.jcr.cron.sanityCheck}")
    open fun cron() {
        val started = System.currentTimeMillis()
        log.info("JCR sanity check job started.")
        val job = this
        Thread {
            try {
                val jobContext = JobExecutionContext(job)
                executeJob(jobContext)
                val numberOfVisitedNodes = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_NODES)
                val numberOfVisitedFiles = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_FILES)
                val msgPart1 =
                    "JCR sanity check job finished after ${(System.currentTimeMillis() - started).formatMillis()}"
                val msgPart2 =
                    "${numberOfVisitedFiles.format()} files and ${numberOfVisitedNodes.format()} nodes checked: errors=${jobContext.errors.size}, warnings=${jobContext.warnings.size}."
                if (jobContext.status == JobExecutionContext.Status.ERRORS) {
                    log.error { "$msgPart1 with errors. $msgPart2" }
                } else {
                    log.info { "$msgPart1. $msgPart2" }
                }
            } catch (ex: Throwable) {
                log.error("While executing hibernate search re-index job: " + ex.message, ex)
            }
        }.start()
    }

    override fun executeJob() {
        var failedChecks = 0
        var totalSizeOfFiles = 0L
        val walker = object : RepoTreeWalker(repoService) {
            override fun visitFile(fileNode: Node, fileObject: FileObject) {
                totalSizeOfFiles += fileObject.size ?: 0
                fileObject.checksum.let { repoChecksum ->
                    if (repoChecksum != null && repoChecksum.length > 10) {
                        val checksum =
                            repoService.getFileInputStream(fileNode, fileObject, true, useEncryptedFile = true)
                                .use { istream -> RepoService.checksum(istream) }
                        if (!validateChecksum(checksum, repoChecksum)) {
                            ++failedChecks
                            val msg =
                                "Checksum of file '${fileObject.fileName}' from repository '${normalizeChecksum(checksum)}' differs from repository value '${
                                    normalizeChecksum(
                                        repoChecksum
                                    )
                                }'! ['${fileNode.path}']"
                            jobExecutionContext.addError(msg)
                            log.error { msg }
                        }
                    } else {
                        val msg =
                            "Checksum of file '${fileObject.fileName}' from repository not given (skipping checksum check). ['${fileNode.path}']"
                        jobExecutionContext.addWarning(msg)
                        log.error { msg }
                    }
                }
                if (fileObject.fileExtension == "zip") {
                    // Check mode of zip files (encryption).
                    if (fileObject.zipMode == null) {
                        val newZipMode =
                            repoService.getFileInputStream(fileNode, fileObject, true, useEncryptedFile = true)
                                .use { istream -> ZipUtils.determineZipMode(istream) }
                        if (newZipMode != null) {
                            fileNode.setProperty(RepoService.PROPERTY_ZIP_MODE, newZipMode.name)
                        }
                    }
                }
                fileObject.size.let { repoSize ->
                    if (repoSize == null) {
                        val msg =
                            "Size of file '${fileObject.fileName}' from repository not given (skipping file size check). ['${fileNode.path}']"
                        jobExecutionContext.addWarning(msg)
                        log.info { msg }
                    } else {
                        val fileSize = repoService.getFileSize(fileNode, fileObject, true)
                        if (fileSize != repoSize) {
                            val msg =
                                "Size of file from repository '${fileNode.path}': '${fileObject.fileName}'=${
                                    FormatterUtils.format(
                                        fileSize
                                    )
                                } differs from repository value ${FormatterUtils.format(repoSize)}!"
                            jobExecutionContext.addError(msg)
                            log.error { msg }
                        }
                    }
                }
            }
        }
        walker.walk()
        jobExecutionContext.setAttribute(NUMBER_OF_VISITED_NODES, walker.numberOfVisitedNodes)
        jobExecutionContext.setAttribute(NUMBER_OF_VISITED_FILES, walker.numberOfVisitedFiles)
        jobExecutionContext.addMessage(
            "Checksums of ${walker.numberOfVisitedFiles.format()} files (${walker.numberOfVisitedNodes.format()} nodes) checked."
        )
        if (failedChecks > 0) {
            jobExecutionContext.addError(
                "Checksums of $failedChecks/${walker.numberOfVisitedFiles.format()} files (${totalSizeOfFiles.formatBytes()}, ${walker.numberOfVisitedNodes.format()} nodes) failed."
            )
        }
    }

    open fun execute(): CheckResult {
        val jobContext = JobExecutionContext(this)
        executeJob(jobContext)
        val errors = jobContext.errors.map { it.message }.toMutableList()
        val warnings = jobContext.warnings.map { it.message }.toMutableList()
        val numberOfVisitedNodes = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_NODES)
        val numberOfVisitedFiles = jobContext.getAttributeAsInt(NUMBER_OF_VISITED_FILES)
        return CheckResult(errors, warnings, numberOfVisitedFiles ?: -1, numberOfVisitedNodes ?: -1)
    }

    private fun validateChecksum(checksum1: String, checksum2: String): Boolean {
        val c1 = normalizeChecksum(checksum1)
        val c2 = normalizeChecksum(checksum2)
        return c1 == c2
    }

    private fun normalizeChecksum(checksum: String): String {
        return subString(subString(checksum, '='), ' ')
    }

    private fun subString(checksum: String, ch: Char): String {
        val idx = checksum.indexOf(ch)
        return if (idx > 0 && checksum.length > idx) {
            checksum.substring(idx + 1)
        } else {
            checksum
        }
    }

    companion object {
        const val NUMBER_OF_VISITED_NODES = "numberOfVisitedNodes"
        const val NUMBER_OF_VISITED_FILES = "numberOfVisitedFiles"

    }
}
