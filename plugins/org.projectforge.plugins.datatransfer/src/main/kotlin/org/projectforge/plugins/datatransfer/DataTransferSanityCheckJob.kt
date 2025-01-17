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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import org.projectforge.business.jobs.CronSanityCheckJob
import org.projectforge.common.extensions.format
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jobs.AbstractJob
import org.projectforge.jobs.JobExecutionContext
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataTransferSanityCheckJob : AbstractJob("Check data transfer files.") {
    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

    @Autowired
    private lateinit var cronSanityCheckJob: CronSanityCheckJob

    @PostConstruct
    private fun postConstruct() {
        cronSanityCheckJob.registerJob(this)
    }

    override fun executeJob() {
        var areaCounter = 0
        var missingInJcrCounter = 0
        var orphanedCounter = 0
        var fileCounter = 0
        dataTransferAreaDao.selectAll(checkAccess = false).forEach { area ->
            ++areaCounter
            val areaName = "'${area.displayName}'"
            try {
                val attachments = attachmentsService.getAttachments(
                    dataTransferAreaPagesRest.jcrPath!!,
                    area.id!!,
                    dataTransferAreaPagesRest.attachmentsAccessChecker,
                    checkAccess = false,
                )
                val attachmentsCounter = area.attachmentsCounter ?: 0
                fileCounter += attachmentsCounter
                if (attachmentsCounter == 0 && attachments.isNullOrEmpty()) {
                    // No attachments given/expected, nothing to check.
                    return@forEach
                }
                jobExecutionContext.addMessage("Checking data transfer area $areaName with ${attachmentsCounter} attachments.")
                attachments?.forEach { attachment ->
                    if (attachment.size == 0L) {
                        jobExecutionContext.addWarning("Empty attachment ${attachment.name} in area $areaName.")
                    }
                }
                if (attachmentsCounter != attachments?.size) {
                    jobExecutionContext.addWarning("Attachments counter in area $areaName is ${attachmentsCounter}, but found ${attachments?.size} attachments.")
                }
                val areaAttachmentIds = area.attachmentsIds?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
                val jcrAttachmentIds = attachments?.map { it.fileId.toString() } ?: emptyList()
                val missingInJcr = areaAttachmentIds - jcrAttachmentIds
                val unknown = jcrAttachmentIds - areaAttachmentIds
                if (missingInJcr.isNotEmpty()) {
                    jobExecutionContext.addError("${missingInJcr.size}/$attachmentsCounter attachments missing in JCR for area $areaName: ${missingInJcr.joinToString()}")
                    missingInJcrCounter += missingInJcr.size
                }
                if (unknown.isNotEmpty()) {
                    jobExecutionContext.addWarning("${unknown.size} unknown (orphaned) attachments in JCR for area $areaName: ${unknown.joinToString()}")
                    orphanedCounter += unknown.size
                }
            } catch (ex: Exception) {
                jobExecutionContext.addWarning("Error while checking data transfer area $areaName: ${ex.message}")
            }
        }
        val baseMsg = "Checked ${fileCounter.format()} files in ${areaCounter.format()} data transfer areas"
        if (missingInJcrCounter > 0 ) {
            jobExecutionContext.addError("$baseMsg: $missingInJcrCounter missed, $orphanedCounter orphaned attachments.")
        } else if (orphanedCounter > 0) {
            jobExecutionContext.addWarning("$baseMsg: $orphanedCounter orphaned attachments.")
        } else {
            jobExecutionContext.addMessage("$baseMsg.")
        }
    }
}
