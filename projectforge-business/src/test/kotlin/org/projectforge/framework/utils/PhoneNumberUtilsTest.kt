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

package org.projectforge.framework.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PhoneNumberUtilsTest {

    @Test
    fun `test normalize German phone numbers with various formats`() {
        // National format with leading 0
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("0561 / 316793-0"))
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("0561 316793-0"))
        assertEquals("+49 561316793-0", PhoneNumberUtils.normalizePhoneNumber("0561/316793-0")) // No space before slash
        assertEquals("+49 5613167930", PhoneNumberUtils.normalizePhoneNumber("05613167930"))

        // With (0) notation
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("+49 (0) 561 / 316793-0"))
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("+49 (0) 561 316793-0"))

        // Already normalized
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("+49 561 316793-0"))

        // Mobile numbers
        assertEquals("+49 170 1234567", PhoneNumberUtils.normalizePhoneNumber("0170 1234567"))
        assertEquals("+49 170 1234567", PhoneNumberUtils.normalizePhoneNumber("+49 170 1234567"))
        assertEquals("+49 151 12345678", PhoneNumberUtils.normalizePhoneNumber("0151 / 12345678"))

        // Different area codes - no splitting, just cleaned
        assertEquals("+49 30 12345-678", PhoneNumberUtils.normalizePhoneNumber("030 12345-678"))
        assertEquals("+49 89 12345678", PhoneNumberUtils.normalizePhoneNumber("089 12345678"))
        assertEquals("+49 221 123456", PhoneNumberUtils.normalizePhoneNumber("0221 123456"))
    }

    @Test
    fun `test normalize international phone numbers`() {
        // UK numbers - just cleaned format
        assertEquals("+44 20 12345678", PhoneNumberUtils.normalizePhoneNumber("+44 20 12345678"))
        assertEquals("+44 20 12345678", PhoneNumberUtils.normalizePhoneNumber("0044 20 12345678"))

        // US numbers (extension detected due to hyphen)
        assertEquals("+1 415 555-1234", PhoneNumberUtils.normalizePhoneNumber("+1 415 555-1234"))
        assertEquals("+1 415 555 1234", PhoneNumberUtils.normalizePhoneNumber("001 415 555 1234")) // Spaces preserved

        // Swiss numbers
        assertEquals("+41 44 1234567", PhoneNumberUtils.normalizePhoneNumber("+41 44 1234567"))
        assertEquals("+41 44 1234567", PhoneNumberUtils.normalizePhoneNumber("0041 44 1234567"))

        // Russia
        assertEquals("+7 495 1234567", PhoneNumberUtils.normalizePhoneNumber("+7 495 1234567"))
    }

    @Test
    fun `test normalize with extensions`() {
        assertEquals("+49 561 12345-0", PhoneNumberUtils.normalizePhoneNumber("0561 12345-0"))
        assertEquals("+49 561 12345-78", PhoneNumberUtils.normalizePhoneNumber("0561 12345-78"))
        assertEquals("+49 561 12345-789", PhoneNumberUtils.normalizePhoneNumber("0561 12345-789"))
        assertEquals("+49 30 123456-1", PhoneNumberUtils.normalizePhoneNumber("+49 30 123456-1"))
    }

    @Test
    fun `test normalize with special characters and formatting`() {
        // Dots - treated as separator between number blocks
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("0561.316793-0"))

        // Multiple spaces
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("0561   316793 - 0"))

        // Parentheses - leading 0 inside parentheses is not removed (country extraction happens first)
        assertEquals("+49 0561 316793-0", PhoneNumberUtils.normalizePhoneNumber("(0561) 316793-0"))

        // Mixed formatting
        assertEquals("+49 561 316793-0", PhoneNumberUtils.normalizePhoneNumber("+49 / 561 / 316793 - 0"))
    }

    @Test
    fun `test normalize with null and empty inputs`() {
        assertNull(PhoneNumberUtils.normalizePhoneNumber(null))
        assertNull(PhoneNumberUtils.normalizePhoneNumber(""))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("  "))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("   \t  "))
    }

    @Test
    fun `test normalize with invalid inputs`() {
        // Only non-digit characters
        assertNull(PhoneNumberUtils.normalizePhoneNumber("abc"))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("---"))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("( )"))
    }

    @Test
    fun `test normalize with custom country prefix`() {
        assertEquals("+1 561 3167930", PhoneNumberUtils.normalizePhoneNumber("0561 3167930", "+1"))
        assertEquals("+44 561 3167930", PhoneNumberUtils.normalizePhoneNumber("0561 3167930", "+44"))
        assertEquals("+41 561 3167930", PhoneNumberUtils.normalizePhoneNumber("0561 3167930", "+41"))
    }

    @Test
    fun `test phone numbers match`() {
        // Same number, different formats
        assertTrue(PhoneNumberUtils.phoneNumbersMatch("0561 316793-0", "+49 561 316793-0"))
        assertTrue(PhoneNumberUtils.phoneNumbersMatch("+49 561 / 316793-0", "+49 561 316793-0"))
        assertTrue(PhoneNumberUtils.phoneNumbersMatch("0561 316793-0", "0561 316793-0")) // Same format
        assertTrue(PhoneNumberUtils.phoneNumbersMatch("+49 (0) 561 316793-0", "0561 316793-0"))

        // Different numbers
        assertFalse(PhoneNumberUtils.phoneNumbersMatch("0561 316793-0", "0561 316793-1"))
        assertFalse(PhoneNumberUtils.phoneNumbersMatch("0561 316793", "0562 316793"))

        // Null cases
        assertFalse(PhoneNumberUtils.phoneNumbersMatch(null, "+49 561 316793-0"))
        assertFalse(PhoneNumberUtils.phoneNumbersMatch("+49 561 316793-0", null))
        assertFalse(PhoneNumberUtils.phoneNumbersMatch(null, null))
    }

    @Test
    fun `test real world examples`() {
        // From email signatures - spaces normalized
        assertEquals("+49 561 31 67 93-0", PhoneNumberUtils.normalizePhoneNumber("Tel: +49 561 / 31 67 93 - 0"))
        assertEquals("+49 561 31 67 93-11", PhoneNumberUtils.normalizePhoneNumber("Fax: +49 561 / 31 67 93- 11"))
        assertEquals("+49 561 123456-789", PhoneNumberUtils.normalizePhoneNumber("Phone: +49 561 123456-789"))

        // From address book
        assertEquals("+49 170 1234567", PhoneNumberUtils.normalizePhoneNumber("Mobil: +49 170 1234567"))
        assertEquals("+49 30 98765432", PhoneNumberUtils.normalizePhoneNumber("Tel: +49 30 98765432"))

        // Short numbers
        assertEquals("+49 89 12345", PhoneNumberUtils.normalizePhoneNumber("089 12345"))
    }

    @Test
    fun `test edge cases with area codes`() {
        // Note: The function preserves the formatting after country code extraction
        // No automatic area code detection/splitting

        // Various area code lengths - just cleaned
        assertEquals("+49 561 123456", PhoneNumberUtils.normalizePhoneNumber("0561 123456"))
        assertEquals("+49 6221 123456", PhoneNumberUtils.normalizePhoneNumber("06221 123456"))
        assertEquals("+49 170 1234567", PhoneNumberUtils.normalizePhoneNumber("0170 1234567"))
    }
}
