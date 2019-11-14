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

import org.projectforge.common.BeanHelper
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import org.slf4j.LoggerFactory
import java.util.*
import javax.persistence.criteria.Predicate

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
        val resultSetSupport: Boolean = true) {

    abstract fun match(obj: Any): Boolean
    internal abstract fun asPredicate(ctx: DBCriteriaContext<*>): Predicate
    internal open fun addTo(qb: DBQueryBuilderByFullText<*>) {
        throw UnsupportedOperationException("Operation '${this.javaClass}' not supported by full text query.")
    }

    /**
     * As Json
     */
    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DBPredicate::class.java)
    }

    class Equal(field: String, val value: Any)
        : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { isEquals(value, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [equal] cb.equal('$field'), '$value')")
            return ctx.cb.equal(ctx.getField<Any>(field!!), value)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.equal(field!!, value)
        }
    }

    class NotEqual(field: String, val value: Any)
        : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { !isEquals(value, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [notEqual] cb.notEqual('$field'), '$value')")
            return ctx.cb.notEqual(ctx.getField<Any>(field!!), value)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.notEqual(field!!, value)
        }
    }

    class IsIn<T>(field: String, vararg val values: T)
        : DBPredicate(field, false) {
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
         * Convert this predicate to JPA criteria for where clause in select. If only value is given, an equal predicate will
         * be returned.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [in] cb.in($field.in[${values.joinToString(", ", "'", "'")}])")
            if (values.isNullOrEmpty()) {
                if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [in] cb.isNull('$field'), '${values[0]}') (uses equal, because no value is given).")
                return ctx.cb.isNull(ctx.getField<Any>(field!!))
            }
            if (values.size == 1) {
                if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [in] cb.equal('$field'), '${values[0]}') (uses equal, because only one value is given).")
                return ctx.cb.equal(ctx.getField<Any>(field!!), values[0])
            }
            val inClause = ctx.cb.`in`(ctx.getField<Any>(field!!))
            for (value in values) {
                inClause.value(value)
            }
            return inClause
        }
    }

    class Between<T : Comparable<T>>(field: String, val from: T, val to: T)
        : DBPredicate(field, true) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [between] cb.between($field, $from, $to)")
            return ctx.cb.between(ctx.getField<T>(field!!), from, to)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.between(field!!, from, to)
        }
    }

    class Greater<O : Comparable<O>>(field: String, val from: O)
        : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { innerMatch(it) }
        }

        private fun innerMatch(value: Any?): Boolean {
            if (value == null) return false
            if (value::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return from < value as O
            }
            log.warn("GreaterEqual operator fails, because value isn't of type ${from::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [greater] cb.greaterThan($field, $from)")
            return ctx.cb.greaterThan(ctx.getField<O>(field!!), from)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.greater(field!!, from)
        }
    }

    class GreaterEqual<O : Comparable<O>>(field: String, val from: O)
        : DBPredicate(field, true) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [greaterEqual] cb.greaterThanOrEqualTo($field, $from)")
            return ctx.cb.greaterThanOrEqualTo(ctx.getField<O>(field!!), from)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.greaterEqual(field!!, from)
        }
    }

    class Less<O : Comparable<O>>(field: String, val to: O)
        : DBPredicate(field, true) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [less] cb.lessThan($field, $to)")
            return ctx.cb.lessThan(ctx.getField<O>(field!!), to)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.less(field!!, to)
        }
    }

    class LessEqual<O : Comparable<O>>(field: String, val to: O)
        : DBPredicate(field, true) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [lessEqual] cb.lessThanOrEqualTo($field, $to)")
            return ctx.cb.lessThanOrEqualTo(ctx.getField<O>(field!!), to)
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.lessEqual(field!!, to)
        }
    }

    class Like(field: String, val expectedValue: String, private val ignoreCase: Boolean = true, autoWildcardSearch: Boolean = false)
        : DBPredicate(field, true) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [like] cb.like(cb.lower($field, ${queryString.toLowerCase()}))")
            return ctx.cb.like(ctx.cb.lower(ctx.getField<String>(field!!)), queryString.toLowerCase())
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.ilike(field!!, expectedValue)
        }
    }

    class FullSearch(private val expectedValue: String, autoWildcardSearch: Boolean = false)
        : DBPredicate(null, true, false, false) {
        private var queryString: String

        init {
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

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.fulltextSearch(queryString)
        }

        fun multiFieldFulltextQueryRequired(): Boolean {
            for (str in expectedValue.split(' ', '\t', '\n')) {
                if (str.matches("""[A-Za-z][A-Za-z0-9_\.]*:.+""".toRegex()))
                    return true
            }
            return false
        }
    }

    class IsNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it == null }
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [isNull] cb.isNull($field)")
            return ctx.cb.isNull(ctx.getField<Any>(field!!))
        }
    }

    class IsNotNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it != null }
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [isNotNull] cb.isNotNull($field)")
            return ctx.cb.isNotNull(ctx.getField<Any>(field!!))
        }
    }

    class Not(val predicate: DBPredicate) : DBPredicate(null, false) {
        override fun match(obj: Any): Boolean {
            return !predicate.match(obj)
        }

        override fun asPredicate(ctx: DBCriteriaContext<*>): Predicate {
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [not] cb.not(...) started.")
            val result = ctx.cb.not(predicate.asPredicate(ctx))
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [not] cb.not(...) ended.")
            return result
        }
    }

    class And(private vararg val predicates: DBPredicate) : DBPredicate(null, false) {
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
            if (predicates.isNullOrEmpty()) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [and] cb.and(...) started.")
            val result = ctx.cb.and(*predicates.map { it.asPredicate(ctx) }.toTypedArray())
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [and] cb.and(...) ended.")
            return result
        }

        override fun addTo(qb: DBQueryBuilderByFullText<*>) {
            qb.and(*predicates)
        }
    }

    class Or(private vararg val predicates: DBPredicate) : DBPredicate(null, false) {

        override fun match(obj: Any): Boolean {
            if (predicates.isNullOrEmpty()) {
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
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [or] cb.or(...) started.")
            val result = ctx.cb.or(*predicates.map { it.asPredicate(ctx) }.toTypedArray())
            if (log.isDebugEnabled) log.debug("Adding criteria search (${ctx.entityName}): [or] cb.or(...) ended.")
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

    /**
     * Replaces trailing and leading '*' by '%' or vica versa. Appends '%' (or '*') to alphanumeric strings doesn't start or end with '%' or '*'.
     */

    internal fun modifySearchString(str: String, oldChar: Char, newChar: Char, autoWildcardSearch: Boolean = false): String {
        var queryString = str.trim()
        if (queryString.endsWith(oldChar))
            queryString = "${queryString.substring(0, queryString.length - 1)}$newChar"
        if (queryString.startsWith(oldChar))
            queryString = "$newChar${queryString.substring(1)}"
        else if (autoWildcardSearch)
            queryString = HibernateSearchFilterUtils.modifySearchString(queryString, "$newChar", false) // Always look for keyword* (\p{L} means all letters in all languages.
        return queryString
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
