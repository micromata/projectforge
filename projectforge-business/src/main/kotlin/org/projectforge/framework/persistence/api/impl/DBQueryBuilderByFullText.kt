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

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.apache.commons.lang3.math.NumberUtils
import org.hibernate.search.engine.search.common.BooleanOperator
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep
import org.hibernate.search.mapper.orm.Search
import org.projectforge.common.logging.LogUtils.logDebugFunCall
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
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
    /**
     * Only for fall back to criteria search if no predicates found for full text search.
     */
    private val queryFilter: QueryFilter,
    val useMultiFieldQueryParser: Boolean = false
) {

    private var searchQueryOptionsStep: SearchQueryOptionsStep<*, *, *, *, *>
    private lateinit var searchPredicateFactory: SearchPredicateFactory
    private lateinit var boolCollector: BooleanPredicateOptionsCollector<*>
    private val searchSession = Search.session(entityManager)
    private val sortOrders = mutableListOf<SortProperty>()
    private val multiFieldQuery = mutableListOf<String>()
    private val searchClassInfo: HibernateSearchClassInfo
    private var simpleSearchStringAvailable = false


    init {
        if (useMultiFieldQueryParser) {
            throw UnsupportedOperationException("MultiFieldQueryParser not yet implemented.")
        }
        logDebugFunCall(log) { it.mtd("init") }
        searchQueryOptionsStep = searchSession.search(baseDao.doClass).where { f ->
            searchPredicateFactory = f
            f.bool().with { bool ->
                boolCollector = bool
                //b.must(f.match().field("field1").matching("value1"))
                //bool.must(f.range().field("age").atLeast(minAge))
                //b.should(f.match().field("field2").matching("value2"))
                //b.mustNot(f.match().field("field3").matching("value3"))
            }
        }
        searchClassInfo = HibernateSearchMeta.getClassInfo(baseDao)
    }

    private fun fieldSupported(field: String?): Boolean {
        field ?: return false
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField == null) {
            logDebugFunCall(log) {
                it.mtd("fieldSupported(field)").msg("field not supported (no hibernate search field)")
                    .params("field" to field)
            }
        } else {
            logDebugFunCall(log) { it.mtd("fieldSupported(field)").msg("field supported").params("field" to field) }
        }
        return luceneField != null
    }

    private fun fieldSupported(predicate: DBPredicate): Boolean {
        return fieldSupported(predicate.field)
    }

    /**
     * @return true, if the predicate was added to query builder, otherwise false (must be handled as result predicate instead).
     */
    fun add(predicate: DBPredicate): Boolean {
        logDebugFunCall(log) { it.mtd("add(predicate)").params("predicate" to predicate.javaClass.simpleName) }
        if (!predicate.fullTextSupport) {
            log.debug { "add(predicate): Ignoring predicate $predicate (no fullTextSupport)." }
            return false
        }
        if (predicate !is DBPredicate.FullSearch && !fieldSupported(predicate)) {
            return false
        }
        log.debug { "add(predicate): Adding full text predicate $predicate." }
        predicate.addTo(this)
        return true
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun equal(field: String, value: Any): Boolean {
        logDebugFunCall(log) { it.mtd("equal(field, value)").params("field" to field, "value" to value) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField == null) {
            logDebugNoHibernateSearchField("equal(field, value)", field, value)
            return false
        }
        log.debug { "equal(field, value): Adding field '$field' (no HibernateSearch field)." }
        /*if (useMultiFieldQueryParser) {
            val valueString = formatMultiParserValue(field, value)
            if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [equal] +$luceneField:$valueString")
            multiFieldQuery.add("+$luceneField:$valueString")
        } else {*/
        log.debug { "Adding fulltext search (${baseDao.doClass.simpleName}): [equal] boolJunction.must(qb.keyword().onField('$field').matching('$value')...)" }
        boolCollector.must(searchPredicateFactory.match().field(field).matching(value))
        //}
        return true
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun notEqual(field: String, value: Any): Boolean {
        logDebugFunCall(log) { it.mtd("notEqual(field, value)").params("field" to field, "value" to value) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField == null) {
            logDebugNoHibernateSearchField("notEqual(field, value)", field, value)
            return false
        }
        if (useMultiFieldQueryParser) {
            val valueString = formatMultiParserValue(field, value)
            if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [notEqual] -$luceneField:$valueString")
            multiFieldQuery.add("-$luceneField:$valueString")
        } else {
            if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [notEqual] boolJunction.must(qb.keyword().onField('$luceneField').matching('$value')...).not()")
            boolCollector.mustNot(searchPredicateFactory.match().field(luceneField).matching(value))
        }
        return true
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> between(field: String, from: O, to: O): Boolean {
        logDebugFunCall(log) {
            it.mtd("between(field, from, to)").params("field" to field, "from" to from, "to" to to)
        }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                val toString = formatMultiParserValue(field, to)
                log.debug { "Adding multifieldQuery (${baseDao.doClass.simpleName}): [between] +$luceneField:[$fromString TO $toString]" }
                multiFieldQuery.add("+$luceneField:[$fromString TO $toString]")
            } else {
                log.debug { "Adding fulltext search (${baseDao.doClass.simpleName}): [between] boolJunction.must(qb.range().onField('$luceneField').from('$from').to('$to')...)" }
                boolCollector.must(searchPredicateFactory.range().field(luceneField).between(from, to))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> greater(field: String, from: O): Boolean {
        logDebugFunCall(log) { it.mtd("greater(field, from)").params("field" to field, "from" to from) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                log.debug { "Adding multifieldQuery (${baseDao.doClass.simpleName}): [greater] +$luceneField:{$fromString TO *}" }
                multiFieldQuery.add("+$luceneField:{$fromString TO *}")
            } else {
                log.debug { "Adding fulltext search (${baseDao.doClass.simpleName}): [greater] boolJunction.must(qb.range().onField('$luceneField').above('$from').excludeLimit()...)" }
                boolCollector.must(searchPredicateFactory.range().field(luceneField).greaterThan(from))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> greaterEqual(field: String, from: O): Boolean {
        logDebugFunCall(log) { it.mtd("greaterEqual(field, from)").params("field" to field, "from" to from) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                log.debug { "Adding multifieldQuery (${baseDao.doClass.simpleName}): [greaterEqual] +$luceneField:[$fromString TO *]" }
                multiFieldQuery.add("+$luceneField:[$fromString TO *]")
            } else {
                log.debug { "Adding fulltext search (${baseDao.doClass.simpleName}): [greaterEqual] boolJunction.must(qb.range().onField('$luceneField').above('$from')...)" }
                boolCollector.must(searchPredicateFactory.range().field(luceneField).atLeast(from))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> less(field: String, to: O): Boolean {
        logDebugFunCall(log) { it.mtd("less(field, to)").params("field" to field, "to" to to) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val toString = formatMultiParserValue(field, to)
                log.debug { "Adding multifieldQuery (${baseDao.doClass.simpleName}): [less] +$luceneField:{* TO $toString}" }
                multiFieldQuery.add("+$luceneField:{* TO $toString}")
            } else {
                log.debug { "Adding fulltext search (${baseDao.doClass.simpleName}): [less] boolJunction.must(qb.range().below('$luceneField').excludeLimit()...)" }
                boolCollector.must(searchPredicateFactory.range().field(luceneField).lessThan(to))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> lessEqual(field: String, to: O): Boolean {
        logDebugFunCall(log) { it.mtd("lessEqual(field, to)").params("field" to field, "to" to to) }
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val toString = formatMultiParserValue(field, to)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [lessEqual] +$luceneField:[* TO $toString]")
                multiFieldQuery.add("+$luceneField:[* TO $toString]")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [lessEqual] boolJunction.must(qb.range().below('$luceneField')...)")
                boolCollector.must(searchPredicateFactory.range().field(luceneField).atMost(to))
            }
            return true
        }
        return false
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

    fun ilike(field: String, value: String) {
        logDebugFunCall(log) { it.mtd("ilike(field, value)").params("field" to field, "value" to value) }
        search(value, field)
    }

    fun and(vararg predicates: DBPredicate) {
        logDebugFunCall(log) {
            it.mtd("and(predicates)").params("predicates" to predicates.joinToString { it.javaClass.simpleName })
        }
        if (predicates.isEmpty()) return
        if (useMultiFieldQueryParser) {
            for (predicate in predicates) {
                predicate.addTo(this)
            }
        } else {
            predicates.forEach {
                it.addTo(this)
            }
        }
    }

    fun fulltextSearch(searchString: String) {
        logDebugFunCall(log) { it.mtd("fulltextSearch(searchString)").params("searchString" to searchString) }
        if (searchClassInfo.numericFieldNames.isNotEmpty() && NumberUtils.isCreatable(searchString)) {
            val number = NumberUtils.createNumber(searchString)
            search(number, *searchClassInfo.numericFieldNames)
        } else if (queryFilter.fullTextSearchFields.isNullOrEmpty()) {
            search(searchString, *searchClassInfo.stringFieldNames)
        } else {
            search(searchString, *queryFilter.fullTextSearchFields!!)
        }
    }

    /**
     * @param resultPredicates List of predicates to be used for filtering the result list afterward (not handled by full text search).
     */
    fun createResultIterator(resultPredicates: List<DBPredicate>): DBResultIterator<O> {
        return when {
            /*useMultiFieldQueryParser -> {
                DBFullTextResultIterator(
                    baseDao,
                    searchSession,
                    resultPredicates,
                    queryFilter,
                    sortOrders.toTypedArray(),
                    multiFieldQuery = multiFieldQuery
                )
            }*/

            !simpleSearchStringAvailable && !boolCollector.hasClause() -> { // Shouldn't occur:
                // Neither simpleSearchString nor where claus available, so use normal criteria search without where clause.
                DBQueryBuilderByCriteria(baseDao, entityManager, queryFilter).createResultIterator(resultPredicates)
            }

            else -> {
                DBFullTextResultIterator(
                    baseDao,
                    searchSession,
                    resultPredicates,
                    queryFilter,
                    sortOrders.toTypedArray(),
                    searchQueryOptionsStep = searchQueryOptionsStep,
                )
            }
        }
    }

    private fun search(value: String, vararg fields: String) {
        if (value.isBlank()) {
            return
        }
        logDebugFunCall(log) {
            it.mtd("search(value, fields)").params("value" to value, "fields" to fields.joinToString())
        }
        searchPredicateFactory.simpleQueryString()
            .fields(*fields)
            .matching(value) // search in Lucene format
            .defaultOperator(BooleanOperator.AND) // Default is OR
        simpleSearchStringAvailable = true
    }

    private fun search(value: Number, vararg fields: String) {
        search(value.toString(), *fields)
    }

    fun addOrder(sortProperty: SortProperty) {
        sortOrders.add(sortProperty)
    }

    private fun logDebugNoHibernateSearchField(method: String, field: String, value: Any?) {
        if (!log.isDebugEnabled) {
            return
        }
        log.debug { "$method: Ignoring field '$field' (no HibernateSearch field), value=$value" }
    }

    companion object {
        private val localDateFormat = DateTimeFormatter.ofPattern("'+00000'yyyyMMdd")
    }
}
