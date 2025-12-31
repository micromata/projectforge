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

package org.projectforge.common.i18n

import java.lang.IllegalArgumentException

/**
 * I18n params are params for localized message which will be localized itself, if paramType == VALUE (default).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MessageParam {
  /**
   * @return The value for the localized message.
   */
  val value: Any?
  private val paramType: MessageParamType

  /**
   */
  constructor(value: Any?) {
    this.value = value
    paramType = MessageParamType.VALUE
  }

  /**
   */
  constructor(value: I18nEnum) {
    this.value = value.i18nKey
    paramType = MessageParamType.I18N_KEY
  }

  /**
   * Will be interpreted as value.
   */
  constructor(value: String?) {
    this.value = value
    paramType = MessageParamType.VALUE
  }

  /**
   * @value Value or i18n key, if paramType = I18N_KEY
   * @paramType
   */
  constructor(value: String?, paramType: MessageParamType) {
    this.value = value
    this.paramType = paramType
  }

  /**
   * @return The key for the localized message.
   * @throws IllegalArgumentException if paramType is not I18N_KEY or the value is not an instance of java.lang.String.
   */
  val i18nKey: String?
    get() {
      if (isI18nKey()) {
        return value as String?
      }
      throw IllegalArgumentException(
        "getI18nKey is called, but paramType is not I18N_KEY or value is not an instance of java.lang.String"
      )
    }

  /**
   * @return True, if paramType is I18N_KEY and the value is an instance of java.lang.String
   */
  fun isI18nKey(): Boolean {
    return paramType == MessageParamType.I18N_KEY && value is String
  }

  override fun toString(): String {
    return value.toString()
  }
}
