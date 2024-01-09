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

package org.projectforge.ui

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

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

  /**
   * @param variables If given, variables will be given to action.
   * @param merge If given, action.merge will be set.
   * @param type If given, action.type will be set.
   */
  fun createToastResponseEntity(
    message: String,
    color: UIColor = UIColor.INFO,
    variables: MutableMap<String, Any>? = null,
    merge: Boolean? = null,
    targetType: TargetType? = null,
  ): ResponseEntity<ResponseAction> {
    return ResponseEntity(createToastResponseAction(message, color, variables, merge, targetType), HttpStatus.OK)
  }

  /**
   * @param variables If given, variables will be given to action.
   * @param merge If given, action.merge will be set.
   * @param type If given, action.type will be set.
   */
  fun createToastResponseAction(
    message: String,
    color: UIColor = UIColor.INFO,
    variables: MutableMap<String, Any>? = null,
    merge: Boolean? = null,
    targetType: TargetType? = null,
  ): ResponseAction {
    val action = createToast(message, color)
    variables?.let { vars ->
      action.addVariables(vars)
    }
    action.merge = merge
    targetType?.let {
      action.targetType = targetType
    }
    return action
  }

  fun createExceptionToast(ex: UserException): ResponseAction {
    return createToast(translateMsg(ex), color = UIColor.DANGER)
  }
}
