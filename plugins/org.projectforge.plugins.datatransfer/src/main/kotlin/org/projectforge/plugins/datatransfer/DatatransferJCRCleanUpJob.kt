/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal

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

  @Autowired
  private lateinit var pluginAdminService: PluginAdminService

  /**
   * @return number of deleted files (for test cases).
   */
  // Every hour, starting 10 minutes after starting.
  @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 600 * 1000)
  fun execute(): Int {
    if (!pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }) {
      log.info("Plugin data transfer not activated. Don't need clean-up job.")
      return -1
    }
    log.info("Data transfer clean-up job started.")
    val startTimeInMillis = System.currentTimeMillis()

    val processedDBOs = mutableListOf<Long>() // For checking orphaned areas.

    var deletedCounter = 0
    var deletedSize: Long = 0
    var preservedCounter = 0
    var preservedSize: Long = 0
    // First of all, try to check all attachments of active areas:
    dataTransferAreaDao.loadAll(checkAccess = false).forEach { dbo ->
      dbo.id?.let { id ->
        processedDBOs.add(id)
        val expiryMillis = (dbo.expiryDays ?: 30).toLong() * MILLIS_PER_DAY
        val attachments = attachmentsService.internalGetAttachments(
          dataTransferAreaPagesRest.jcrPath!!,
          id
        )
        attachments?.forEach { attachment ->
          val time = attachment.lastUpdate?.time ?: attachment.created?.time
          if (time == null || startTimeInMillis - time > expiryMillis) {
            log.info { "Deleting expired attachment of area '${dbo.areaName}': $attachment" }
            attachment.fileId?.let { fileId ->
              attachmentsService.internalDeleteAttachment(
                dataTransferAreaPagesRest.jcrPath!!,
                fileId,
                dataTransferAreaDao,
                dbo,
                userString = SYSTEM_USER
              )
            }
            ++deletedCounter
            deletedSize += attachment.size ?: 0
          } else {
            log.debug { "Attachment of area '${dbo.areaName}' not yet expired: $attachment" }
            ++preservedCounter
            preservedSize += attachment.size ?: 0
          }
        }
      }
    }

    val nodePath = repoService.getAbsolutePath(dataTransferAreaPagesRest.jcrPath)
    val nodeInfo = repoService.getNodeInfo(nodePath, true)
    nodeInfo.children?.let { children ->
      for (child in children) {
        val dbId = NumberHelper.parseLong(child.name)
        if (dbId == null) {
          log.warn { "Oups, name of node isn't of type int (db id): '${child.name}'. Ignoring node." }
          continue
        }
        if (processedDBOs.any { it == dbId }) {
          continue
        }
        val files = mutableListOf<String>()
        child.findDescendant(AttachmentsService.DEFAULT_NODE, RepoService.NODENAME_FILES)?.children?.forEach {
          deletedCounter++
          deletedSize += it.getProperty("size")?.value?.long ?: 0
          files.add("file=[${it.name}: '${it.getProperty("fileName")?.value?.string}' (${FormatterUtils.formatBytes(it.getProperty("size")?.value?.long)})]")
        }
        log.info { "Removing orphaned node (area was deleted): ${files.joinToString(", ")}" }
        repoService.deleteNode(child)
      }
      log.info(
        "JCR clean-up job finished after ${(System.currentTimeMillis() - startTimeInMillis) / 1000} seconds. Number of deleted files: $deletedCounter (${
          FormatterUtils.formatBytes(
            deletedSize
          )
        }), remaining size: $preservedCounter (${
          FormatterUtils.formatBytes(
            preservedSize
          )
        })."
      )
    }
    repoService.cleanup()
    return deletedCounter
  }

  companion object {
    internal const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24

    internal val BD_MILLIS_PER_DAY = BigDecimal(MILLIS_PER_DAY)

    internal const val SYSTEM_USER = "ProjectForge system"
  }
}
