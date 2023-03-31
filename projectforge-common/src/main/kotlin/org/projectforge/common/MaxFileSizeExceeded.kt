/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.projectforge.common.i18n.UserException
import java.util.*

class MaxFileSizeExceeded(
  val maxFileSize: Long?,
  val fileSize: Long?,
  val fileName: String? = null,
  /**
   * For admins to log, which spring property may be configure to increase max size of files (projectforge.properties).
   */
  val maxFileSizeSpringProperty: String? = null,
  displayUserMessage: Boolean = true,
  val info: Any? = null
) :
  UserException(
    "file.upload.error.maxSizeExceeded",
    fileName ?: "<unknown>",
    FormatterUtils.formatBytes(fileSize),
    FormatterUtils.formatBytes(maxFileSize)
  ) {
  init {
    val maxFileSizeFormatted = FormatterUtils.formatBytes(maxFileSize, Locale.ENGLISH)
    if (maxFileSizeSpringProperty != null) {
      logHintMessage =
        "You may increase this file size by configuring parameter '$maxFileSizeSpringProperty=$maxFileSizeFormatted' in config properties file."
    }
    this.displayUserMessage = displayUserMessage
  }
}
