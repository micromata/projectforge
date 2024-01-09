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

package org.projectforge.jcr

import mu.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.common.MaxFileSizeExceeded

private val log = KotlinLogging.logger {}

/**
 * Checks the file size before storing it.
 */
interface FileSizeChecker {
  /**
   * Checks the size of the given file.
   * @param Optional data e. g. for fileSizeChecker of data transfer area size.
   * @param displayUserMessage See [org.projectforge.common.i18n.UserException]
   * @return Exception to throw by caller if file is to big or null, if file size is accepted.
   */
  fun checkSize(file: FileInfo, data: Any? = null, displayUserMessage: Boolean = true)

  /**
   * In bytes.
   */
  abstract val maxFileSize: Long

  val maxFileSizeKB
   get() = (maxFileSize / 1024).toInt()

  fun checkSize(
    file: FileInfo,
    maxFileSize: Long,
    maxFileSizeSpringProperty: String?,
    displayUserMessage: Boolean = true
  ) {
    file.size?.let { fileSize ->
      if (fileSize > maxFileSize) {
        throw MaxFileSizeExceeded(
          maxFileSize,
          fileSize,
          file.fileName ?: "<unknown>",
          maxFileSizeSpringProperty,
          displayUserMessage = displayUserMessage
        )
      }
    }
    if (file.size == null) {
      val maxFileSizeFormatted = FormatterUtils.formatBytes(maxFileSize)
      log.warn { "Can't check maximum file size of $maxFileSizeFormatted. Can't detect file size. File will be stored: $file." }
    }
  }
}
