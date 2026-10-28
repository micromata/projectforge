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
    private val example = NumberRanges(listOf(internal, legacy, new), allowFreeNumbers = false)

    @Test
    fun `project numbers of all three ranges are computed and decomposed`() {
        assertEquals(412345, example.projectNumber(internal, null, 12345))
        assertEquals(512345, example.projectNumber(legacy, 123, 45))
        assertEquals(100011, example.projectNumber(new, 10001, 1))
        assertEquals(123456, example.projectNumber(new, 12345, 6))
        assertEquals(199999, example.projectNumber(new, 19999, 9))

        assertEquals(12345L, example.customerNumber(123456))
        assertEquals(6, example.localNumber(123456))
        assertEquals(123L, example.customerNumber(512345))
        assertEquals(45, example.localNumber(512345))
        assertNull(example.customerNumber(412345))
        assertEquals(12345, example.localNumber(412345))
        assertNull(example.customerNumber(712345), "Prefix 7 belongs to no range.")
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
        assertThrows<IllegalArgumentException> { example.projectNumber(new, 10001, 10) }
        assertThrows<IllegalArgumentException> { example.projectNumber(legacy, 123, 100) }
        assertThrows<IllegalArgumentException> { example.projectNumber(new, 999, 1) }
        assertThrows<IllegalArgumentException> { example.projectNumber(new, null, 1) }
        assertThrows<IllegalArgumentException> { example.projectNumber(internal, 0, 1) }
    }

    @Test
    fun `ranges are found by customer, prefix and project number`() {
        assertEquals(legacy, example.rangeForCustomer(0))
        assertEquals(legacy, example.rangeForCustomer(999))
        assertNull(example.rangeForCustomer(1000))
        assertEquals(new, example.rangeForCustomer(10000))
        assertNull(example.rangeForCustomer(20000))
        assertEquals(new, example.rangeForPrefix(1))
        assertNull(example.rangeForPrefix(6))
        assertEquals(internal, example.rangeForProjectNumber(400000))
        assertEquals(internal, example.rangeForProjectNumber(499999))
        assertNull(example.rangeForProjectNumber(42))
        assertEquals(internal, example.rangeWithoutCustomers)
    }

    @Test
    fun `kost2 numbers are formatted and parsed by the segments of their prefix`() {
        assertEquals("1.2345.6.78", example.formatKost2(12345678))
        assertEquals("5.123.45.67", example.formatKost2(51234567))
        assertEquals("4.123.45.67", example.formatKost2(41234567))
        assertEquals("6.000.10.00", example.formatKost2(60001000), "Default segments for prefixes without range.")
        assertEquals("1.0001.1.02", example.formatKost2(10001102))
        assertEquals("12345678", example.formatKost2(12345678, ""))
        assertEquals("1.2345.6.78", example.formatKost2(1, 234, 56, 78L))
        assertEquals("1.0001.1.--", example.formatKost2(1, 0, 11, null))
        assertEquals("5.123.45.--", example.formatKost2(5, 123, 45, null))
        assertEquals("5.012.03.04", example.formatKost2(5, 12, 3, 4L))
        assertEquals("51234567", example.formatKost2(5, 123, 45, 67L, ""))
        assertEquals("5.1234.45.67", example.formatKost2(5, 1234, 45, 67L), "Invalid values aren't split wrongly.")
        assertEquals("1.0001.1", example.formatProjectNumber(100011))
        assertEquals("5.123.45", example.formatProjectNumber(512345))

        // Positional storage format of Kost2DO: 1 / 3 / 2 / 2 digits.
        assertArrayEquals(intArrayOf(1, 234, 56, 78), example.parseKost2("1.2345.6.78"))
        assertArrayEquals(intArrayOf(1, 234, 56, 78), example.parseKost2("12345678"))
        assertArrayEquals(intArrayOf(5, 123, 45, 67), example.parseKost2(" 5.123.45.67 "))
        assertArrayEquals(intArrayOf(6, 0, 10, 0), example.parseKost2("6.000.10.00"))
        assertNull(example.parseKost2("1.234.56.78"), "Old segmentation isn't accepted for range 1.")
        assertNull(example.parseKost2("5.1234.5.67"))
        assertNull(example.parseKost2("5.123.45"))
        assertNull(example.parseKost2("1234567"))
        assertNull(example.parseKost2("5.12a.45.67"))
        assertNull(example.parseKost2(null))

        assertArrayEquals(intArrayOf(1, 234, 56), example.parseKost2Prefix("1.2345.6"))
        assertArrayEquals(intArrayOf(1, 0, 73), example.parseKost2Prefix("1.7.3"), "Leading zeros may be left out.")
        assertArrayEquals(intArrayOf(5, 999, 10), example.parseKost2Prefix(" 5.999.10 "))
        assertArrayEquals(intArrayOf(6, 0, 10), example.parseKost2Prefix("6.000.10"))
        assertArrayEquals(intArrayOf(5, 123, 45), example.parseKost2Prefix("512345"))
        assertNull(example.parseKost2Prefix("1.234.56"), "Old segmentation isn't accepted for range 1.")
        assertNull(example.parseKost2Prefix("5.1234.5"))
        assertNull(example.parseKost2Prefix("5.123"))
    }

    @Test
    fun `segments not configured follow the digits of the range`() {
        val range7 = range(prefix = 7, customerMin = 70001, customerMax = 79999, projectDigits = 1)
        assertEquals(listOf(1, 4, 1, 2), range7.segments)
        assertEquals(listOf(1, 3, 2, 2), legacy.segments)
        assertEquals(NumberRange.DEFAULT_SEGMENTS, internal.segments, "Range without customers.")
        assertEquals(listOf(1, 2, 3, 2), range(prefix = 5, customerMin = 0, customerMax = 99, projectDigits = 3).segments)
        val ranges = NumberRanges(listOf(internal, legacy, range7))
        assertEquals("7.0001.0", ranges.formatProjectNumber(700010))
        assertEquals("7.0001", ranges.formatCustomerNumber(70001))
        assertEquals("7.0001.0.02", ranges.formatKost2(70001002))
        // Configured segments win:
        assertEquals(listOf(1, 3, 2, 2), range(prefix = 7, customerMin = 70001, customerMax = 79999, projectDigits = 1,
            segments = listOf(1, 3, 2, 2)).segments)
    }

    @Test
    fun `numbers are validated against the ranges`() {
        assertNull(example.validateCustomerNumber(10001))
        assertNull(example.validateCustomerNumber(0))
        assertEquals(NumberRangeViolation.CUSTOMER_NOT_IN_RANGE, example.validateCustomerNumber(2000))
        assertEquals(NumberRangeViolation.INVALID_NUMBER, example.validateCustomerNumber(-1))
        assertEquals(NumberRangeViolation.PROJECT_NOT_IN_RANGE, example.validateFreeProjectNumber(42))

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
        assertEquals(3, example.nextFreeLocalNumber(new, listOf(0, 1, 2, 4)))
        assertNull(example.nextFreeLocalNumber(new, (0..9).toList()), "Only 10 projects per customer.")
        assertEquals(10, example.nextFreeLocalNumber(legacy, (0..9).toList()))
        // First proposed number, the lower ones once all higher ones are taken:
        assertEquals(1, example.nextFreeLocalNumber(legacy, emptyList(), start = 1))
        assertEquals(3, example.nextFreeLocalNumber(legacy, listOf(1, 2), start = 1))
        assertEquals(0, example.nextFreeLocalNumber(legacy, (1..99).toList(), start = 1))
        assertNull(example.nextFreeLocalNumber(legacy, (0..99).toList(), start = 1))
        assertEquals(9, example.nextFreeLocalNumber(new, emptyList(), start = 42), "Limited to the range.")
        assertEquals(10002L, example.nextFreeCustomerNumber(new, listOf(10000L, 10001L)))
        assertNull(example.nextFreeCustomerNumber(internal, emptyList()))
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
        assertEquals(example, NumberRangeService.select(example, legacyRanges) { true })
        assertEquals(legacyRanges, NumberRangeService.select(unconfigured, legacyRanges) { true })
        assertEquals(unconfigured, NumberRangeService.select(unconfigured, legacyRanges) { false })
        // Configured ranges don't ask for the former ones (no database query):
        NumberRangeService.select(example, legacyRanges) { throw AssertionError("Not to be asked.") }
    }

    private fun range(
        prefix: Int,
        customerMin: Long? = null,
        customerMax: Long? = null,
        projectDigits: Int = 2,
        segments: List<Int>? = null,
    ) = NumberRange().also {
        it.prefix = prefix
        it.customerMin = customerMin
        it.customerMax = customerMax
        it.projectDigits = projectDigits
        segments?.let { segments -> it.segments = segments }
    }
}
