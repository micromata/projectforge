/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.kost

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.and
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ge
import org.projectforge.framework.persistence.api.QueryFilter.Companion.gt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.le
import org.projectforge.framework.persistence.api.QueryFilter.Companion.lt
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.utils.SQLHelper.ensureUniqueResult
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.stereotype.Repository

@Repository
open class BuchungssatzDao : BaseDao<BuchungssatzDO>(BuchungssatzDO::class.java) {
    override fun getAdditionalSearchFields(): Array<String> {
        return ADDITIONAL_SEARCH_FIELDS
    }

    /**
     * List of all years witch BuchungssatzDO entries: select min(year), max(year) from t_fibu_buchungssatz.
     */
    open val years: IntArray
        get() {
            @Suppress("UNCHECKED_CAST")
            val list = em.createQuery("select min(year), max(year) from BuchungssatzDO t").resultList as MutableList<Array<*>?>
            if (list.size == 0 || list[0] == null || list[0]!![0] == null) {
                return intArrayOf(now().year)
            }
            val minYear = list[0]!![0] as Int
            val maxYear = list[0]!![1] as Int
            if (minYear > maxYear || maxYear - minYear > 30) {
                throw UnsupportedOperationException("Paranoia Exception")
            }
            val res = IntArray(maxYear - minYear + 1)
            var i = 0
            for (year in maxYear downTo minYear) {
                res[i++] = year
            }
            return res
        }

    open fun getBuchungssatz(year: Int, month: Int, satznr: Int): BuchungssatzDO? {
        return ensureUniqueResult(
                em.createNamedQuery(BuchungssatzDO.FIND_BY_YEAR_MONTH_SATZNR, BuchungssatzDO::class.java)
                        .setParameter("year", year)
                        .setParameter("month", month)
                        .setParameter("satznr", satznr))
    }

    private fun validateTimeperiod(myFilter: BuchungssatzFilter): Boolean {
        val fromMonth = myFilter.fromMonth
        val fromYear = myFilter.fromYear
        val toMonth = myFilter.toMonth
        val toYear = myFilter.toYear
        if (fromMonth != null && fromYear == null || toMonth != null && toYear == null) {
            // No month should be given without year.
            return false
        }
        return if (fromYear != null && toYear != null) {
            if (fromYear == toYear) { // Same year, if both month given, fromMonth must be <= toMonth.
                // Returns true, if no or only one month is given.
                fromMonth == null || toMonth == null || fromMonth <= toMonth
            } else fromYear < toYear
        } else true
        // No year or one year is given.
    }

    open override fun getList(filter: BaseSearchFilter): List<BuchungssatzDO> {
        accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP)
        val myFilter: BuchungssatzFilter
        myFilter = if (filter is BuchungssatzFilter) {
            filter
        } else {
            BuchungssatzFilter(filter)
        }
        val queryFilter = QueryFilter(filter)
        if (!validateTimeperiod(myFilter)) {
            throw UserException("fibu.buchungssatz.error.invalidTimeperiod")
        }
        queryFilter.maxRows = QUERY_FILTER_MAX_ROWS
        val fromMonth = myFilter.fromMonth
        val fromYear = myFilter.fromYear
        val toMonth = myFilter.toMonth
        val toYear = myFilter.toYear
        // Same year:
        if (fromYear != null && toYear != null) {
            // Both years are given
            if (fromMonth != null || toMonth != null) {
                val or = DBPredicate.Or();
                queryFilter.add(or)
                // At least one month is given, check same year:
                if (fromMonth != null) {
                    val and = DBPredicate.And()
                    or.add(and)
                    and.add(eq("year", fromYear))
                    and.add(ge("month", fromMonth))
                    if (toMonth != null) {
                        if (fromYear == toYear) {
                            // toYear is same year, so month mus be ge than formMonth and le than toMonth
                            and.add(le("month", toMonth))
                        } else {
                            // toYear is another year:
                            or.add(DBPredicate.And()
                                    .add(eq("year", toYear))
                                    .add(le("month", toMonth)))
                        }
                        or.add(and(gt("year", fromYear),
                                lt("year", toYear)))

                    } else {
                        // fromMonth given but toMonth not:
                        or.add(and(gt("year", fromYear),
                                le("year", toYear)))
                    }
                } else if (toMonth != null) {
                    // fromMonth isn't given:
                    or.add(DBPredicate.And(
                            eq("year", toYear),
                            le("month", toMonth)))
                    // fromMonth not given but toMonth is:
                    or.add(and(ge("year", fromYear),
                            lt("year", toYear)))
                }
            } else {
                // No month given:
                queryFilter.add(and(
                        ge("year", fromYear),
                        le("year", toYear)));

            }
        } else if (fromYear != null) {
            // Only from Year given:
            if (fromMonth != null) {
                queryFilter.add(or(
                        and(eq("year", fromYear), ge("month", fromMonth)),
                        gt("year", fromYear)
                ))
            } else {
                queryFilter.add(ge("year", fromYear))
            }
        } else if (toYear != null) {
            // Only to Year given:
            if (toMonth != null) {
                queryFilter.add(or(
                        and(eq("year", toYear), le("month", toMonth)),
                        lt("year", toYear)
                ))
            } else {
                queryFilter.add(le("year", toYear))
            }
        } // else: nothing given: no time period range.
        queryFilter.addOrder(asc("year")).addOrder(asc("month")).addOrder(asc("satznr"))
        return getList(queryFilter)
    }

    /**
     * User must member of group finance or controlling.
     */
    open override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP,
                ProjectForgeGroup.CONTROLLING_GROUP)
    }

    /**
     * @see .hasUserSelectAccess
     */
    open override fun hasUserSelectAccess(user: PFUserDO, obj: BuchungssatzDO, throwException: Boolean): Boolean {
        return hasUserSelectAccess(user, throwException)
    }

    /**
     * User must member of group finance.
     */
    open override fun hasAccess(user: PFUserDO, obj: BuchungssatzDO, oldObj: BuchungssatzDO,
                                operationType: OperationType, throwException: Boolean): Boolean {
        return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP)
    }

    open override fun newInstance(): BuchungssatzDO {
        return BuchungssatzDO()
    }

    companion object {
        @JvmStatic
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("kost1.nummer", "kost1.description",
                "kost2.nummer",
                "kost2.description", "kost2.comment", "kost2.projekt.name", "kost2.projekt.kunde.name", "konto.nummer",
                "gegenKonto.nummer")
        /**
         * Need more results:
         */
        private const val QUERY_FILTER_MAX_ROWS = 100000
    }
}
