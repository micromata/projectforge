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

package org.projectforge.ui

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg

data class ValidationError(var message: String? = null,
                           var fieldId: String? = null,
                           var messageId: String? = null) {
    companion object {
        fun create(i18nKey: String, fieldId: String? = null): ValidationError {
            val validationError = ValidationError()
            validationError.fieldId = fieldId
            validationError.messageId = i18nKey
            validationError.message = translate(i18nKey)
            return validationError
        }

        fun createFieldRequired(clazz: Class<*>, fieldId: String): ValidationError {
            val i18nKey = "validation.error.fieldRequired"
            val fieldI18nKey = ElementsRegistry.getElementInfo(clazz, fieldId)?.i18nKey
            val fieldName = if (fieldI18nKey != null) translate(fieldI18nKey) else fieldId
            val validationError = ValidationError()
            validationError.fieldId = fieldId
            validationError.messageId = i18nKey
            validationError.message = translateMsg(i18nKey, fieldName)
            return validationError
        }
    }
}
