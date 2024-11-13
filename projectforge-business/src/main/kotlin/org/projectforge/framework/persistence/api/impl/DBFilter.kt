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

package org.projectforge.framework.persistence.api.impl

import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.SortProperty

/**
 * DBFilter is created by QueryFilter and hold all predicates for building a query.
 * There are three types of predicates:
 * 1. Criteria predicates: These predicates are used for criteria search.
 * 2. Full text predicates: These predicates are used for full text search.
 * 3. Result predicates: These predicates are used for filtering the result set.
 * The predicates are evaluated by the DBQuery.
 * The DBFilter is used for building the query and for filtering the result set.
 */
class DBFilter(
    var sortAndLimitMaxRowsWhileSelect: Boolean = true,
    var maxRows: Int = 50,
    val searchFields: Array<String>?
) {

    val allPredicates = mutableListOf<DBPredicate>()

    val sortProperties = mutableListOf<SortProperty>()

    fun add(predicate: DBPredicate) {
        allPredicates.add(predicate)
    }

    fun add(sortProperty: SortProperty) {
        sortProperties.add(sortProperty)
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }
}
