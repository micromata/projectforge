package org.projectforge.ui

import org.projectforge.framework.i18n.translate

data class ValidationError(var message: String? = null,
                           var fieldId: String? = null,
                           var messageId: String? = null) {
    companion object {
        fun create(i18nKey : String, fieldId : String? = null) : ValidationError {
            val validationError = ValidationError()
            validationError.fieldId = fieldId
            validationError.messageId = i18nKey
            validationError.message = translate(i18nKey)
            return validationError
        }
    }
}