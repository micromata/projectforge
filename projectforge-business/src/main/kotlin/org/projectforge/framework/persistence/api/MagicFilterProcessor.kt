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

import org.projectforge.business.task.TaskDO
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import java.util.*

/** Transforms MagicFilterEntries to DBFilterExpressions. */
object MagicFilterProcessor {
    fun doIt(entityClass: Class<*>, magicFilter: MagicFilter): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.deleted = magicFilter.deleted
        queryFilter.maxRows = magicFilter.maxRows
        queryFilter.searchHistory = magicFilter.searchHistory
        queryFilter.sortAndLimitMaxRowsWhileSelect = magicFilter.sortAndLimitMaxRowsWhileSelect
        queryFilter.sortProperties = magicFilter.sortProperties.map {
            var property = it.property
            if (property.indexOf('.') > 0)
                property = property.substring(property.indexOf('.') + 1)
            SortProperty(property, it.sortOrder)
        }.toMutableList()
        for (magicFilterEntry in magicFilter.entries) {
            if (magicFilterEntry.field.isNullOrBlank()) {
                // Full text search (no field given).
                queryFilter.addFullTextSearch(magicFilterEntry.value.value)
            } else {
                // Field search.
                createFieldSearchEntry(entityClass, queryFilter, magicFilterEntry, magicFilter.autoWildcardSearch)
            }
        }
        return queryFilter
    }

    internal fun createFieldSearchEntry(entityClass: Class<*>, queryFilter: QueryFilter, magicFilterEntry: MagicFilterEntry, autoWildcardSearch: Boolean) {
        val field = magicFilterEntry.field!!
        if (isHistoryEntry(field)) {
            if (isModifiedInterval(field)) {
                queryFilter.modifiedFrom = PFDateTime.parseUTCDate(magicFilterEntry.value.fromValue)
                queryFilter.modifiedTo = PFDateTime.parseUTCDate(magicFilterEntry.value.toValue)
            } else if (isModifiedByUserId(field)) {
                queryFilter.modifiedByUserId = magicFilterEntry.value.value?.toIntOrNull()
            }
            return
        }
        val fieldType = PropUtils.getField(entityClass, field)?.type ?: String::class.java
        if (fieldType == String::class.java) {
            val str = magicFilterEntry.value.value?.trim() ?: ""
            val predicate = DBPredicate.Like(field, str, autoWildcardSearch = autoWildcardSearch)
            queryFilter.add(predicate)
            return
        }
        if (fieldType == Date::class.java) {
            val valueDate = PFDateTime.parseUTCDate(magicFilterEntry.value.value)?.utilDate
            val fromDate = PFDateTime.parseUTCDate(magicFilterEntry.value.fromValue)?.utilDate
            val toDate = PFDateTime.parseUTCDate(magicFilterEntry.value.toValue)?.utilDate
            if (fromDate != null || toDate != null) {
                queryFilter.add(QueryFilter.interval(field, fromDate, toDate))
            } else if (valueDate != null) {
                queryFilter.add(QueryFilter.eq(field, valueDate))
            } else {
                queryFilter.add(QueryFilter.isNull(field))
            }
        } else if (fieldType == Integer::class.java) {
            val valueInt = NumberHelper.parseInteger(magicFilterEntry.value.value)
            val fromInt = NumberHelper.parseInteger(magicFilterEntry.value.fromValue)
            val toInt = NumberHelper.parseInteger(magicFilterEntry.value.toValue)
            if (fromInt != null || toInt != null) {
                queryFilter.add(QueryFilter.interval(field, fromInt, toInt))
            } else if (valueInt != null) {
                queryFilter.add(QueryFilter.eq(field, valueInt))
            } else {
                queryFilter.add(QueryFilter.isNull(field))
            }
        } else if (TaskDO::class.java.isAssignableFrom(fieldType)) {
            val valueInt = magicFilterEntry.value.value?.toIntOrNull()
            queryFilter.add(QueryFilter.taskSearch(field, valueInt, true))
        } else if (BaseDO::class.java.isAssignableFrom(fieldType)) {
            val valueInt = magicFilterEntry.value.value?.toIntOrNull()
            if (valueInt != null) {
                queryFilter.add(QueryFilter.eq(field, valueInt))
            } else {
                queryFilter.add(QueryFilter.isNull(field))
            }
        } else {
            log.warn("Search entry of type '${fieldType.name}' not yet supported for field '$field'.")
        }
    }

    internal fun isHistoryEntry(field: String?): Boolean {
        if (field == null)
            return false
        for (historySearch in MagicFilterEntry.HistorySearch.values()) {
            if (historySearch.fieldName == field) {
                return true
            }
        }
        return false
    }

    internal fun isModifiedInterval(field: String?): Boolean {
        return field == MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.fieldName
    }

    internal fun isModifiedByUserId(field: String?): Boolean {
        return field == MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName
    }

    private val log = LoggerFactory.getLogger(MagicFilterProcessor::class.java)
}
