package org.projectforge.rest.core

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.ValidationError

/**
 * This class provides some convenience methods for field validations.
 */

class Validation {
    companion object {
        fun validateInteger(validationErrors: MutableList<ValidationError>, fieldId: String, value: String, minValue: Int?, maxValue: Int?) {
            if (value.isNullOrBlank())
                return
            try {
                val number = Integer.parseInt(value)
                if (minValue != null) {
                    if (maxValue != null) {
                        if (number < minValue || number > maxValue)
                            validationErrors.add(ValidationError(translateMsg("validation.error.range.integerOutOfRange", minValue, maxValue), fieldId = fieldId))
                    } else if (number < minValue)
                        validationErrors.add(ValidationError(translateMsg("validation.error.range.integerToLow", minValue), fieldId = fieldId))
                } else if (maxValue != null && number > maxValue)
                    validationErrors.add(ValidationError(translateMsg("validation.error.range.integerToHigh", maxValue), fieldId = fieldId))
            } catch (ex: NumberFormatException) {
                validationErrors.add(ValidationError(translate("validation.error.format.integer"), fieldId = fieldId))
            }
        }
    }
}
