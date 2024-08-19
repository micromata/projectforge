/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.utils

import jakarta.persistence.Tuple
import org.projectforge.framework.time.PFDateTime
import java.time.LocalDate
import java.time.Year
import java.util.*

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object SQLHelper {
    /**
     * Usage:<br></br>
     * <pre>
     * final Object[] minMaxDate = getSession().createNamedQuery(ContractDO.SELECT_MIN_MAX_DATE, Object[].class)
     * .getSingleResult();
     * return SQLHelper.getYears((Date)minMaxDate[0], (Date)minMaxDate[1]);
    </pre> *
     *
     * @return Array of years in descendent order. If min or max is null, the current year is returned.
     */
    @JvmStatic
    fun getYears(min: Any?, max: Any?): IntArray {
        if (min == null || max == null) {
            return intArrayOf(Year.now().value)
        }
        if (min is Date || max is Date) {
            return getYears(min as Date, max as Date)
        }
        return getYears(min as LocalDate, max as LocalDate)
    }

    @JvmStatic
    fun getYears(minYear: Int?, maxYear: Int?): IntArray {
        val min = minYear ?: Year.now().value
        val max = maxYear ?: Year.now().value
        if (min > max || max - min > 30) {
            throw UnsupportedOperationException("Paranoia Exception")
        }
        val res = IntArray(max - min + 1)
        var i = 0
        for (year in max downTo min) {
            res[i++] = year
        }
        return res
    }

    @JvmStatic
    fun getYearsByTupleOfDate(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair(PFDateTime.from(minMaxDate[0] as Date).year, PFDateTime.from(minMaxDate[1] as Date).year)
        }
        return getYears(result.first, result.second)
    }

    @JvmStatic
    fun getYearsByTupleOfLocalDate(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair((minMaxDate[0] as LocalDate).year, (minMaxDate[1] as LocalDate).year)
        }
        return getYears(result.first, result.second)
    }

    @JvmStatic
    fun getYearsByTupleOfYears(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair(minMaxDate[0] as Int, minMaxDate[1] as Int)
        }
        return getYears(result.first, result.second)
    }
}
