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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.ui.ValidationError
import java.util.*

class ValidationTest {

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

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml.createForJunitTests()
            val user = PFUserDO()
            user.locale = Locale.GERMAN
            ThreadLocalUserContext.setUserContext(UserContext(user, null))
        }
    }
}
