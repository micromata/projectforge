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

import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.Session
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.hibernate.sql.JoinType
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.slf4j.LoggerFactory
import java.util.*

class MagicFilterQuery(val filter: MagicFilter,
                       /**
                        * The class of the entities to search for.
                        */
                       val clazz: Class<*>,
                       var associationPath: String? = null,
                       ignoreTenant: Boolean = false,
                       var alias: String? = null,
                       /**
                        * Locale is needed for lucene stemmers (hibernate search).
                        * Will be get from thread locale user context.
                        */
                       var locale: Locale? = null,
                       /**
                        * If an error occured (e. g. lucene parse exception) this message will be returned.
                        */
                       var errorMessage: String? = null) {

    private val log = LoggerFactory.getLogger(MagicFilterQuery::class.java)

    private val queryElements = mutableListOf<Any>()

    var fetchMode: FetchMode? = null

    init {
        val tenantService = ApplicationContextProvider.getApplicationContext().getBean(TenantService::class.java)
        if (!ignoreTenant && tenantService.isMultiTenancyAvailable) {
            val userContext = ThreadLocalUserContext.getUserContext()
            val currentTenant = userContext.currentTenant
            if (currentTenant != null) {
                if (currentTenant.isDefault == true) {
                    this.add(Restrictions.or(Restrictions.eq("tenant", userContext.currentTenant),
                            Restrictions.isNull("tenant")))
                } else {
                    this.add(Restrictions.eq("tenant", userContext.currentTenant))
                }
            }
        }
        locale = ThreadLocalUserContext.getLocale()
    }

    fun setFetchMode(associationPath: String, mode: FetchMode) {
        this.associationPath = associationPath
        this.fetchMode = mode
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
            var lo: java.sql.Date? = null
            var hi: java.sql.Date? = null
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
            add(Restrictions.between(dateField, lo, hi))
        }
    }

    fun buildCriteria(session: Session, clazz: Class<*>): Criteria {
        val criteria = session.createCriteria(clazz)
        buildCriteria(criteria)
        return criteria
    }

    private fun buildCriteria(criteria: Criteria) {
        for (obj in queryElements) {
            if (obj is Criterion) {
                criteria.add(obj)
            } else if (obj is Order) {
                criteria.addOrder(obj)
            } else if (obj is Alias) {
                criteria.createAlias(obj.arg0, obj.arg1, obj.joinType)
            }
        }
        if (associationPath != null) {
            criteria.setFetchMode(associationPath, fetchMode)
        }
        if (filter.sortAndLimitMaxRowsWhileSelect) {
            if (filter.maxRows > 0) {
                criteria.setMaxResults(filter.maxRows)
            }
            filter.sortProperties.forEach {
                if (it.sortOrder == SortOrder.DESCENDING)
                    criteria.addOrder(Order.desc(it.property))
                else
                    criteria.addOrder(Order.asc(it.property))
            }
        }
    }

    fun add(criterion: Criterion) {
        queryElements.add(criterion)
    }

    fun addOrder(order: Order) {
        queryElements.add(order)
    }

    /**
     * @see org.hibernate.Criteria.createAlias
     */
    fun addAlias(arg0: String, arg1: String) {
        queryElements.add(Alias(arg0, arg1))
    }

    /**
     * @see org.hibernate.Criteria.createAlias
     */
    fun createAlias(arg0: String, arg1: String, joinType: JoinType) {
        queryElements.add(Alias(arg0, arg1, joinType))
    }


    internal inner class Alias {
        var arg0: String

        var arg1: String

        var joinType: JoinType = JoinType.INNER_JOIN

        constructor(arg0: String, arg1: String) {
            this.arg0 = arg0
            this.arg1 = arg1
        }

        constructor(arg0: String, arg1: String, joinType: JoinType) {
            this.arg0 = arg0
            this.arg1 = arg1
            this.joinType = joinType
        }
    }
}
