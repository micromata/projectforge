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

import org.projectforge.framework.persistence.api.SortProperty
import  javax.persistence.criteria.JoinType

class DBFilter(
        /**
         * Optional entries for searching (keywords, field search, range search etc.)
         */
        var allEntries: MutableList<DBFilterEntry> = mutableListOf(),
        var sortAndLimitMaxRowsWhileSelect: Boolean = true,
        var maxRows: Int = 50,
        /**
         * If true, only deleted entries will be shown. If false, no deleted entries will be shown. If null, all entries will be shown.
         */
        var deleted: Boolean? = false,
        var searchHistory: String? = null) {

    class Alias(val field: String, val alias: String, val joinType: JoinType?)

    private val resultMatcher = mutableListOf<DBResultMatcher>()
    private val aliasList = mutableListOf<Alias>()

    fun add(entry: DBFilterEntry) {
        this.allEntries.add(entry)
    }

    fun add(entry: DBResultMatcher) {
        this.resultMatcher.add(entry)
    }

    fun addAlias(field: String, alias: String, joinType: JoinType? = null) {
        aliasList.add(Alias(field, alias, joinType))
    }

    val criteriaSearchEntries
        get() = allEntries.filter { !it.fulltextSearch }

    val fulltextSearchEntries
        get() = allEntries.filter { it.fulltextSearch }

    @Transient
    internal val log = org.slf4j.LoggerFactory.getLogger(DBFilter::class.java)

    var sortProperties = mutableListOf<SortProperty>()
}
