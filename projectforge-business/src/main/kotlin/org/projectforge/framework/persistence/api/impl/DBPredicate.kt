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

import jakarta.persistence.criteria.Predicate
import mu.KotlinLogging
import org.apache.commons.lang3.math.NumberUtils
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory
import org.projectforge.common.BeanHelper
import org.projectforge.common.logging.LogUtils.logDebugFunCall
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * After querying, every result entry is matched against matchers (for fields not supported by the full text query).
 */
abstract class DBPredicate(
    val field: String?,
    /**
     * True if this predicate may be attached to a full text query for indexed fields.
     */
    open val fullTextSupport: Boolean = false,
    /**
     * True if this predicate may be attached to a criteria search (as predicate).
     */
    val criteriaSupport: Boolean = true,
    /**
     * True if this predicate may be checked while iterating the result set.
     */
    val resultSetSupport: Boolean = true
) {

    abstract fun match(obj: Any): Boolean

    /**
     * @return created JPA criteria predicate for this predicate, or null, if no predicate is needed.
     */
    internal abstract fun asPredicate(ctx: DBCriteriaContext<*>): Predicate?
    internal open fun handle(
        searchPredicateFactory: SearchPredicateFactory,
        boolCollector: BooleanPredicateOptionsCollector<*>,
        searchClassInfo: HibernateSearchClassInfo,
    ) {
        throw UnsupportedOperationException("Operation '${this.javaClass}' not supported by full text query.")
    }

    internal open fun supportedByFullTextQuery(searchClassInfo: HibernateSearchClassInfo): Boolean {
        if (!fullTextSupport || field == null) {
            return false
        }
        val luceneField = searchClassInfo.get(field)?.luceneField
        return if (luceneField == null) {
            logDebugFunCall(log) {
                it.mtd("${this.javaClass.simpleName}:handledByFullTextQuery").msg("Field '$field' not indexed.")
            }
            false
        } else {
            logDebugFunCall(log) {
                it.mtd("${this.javaClass.simpleName}:handledByFullTextQuery").msg("Field '$field' indexed.")
            }
            true
        }
    }

    /**
     * As Json
     */
    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }

    companion object {
        /**
         * Replaces trailing and leading '*' by '%' or vica versa. Appends '%' (or '*') to alphanumeric strings doesn't start or end with '%' or '*'.
         */
        internal fun modifySearchString(
            str: String,
            oldChar: Char,
            newChar: Char,
            autoWildcardSearch: Boolean = false
        ): String {
            logDebugFunCall(log) {
                it.mtd("modifySearchString(...)").params(
                    "str" to str,
                    "oldChar" to oldChar,
                    "newChar" to newChar,
                    "autoWildcardSearch" to autoWildcardSearch
                )
            }
            var queryString = str.trim()
            if (queryString.endsWith(oldChar))
                queryString = "${queryString.substring(0, queryString.length - 1)}$newChar"
            if (queryString.startsWith(oldChar))
                queryString = "$newChar${queryString.substring(1)}"
            else if (autoWildcardSearch)
                queryString = HibernateSearchFilterUtils.modifySearchString(
                    queryString,
                    "$newChar",
                    false
                ) // Always look for keyword* (\p{L} means all letters in all languages.
            logDebugFunCall(log) { it.mtd("modifySearchString(...)").msg("Modified search string: $queryString") }
            return queryString
        }
    }

    class Equal(field: String, val value: Any) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { isEquals(value, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            logDebugFunCall(log) {
                it.mtd("Equal.asPredicate(ctx)").msg("entity=${ctx.entityName},field=$field,value=$value")
            }
            return ctx.cb.equal(ctx.getField<Any>(field!!), value)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("Equal.handle(...)").msg("bool.must(f.match().field(\"$field\").matching($value))")
            }
            boolCollector.must(searchPredicateFactory.match().field(field).matching(value))
        }
    }

    class NotEqual(field: String, val value: Any) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { !isEquals(value, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            logDebugFunCall(log) {
                it.mtd("NotEqual.asPredicate(ctx)").msg("entity=${ctx.entityName},field=$field,value=$value")
            }
            return ctx.cb.notEqual(ctx.getField<Any>(field!!), value)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("NotEqual.handle(...)").msg("bool.mustNot(f.match().field(\"$field\").matching($value))")
            }
            boolCollector.mustNot(searchPredicateFactory.match().field(field).matching(value))
        }
    }

    class IsIn<T>(field: String, val values: Collection<T>) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            for (v in values) {
                if (isEquals(v, value)) {
                    return true
                }
            }
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select. If only one value is given, an equal predicate will
         * be returned.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate? {
            logDebugFunCall(log) {
                it.mtd("IsIn.asPredicate(ctx)").msg(
                    "entity=${ctx.entityName},field=$field,value=${
                        values.joinToString(
                            ", ",
                            "'",
                            "'"
                        )
                    }"
                )
            }
            if (values.isEmpty()) {
                log.debug { "Ignoring criteria search (${ctx.entityName}): [in] cb.in('$field'), because no value is given." }
                return null
            }
            if (values.size == 1) {
                log.debug { "Adding criteria search (${ctx.entityName}): [in] cb.equal('$field'), '${values.first()}') (uses equal, because only one value is given)." }
                return ctx.cb.equal(ctx.getField<Any>(field!!), values.first())
            }
            val inClause = ctx.cb.`in`(ctx.getField<Any>(field!!))
            for (value in values) {
                inClause.value(value)
            }
            return inClause
        }
    }

    class Between<T : Comparable<T>>(field: String, val from: T, val to: T) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (from::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return from <= value as T && value <= to
            }
            log.warn("Between operator fails, because value isn't of type ${from::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [between] cb.between($field, $from, $to)" }
            return ctx.cb.between(ctx.getField<T>(field!!), from, to)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("Between.handle(...)").msg("bool.must(f.range().field(\"$field\").between($from, $to))")
            }
            boolCollector.must(searchPredicateFactory.range().field(field).between(from, to))
        }
    }

    class Greater<O : Comparable<O>>(field: String, val from: O) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (value::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return from < value as O
            }
            log.warn { "Greater operator fails, because value isn't of type ${from::class.java}: $value" }
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [greater] cb.greaterThan($field, $from)" }
            return ctx.cb.greaterThan(ctx.getField<O>(field!!), from)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("Greater.handle(...)").msg("bool.must(f.range().field(\"$field\").greaterThan($from))")
            }
            boolCollector.must(searchPredicateFactory.range().field(field).greaterThan(from))
        }
    }

    class GreaterEqual<O : Comparable<O>>(field: String, val from: O) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (value::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return from <= value as O
            }
            log.warn("GreaterEqual operator fails, because value isn't of type ${from::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [greaterEqual] cb.greaterThanOrEqualTo($field, $from)" }
            return ctx.cb.greaterThanOrEqualTo(ctx.getField<O>(field!!), from)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("GreaterEqual.handle(...)").msg("bool.must(f.range().field(\"$field\").atLeast($from))")
            }
            boolCollector.must(searchPredicateFactory.range().field(field).atLeast(from))
        }
    }

    class Less<O : Comparable<O>>(field: String, val to: O) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (value::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return (value as O) < to
            }
            log.warn("Less operator fails, because value isn't of type ${to::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [less] cb.lessThan($field, $to)" }
            return ctx.cb.lessThan(ctx.getField<O>(field!!), to)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("Less.handle(...)").msg("bool.must(f.range().field(\"$field\").lessThan($to))")
            }
            boolCollector.must(searchPredicateFactory.range().field(field).lessThan(to))
        }
    }

    class LessEqual<O : Comparable<O>>(field: String, val to: O) : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (value::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return value as O <= to
            }
            log.warn("LessEqual operator fails, because value isn't of type ${to::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [lessEqual] cb.lessThanOrEqualTo($field, $to)" }
            return ctx.cb.lessThanOrEqualTo(ctx.getField<O>(field!!), to)
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            logDebugFunCall(log) {
                it.mtd("LessEqual.handle(...)").msg("bool.must(f.range().field(\"$field\").atMost($to))")
            }
            boolCollector.must(searchPredicateFactory.range().field(field).atMost(to))
        }
    }

    class Like(
        field: String,
        val expectedValue: String,
        private val ignoreCase: Boolean = true,
        autoWildcardSearch: Boolean = false
    ) : DBPredicate(field, true) {
        internal var plainString: String
        internal val matchType: MatchType
        internal var queryString: String

        init {
            queryString = modifySearchString(expectedValue, '*', '%', autoWildcardSearch)
            plainString = queryString
            if (plainString.startsWith("%")) {
                plainString = plainString.substring(1)
                if (plainString.endsWith('%')) {
                    matchType = MatchType.CONTAINS
                    plainString = plainString.substring(0, plainString.length - 1)
                } else {
                    matchType = MatchType.ENDS_WITH
                }
            } else if (plainString.endsWith('%')) {
                matchType = MatchType.STARTS_WITH
                plainString = plainString.substring(0, plainString.length - 1)
            } else {
                matchType = MatchType.EXACT
            }
        }

        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it as String) }
        }

        private fun innerMatch(value: String?): Boolean {
            logDebugFunCall(log) { it.mtd("Like.innerMatch").params("value" to value) }
            if (value == null) return false
            return when (matchType) {
                MatchType.CONTAINS -> value.contains(plainString, ignoreCase)
                MatchType.EXACT -> value.equals(plainString, ignoreCase)
                MatchType.STARTS_WITH -> value.startsWith(plainString, ignoreCase)
                MatchType.ENDS_WITH -> value.endsWith(plainString, ignoreCase)
            }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [like] cb.like(cb.lower($field, ${queryString.lowercase()}))" }
            return ctx.cb.like(ctx.cb.lower(ctx.getField<String>(field!!)), queryString.lowercase())
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo
        ) {
            val term = queryString.replace('%', '*')
            logDebugFunCall(log) {
                it.mtd("Like.handle(...)").msg("bool.must(f.match().field(\"$field\").matching(\"$term\"))")
            }
            boolCollector.must(
                searchPredicateFactory.match().field(field)
                    .matching(term)
            )
        }
    }

    class FullSearch(
        expectedValue: String,
        private val searchFields: Array<String>?,
        autoWildcardSearch: Boolean = false
    ) :
        DBPredicate(null, true, false, false) {
        private var queryString: String

        init {
            logDebugFunCall(log) { it.mtd("FullSearch.init") }
            queryString = modifySearchString(expectedValue, '%', '*', autoWildcardSearch)
        }

        override fun match(obj: Any): Boolean {
            throw UnsupportedOperationException("match method only available for full text search!")
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            throw UnsupportedOperationException("Full text search without field not available as criteria predicate!")
        }

        override fun supportedByFullTextQuery(searchClassInfo: HibernateSearchClassInfo): Boolean {
            return true
        }

        override fun handle(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            searchClassInfo: HibernateSearchClassInfo,
        ) {
            logDebugFunCall(log) { it.mtd("FullSearch.handledByFullTextQuery(...)") }
            if (NumberUtils.isCreatable(queryString)) {
                val number = NumberUtils.createNumber(queryString)
                search(
                    searchPredicateFactory,
                    boolCollector,
                    number,
                    searchClassInfo.numericFields,
                    searchClassInfo.stringFieldNames
                )
            } else if (searchFields.isNullOrEmpty()) {
                search(searchPredicateFactory, boolCollector, queryString, searchClassInfo.stringFieldNames)
            } else {
                search(searchPredicateFactory, boolCollector, queryString, searchFields)
            }
        }

        private fun search(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            value: String,
            fields: Array<String>,
        ) {
            if (value.isBlank()) {
                return
            }
            logDebugFunCall(log) {
                it.mtd("search(value, fields)")
                    .msg("bool.must(f.simpleQueryString().fields(${fields.joinToString()}).matching(\"$value\").defaultOperator(BooleanOperator.AND))")
            }
            boolCollector.must(
                searchPredicateFactory.queryString()
                    .fields(*fields)
                    .matching(value)
                //.defaultOperator(BooleanOperator.AND)
            )
        }

        private fun search(
            searchPredicateFactory: SearchPredicateFactory,
            boolCollector: BooleanPredicateOptionsCollector<*>,
            value: Number,
            numericFields: Array<Pair<String, Class<*>>>,
            stringFields: Array<String>,
        ) {
            val predicates = numericFields.map {
                searchPredicateFactory
                    .range()
                    .field(it.first)
                    .between(getValue(value, it.second), getValue(value, it.second))
            }.toTypedArray()
            logDebugFunCall(log) {
                it.mtd("search(value, fields)")
                    .msg("bool.must(or(f.id().matching(${value}), f.range()[${numericFields.joinToString { "${it.first}:${it.second}" }}], f.queryString().fields(${stringFields.joinToString()}).matching(\"$value\")))")
            }
            boolCollector.must(
                searchPredicateFactory.or(
                    searchPredicateFactory
                        .id()
                        .matching(value.toLong()),
                    searchPredicateFactory.queryString()
                        .fields(*stringFields)
                        .matching(value.toString()),
                    *predicates
                )
            )
        }


        /*override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            logDebugFunCall(log) { it.mtd("FullSearch.addTo(qb)").msg("queryString=$queryString") }
            qb.fulltextSearch(queryString)
        }*/
    }

    fun getValue(value: Number, type: Class<*>): Any {
        return when (type) {
            Int::class.java, Integer::class.java -> value.toInt()
            Long::class.java, java.lang.Long::class.java -> value.toLong()
            Float::class.java, java.lang.Float::class.java -> value.toFloat()
            Double::class.java, java.lang.Double::class.java -> value.toDouble()
            BigDecimal::class.java -> BigDecimal(value.toString())
            BigInteger::class.java -> BigInteger(value.toString())
            else -> value
        }
    }

    class IsNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it == null }
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [isNull] cb.isNull($field)" }
            return ctx.cb.isNull(ctx.getField<Any>(field!!))
        }
    }

    class IsNotNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it != null }
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [isNotNull] cb.isNotNull($field)" }
            return ctx.cb.isNotNull(ctx.getField<Any>(field!!))
        }
    }

    class Not(val predicate: DBPredicate) : DBPredicate(null, false) {
        override fun match(obj: Any): Boolean {
            return !predicate.match(obj)
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [not] cb.not(...) started." }
            val result = ctx.cb.not(predicate.asPredicate(ctx))
            log.debug { "Adding criteria search (${ctx.entityName}): [not] cb.not(...) ended." }
            return result
        }
    }

    class And(vararg predicates: DBPredicate) : DBPredicate(null, false) {
        private val predicates = mutableListOf<DBPredicate>()

        init {
            this.predicates.addAll(predicates)
        }

        /**
         * @return this for chaining.
         */
        fun add(predicate: DBPredicate): And {
            predicates.add(predicate)
            return this
        }

        override val fullTextSupport: Boolean
            get() {
                for (predicate in predicates) {
                    if (!predicate.fullTextSupport) {
                        return false
                    }
                }
                return true
            }

        override fun match(obj: Any): Boolean {
            if (predicates.isEmpty()) {
                return false
            }
            for (matcher in predicates) {
                if (!matcher.match(obj)) {
                    return false
                }
            }
            return true
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [and] cb.and(...) started." }
            val result = ctx.cb.and(*predicates.map { it.asPredicate(ctx) }.toTypedArray())
            log.debug { "Adding criteria search (${ctx.entityName}): [and] cb.and(...) ended." }
            return result
        }
    }

    class Or(vararg predicates: DBPredicate) : DBPredicate(null, false) {
        private val predicates = mutableListOf<DBPredicate>()

        init {
            this.predicates.addAll(predicates)
        }

        /**
         * @return this for chaining.
         */
        fun add(predicate: DBPredicate): Or {
            predicates.add(predicate)
            return this
        }

        override fun match(obj: Any): Boolean {
            if (predicates.isEmpty()) {
                return false
            }
            for (matcher in predicates) {
                if (matcher.match(obj)) {
                    return true
                }
            }
            return false
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            log.debug { "Adding criteria search (${ctx.entityName}): [or] cb.or(...) started." }
            val result = ctx.cb.or(*predicates.map { it.asPredicate(ctx) }.toTypedArray())
            log.debug { "Adding criteria search (${ctx.entityName}): [or] cb.or(...) ended." }
            return result
        }
    }

    /**
     * Evaluates whether the field matches the given match method or not. Nested fields are supported.
     * @param obj The Object of the result set to evaluate
     * @param field The field name of the object to evaluate (nested fields are supported as well as Arrays and Iterables).
     * @param match The evaluating method to call.
     * @return True if specified property of object by field name matches (or any of the properties if multiple values are found for a property).
     */
    internal fun fieldValueMatch(obj: Any, field: String, match: (value: Any?) -> Boolean): Boolean {
        if (!field.contains('.')) {
            return match(getProperty(obj, field))
        }
        return fieldValueMatch(obj, field.split('.'), 0, match)
    }

    /**
     * For recursive processing of nested properties...
     */
    private fun fieldValueMatch(obj: Any?, path: List<String>, idx: Int, match: (value: Any?) -> Boolean): Boolean {
        val nestedObj = getProperty(obj, path[idx])
        if (nestedObj == null) {
            return match(nestedObj)
        }
        when (nestedObj) {
            is Iterable<*> -> {
                for (it in nestedObj) {
                    val result =
                        if (hasNext(path, idx))
                            fieldValueMatch(it, path, idx + 1, match)
                        else match(it)
                    if (result) return true
                }
            }

            is Array<*> -> {
                for (it in nestedObj) {
                    val result =
                        if (hasNext(path, idx))
                            fieldValueMatch(it, path, idx + 1, match)
                        else match(it)
                    if (result) return true
                }
            }

            else -> {
                if (hasNext(path, idx))
                    return fieldValueMatch(nestedObj, path, idx + 1, match)
                return match(nestedObj)
            }
        }
        return false
    }

    internal fun isEquals(val1: Any?, val2: Any?): Boolean {
        if (val1 == null && val2 == null) return true   // null is equal null
        if (val1 == null || val2 == null) return false  // null isn't equal to non-null.
        if (val1::class.java.isEnum || val2::class.java.isEnum) {
            return Objects.equals(val1.toString(), val2.toString()) // enums should match their string representations.
        }
        return Objects.equals(val1, val2)
    }

    private fun getProperty(obj: Any?, field: String): Any? {
        if (obj == null)
            return null
        return BeanHelper.getProperty(obj, field)
    }

    private fun hasNext(path: List<String>, idx: Int): Boolean {
        return path.size > idx + 1
    }
}
