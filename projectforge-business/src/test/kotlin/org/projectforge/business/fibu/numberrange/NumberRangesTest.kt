/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.numberrange

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NumberRangesTest {
    private val internal = range(prefix = 4, projectDigits = 5)
    private val legacy = range(prefix = 5, customerMin = 0, customerMax = 999, projectDigits = 2)
    private val new = range(prefix = 1, customerMin = 10000, customerMax = 19999, projectDigits = 1, segments = listOf(1, 4, 1, 2))
    private val micromata = NumberRanges(listOf(internal, legacy, new), allowFreeNumbers = false)

    @Test
    fun `project numbers of all three ranges are computed and decomposed`() {
        assertEquals(412345, micromata.projectNumber(internal, null, 12345))
        assertEquals(512345, micromata.projectNumber(legacy, 123, 45))
        assertEquals(100011, micromata.projectNumber(new, 10001, 1))
        assertEquals(123456, micromata.projectNumber(new, 12345, 6))
        assertEquals(199999, micromata.projectNumber(new, 19999, 9))

        assertEquals(12345L, micromata.customerNumber(123456))
        assertEquals(6, micromata.localNumber(123456))
        assertEquals(123L, micromata.customerNumber(512345))
        assertEquals(45, micromata.localNumber(512345))
        assertNull(micromata.customerNumber(412345))
        assertEquals(12345, micromata.localNumber(412345))
        assertNull(micromata.customerNumber(712345), "Prefix 7 belongs to no range.")
    }

    @Test
    fun `the customer number itself is part of the project number, whatever customer-min is`() {
        val from10001 = range(prefix = 1, customerMin = 10001, customerMax = 19999, projectDigits = 1)
        val from1 = range(prefix = 5, customerMin = 1, customerMax = 999, projectDigits = 2)
        val ranges = NumberRanges(listOf(from10001, from1))
        assertEquals(100013, ranges.projectNumber(from10001, 10001, 3), "1.0001.3, not 1.0000.3.")
        assertEquals(512345, ranges.projectNumber(from1, 123, 45), "5.123.45, not 5.122.45.")
        assertEquals(10001L, ranges.customerNumber(100013))
        assertEquals(123L, ranges.customerNumber(512345))
        assertNull(ranges.customerNumber(100003), "Customer 10000 is not of the range.")
        assertNull(ranges.customerNumber(500045), "Customer 0 is not of the range.")
    }

    @Test
    fun `customers of a range must share the digits in front of their part`() {
        // With 1 project digit the last 4 digits are the customer's part: 9000-10999 would give 9000 and 10999 the
        // parts 9000 and 0999, but 10000 and 0 the same one.
        assertThrows<IllegalArgumentException> {
            NumberRanges(listOf(range(prefix = 1, customerMin = 9000, customerMax = 10999, projectDigits = 1)))
        }
        NumberRanges(listOf(range(prefix = 1, customerMin = 10001, customerMax = 19999, projectDigits = 1)))
    }

    @Test
    fun `local numbers and customers outside the range are rejected`() {
        assertThrows<IllegalArgumentException> { micromata.projectNumber(new, 10001, 10) }
        assertThrows<IllegalArgumentException> { micromata.projectNumber(legacy, 123, 100) }
        assertThrows<IllegalArgumentException> { micromata.projectNumber(new, 999, 1) }
        assertThrows<IllegalArgumentException> { micromata.projectNumber(new, null, 1) }
        assertThrows<IllegalArgumentException> { micromata.projectNumber(internal, 0, 1) }
    }

    @Test
    fun `ranges are found by customer, prefix and project number`() {
        assertEquals(legacy, micromata.rangeForCustomer(0))
        assertEquals(legacy, micromata.rangeForCustomer(999))
        assertNull(micromata.rangeForCustomer(1000))
        assertEquals(new, micromata.rangeForCustomer(10000))
        assertNull(micromata.rangeForCustomer(20000))
        assertEquals(new, micromata.rangeForPrefix(1))
        assertNull(micromata.rangeForPrefix(6))
        assertEquals(internal, micromata.rangeForProjectNumber(400000))
        assertEquals(internal, micromata.rangeForProjectNumber(499999))
        assertNull(micromata.rangeForProjectNumber(42))
        assertEquals(internal, micromata.rangeWithoutCustomers)
    }

    @Test
    fun `kost2 numbers are formatted and parsed by the segments of their prefix`() {
        assertEquals("1.2345.6.78", micromata.formatKost2(12345678))
        assertEquals("5.123.45.67", micromata.formatKost2(51234567))
        assertEquals("4.123.45.67", micromata.formatKost2(41234567))
        assertEquals("6.000.10.00", micromata.formatKost2(60001000), "Default segments for prefixes without range.")
        assertEquals("1.0001.1.02", micromata.formatKost2(10001102))
        assertEquals("12345678", micromata.formatKost2(12345678, ""))
        assertEquals("1.2345.6.78", micromata.formatKost2(1, 234, 56, 78L))
        assertEquals("1.0001.1.--", micromata.formatKost2(1, 0, 11, null))
        assertEquals("5.123.45.--", micromata.formatKost2(5, 123, 45, null))
        assertEquals("5.012.03.04", micromata.formatKost2(5, 12, 3, 4L))
        assertEquals("51234567", micromata.formatKost2(5, 123, 45, 67L, ""))
        assertEquals("5.1234.45.67", micromata.formatKost2(5, 1234, 45, 67L), "Invalid values aren't split wrongly.")
        assertEquals("1.0001.1", micromata.formatProjectNumber(100011))
        assertEquals("5.123.45", micromata.formatProjectNumber(512345))

        // Positional storage format of Kost2DO: 1 / 3 / 2 / 2 digits.
        assertArrayEquals(intArrayOf(1, 234, 56, 78), micromata.parseKost2("1.2345.6.78"))
        assertArrayEquals(intArrayOf(1, 234, 56, 78), micromata.parseKost2("12345678"))
        assertArrayEquals(intArrayOf(5, 123, 45, 67), micromata.parseKost2(" 5.123.45.67 "))
        assertArrayEquals(intArrayOf(6, 0, 10, 0), micromata.parseKost2("6.000.10.00"))
        assertNull(micromata.parseKost2("1.234.56.78"), "Old segmentation isn't accepted for range 1.")
        assertNull(micromata.parseKost2("5.1234.5.67"))
        assertNull(micromata.parseKost2("5.123.45"))
        assertNull(micromata.parseKost2("1234567"))
        assertNull(micromata.parseKost2("5.12a.45.67"))
        assertNull(micromata.parseKost2(null))

        assertArrayEquals(intArrayOf(1, 234, 56), micromata.parseKost2Prefix("1.2345.6"))
        assertArrayEquals(intArrayOf(1, 0, 73), micromata.parseKost2Prefix("1.7.3"), "Leading zeros may be left out.")
        assertArrayEquals(intArrayOf(5, 999, 10), micromata.parseKost2Prefix(" 5.999.10 "))
        assertArrayEquals(intArrayOf(6, 0, 10), micromata.parseKost2Prefix("6.000.10"))
        assertArrayEquals(intArrayOf(5, 123, 45), micromata.parseKost2Prefix("512345"))
        assertNull(micromata.parseKost2Prefix("1.234.56"), "Old segmentation isn't accepted for range 1.")
        assertNull(micromata.parseKost2Prefix("5.1234.5"))
        assertNull(micromata.parseKost2Prefix("5.123"))
    }

    @Test
    fun `numbers are validated against the ranges`() {
        assertNull(micromata.validateCustomerNumber(10001))
        assertNull(micromata.validateCustomerNumber(0))
        assertEquals(NumberRangeViolation.CUSTOMER_NOT_IN_RANGE, micromata.validateCustomerNumber(2000))
        assertEquals(NumberRangeViolation.INVALID_NUMBER, micromata.validateCustomerNumber(-1))
        assertEquals(NumberRangeViolation.PROJECT_NOT_IN_RANGE, micromata.validateFreeProjectNumber(42))

        val withFree = NumberRanges(listOf(internal, legacy, new), allowFreeNumbers = true)
        assertNull(withFree.validateCustomerNumber(2000))
        assertNull(withFree.validateFreeProjectNumber(42))
        assertNull(withFree.validateFreeProjectNumber(712345))
        assertEquals(NumberRangeViolation.FREE_NUMBER_IN_RANGE, withFree.validateFreeProjectNumber(512345))
        assertEquals(NumberRangeViolation.INVALID_NUMBER, withFree.validateFreeProjectNumber(0))
    }

    @Test
    fun `without configured ranges all numbers are free`() {
        val none = NumberRanges(emptyList(), allowFreeNumbers = false)
        assertFalse(none.isConfigured)
        assertTrue(none.allowFreeNumbers)
        assertNull(none.validateCustomerNumber(4711))
        assertNull(none.validateFreeProjectNumber(512345))
        assertNull(none.rangeForCustomer(123))
        assertEquals("5.123.45.67", none.formatKost2(51234567))
        assertArrayEquals(intArrayOf(5, 123, 45, 67), none.parseKost2("5.123.45.67"))
    }

    @Test
    fun `next free numbers respect the size of the range`() {
        assertEquals(3, micromata.nextFreeLocalNumber(new, listOf(0, 1, 2, 4)))
        assertNull(micromata.nextFreeLocalNumber(new, (0..9).toList()), "Only 10 projects per customer.")
        assertEquals(10, micromata.nextFreeLocalNumber(legacy, (0..9).toList()))
        assertEquals(10002L, micromata.nextFreeCustomerNumber(new, listOf(10000L, 10001L)))
        assertNull(micromata.nextFreeCustomerNumber(internal, emptyList()))
    }

    @Test
    fun `inconsistent configurations are rejected`() {
        assertThrows<IllegalArgumentException> { NumberRanges(listOf(legacy, range(prefix = 5, projectDigits = 5))) }
        assertThrows<IllegalArgumentException> {
            NumberRanges(listOf(legacy, range(prefix = 1, customerMin = 900, customerMax = 1999, projectDigits = 1)))
        }
        // 10000 customers with 2 project digits don't fit into 5 digits:
        assertThrows<IllegalArgumentException> {
            NumberRanges(listOf(range(prefix = 1, customerMin = 10000, customerMax = 19999, projectDigits = 2)))
        }
        assertThrows<IllegalArgumentException> { NumberRanges(listOf(range(prefix = 0, projectDigits = 5))) }
        assertThrows<IllegalArgumentException> { NumberRanges(listOf(range(prefix = 4, projectDigits = 2))) }
        assertThrows<IllegalArgumentException> {
            NumberRanges(listOf(range(prefix = 1, customerMin = 0, customerMax = 999, segments = listOf(1, 3, 2, 3))))
        }
        assertThrows<IllegalArgumentException> {
            NumberRanges(listOf(range(prefix = 1, customerMin = 0, customerMax = null)))
        }
    }

    @Test
    fun `legacy ranges map the former Kost2 layout 4 and 5`() {
        val legacyRanges = NumberRanges(NumberRanges.legacyRanges())
        assertEquals(listOf(4, 5), legacyRanges.ranges.map { it.prefix })
        assertEquals(512345, legacyRanges.projectNumber(legacyRanges.rangeForCustomer(123)!!, 123, 45))
        assertEquals(412345, legacyRanges.projectNumber(legacyRanges.rangeForPrefix(4)!!, null, 12345))
        assertNull(legacyRanges.rangeForCustomer(1000), "Customers above 999 have free numbers.")
        assertEquals("5.123.45.67", legacyRanges.formatKost2(51234567))
        assertEquals("4.123.45.67", legacyRanges.formatKost2(41234567))
    }

    @Test
    fun `the former ranges apply only without configured ranges and only if in use`() {
        val legacyRanges = NumberRanges(NumberRanges.legacyRanges())
        val unconfigured = NumberRanges(emptyList())
        assertEquals(micromata, NumberRangeService.select(micromata, legacyRanges) { true })
        assertEquals(legacyRanges, NumberRangeService.select(unconfigured, legacyRanges) { true })
        assertEquals(unconfigured, NumberRangeService.select(unconfigured, legacyRanges) { false })
        // Configured ranges don't ask for the former ones (no database query):
        NumberRangeService.select(micromata, legacyRanges) { throw AssertionError("Not to be asked.") }
    }

    private fun range(
        prefix: Int,
        customerMin: Long? = null,
        customerMax: Long? = null,
        projectDigits: Int = 2,
        segments: List<Int> = NumberRange.DEFAULT_SEGMENTS,
    ) = NumberRange().also {
        it.prefix = prefix
        it.customerMin = customerMin
        it.customerMax = customerMax
        it.projectDigits = projectDigits
        it.segments = segments
    }
}
