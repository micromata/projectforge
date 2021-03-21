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

package org.projectforge.ui

import org.projectforge.rest.i18n.I18nUtils

/**
 * Helper for creating toast at client browser.
 */
object UIToast {
  fun createToast(message: String, color: UIColor = UIColor.INFO): ResponseAction {
    return ResponseAction(
      message = ResponseAction.Message(
        message = message,
        color = color
      ), targetType = TargetType.TOAST
    )
  }

  fun createMaxFileExceededToast(fileName: String?, fileSize: Long, maxFileSize: Long): ResponseAction {
    return createToast(I18nUtils.translateMaxSizeExceeded(fileName, fileSize, maxFileSize), color = UIColor.DANGER)
  }
}
