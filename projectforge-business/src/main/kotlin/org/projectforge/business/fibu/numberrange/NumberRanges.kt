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

import org.projectforge.business.fibu.numberrange.NumberRange.Companion.KOST2_DIGITS
import org.projectforge.business.fibu.numberrange.NumberRange.Companion.PROJECT_NUMBER_DIGITS
import org.projectforge.business.fibu.numberrange.NumberRange.Companion.PROJECT_PREFIX_FACTOR
import org.projectforge.business.fibu.numberrange.NumberRange.Companion.pow10

/**
 * The validated set of configured number ranges with the whole arithmetic of customer, project and Kost2 numbers.
 * Pure logic without Spring, used by [NumberRangeService].
 *
 * Kost2 numbers are returned by [parseKost2] in the positional storage format of Kost2DO (nummernkreis = digit 1,
 * bereich = digits 2-4, teilbereich = digits 5-6, kost2Art = digits 7-8), independent of the segments of the range.
 *
 * @throws IllegalArgumentException if the configuration is inconsistent.
 */
class NumberRanges(
    ranges: List<NumberRange>,
    allowFreeNumbers: Boolean = true,
    val defaultSegments: List<Int> = NumberRange.DEFAULT_SEGMENTS,
) {
    val ranges: List<NumberRange> = ranges.sortedBy { it.prefix }

    /** Free numbers are always allowed if no range is configured. */
    val allowFreeNumbers: Boolean = allowFreeNumbers || ranges.isEmpty()

    val isConfigured: Boolean
        get() = ranges.isNotEmpty()

    init {
        validateSegments(defaultSegments, "default-segments")
        this.ranges.forEach { validate(it) }
        this.ranges.groupBy { it.prefix }.filter { it.value.size > 1 }.keys.firstOrNull()?.let {
            throw IllegalArgumentException("Number ranges: prefix $it is used more than once.")
        }
        val withCustomers = this.ranges.filter { it.hasCustomers }
        withCustomers.forEachIndexed { i, a ->
            withCustomers.drop(i + 1).forEach { b ->
                if (a.customerMin!! <= b.customerMax!! && b.customerMin!! <= a.customerMax!!) {
                    throw IllegalArgumentException("Number ranges: customer numbers of $a and $b overlap.")
                }
            }
        }
    }

    fun rangeForPrefix(prefix: Int?): NumberRange? = ranges.find { it.prefix == prefix }

    fun rangeForCustomer(customerNumber: Long?): NumberRange? {
        customerNumber ?: return null
        return ranges.find { it.containsCustomer(customerNumber) }
    }

    fun rangeForProjectNumber(projectNumber: Int?): NumberRange? {
        projectNumber ?: return null
        return ranges.find { it.containsProjectNumber(projectNumber) }
    }

    /** The range for projects without customer, if exactly one is configured (e.g. internal projects). */
    val rangeWithoutCustomers: NumberRange?
        get() = ranges.filter { !it.hasCustomers }.singleOrNull()

    /**
     * @param customerNumber Required for a range with customers, must be null otherwise.
     * @throws IllegalArgumentException if the numbers don't fit the range.
     */
    fun projectNumber(range: NumberRange, customerNumber: Long?, localNumber: Int): Int {
        require(localNumber in 0..range.maxLocalNumber) {
            "Local project number $localNumber not in 0..${range.maxLocalNumber} of $range."
        }
        if (!range.hasCustomers) {
            require(customerNumber == null) { "Range $range has no customers, but customer $customerNumber given." }
            return range.firstProjectNumber + localNumber
        }
        require(customerNumber != null && range.containsCustomer(customerNumber)) {
            "Customer $customerNumber not in $range."
        }
        val customerOffset = (customerNumber - range.customerMin!!).toInt()
        return range.firstProjectNumber + customerOffset * pow10(range.projectDigits) + localNumber
    }

    /** Customer number encoded in the given project number, null for free numbers and ranges without customers. */
    fun customerNumber(projectNumber: Int): Long? {
        val range = rangeForProjectNumber(projectNumber) ?: return null
        val min = range.customerMin ?: return null
        val customer = min + (projectNumber - range.firstProjectNumber) / pow10(range.projectDigits)
        return if (customer <= range.customerMax!!) customer else null
    }

    /** Local project number (per customer or range), null for free project numbers. */
    fun localNumber(projectNumber: Int): Int? {
        val range = rangeForProjectNumber(projectNumber) ?: return null
        return (projectNumber - range.firstProjectNumber) % pow10(range.projectDigits)
    }

    fun validateCustomerNumber(customerNumber: Long?): NumberRangeViolation? {
        if (customerNumber == null || customerNumber < 0) {
            return NumberRangeViolation.INVALID_NUMBER
        }
        if (rangeForCustomer(customerNumber) == null && !allowFreeNumbers) {
            return NumberRangeViolation.CUSTOMER_NOT_IN_RANGE
        }
        return null
    }

    /**
     * Validates a free project number (of a project whose customer belongs to no range): it must be positive and must
     * not lie in the project numbers of a range, otherwise it would be ambiguous.
     */
    fun validateFreeProjectNumber(projectNumber: Int?): NumberRangeViolation? {
        if (projectNumber == null || projectNumber <= 0) {
            return NumberRangeViolation.INVALID_NUMBER
        }
        if (!allowFreeNumbers) {
            return NumberRangeViolation.PROJECT_NOT_IN_RANGE
        }
        if (rangeForProjectNumber(projectNumber) != null) {
            return NumberRangeViolation.FREE_NUMBER_IN_RANGE
        }
        return null
    }

    /** Lowest customer number of the range not in [usedNumbers], null if the range is exhausted. */
    fun nextFreeCustomerNumber(range: NumberRange, usedNumbers: Collection<Long>): Long? {
        val min = range.customerMin ?: return null
        val used = usedNumbers.toSet()
        return (min..range.customerMax!!).firstOrNull { it !in used }
    }

    /** Lowest local project number not in [usedLocalNumbers], null if exhausted (e.g. 10 projects for 1 digit). */
    fun nextFreeLocalNumber(range: NumberRange, usedLocalNumbers: Collection<Int>): Int? {
        val used = usedLocalNumbers.toSet()
        return (0..range.maxLocalNumber).firstOrNull { it !in used }
    }

    fun segmentsForPrefix(prefix: Int?): List<Int> = rangeForPrefix(prefix)?.segments ?: defaultSegments

    /**
     * @param kost2Number 8-digit Kost2 number, e.g. 12345678.
     * @return e.g. "1.2345.6.78" for segments 1,4,1,2.
     */
    fun formatKost2(kost2Number: Int, delimiter: String = "."): String {
        val digits = kost2Number.toString().padStart(KOST2_DIGITS, '0')
        return split(digits, segmentsForPrefix(digits[0].digitToInt())).joinToString(delimiter)
    }

    /**
     * Formats a Kost2 given in the positional storage format of Kost2DO.
     * @param kost2Art Null is shown as "--" (Kost2 without type).
     * @return e.g. "1.2345.6.78" for (1, 234, 56, 78) and segments 1,4,1,2.
     */
    fun formatKost2(nummernkreis: Int, bereich: Int, teilbereich: Int, kost2Art: Long?, delimiter: String = "."): String {
        val parts = listOf(
            nummernkreis.toString(),
            bereich.toString().padStart(3, '0'),
            teilbereich.toString().padStart(2, '0'),
            kost2Art?.toString()?.padStart(2, '0') ?: "--",
        )
        val digits = parts.joinToString("")
        if (digits.length != KOST2_DIGITS) {
            // Out of range values (no valid Kost2), shown unchanged instead of being split wrongly:
            return parts.joinToString(delimiter)
        }
        return split(digits, segmentsForPrefix(nummernkreis)).joinToString(delimiter)
    }

    /**
     * @param projectNumber 6-digit project number, e.g. 123456.
     * @return e.g. "1.2345.6" for segments 1,4,1,2 (the first three segments of the Kost2).
     */
    fun formatProjectNumber(projectNumber: Int, delimiter: String = "."): String {
        val digits = projectNumber.toString().padStart(PROJECT_NUMBER_DIGITS, '0')
        val segments = segmentsForPrefix(digits[0].digitToInt()).dropLast(1)
        return split(digits, segments).joinToString(delimiter)
    }

    /**
     * @param formatted If true, a customer of a range is shown by the first two Kost2 segments of its projects
     * ("5.123", "1.2345"), otherwise by its number padded to the digits of the range ("123", "12345").
     * Free customer numbers are shown unchanged.
     */
    fun formatCustomerNumber(customerNumber: Long, formatted: Boolean = true): String {
        val range = rangeForCustomer(customerNumber) ?: return customerNumber.toString()
        if (!formatted) {
            return customerNumber.toString().padStart(range.customerMax.toString().length, '0')
        }
        val projectNumber = projectNumber(range, customerNumber, 0)
        return formatProjectNumber(projectNumber).split('.').take(2).joinToString(".")
    }

    /**
     * @param formatted If true, a project number of a range is segmented ("5.123.45", "1.2345.6"), otherwise the
     * 6-digit number is returned. Free project numbers are shown unchanged.
     */
    fun formatProjectNumber(projectNumber: Int, formatted: Boolean): String {
        if (!formatted || rangeForProjectNumber(projectNumber) == null) {
            return projectNumber.toString()
        }
        return formatProjectNumber(projectNumber)
    }

    /**
     * Parses 8 digits ("12345678") or the delimited form matching the segments of the prefix ("1.2345.6.78" for
     * segments 1,4,1,2; "5.123.45.67" for 1,3,2,2).
     * @return [nummernkreis, bereich, teilbereich, kost2Art] in the positional storage format, null if not parse-able.
     */
    fun parseKost2(kostString: String?): IntArray? {
        val str = kostString?.trim() ?: return null
        val digits = if (DIGITS_8.matches(str)) {
            str
        } else {
            val parts = str.split('.')
            if (parts.any { !ONLY_DIGITS.matches(it) } || parts[0].length != 1) {
                return null
            }
            if (parts.map { it.length } != segmentsForPrefix(parts[0].toInt())) {
                return null
            }
            parts.joinToString("")
        }
        return intArrayOf(
            digits.substring(0, 1).toInt(),
            digits.substring(1, 4).toInt(),
            digits.substring(4, 6).toInt(),
            digits.substring(6, 8).toInt(),
        )
    }

    private fun validate(range: NumberRange) {
        require(range.prefix in 1..9) { "Number ranges: prefix of $range must be 1-9." }
        require(range.projectDigits in 1..(PROJECT_NUMBER_DIGITS - 1)) {
            "Number ranges: project-digits of $range must be 1-${PROJECT_NUMBER_DIGITS - 1}."
        }
        validateSegments(range.segments, "segments of $range")
        val min = range.customerMin
        val max = range.customerMax
        require((min == null) == (max == null)) { "Number ranges: give both customer-min and customer-max for $range." }
        if (min == null || max == null) {
            require(range.projectDigits == PROJECT_NUMBER_DIGITS - 1) {
                "Number ranges: $range has no customers, so project-digits must be ${PROJECT_NUMBER_DIGITS - 1}."
            }
            return
        }
        require(min in 0..max) { "Number ranges: customer-min of $range must be in 0..customer-max." }
        require((max - min + 1) * pow10(range.projectDigits) <= PROJECT_PREFIX_FACTOR) {
            "Number ranges: ${max - min + 1} customers with ${range.projectDigits} project digits don't fit into" +
                    " the ${PROJECT_NUMBER_DIGITS - 1} digits after the prefix of $range."
        }
    }

    private fun validateSegments(segments: List<Int>, label: String) {
        require(segments.size >= 2 && segments[0] == 1 && segments.all { it > 0 } && segments.sum() == KOST2_DIGITS
                && segments.last() == 2) {
            "Number ranges: $label must start with 1, end with 2 (Kost2 type) and sum up to $KOST2_DIGITS: $segments"
        }
    }

    private fun split(digits: String, segments: List<Int>): List<String> {
        var pos = 0
        return segments.map { len -> digits.substring(pos, pos + len).also { pos += len } }
    }

    companion object {
        private val DIGITS_8 = Regex("\\d{$KOST2_DIGITS}")
        private val ONLY_DIGITS = Regex("\\d+")
    }
}
