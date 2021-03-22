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
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class JCRCleanUpJob {
  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  // projectforge.jcr.cron.backup=5 30 0 * * *
  @Scheduled(cron = "\${projectforge.plugin.datatransfer.cron.cleanup:5 30 0 * * *}")
  fun execute() {
    log.info("Data transfer clean-up job started.")
    val time = System.currentTimeMillis()

    dataTransferAreaDao.internalLoadAll().forEach { dbo ->
      dbo.id?.let { id ->
        val expiryMillis = (dbo.expiryDays ?: 30) * MILLIS_PER_DAY
        val now = System.currentTimeMillis()
        val attachments = attachmentsService.getAttachments(
          dataTransferAreaPagesRest.jcrPath!!,
          id,
          dataTransferAreaPagesRest.attachmentsAccessChecker
        )
        attachments?.forEach { attachment ->
          val time = attachment.lastUpdate?.time ?: attachment.created?.time
          if (time == null || now - time > expiryMillis) {
            log.info{"Deleting expired attachment of area '${dbo.areaName}': $attachment"}
            attachment.fileId?.let { fileId ->
              attachmentsService.internalDeleteAttachment(
                dataTransferAreaPagesRest.jcrPath!!,
                fileId,
                dataTransferAreaDao,
                dbo
              )
            }
          }
        }
      }
    }

    log.info("JCR clean-up job finished after ${(System.currentTimeMillis() - time) / 1000} seconds.")
  }

  companion object {
    private const val MILLIS_PER_DAY = 1000 * 60 * 60 * 24
  }
}
