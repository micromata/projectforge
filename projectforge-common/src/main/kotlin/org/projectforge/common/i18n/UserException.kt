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

package org.projectforge.common.i18n

import org.apache.commons.lang3.builder.ToStringBuilder
import org.projectforge.common.ProjectForgeException
import java.util.*

/**
 * This Exception will be thrown by the application and the message should be displayed.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class UserException(val i18nKey: String) : ProjectForgeException(i18nKey) {

  /**
   * @return The params for the localized message if exist, otherwise null.
   */
  var params: Array<out Any?>? = null
    protected set

  /**
   * The i18n params if set.
   */
  var msgParams: Array<out MessageParam>? = null
    protected set

  /**
   * @return Name of the causedByField, the exception is caused by, if any.
   */
  var causedByField: String? = null
    protected set

  /**
   * This message will be logged for giving more information for the admins.
   */
  var logHintMessage: String? = null

  /**
   * If true, the user will get a message, otherwise an exception is returned.
   * For React: if true, the user will get a Toast-message with http status ok, if false, a bad request is returned.
   * Used by GlobalDefaultExceptionHandlung
   */
  var displayUserMessage: Boolean = true

  /**
   * @param i18nKey Key for the localized message.
   * @param params Params, if message has params.
   */
  constructor(i18nKey: String, vararg params: Any?) : this(i18nKey) {
    this.params = params
  }

  /**
   * @param i18nKey Key for the localized message.
   * @param msgParams Params, if message has params.
   */
  constructor(i18nKey: String, vararg msgParams: MessageParam) : this(i18nKey) {
    this.msgParams = msgParams
  }

  fun setCausedByField(causedByField: String?): UserException {
    this.causedByField = causedByField
    return this
  }

  /**
   * @param bundle
   * @return The params for the localized message if exist (prepared for using with MessageFormat), otherwise params
   * will be returned.
   */
  fun getParams(bundle: ResourceBundle): Array<out Any?>? {
    if (msgParams == null) {
      return params
    }
    val args = arrayOfNulls<Any>(msgParams!!.size)
    for (i in msgParams!!.indices) {
      if (msgParams!![i].isI18nKey()) {
        args[i] = bundle.getString(msgParams!![i].i18nKey)
      } else {
        args[i] = msgParams!![i]
      }
    }
    return args
  }

  override fun toString(): String {
    val builder = ToStringBuilder(this)
    builder.append("i18nKey", i18nKey)
    if (params != null) {
      builder.append("params", params)
    }
    if (msgParams != null) {
      builder.append("msgParams", msgParams)
    }
    return builder.toString()
  }

  companion object {
    private const val serialVersionUID = 6829353575475092038L
    const val I18N_KEY_FLOWSCOPE_NOT_EXIST = "exception.flowscope.notExists"
    const val I18N_KEY_PLEASE_CONTACT_DEVELOPER_TEAM = "exception.pleaseContactDeveloperTeam"
  }
}
