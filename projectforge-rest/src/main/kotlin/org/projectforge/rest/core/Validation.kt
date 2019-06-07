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

package org.projectforge.rest.core

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.ValidationError

/**
 * This class provides some convenience methods for field validations.
 */

class Validation {
    companion object {
        internal val MSG_INT_FORMAT_ERROR = "validation.error.format.integer"
        internal val MSG_INT_OUT_OF_RANGE = "validation.error.range.integerOutOfRange"
        internal val MSG_INT_TO_LOW = "validation.error.range.integerOutOfRange"
        internal val MSG_INT_TO_HIGH = "validation.error.range.integerOutOfRange"
        fun validateInteger(validationErrors: MutableList<ValidationError>,
                            fieldId: String,
                            value: String?,
                            minValue: Int? = null,
                            maxValue: Int? = null,
                            formatNumber: Boolean = true) {
            if (value.isNullOrBlank())
                return
            try {
                val number = Integer.parseInt(value)
                val minValueParam: Any? = if (formatNumber) minValue else minValue?.toString()
                val maxValueParam: Any? = if (formatNumber) maxValue else maxValue?.toString()
                if (minValue != null) {
                    if (maxValue != null) {
                        if (number < minValue || number > maxValue)
                            validationErrors.add(ValidationError(translateMsg(MSG_INT_OUT_OF_RANGE, minValueParam!!, maxValueParam!!),
                                    fieldId = fieldId,
                                    messageId = MSG_INT_OUT_OF_RANGE))
                    } else if (number < minValue)
                        validationErrors.add(ValidationError(translateMsg(MSG_INT_TO_LOW, minValueParam!!),
                                fieldId = fieldId,
                                messageId = MSG_INT_TO_LOW))
                } else if (maxValue != null && number > maxValue)
                    validationErrors.add(ValidationError(translateMsg(MSG_INT_TO_HIGH, maxValueParam!!),
                            fieldId = fieldId,
                            messageId = MSG_INT_TO_HIGH))
            } catch (ex: NumberFormatException) {
                validationErrors.add(ValidationError(translate(MSG_INT_FORMAT_ERROR),
                        fieldId = fieldId,
                        messageId = MSG_INT_FORMAT_ERROR))
            }
        }
    }
}
