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

import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.time.PFDateTime
import java.time.LocalDate
import java.time.Year
import java.util.*
import jakarta.persistence.TypedQuery

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
    fun getYears(min: Date?, max: Date?): IntArray {
        if (min == null || max == null) {
            return intArrayOf(Year.now().value)
        }
        val from = PFDateTime.from(min).year
        val to= PFDateTime.from(max).year
        return getYears(from, to)
    }

    @JvmStatic
    fun getYears(min: LocalDate?, max: LocalDate?): IntArray {
        if (min == null || max == null) {
            return intArrayOf(LocalDate.now().year)
        }
        return getYears(min.year, max.year)
    }

    @JvmStatic
    fun getYears(min: Int?, max: Int?): IntArray {
        if (min == null || max == null) {
            return intArrayOf(Year.now().value)
        }
        val res = IntArray(max - min + 1)
        var i = 0
        for (year in max downTo min) {
            res[i++] = year
        }
        return res
    }

    /**
     * Do a query.list() call and ensures that the result is either null/empty or the result list has only one element (size == 1).
     * If multiple entries were received, an Exception will be thrown
     * <br></br>
     * Through this method ProjectForge ensures, that some entities are unique by their defined attributes (invoices with unique number etc.), especially
     * if the uniquness can't be guaranteed by a data base constraint.
     * <br></br>
     * An internal error prevents the system on proceed with inconsistent (multiple) data entries.
     *
     * @param errorMessage An optional error message to display.
     * @throws InternalErrorException if the list is not empty and has more than one elements (size > 1).
     */
    @JvmStatic
    @JvmOverloads
    fun <T> ensureUniqueResult(query: TypedQuery<T>, nullAllowed: Boolean = true, errorMessage: String? = null): T? {
        val list = query.resultList
        if (nullAllowed && list.isNullOrEmpty())
            return null
        if (list.size != 1) {
            throw InternalErrorException("Internal error: ProjectForge requires a single entry, but found ${list.size} entries: ${queryToString(query, errorMessage)}")
        }
        return list[0]
    }


    internal fun queryToString(query: TypedQuery<*>, errorMessage: String?): String {
        val queryString = query.unwrap(org.hibernate.Query::class.java).getQueryString()
        val sb = StringBuilder()
        sb.append("query='$queryString', params=[") //query.getQueryString())
        var first = true
        try {
            for (param in query.parameters) { // getParameterMetadata().getNamedParameterNames()
                if (!first)
                    sb.append(",")
                else
                    first = false
                sb.append("${param.name}=[${query.getParameterValue(param)}]")
            }
        } catch (ex: Exception) {
            // Do nothing: Session/EntityManager closed.
        }
        sb.append("]")
        if (StringUtils.isNotBlank(errorMessage))
            sb.append(", msg=[$errorMessage]")
        return sb.toString()
    }
}
