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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileSizeChecker
import org.projectforge.plugins.datatransfer.DataTransferAreaDao.Companion.calculateMaxUploadFileSize

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class DataTransferFileSizeChecker(val globalMaxFileSizeOfDataTransfer: Long) : FileSizeChecker {

  override fun checkSize(file: FileInfo, data: Any?, displayUserMessage: Boolean) {
    checkSize(
      file,
      globalMaxFileSizeOfDataTransfer,
      DataTransferAreaDao.MAX_FILE_SIZE_SPRING_PROPERTY,
      displayUserMessage
    )
    if (data == null || data !is DataTransferAreaDO) {
      log.warn { "maxUploadsizeKB of area not given. area not given or not of Type DataTransferAreadDO: $data" }
      return
    }
    val capacity = calculateMaxUploadFileSize(data)
    checkSize(file, capacity, null)
  }

  override val maxFileSize: Long
    get() = throw java.lang.IllegalArgumentException("Use DataTransferAreaDao.getFreeCapacity instead.")
}
