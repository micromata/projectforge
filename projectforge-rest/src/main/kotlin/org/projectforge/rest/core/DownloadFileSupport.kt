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

package org.projectforge.rest.core

import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.ui.*
import java.time.temporal.ChronoUnit
import javax.servlet.http.HttpServletRequest

/**
 * For temporarily downloads of files, stored in [ExpiringSessionAttributes].
 */
class DownloadFileSupport(
  val expiringSessionAttribute: String,
  val downloadExpiryMinutes: Int,
) {
  data class DownloadFile(
    val filename: String,
    val bytes: ByteArray,
    val availableUntil: String,
  ) {
    val sizeHumanReadable
      get() = NumberHelper.formatBytes(bytes.size)
  }

  /**
   * Usable as Json-object for client exchange.
   */
  class Download() {
    constructor(file: DownloadFile): this() {
      filename = file.filename
      fileSize = file.sizeHumanReadable
      availableUntil = file.availableUntil
      filenameAndSize = "$filename ($fileSize)"
    }
    var filename: String? = null
    var fileSize: String? = null
    var availableUntil: String? = null
    var filenameAndSize: String? = null
  }

  /**
   * @param useDataObject If set (default), the field download.filenameAndSize of the data object is used, otherwise
   * the value will be set directly from given download param.
   */
  fun createDownloadFieldset(title: String?, restPath: String, download: Download, useDataObject: Boolean = true): UIFieldset {
    val availableUntil = translateMsg("scripting.download.filename.additional", download.availableUntil)
    val fieldset = UIFieldset(title = title)
    fieldset
      .add(
        UIReadOnlyField(
          "download.filenameAndSize",
          label = "file",
          additionalLabel = "'$availableUntil",
          value = if (useDataObject) null else download.filename,
        )
      )
      .add(
        UIButton.createDownloadButton(
          responseAction = ResponseAction(
            url = restPath,
            targetType = TargetType.DOWNLOAD
          )
        )
      )
    return fieldset
  }

  fun getDownloadFile(request: HttpServletRequest): DownloadFile? {
    return ExpiringSessionAttributes.getAttribute(
      request,
      expiringSessionAttribute, DownloadFile::class.java
    )
  }

  internal fun storeDownloadFile(
    request: HttpServletRequest,
    filename: String,
    bytes: ByteArray,
  ) {
    val expires = PFDateTime.now().plus(downloadExpiryMinutes.toLong(), ChronoUnit.MINUTES)
    val expiresTime = expires.format(DateFormatType.TIME_OF_DAY_MINUTES)
    val downloadFile = DownloadFile(filename, bytes, expiresTime)
    ExpiringSessionAttributes.setAttribute(
      request,
      expiringSessionAttribute, downloadFile,
      downloadExpiryMinutes
    )
  }
}
