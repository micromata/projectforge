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
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.impl.DBFilter
import org.projectforge.framework.persistence.api.impl.DBHistorySearchParams
import org.projectforge.framework.persistence.api.impl.DBJoin
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.slf4j.LoggerFactory
import java.util.*
import javax.persistence.criteria.JoinType

/**
 * If no maximum number of results is defined, MAX_ROWS is used as max value.
 */
const val QUERY_FILTER_MAX_ROWS: Int = 10000

/**
 * Convenient helper to create database queries (criteria search, full text search and search in result lists).
 * It will be automatically detected, which kind of database query is needed (critery, full text or multi field full text query).
 * Field of the index of hibernate search will be detected and used in full text queries, if not indexed, the result will be filtered.
 *
 * You may add your predicates (independent of which strategy is used behind). The query strategy is automatically done.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class QueryFilter @JvmOverloads constructor(filter: BaseSearchFilter? = null,
                                            val ignoreTenant: Boolean = false) {
    private val predicates = mutableListOf<DBPredicate>()

    val joinList = mutableListOf<DBJoin>()

    var sortProperties = mutableListOf<SortProperty>()

    private val historyQuery = DBHistorySearchParams()

    /**
     * If true, any searchstring (alphanumeric) without wildcard will be changed to '<searchString>*'.
     */
    var autoWildcardSearch: Boolean = false

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
            this.autoWildcardSearch = true
            // Legacy for old implementation:
            if (!filter.ignoreDeleted) {
                deleted = filter.deleted
            }
            if (filter.isSearchHistory && !filter.searchString.isNullOrBlank()) {
                searchHistory = filter.searchString
            }
            if (filter.isSearchNotEmpty) {
                addFullTextSearch(filter.searchString, autoWildcardSearch)
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
     * Create an alias for criteria search, used for Joins.
     * @param attr The attribute to create a alias (JoinSet) for. Nested properties are supported (order.positions).
     * @param fetch
     * @param joinType [JoinType.INNER] is default.
     * @param parent If not given, root is used. If given, the parent is used as root path of the attr.
     * @return this for chaining.
     */
    @JvmOverloads
    fun createJoin(attr: String, joinType: JoinType = JoinType.INNER, fetch: Boolean = false, parent: String? = null): QueryFilter {
        joinList.add(DBJoin(attr, joinType, fetch, parent))
        return this
    }

    /**
     * Does nothing if str is null or blank.
     */
    fun addFullTextSearch(str: String?, autoWildcardSearch: Boolean = false) {
        if (str.isNullOrBlank()) return
        predicates.add(DBPredicate.FullSearch(str, autoWildcardSearch))
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

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
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

        @JvmOverloads
        @JvmStatic
        fun like(field: String, value: String, autoWildcardSearch: Boolean = false): DBPredicate.Like {
            return DBPredicate.Like(field, value, autoWildcardSearch)
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
            return DBPredicate.Less(field, value)
        }

        /**
         * @param from if given, ge or between search is used.
         * @param to if given, le or between search is used.
         */
        @JvmStatic
        fun <T : Comparable<T>> interval(field: String, from: T?, to: T?): DBPredicate {
            if (from != null)
                return if (to != null)
                    DBPredicate.Between(field, from, to)
                else
                    DBPredicate.GreaterEqual(field, from)
            else if (to != null)
                return DBPredicate.LessEqual(field, to)
            throw UnsupportedOperationException("interval needs at least one value ('from' and/or 'to').")
        }

        @JvmStatic
        fun <T> isIn(field: String, values: Collection<*>): DBPredicate.IsIn<T> {
            @Suppress("UNCHECKED_CAST")
            return DBPredicate.IsIn(field, *(values.toTypedArray() as Array<T>))
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
            return if (node == null) {
                log.warn("Can't query for given task id #$taskId, no such task node found.")
                DBPredicate.IsNull(field)
            } else {
                if (recursive) {
                    val taskIds = node.descendantIds
                    taskIds.add(node.id)
                    if (log.isDebugEnabled) {
                        log.debug("search in tasks: $taskIds")
                    }
                    DBPredicate.IsIn(field, taskIds)
                } else {
                    DBPredicate.Equal(field, taskId)
                }
            }
        }
    }
}
