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

/**
 * A number range (Nummernkreis) for customers and projects, defined only by configuration
 * (`projectforge.fibu.number-ranges.ranges[n]`, see [NumberRangeConfig]).
 *
 * A project of a number range gets a 6-digit project number, its first digit is the [prefix]:
 * ```
 * projectNumber = prefix·10⁵ + (customer mod 10^customerDigits)·10^projectDigits + localNumber
 * projectNumber = prefix·10⁵ + localNumber                                  (range without customers)
 * ```
 * with `customerDigits = 5 − projectDigits`: the customer number itself is part of the project number (its
 * last [customerDigits] digits), whatever [customerMin] is - customer 10001 of the range 10001-19999 is
 * `1.0001`, customer 123 of 0-999 is `5.123`. Hence all customers of a range share the digits in front of
 * those (see [NumberRanges] validation).
 * If cost units (Kost2) are configured, the 8-digit Kost2 of a project is `projectNumber·100 + kost2Art`, shown and
 * entered with the given [segments], e.g. `1,4,1,2` for `1.####.#.##`.
 */
class NumberRange {
    /** First digit of the project numbers and of the Kost2 numbers of this range (1-9). */
    var prefix: Int = 0

    /** Display name, e.g. "Intern". */
    var name: String? = null

    /** Lowest customer number of this range. Null for a range without customers (e.g. internal projects). */
    var customerMin: Long? = null

    /** Highest customer number of this range. Must be given together with [customerMin]. */
    var customerMax: Long? = null

    /** Number of digits of the local project number (per customer or, without customers, of the whole range). */
    var projectDigits: Int = 2

    /** Segments of the 8-digit Kost2 for display and input, e.g. 1,3,2,2 for `#.###.##.##`. */
    var segments: List<Int> = DEFAULT_SEGMENTS

    val hasCustomers: Boolean
        get() = customerMin != null

    /** Number of digits of the customer part of a project number, e.g. 3 for 2 project digits. */
    val customerDigits: Int
        get() = PROJECT_NUMBER_DIGITS - 1 - projectDigits

    /**
     * The customer part of the customer's project numbers: its last [customerDigits] digits, e.g. 1 for
     * 10001 (4 digits) and 123 for 123 (3 digits).
     */
    fun customerPart(customerNumber: Long): Int = (customerNumber % pow10(customerDigits)).toInt()

    /** Highest local project number, e.g. 99 for 2 project digits. */
    val maxLocalNumber: Int
        get() = pow10(projectDigits) - 1

    /** First project number of this range, e.g. 500000. */
    val firstProjectNumber: Int
        get() = prefix * PROJECT_PREFIX_FACTOR

    /** Last project number of this range, e.g. 599999. */
    val lastProjectNumber: Int
        get() = firstProjectNumber + PROJECT_PREFIX_FACTOR - 1

    fun containsCustomer(customerNumber: Long): Boolean {
        val min = customerMin ?: return false
        val max = customerMax ?: return false
        return customerNumber in min..max
    }

    fun containsProjectNumber(projectNumber: Int): Boolean {
        return projectNumber in firstProjectNumber..lastProjectNumber
    }

    override fun toString(): String {
        val customers = if (hasCustomers) "customers $customerMin-$customerMax" else "no customers"
        return "NumberRange(prefix=$prefix, name=$name, $customers, projectDigits=$projectDigits, segments=$segments)"
    }

    companion object {
        /** Number of digits of a project number of a number range. */
        const val PROJECT_NUMBER_DIGITS = 6

        /** Number of digits of a Kost2 number. */
        const val KOST2_DIGITS = 8

        /** 10⁵: the factor of the prefix in a 6-digit project number. */
        const val PROJECT_PREFIX_FACTOR = 100_000

        val DEFAULT_SEGMENTS = listOf(1, 3, 2, 2)

        internal fun pow10(exp: Int): Int {
            var result = 1
            repeat(exp) { result *= 10 }
            return result
        }
    }
}
