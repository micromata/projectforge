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

import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.persistence.api.impl.DBFilter
import org.projectforge.framework.persistence.api.impl.DBHistorySearchParams
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.slf4j.LoggerFactory
import java.util.*

/**
 * If no maximum number of results is defined, MAX_ROWS is used as max value.
 */
const val QUERY_FILTER_MAX_ROWS: Int = 10000;

/**
 * Stores the expressions and settings for creating a hibernate criteria object. This template is useful for avoiding
 * the need of a hibernate session in the stripes action classes.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class QueryFilter @JvmOverloads constructor(filter: BaseSearchFilter? = null, val ignoreTenant: Boolean = false) {
    private val predicates = mutableListOf<DBPredicate>()

    var sortProperties = mutableListOf<SortProperty>()

    private val historyQuery = DBHistorySearchParams()

    /**
     * If null, deleted and normal entries will be queried.
     */
    var deleted: Boolean? = null

    var searchHistory: String?
        get() = historyQuery.searchHistory
        set(value) {
            historyQuery.searchHistory = value
        }

    var modifiedFrom: PFDateTime?
        get() = historyQuery.modifiedFrom
        set(value) {
            historyQuery.modifiedFrom = value
        }

    var modifiedTo: PFDateTime?
        get() = historyQuery.modifiedTo
        set(value) {
            historyQuery.modifiedTo = value
        }

    var modifiedByUserId: Int?
        get() = historyQuery.modifiedByUserId
        set(value) {
            historyQuery.modifiedByUserId = value
        }

    var maxRows: Int = 50

    var sortAndLimitMaxRowsWhileSelect: Boolean = true

    init {
        maxRows = QUERY_FILTER_MAX_ROWS
        if (filter != null) {
            // Legacy for old implementation:
            if (!filter.ignoreDeleted) {
                deleted = filter.deleted
            }
            if (filter.isSearchHistory && !filter.searchString.isNullOrBlank()) {
                searchHistory = filter.searchString
            }
            if (filter.isSearchNotEmpty) {
                addFullTextSearch(filter.searchString)
            }
            if (filter.useModificationFilter) {
                if (filter.modifiedSince != null) modifiedFrom = PFDateTime.from(filter.modifiedSince)
                else if (filter.startTimeOfModification != null) modifiedFrom = PFDateTime.from(filter.startTimeOfModification)
                if (filter.stopTimeOfModification != null) modifiedTo = PFDateTime.from(filter.stopTimeOfModification)
            }
            if (filter.modifiedByUserId != null) modifiedByUserId = filter.modifiedByUserId
            // if (filter.maxRows > 0) maxRows = filter.maxRows // Legacy gets whole result list and supports pagination.
        }
    }


    /**
     * @return this for chaining
     */
    fun add(predicate: DBPredicate): QueryFilter {
        predicates.add(predicate)
        return this
    }

    fun addOrder(vararg sortProperty: SortProperty): QueryFilter {
        sortProperties.addAll(sortProperty)
        return this
    }

    /**
     * Does nothing if str is null or blank.
     */
    fun addFullTextSearch(str: String?) {
        if (str.isNullOrBlank()) return
        predicates.add(DBPredicate.FullSearch(str))
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

    fun createDBFilter(): DBFilter {
        val dbFilter = DBFilter(sortAndLimitMaxRowsWhileSelect, maxRows)
        if (deleted != null) {
            dbFilter.predicates.add(DBPredicate.Equal("deleted", deleted == true))
        }
        predicates.forEach {
            dbFilter.predicates.add(it)
        }
        sortProperties.forEach {
            dbFilter.sortProperties.add(it)
        }
        return dbFilter
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueryFilter::class.java)

        @JvmStatic
        fun isNull(field: String): DBPredicate.IsNull {
            return DBPredicate.IsNull(field)
        }

        @JvmStatic
        fun isNotNull(field: String): DBPredicate.IsNotNull {
            return DBPredicate.IsNotNull(field)
        }

        @JvmStatic
        fun eq(field: String, value: Any): DBPredicate.Equal {
            return DBPredicate.Equal(field, value)
        }

        @JvmStatic
        fun ne(field: String, value: Any): DBPredicate.NotEqual {
            return DBPredicate.NotEqual(field, value)
        }

        @JvmStatic
        fun like(field: String, value: String): DBPredicate.Like {
            return DBPredicate.Like(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> between(field: String, from: O, to: O): DBPredicate.Between<O> {
            return DBPredicate.Between(field, from, to)
        }

        @JvmStatic
        fun <O : Comparable<O>> ge(field: String, value: O): DBPredicate.GreaterEqual<O> {
            return DBPredicate.GreaterEqual(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> gt(field: String, value: O): DBPredicate.Greater<O> {
            return DBPredicate.Greater(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> le(field: String, value: O): DBPredicate.LessEqual<O> {
            return DBPredicate.LessEqual(field, value)
        }

        @JvmStatic
        fun <O : Comparable<O>> lt(field: String, value: O): DBPredicate {
            return DBPredicate.Less<O>(field, value)
        }

        /**
         * @param from if given, ge or between search is used.
         * @param to if given, le or between search is used.
         */
        @JvmStatic
        fun <T : Comparable<T>> interval(field: String, from: T?, to: T?): DBPredicate {
            if (from != null)
                if (to != null)
                    return DBPredicate.Between(field, from, to)
                else
                    return DBPredicate.GreaterEqual(field, from)
            else if (to != null)
                return DBPredicate.LessEqual(field, to)
            throw UnsupportedOperationException("interval needs at least one value ('from' and/or 'to').")
        }

        @JvmStatic
        fun <T> isIn(field: String, vararg values: T): DBPredicate.IsIn<T> {
            return DBPredicate.IsIn(field, *values)
        }

        @JvmStatic
        fun not(matcher: DBPredicate): DBPredicate.Not {
            return DBPredicate.Not(matcher)
        }

        @JvmStatic
        fun and(vararg matchers: DBPredicate): DBPredicate.And {
            return DBPredicate.And(*matchers)
        }

        @JvmStatic
        fun or(vararg matchers: DBPredicate): DBPredicate.Or {
            return DBPredicate.Or(*matchers)
        }

        @JvmStatic
        fun taskSearch(field: String, taskId: Int?, recursive: Boolean): DBPredicate {
            if (taskId == null) {
                return DBPredicate.IsNull(field)
            }
            val node = TaskTreeHelper.getTaskTree().getTaskNodeById(taskId)
            if (node == null) {
                log.warn("Can't query for given task id #$taskId, no such task node found.")
                return DBPredicate.IsNull(field)
            } else {
                val recursive = true
                if (recursive) {
                    val taskIds = node.descendantIds
                    taskIds.add(node.id)
                    if (log.isDebugEnabled) {
                        log.debug("search in tasks: $taskIds")
                    }
                    return DBPredicate.IsIn(field, taskIds)
                } else {
                    return DBPredicate.Equal(field, taskId)
                }
            }
        }
    }
}
