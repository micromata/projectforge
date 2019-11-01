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

package org.projectforge.framework.persistence.api.impl

import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.SortProperty

class DBFilter(
        var sortAndLimitMaxRowsWhileSelect: Boolean = true,
        var maxRows: Int = 50) {
    class Statistics {
        var fullTextRequired: Boolean = false
        var numberOfCriteriaPredicates: Int = 0
        var numberOfFullTextQueries: Int = 0
        var numberOfResultPredicates: Int = 0
    }

    val predicates = mutableListOf<DBPredicate>()

    val sortProperties = mutableListOf<SortProperty>()


    fun createStatistics(): Statistics {
        var fullTextRequired: Boolean = false
        for (it in predicates) {
            if (!it.criteriaSupport && !it.resultSetSupport) {
                fullTextRequired = true
                break
            }
        }
        val statistics = Statistics()
        statistics.fullTextRequired = fullTextRequired
        predicates.forEach {
            if (fullTextRequired) {
                if (it.fullTextSupport) {
                    ++statistics.numberOfFullTextQueries
                } else {
                    ++statistics.numberOfResultPredicates
                }
            } else {
                if (it.criteriaSupport) {
                    ++statistics.numberOfCriteriaPredicates
                } else {
                    ++statistics.numberOfResultPredicates
                }
            }
        }
        return statistics
    }

    @Transient
    internal val log = org.slf4j.LoggerFactory.getLogger(DBFilter::class.java)

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }
}
