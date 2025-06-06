/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.i18n

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.i18n.UserException
import java.util.*

fun translate(locale: Locale?, i18nKey: String?): String {
  if (i18nKey == null) return "???"
  if (i18nKey.startsWith("'")) {
    return i18nKey.substring(1)
  }
  return I18nHelper.getLocalizedMessage(locale, i18nKey)
}

fun translate(i18nKey: String?): String {
  return translate(null, i18nKey)
}

/**
 * @return The translated key if given and doesn't start with "'", otherwise the i18nKey itself is returned.
 */
fun translateIfKey(i18nKey: String?): String? {
  i18nKey ?: return null
  if (i18nKey.startsWith("'")) {
    return i18nKey.substring(1)
  }
  return translate(null, i18nKey)
}

fun translateMsg(ex: UserException): String {
  ex.msgParams?.let {
    if (!it.isNullOrEmpty()) {
      return I18nHelper.getLocalizedMessage(ex.i18nKey, *it)
    }
  }
  ex.params?.let {
    if (!it.isNullOrEmpty()) {
      return I18nHelper.getLocalizedMessage(ex.i18nKey, *it)
    }
  }
  return translate(ex.i18nKey)
}

/**
 * Translates true values to 'yes' and false/null values to 'no'. (Using the user's language.)
 */
fun translate(value: Boolean?): String {
  return translate(if (value == true) "yes" else "no")
}

/**
 * Translates true values to 'yes' and false/null values to 'no'. (Using the user's language.)
 */
fun translate(value: I18nEnum?): String {
  value ?: return ""
  return translate(value.i18nKey)
}

fun translateMsg(i18nKey: String, vararg params: Any?): String {
  return I18nHelper.getLocalizedMessage(i18nKey, *params)
}

fun translateMsg(locale: Locale?, i18nKey: String, vararg params: Any?): String {
  return I18nHelper.getLocalizedMessage(locale, i18nKey, *params)
}

/**
 * If the given string starts with ', the title without ' is returned, otherwise [translate] will be called.
 */
fun autoTranslate(text: String?): String {
  if (text == null) return "???"
  if (text.startsWith("'"))
    return text.substring(1)
  return translate(text)
}

fun addTranslations(
  vararg i18nKeys: String,
  translations: MutableMap<String, String> = mutableMapOf()
): MutableMap<String, String> {
  i18nKeys.forEach {
    translations[it] = translate(it)
  }
  return translations
}
