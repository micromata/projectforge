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
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.impl.DBFilter
import org.projectforge.framework.persistence.api.impl.DBFilterEntry
import org.projectforge.framework.persistence.api.impl.MatchType
import org.projectforge.framework.persistence.api.impl.SearchType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import java.util.*

/** Transforms MagicFilterEntries to DBFilterExpressions. */
object MagicFilterProcessor {
    fun doIt(entityClass: Class<*>, magicFilter: MagicFilter): DBFilter {
        val dbFilter = DBFilter()
        dbFilter.deleted = magicFilter.deleted
        dbFilter.maxRows = magicFilter.maxRows
        dbFilter.searchHistory = magicFilter.searchHistory
        dbFilter.sortAndLimitMaxRowsWhileSelect = magicFilter.sortAndLimitMaxRowsWhileSelect
        dbFilter.sortProperties = magicFilter.sortProperties
        for (magicFilterEntry in magicFilter.entries) {
            // Workarround for frontend-bug: (search string without field is given as field, not as value:
            if (magicFilterEntry.value.isNullOrBlank()) {
                // Full text search (no field given).
                dbFilter.entries.add(DBFilterEntry(value = magicFilterEntry.field, fulltextSearch = true))
            } else if (magicFilterEntry.field.isNullOrBlank()) {
                // Full text search (no field given).
                dbFilter.entries.add(DBFilterEntry(value = magicFilterEntry.value, fulltextSearch = true))
            } else {
                // Field search.
                dbFilter.entries.add(createFieldSearchEntry(entityClass, magicFilterEntry))
            }
        }
        return dbFilter
    }

    internal fun createFieldSearchEntry(entityClass: Class<*>, magicFilterEntry: MagicFilterEntry): DBFilterEntry {
        val entry = DBFilterEntry()
        entry.field = magicFilterEntry.field
        entry.value = magicFilterEntry.value
        entry.fulltextSearch = false
        val fieldType = PropUtils.getField(entityClass, entry.field)?.type ?: String::class.java
        entry.type = fieldType
        if (fieldType == String::class.java) {
            entry.searchType = if (entry.field.isNullOrBlank()) SearchType.STRING_SEARCH else SearchType.FIELD_STRING_SEARCH
            val str = magicFilterEntry.value?.trim() ?: ""
            var plainStr = str
            val dbStr: String
            if (str.startsWith("*")) {
                plainStr = plainStr.substring(1)
                if (str.endsWith("*")) {
                    plainStr = plainStr.substring(0, plainStr.lastIndex)
                    dbStr = "%$plainStr%"
                    entry.matchType = MatchType.CONTAINS
                } else {
                    dbStr = "%$plainStr"
                    entry.matchType = MatchType.STARTS_WITH
                }
            } else {
                if (str.endsWith("*")) {
                    plainStr = plainStr.substring(0, plainStr.lastIndex)
                    dbStr = "$plainStr%"
                    entry.matchType = MatchType.ENDS_WITH
                } else {
                    entry.matchType = MatchType.EXACT
                    dbStr = plainStr
                }
            }
            entry.plainSearchString = plainStr
            entry.dbSearchString = dbStr.toLowerCase()
        } else if (fieldType == Date::class.java) {
            entry.fromValueDate = PFDateTime.parseUTCDate(magicFilterEntry.fromValue)
            entry.toValueDate = PFDateTime.parseUTCDate(magicFilterEntry.toValue)
        } else if (fieldType == Integer::class.java) {
            entry.valueInt = NumberHelper.parseInteger(magicFilterEntry.value)
            entry.fromValueInt = NumberHelper.parseInteger(magicFilterEntry.fromValue)
            entry.toValueInt = NumberHelper.parseInteger(magicFilterEntry.toValue)
        } else if (BaseDO::class.java.isAssignableFrom(fieldType)) {
            entry.valueInt = NumberHelper.parseInteger(magicFilterEntry.value)
        } else {
            log.warn("Search entry of type '${fieldType.name}' not yet supported for field '${entry.field}'.")
        }
        return entry
    }

    fun setCacheRegion(baseDao: BaseDao<*>, criteria: Criteria) {
        criteria.setCacheable(true)
        if (!baseDao.useOwnCriteriaCacheRegion()) {
            return
        }
        criteria.setCacheRegion(baseDao.javaClass.name)
    }

    private val log = LoggerFactory.getLogger(MagicFilterProcessor::class.java)
}
