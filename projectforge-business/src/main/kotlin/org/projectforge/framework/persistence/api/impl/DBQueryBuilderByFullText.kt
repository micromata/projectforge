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
import org.apache.commons.lang3.math.NumberUtils
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep
import org.hibernate.search.mapper.orm.Search
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

/**
 * Query builder for full text search.
 */
internal class DBQueryBuilderByFullText<O : ExtendedBaseDO<Long>>(
    private val baseDao: BaseDao<O>, private val entityManager: EntityManager,
    /**
     * Only for fall back to criteria search if no predicates found for full text search.
     */
    private val queryFilter: QueryFilter, val useMultiFieldQueryParser: Boolean = false
) {

    private val log = LoggerFactory.getLogger(DBQueryBuilderByFullText::class.java)

    private var searchQueryOptionsStep: SearchQueryOptionsStep<*, *, *, *, *>
    private lateinit var searchPredicateFactory: SearchPredicateFactory
    private lateinit var boolJunction: BooleanPredicateOptionsCollector<*>
    private val searchSession = Search.session(entityManager)
    private val sortOrders = mutableListOf<SortProperty>()
    private val multiFieldQuery = mutableListOf<String>()
    private val searchClassInfo: HibernateSearchClassInfo

    init {
        searchQueryOptionsStep = searchSession.search(baseDao.doClass).where { f ->
            searchPredicateFactory = f
            f.bool().with { b ->
                boolJunction = b
                //b.must(f.match().field("field1").matching("value1"))
                //b.should(f.match().field("field2").matching("value2"))
                //b.mustNot(f.match().field("field3").matching("value3"))
            }
        }
        searchClassInfo = HibernateSearchMeta.getClassInfo(baseDao)
    }

    fun fieldSupported(field: String): Boolean {
        return searchClassInfo.containsField(field)
    }

    /*
        val query = searchSession.search(TaskDO::class.java)
            .where { f ->
                f.bool().with { b ->
                    b.must(f.match().field("field1").matching("value1"))
                    b.should(f.match().field("field2").matching("value2"))
                    b.mustNot(f.match().field("field3").matching("value3"))
                }
            }
            */

    /**
     * @return true, if the predicate was added to query builder, otherwise false (must be handled as result predicate instead).
     */
    fun add(predicate: DBPredicate): Boolean {
        if (!predicate.fullTextSupport) return false
        val field = predicate.field
        if (field != null && !searchClassInfo.containsField(field)) return false
        predicate.addTo(this)
        return true
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun equal(field: String, value: Any): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val valueString = formatMultiParserValue(field, value)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [equal] +$luceneField:$valueString")
                multiFieldQuery.add("+$luceneField:$valueString")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [equal] boolJunction.must(qb.keyword().onField('$luceneField').matching('$value')...)")
                boolJunction.must(searchPredicateFactory.match().field(luceneField).matching(value))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun notEqual(field: String, value: Any): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val valueString = formatMultiParserValue(field, value)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [notEqual] -$luceneField:$valueString")
                multiFieldQuery.add("-$luceneField:$valueString")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [notEqual] boolJunction.must(qb.keyword().onField('$luceneField').matching('$value')...).not()")
                boolJunction.mustNot(searchPredicateFactory.match().field(luceneField).matching(value))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> between(field: String, from: O, to: O): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                val toString = formatMultiParserValue(field, to)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [between] +$luceneField:[$fromString TO $toString]")
                multiFieldQuery.add("+$luceneField:[$fromString TO $toString]")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [between] boolJunction.must(qb.range().onField('$luceneField').from('$from').to('$to')...)")
                boolJunction.must(searchPredicateFactory.range().field(luceneField).between(from, to))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> greater(field: String, from: O): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [greater] +$luceneField:{$fromString TO *}")
                multiFieldQuery.add("+$luceneField:{$fromString TO *}")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [greater] boolJunction.must(qb.range().onField('$luceneField').above('$from').excludeLimit()...)")
                boolJunction.must(searchPredicateFactory.range().field(luceneField).greaterThan(from))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> greaterEqual(field: String, from: O): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val fromString = formatMultiParserValue(field, from)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [greaterEqual] +$luceneField:[$fromString TO *]")
                multiFieldQuery.add("+$luceneField:[$fromString TO *]")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [greaterEqual] boolJunction.must(qb.range().onField('$luceneField').above('$from')...)")
                boolJunction.must(searchPredicateFactory.range().field(luceneField).atLeast(from))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> less(field: String, to: O): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val toString = formatMultiParserValue(field, to)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [less] +$luceneField:{* TO $toString}")
                multiFieldQuery.add("+$luceneField:{* TO $toString}")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [less] boolJunction.must(qb.range().below('$luceneField').excludeLimit()...)")
                boolJunction.must(searchPredicateFactory.range().field(luceneField).lessThan(to))
            }
            return true
        }
        return false
    }

    /**
     * @return true if the given field is indexed, otherwise false (dbMatcher should be used instead).
     */
    fun <O : Comparable<O>> lessEqual(field: String, to: O): Boolean {
        val luceneField = searchClassInfo.get(field)?.luceneField
        if (luceneField != null) {
            if (useMultiFieldQueryParser) {
                val toString = formatMultiParserValue(field, to)
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [lessEqual] +$luceneField:[* TO $toString]")
                multiFieldQuery.add("+$luceneField:[* TO $toString]")
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [lessEqual] boolJunction.must(qb.range().below('$luceneField')...)")
                boolJunction.must(searchPredicateFactory.range().field(luceneField).atMost(to))
            }
            return true
        }
        return false
    }

    fun formatMultiParserValue(field: String, value: Any): String {
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
        search(value, field)
    }

    fun and(vararg predicates: DBPredicate) {
        if (predicates.isNullOrEmpty()) return
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
        if (searchClassInfo.numericFieldNames.isNotEmpty() && NumberUtils.isCreatable(searchString)) {
            val number = NumberUtils.createNumber(searchString)
            search(number, *searchClassInfo.numericFieldNames)
        } else if (queryFilter.fullTextSearchFields.isNullOrEmpty()) {
            search(searchString, *searchClassInfo.stringFieldNames)
        } else {
            search(searchString, *queryFilter.fullTextSearchFields!!)
        }
    }

    fun createResultIterator(resultPredicates: List<DBPredicate>): DBResultIterator<O> {
        return when {
            useMultiFieldQueryParser -> {
                DBFullTextResultIterator(
                    baseDao,
                    searchSession,
                    resultPredicates,
                    queryFilter,
                    sortOrders.toTypedArray(),
                    multiFieldQuery = multiFieldQuery
                )
            }

            !boolJunction.hasClause() -> { // Shouldn't occur:
                // No restrictions found, so use normal criteria search without where clause.
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
        if (useMultiFieldQueryParser) {
            if (fields.isNotEmpty() && fields.size == 1) {
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [search] ${fields[0]}:$value")
                multiFieldQuery.add("${fields[0]}:$value")
            } else {
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [search] $value")
                multiFieldQuery.add(value)
            }
        } else {
            val context = if (value.indexOf('*') >= 0) {
                if (fields.size > 1) {
                    if (log.isDebugEnabled) log.debug(
                        "Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.keyword().wildcard().onFields(*).matching('$value')...): fields:${
                            fields.joinToString(
                                ", "
                            )
                        }"
                    )
                    searchPredicateFactory.wildcard().fields(*fields)
                } else {
                    if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.keyword().wildcard().onField('${fields[0]}').matching('$value')...)")
                    searchPredicateFactory.wildcard().field(fields[0])
                }
            } else {
                if (fields.size > 1) {
                    if (log.isDebugEnabled) log.debug(
                        "Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.keyword().onFields(*).matching('$value')...): fields:${
                            fields.joinToString(
                                ", "
                            )
                        }"
                    )
                    searchPredicateFactory.match().fields(*fields)
                } else {
                    if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.keyword().onField('${fields[0]}').matching('$value')...)")
                    searchPredicateFactory.match().field(fields[0])
                }
            }
            // boolJunction = boolJunction.must(context.ignoreAnalyzer().matching(value.lowercase()).createQuery())
            boolJunction.must { p ->
                val step = if (fields.size > 1) {
                    p.match().fields(*fields)
                } else {
                    p.match().field(fields[0])
                }
                step.matching(value.lowercase()).skipAnalysis()
            }
        }
    }

    private fun search(value: Number, vararg fields: String) {
        if (useMultiFieldQueryParser) {
            if (fields.isNotEmpty() && fields.size == 1) {
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [search] ${fields[0]}:$value")
                multiFieldQuery.add("${fields[0]}:$value")
            } else {
                if (log.isDebugEnabled) log.debug("Adding multifieldQuery (${baseDao.doClass.simpleName}): [search] $value")
                multiFieldQuery.add("$value")
            }
        } else {
            val context = if (fields.size > 1) {
                if (log.isDebugEnabled) log.debug(
                    "Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.range().onFields(*).above/below($value)...): fields:${
                        fields.joinToString(
                            ", "
                        )
                    }"
                )
                searchPredicateFactory.range().fields(*fields)
                /*for (idx in 1 until fields.size - 1) {
                    ctx = ctx.andField(fields[idx])
                }*/
            } else {
                if (log.isDebugEnabled) log.debug("Adding fulltext search (${baseDao.doClass.simpleName}): [search] boolJunction.must(qb.keyword().onField('${fields[0]}').matching($value)...)")
                searchPredicateFactory.range().field(fields[0])
            }
            // TODO: This is not working:
            log.warn("This function park is not working, and has never been? Called for fields ${fields.joinToString()} and $value")
            boolJunction.must(context.lessThan(value))
            boolJunction.must(context.greaterThan(value))
        }
    }

    fun addOrder(sortProperty: SortProperty) {
        sortOrders.add(sortProperty)
    }

    companion object {
        private val localDateFormat = DateTimeFormatter.ofPattern("'+00000'yyyyMMdd")
    }
}
