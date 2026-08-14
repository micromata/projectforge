/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.persistence.api.impl.HibernateSearchMeta
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/** Transforms MagicFilterEntries to DBFilterExpressions. */
object MagicFilterProcessor {
    fun doIt(entityClass: Class<*>, magicFilter: MagicFilter, queryFilter: QueryFilter = QueryFilter()): QueryFilter {
        // Deleted flag is also supported as entry:
        magicFilter.deleted = magicFilter.entries.find { it.field == "deleted" }?.value?.value == "true"
        // History search is also supported as entry:
        magicFilter.entries.find { it.field == MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.fieldName }?.value?.value?.let {
            magicFilter.searchHistory = it
        }
        // History user may also be given as id instead of value::
        /*magicFilter.entries.find { it.field == MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName }?.let { entry ->
            entry.value.id?.let { id ->
                entry.value = MagicFilterEntry.Value(id.toString())
            }
        }*/

        queryFilter.deleted = magicFilter.deleted
        /* Not yet implemented (done by frontend, not in database):
        magicFilter.paginationPageSize?.let {
            // queryFilter.paginationPageSize = it
        }*/
        queryFilter.maxRows = magicFilter.maxRows

        queryFilter.searchHistory = magicFilter.searchHistory
        queryFilter.sortAndLimitMaxRowsWhileSelect = magicFilter.sortAndLimitMaxRowsWhileSelect
        queryFilter.sortProperties = magicFilter.sortProperties.map {
            SortProperty(resolveSortProperty(entityClass, it.property), it.sortOrder)
        }.toMutableList()
        queryFilter.extended = magicFilter.extended
        val searchString = magicFilter.searchString;
        if (!searchString.isNullOrBlank()) {
            queryFilter.addFullTextSearch(
                DBPredicate.modifySearchString(
                    searchString,
                    '*',
                    '%',
                    magicFilter.autoWildcardSearch
                )
            )
        }
        for (magicFilterEntry in magicFilter.entries) {
            if (magicFilterEntry.synthetic == true) {
                continue
            } else if (magicFilterEntry.field.isNullOrBlank()) {
                // Full text search (no field given).
                queryFilter.addFullTextSearch(magicFilterEntry.value.value)
            } else if (magicFilterEntry.field == MagicFilter.PAGINATION_PAGE_SIZE) {
                // Do nothing.
            } else if (magicFilterEntry.field == "pageSize") { // was renamed from pageSize to paginationPageSize:
                magicFilterEntry.field = MagicFilter.PAGINATION_PAGE_SIZE
            } else {
                // Field search.
                createFieldSearchEntry(entityClass, queryFilter, magicFilterEntry, magicFilter.autoWildcardSearch)
            }
        }
        return queryFilter
    }

    /**
     * The entity property a column's sort id refers to.
     *
     * A dotted id can mean either of two things, and the leading segment tells them apart:
     *
     * - a **path through the entity**, `kunde.displayName` or `projekt.kunde.name` of an `AuftragDO` —
     *   kept whole, because the criteria builder can join and order by it (see
     *   [DBCriteriaContext.getOrderField]). It used to be shortened unconditionally, which turned
     *   `kunde.displayName` into `displayName` — a property no `AuftragDO` has, so `addOrder` failed,
     *   logged "Can't add order" and left the order book in whatever order the database returned.
     * - a path through a **DTO** that names the wrapper the entity has no property for:
     *   `fibu.employee.user.lastname` of an `EmployeeSalary`. Those leading segments are dropped until
     *   one names a property of the entity.
     *
     * The last segment is deliberately not checked: `displayName` and the other computed values are
     * getters without a backing field, which no reflection over fields can confirm. An unorderable
     * segment is reported by `addOrder` — and a rest class may handle it in `postProcessMagicFilter`
     * beforehand, either by mapping it onto a real column (`Kost1PagesRest`) or by taking it out of the
     * query and sorting the loaded list instead (`OrderEntityRest`).
     */
    internal fun resolveSortProperty(entityClass: Class<*>, property: String): String {
        var result = property
        while (result.contains('.')) {
            val head = result.substringBefore('.')
            // suppressWarning: a DTO-only wrapper is the expected case here, not a defect.
            if (PropUtils.getField(entityClass, head, true) != null) {
                return result
            }
            result = result.substringAfter('.')
        }
        return result
    }

    internal fun createFieldSearchEntry(
        entityClass: Class<*>,
        queryFilter: QueryFilter,
        magicFilterEntry: MagicFilterEntry,
        autoWildcardSearch: Boolean
    ) {
        val field = magicFilterEntry.field!!
        if (isHistoryEntry(field)) {
            if (isModifiedInterval(field)) {
                queryFilter.modifiedFrom = PFDateTimeUtils.parseAndCreateDateTime(magicFilterEntry.value.fromValue)
                queryFilter.modifiedTo = PFDateTimeUtils.parseAndCreateDateTime(magicFilterEntry.value.toValue)
            } else if (isModifiedByUserId(field)) {
                queryFilter.modifiedByUserId = magicFilterEntry.value.id ?: magicFilterEntry.value.value?.toLongOrNull()
            }
            return
        }
        val fieldType = PropUtils.getField(entityClass, field)?.type
        if (fieldType == null) {
            log.debug { "Field '$field' not found in class '${entityClass.name}'." }
            HibernateSearchMeta.getClassInfo(entityClass).get(field)?.let { info ->
                log.debug { "Field '$field' found in HibernateMetaModel '${entityClass.name}'." }
                if (info.persistentField == false) {
                    // Field must not be a persistent field.
                    log.debug { "Field '$field' is persistent field." }
                    val str = magicFilterEntry.value.value?.trim() ?: ""
                    val predicate = DBPredicate.FullSearch(str, arrayOf(field))
                    queryFilter.add(predicate)
                    return
                } else {
                    log.warn { "Field '$field' is a persistent field, but not found via PropUtils of class ${entityClass.name}!?" }
                }
            }
            log.warn { "Field '$field' not found, ignoring in ${entityClass.name}!?" }
            return
        }
        if (fieldType == String::class.java) {
            val str = magicFilterEntry.value.value?.trim() ?: ""
            val predicate = DBPredicate.Like(field, str, autoWildcardSearch = autoWildcardSearch)
            queryFilter.add(predicate)
            return
        }
        if (fieldType == Date::class.java) {
            val valueDate = PFDateTimeUtils.parseAndCreateDateTime(magicFilterEntry.value.value)?.utilDate
            val fromDate = PFDateTimeUtils.parseAndCreateDateTime(magicFilterEntry.value.fromValue)?.utilDate
            val toDate = PFDateTimeUtils.parseAndCreateDateTime(magicFilterEntry.value.toValue)?.utilDate
            if (fromDate != null || toDate != null) {
                queryFilter.add(QueryFilter.interval(field, fromDate, toDate))
            } else if (valueDate != null) {
                queryFilter.add(QueryFilter.eq(field, valueDate))
            } else {
                queryFilter.add(QueryFilter.isNull(field))
            }
        } else if (fieldType == LocalDate::class.java) {
            val valueDate = PFDayUtils.parseDate(magicFilterEntry.value.value)
            val fromDate = PFDayUtils.parseDate(magicFilterEntry.value.fromValue)
            val toDate = PFDayUtils.parseDate(magicFilterEntry.value.toValue)
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
        } else if (fieldType == java.lang.Boolean::class.java) {
            val valueBoolean = magicFilterEntry.value.value == "true"
            queryFilter.add(QueryFilter.eq(field, valueBoolean))
        } else if (TaskDO::class.java.isAssignableFrom(fieldType)) {
            val valueLong = magicFilterEntry.value.value?.toLongOrNull()
            queryFilter.add(QueryFilter.taskSearch(field, valueLong, true))
        } else if (BaseDO::class.java.isAssignableFrom(fieldType)) {
            val valueInt = magicFilterEntry.value.value?.toIntOrNull()
            if (valueInt != null) {
                queryFilter.add(QueryFilter.eq(field, valueInt))
            } else {
                queryFilter.add(QueryFilter.isNull(field))
            }
        } else if (I18nEnum::class.java.isAssignableFrom(fieldType)) {
            val values = magicFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                // "no value set" is one of the selectable values, so it may arrive alone or beside real ones.
                val nullSelected = values.contains(MagicFilterEntry.NULL_VALUE)
                @Suppress("UNCHECKED_CAST")
                val enumConstants = fieldType.enumConstants as Array<Enum<*>>
                val list = values.filter { it != MagicFilterEntry.NULL_VALUE }.map { value ->
                    enumConstants.first { it.name == value }
                }
                val predicate = when {
                    !nullSelected -> DBPredicate.IsIn(field, list)
                    list.isEmpty() -> DBPredicate.IsNull(field)
                    // Both: the field is null *or* one of the chosen values.
                    else -> DBPredicate.Or(DBPredicate.IsNull(field), DBPredicate.IsIn(field, list))
                }
                queryFilter.add(predicate)
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
}
