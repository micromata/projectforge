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

package org.projectforge.framework.persistence.api

import org.projectforge.framework.persistence.api.impl.DBFilter
import org.projectforge.framework.persistence.api.impl.DBResultMatcher
import org.projectforge.framework.time.DateHelper
import org.slf4j.LoggerFactory
import java.util.*
import javax.persistence.criteria.JoinType

/**
 * Stores the expressions and settings for creating a hibernate criteria object. This template is useful for avoiding
 * the need of a hibernate session in the stripes action classes.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class QueryFilter {
    private val log = LoggerFactory.getLogger(QueryFilter::class.java)

    val ignoreTenant: Boolean
    private val dbFilter = DBFilter()

    var filter: BaseSearchFilter? = null
        private set

    /**
     * Creates new QueryFilter with a new SearchFilter as filter.
     */
    constructor() {
        this.filter = BaseSearchFilter()
        this.ignoreTenant = false
    }

    /**
     * @param filter
     * @param ignoreTenant default is false.
     */
    @JvmOverloads
    constructor(filter: BaseSearchFilter?, ignoreTenant: Boolean = false) {
        this.ignoreTenant = ignoreTenant
        if (filter == null) {
            this.filter = BaseSearchFilter()
        } else {
            this.filter = filter
        }
    }

    /**
     * @return this for chaining
     */
    fun add(resultMatcher: DBResultMatcher): QueryFilter {
        dbFilter.add(resultMatcher)
        return this
    }

    /**
     * @param order
     * @return
     * @see org.hibernate.Criteria.addOrder
     */
    fun addOrder(sortProperty: SortProperty): QueryFilter {
        dbFilter.sortProperties.add(sortProperty)
        return this
    }

    /**
     * Adds Expression.between for given time period.
     *
     * @param dateField
     * @param year      if <= 0 do nothing.
     * @param month     if < 0 choose whole year, otherwise given month. (Calendar.MONTH);
     */
    fun setYearAndMonth(dateField: String, year: Int, month: Int) {
        if (year > 0) {
            val cal = DateHelper.getUTCCalendar()
            cal.set(Calendar.YEAR, year)
            val lo: java.sql.Date
            val hi: java.sql.Date
            if (month >= 0) {
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                lo = java.sql.Date(cal.timeInMillis)
                val lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, lastDayOfMonth)
                hi = java.sql.Date(cal.timeInMillis)
            } else {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                lo = java.sql.Date(cal.timeInMillis)
                val lastDayOfYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, lastDayOfYear)
                hi = java.sql.Date(cal.timeInMillis)
            }
            add(between(dateField, lo, hi))
        }
    }

    /**
     * Convenient method signature for Java.
     * @see org.hibernate.Criteria.createAlias
     */
    fun createAlias(field: String, alias: String): QueryFilter {
        dbFilter.addAlias(field, alias, null)
        return this
    }

    /**
     * @see org.hibernate.Criteria.createAlias
     */
    fun createAlias(field: String, alias: String, joinType: JoinType? = null): QueryFilter {
        dbFilter.addAlias(field, alias, joinType)
        return this
    }

    fun getDBFilter(): DBFilter {
        val dbFilter = DBFilter()
        return dbFilter
    }

    companion object {
        @JvmStatic
        fun isNull(field: String): DBResultMatcher {
            return DBResultMatcher.IsNull(field)
        }

        @JvmStatic
        fun isNotNull(field: String): DBResultMatcher {
            return DBResultMatcher.IsNotNull(field)
        }

        @JvmStatic
        fun eq(field: String, value: Any): DBResultMatcher {
            return DBResultMatcher.Equals(field, value)
        }

        @JvmStatic
        fun ne(field: String, value: Any): DBResultMatcher {
            return DBResultMatcher.Equals(field, value)
        }

        @JvmStatic
        fun like(field: String, value: String): DBResultMatcher {
            return DBResultMatcher.Like(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> between(field: String, from: O, to: O): DBResultMatcher {
            return DBResultMatcher.Between(field, from, to)
        }

        @JvmStatic
        fun <O : Comparable<O>> ge(field: String, value: O): DBResultMatcher {
            return DBResultMatcher.GreaterEqual<O>(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> gt(field: String, value: O): DBResultMatcher {
            return DBResultMatcher.Greater<O>(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> le(field: String, value: O): DBResultMatcher {
            return DBResultMatcher.LessEqual<O>(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> lt(field: String, value: O): DBResultMatcher {
            return DBResultMatcher.Less<O>(field, value)
        }

        @JvmStatic
        fun `in`(field: String, vararg values: Any): DBResultMatcher {
            return DBResultMatcher.In(field, values)
        }

        @JvmStatic
        fun not(matcher: DBResultMatcher): DBResultMatcher {
            return DBResultMatcher.Not(matcher)
        }

        @JvmStatic
        fun and(vararg matchers: DBResultMatcher): DBResultMatcher {
            return DBResultMatcher.And(*matchers)
        }

        @JvmStatic
        fun or(vararg matchers: DBResultMatcher): DBResultMatcher {
            return DBResultMatcher.Or(*matchers)
        }
    }
}
