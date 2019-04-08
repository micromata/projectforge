package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.test.AbstractTestBase
import org.projectforge.ui.ValidationError

class ValidationTest : AbstractTestBase() {

    @Test
    fun validationTest() {
        assertNull(validateInteger("field", "42", 0, 100)?.messageId)
        assertNull(validateInteger("field", "0", 0, 100)?.messageId)
        assertNull(validateInteger("field", "100", 0, 100)?.messageId)
        assertNull(validateInteger("field", "-42", -42, 100)?.messageId)
        assertEquals(Validation.MSG_INT_FORMAT_ERROR, validateInteger("field", "ignore", 0, 100)?.messageId)
        assertEquals(Validation.MSG_INT_OUT_OF_RANGE, validateInteger("field", "-42", 0, 100)?.messageId)
        assertEquals(Validation.MSG_INT_OUT_OF_RANGE, validateInteger("field", "-1", 0, 100)?.messageId)
        assertEquals(Validation.MSG_INT_OUT_OF_RANGE, validateInteger("field", "101", 0, 100)?.messageId)
        assertEquals(Validation.MSG_INT_TO_LOW, validateInteger("field", "-1", 0)?.messageId)
        assertEquals(Validation.MSG_INT_TO_HIGH, validateInteger("field", "101", maxValue = 100)?.messageId)
        assertEquals(Validation.MSG_INT_TO_HIGH, validateInteger("field", "-1", maxValue = -10)?.messageId)
    }

    private fun validateInteger(field: String, value: String, minValue: Int? = null, maxValue: Int? = null)
            : ValidationError? {
        val validationErrors = mutableListOf<ValidationError>()
        Validation.validateInteger(validationErrors, field, value, minValue = minValue, maxValue = maxValue)
        if (validationErrors.size > 0)
            return validationErrors[0]
        return null
    }
}