/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

fun translate(i18nKey: String?): String {
    if (i18nKey == null) return "???"
    return I18nHelper.getLocalizedMessage(i18nKey)
}

fun translateMsg(ex: UserException): String {
    if (!ex.msgParams.isNullOrEmpty()) {
        return I18nHelper.getLocalizedMessage(ex.i18nKey, *ex.msgParams)
    } else if (!ex.params.isNullOrEmpty()) {
        return I18nHelper.getLocalizedMessage(ex.i18nKey, *ex.params)
    }
    return translate(ex.i18nKey)
}

fun translateMsg(i18nKey: String, vararg params: Any): String {
    return I18nHelper.getLocalizedMessage(i18nKey, *params)
}

/**
 * If the given string starts with ', the title without ' is returned, otherwise [translate] will be called.
 */
fun autoTranslate(text: String?): String {
    if (text == null) return "???"
    if (text.startsWith("'") == true)
        return text.substring(1)
    return translate(text)
}

fun addTranslations(vararg i18nKeys: String, translations: MutableMap<String, String> = mutableMapOf()): MutableMap<String, String> {
    i18nKeys.forEach {
        translations.put(it, translate(it))
    }
    return translations
}
