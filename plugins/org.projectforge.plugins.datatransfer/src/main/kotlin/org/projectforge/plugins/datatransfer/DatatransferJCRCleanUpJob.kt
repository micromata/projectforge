/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DataTransferJCRCleanUpJob {
  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var repoService: RepoService

  // Cron-Jobs: second (0-59), minute (0-59), hour (0 - 23), day (1 - 31), month (1 - 12), weekday (0 - 7, MON-SUN)
  //@Scheduled(cron = "\${projectforge.plugin.datatransfer.cron.cleanup:0 15 * * * *}")
  @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 600 * 1000)
  fun execute() {
    log.info("Data transfer clean-up job started.")
    val startTimeInMillis = System.currentTimeMillis()

    val processedDBOs = mutableListOf<Int>() // For checking orphaned areas.

    // First of all, try to check all attachments of active areas:
    dataTransferAreaDao.internalLoadAll().forEach { dbo ->
      dbo.id?.let { id ->
        processedDBOs.add(id)
        val expiryMillis = (dbo.expiryDays ?: 30) * MILLIS_PER_DAY
        val attachments = attachmentsService.internalGetAttachments(
          dataTransferAreaPagesRest.jcrPath!!,
          id
        )
        attachments?.forEach { attachment ->
          val time = attachment.lastUpdate?.time ?: attachment.created?.time
          if (time == null || startTimeInMillis - time > expiryMillis) {
            log.info { "**** Simulating: Deleting expired attachment of area '${dbo.areaName}': $attachment" }
            /*attachment.fileId?.let { fileId ->
              attachmentsService.internalDeleteAttachment(
                dataTransferAreaPagesRest.jcrPath!!,
                fileId,
                dataTransferAreaDao,
                dbo
              )
            }*/
          } else {
            log.info { "Attachment of area '${dbo.areaName}' not yet expired: $attachment" }
          }
        }
      }
    }

    val nodePath = repoService.getAbsolutePath(dataTransferAreaPagesRest.jcrPath)
    val nodeInfo = repoService.getNodeInfo(nodePath, true)
    log.info { "Datatransfer: $nodeInfo" }
    nodeInfo.children?.let { children ->
      for (child in children) {
        val dbId = NumberHelper.parseInteger(child.name)
        if (dbId == null) {
          log.warn { "Oups, name of node isn't of type int (db id): '${child.name}'. Ignoring node." }
          continue
        }
        if (processedDBOs.any { it == dbId }) {
          continue
        }
        log.info { "**** Simulating: Removing orphaned node (area was deleted): $child" }
        //repoService.deleteNode(child)
      }
      log.info { "Datatransfer: $nodeInfo" }
      log.info("JCR clean-up job finished after ${(System.currentTimeMillis() - startTimeInMillis) / 1000} seconds.")
    }
  }

  companion object {
    internal const val MILLIS_PER_DAY = 1000 * 60 * 60 * 24
  }
}
