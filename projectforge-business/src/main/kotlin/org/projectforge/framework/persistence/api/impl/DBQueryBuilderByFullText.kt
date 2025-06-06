/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.projectforge.common.logging.LogUtils.logDebugFunCall
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.SortProperty
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * Query builder for full text search.
 */
internal class DBQueryBuilderByFullText<O : ExtendedBaseDO<Long>>(
    private val baseDao: BaseDao<O>,
    private val entityManager: EntityManager,
    val useMultiFieldQueryParser: Boolean = false
) {

    private val sortOrders = mutableListOf<SortProperty>()
    internal val searchClassInfo: HibernateSearchClassInfo

    init {
        if (useMultiFieldQueryParser) {
            throw UnsupportedOperationException("MultiFieldQueryParser not yet implemented.")
        }
        logDebugFunCall(log) { it.mtd("init") }
        searchClassInfo = HibernateSearchMeta.getClassInfo(baseDao.doClass)
    }

    fun formatMultiParserValue(field: String, value: Any): String {
        logDebugFunCall(log) {
            it.mtd("formatMultiParserValue(field, value)").params("field" to field, "value" to value)
        }
        return when (value) {
            is java.time.LocalDate -> {
                localDateFormat.format(value)
            }

            is java.sql.Date -> {
                //if (searchClassInfo.get(field)?.getDateBridgeEncodingType() == EncodingType.NUMERIC) {
                //    value.time.toString()
                //} else {
                SimpleDateFormat("yyyyMMdd").format(value)
                //}
            }

            is java.sql.Timestamp -> {
                //if (searchClassInfo.get(field)?.getDateBridgeEncodingType() == EncodingType.NUMERIC) {
                //    value.time.toString()
                //} else {
                SimpleDateFormat("yyyyMMddHHmmssSSS").format(value)
                //}
            }

            is java.util.Date -> {
                //if (searchClassInfo.get(field)?.getDateBridgeEncodingType() == EncodingType.NUMERIC) {
                (value.time / 60000).toString()
                //} else {
                //    SimpleDateFormat("yyyyMMddHHmmssSSS").format(value)
                //}
            }

            else -> "$value"
        }
    }

    fun and(vararg predicates: DBPredicate) {
        logDebugFunCall(log) {
            it.mtd("and(predicates)").params("predicates" to predicates.joinToString { it.javaClass.simpleName })
        }
        if (predicates.isEmpty()) return
        if (useMultiFieldQueryParser) {
            for (predicate in predicates) {
                // TODO: predicate.addTo(this)
            }
        } else {
            predicates.forEach {
                // TODO: it.addTo(this)
            }
        }
    }

    /**
     * @param resultPredicates List of predicates to be used for filtering the result list afterward (not handled by full text search).
     */
    fun createResultIterator(
        fullTextPredicates: List<DBPredicate>,
        resultPredicates: List<DBPredicate>
    ): DBResultIterator<O> {
        return DBFullTextResultIterator(
            baseDao,
            entityManager,
            fullTextPredicates = fullTextPredicates,
            resultMatchers = resultPredicates,
            sortOrders.toTypedArray(),
        )
    }

    fun addOrder(sortProperty: SortProperty) {
        sortOrders.add(sortProperty)
    }

    companion object {
        private val localDateFormat = DateTimeFormatter.ofPattern("'+00000'yyyyMMdd")
    }
}
