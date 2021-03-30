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
   * @return Exception to throw by caller if file is to big or null, if file size is accepted.
   */
  fun checkSize(file: FileInfo): MaxFileSizeExceeded?

  fun checkSize(file: FileInfo, maxFileSize: Long, maxFileSizeSpringProperty: String?): MaxFileSizeExceeded? {
    file.size?.let {
      if (it > maxFileSize) {
        val ex = MaxFileSizeExceeded(
          maxFileSize,
          "File will not be stored: $file.",
          it,
          file.fileName,
          maxFileSizeSpringProperty,
          info = file
        )
        log.error { ex.message }
        return ex
      }
    }
    if (file.size == null) {
      val maxFileSizeFormatted = FormatterUtils.formatBytes(maxFileSize)
      log.warn { "Can't check maximum file size of $maxFileSizeFormatted. Can't detect file size. File will be stored: $file." }
    }
    return null
  }
}
