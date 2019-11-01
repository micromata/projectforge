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
import org.slf4j.LoggerFactory
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

/**
 * After querying, every result entry is matched against matchers (for fields not supported by the full text query).
 */
abstract class DBPredicate(
        val field: String?,
        /**
         * True if this predicate may be attached to a full text query for indexed fields.
         */
        val fullTextSupport: Boolean = false,
        /**
         * True if this predicate may be attached to a criteria search (as predicate).
         */
        val criteriaSupport: Boolean = true,
        /**
         * True if this predicate may be checked while iterating the result set.
         */
        val resultSetSupport: Boolean = true) {

    abstract fun match(obj: Any): Boolean
    abstract fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate
    abstract fun addTo(qb: DBQueryBuilder<*>)

    companion object {
        private val log = LoggerFactory.getLogger(DBPredicate::class.java)
    }

    class Equals(field: String, val expectedValue: Any)
        : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { Objects.equals(expectedValue, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.equal(getField<Any>(root, field!!), expectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.equal(field!!, expectedValue)
        }
    }

    class NotEquals(field: String, val notExpectedValue: Any)
        : DBPredicate(field, true) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { !Objects.equals(notExpectedValue, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.notEqual(getField<Any>(root, field!!), notExpectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.notEqual(field!!, notExpectedValue)
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
                if (Objects.equals(v, value)) {
                    return true
                }
            }
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            val predicate = getField<Any>(root, field!!).`in`(values)
            return cb.`in`(predicate)
            // Alternative:
            // val inClause = cb.`in`(getField<Any>(root, field!!))
            // for (value in values) {
            //     inClause.value(value)
            // }
            // return inClause
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.isIn(field!!, values)
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.between(getField<T>(root, field!!), from, to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.greaterThan(getField<O>(root, field!!), from)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.greaterThanOrEqualTo(getField<O>(root, field!!), from)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.lessThan(getField<O>(root, field!!), to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.lessThanOrEqualTo(getField<O>(root, field!!), to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.lessEqual(field!!, to)
        }
    }

    class Like(field: String, val expectedValue: String, val ignoreCase: Boolean = true)
        : DBPredicate(field, true) {
        internal var plainString: String
        internal val matchType: MatchType

        init {
            plainString = expectedValue.trim().replace('%', '*')
            if (plainString.startsWith("*")) {
                plainString = plainString.substring(1)
                if (plainString.endsWith('*')) {
                    matchType = MatchType.CONTAINS
                    plainString = plainString.substring(0, plainString.length - 1)
                } else {
                    matchType = MatchType.ENDS_WITH
                }
            } else if (plainString.endsWith('*')) {
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
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.like(cb.lower(getField<String>(root, field!!)), expectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.ilike(field!!, expectedValue)
        }
    }

    class FullSearch(val expectedValue: String)
        : DBPredicate(null, true, false, false) {
        override fun match(obj: Any): Boolean {
            throw UnsupportedOperationException("match method only available for full text search!")
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            throw UnsupportedOperationException("Full text search without field not available as criteria predicate!")
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            if (qb is DBQueryBuilderByFullText<*>) {
                qb.fulltextSearch(expectedValue)
            } else {
                throw UnsupportedOperationException("Fulltext search on all available fields only available for full text queries, not for criteria search!")
            }
        }
    }

    class IsNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it == null }
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.isNull(getField<Any>(root, field!!))
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.isNull(field!!)
        }
    }

    class IsNotNull(field: String) : DBPredicate(field, false) {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(obj, field!!) { it != null }
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.isNotNull(getField<Any>(root, field!!))
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.isNotNull(field!!)
        }
    }

    class Not(val predicate: DBPredicate) : DBPredicate(null, false) {
        override fun match(obj: Any): Boolean {
            return !predicate.match(obj)
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.not(predicate.asPredicate(cb, root))
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.addMatcher(this)
        }
    }

    class And(vararg val predicates: DBPredicate) : DBPredicate(null, false) {

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

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.and(*predicates.map { it.asPredicate(cb, root) }.toTypedArray())
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.addMatcher(this)
        }
    }

    class Or(vararg val predicates: DBPredicate) : DBPredicate(null, false) {

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

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.or(*predicates.map { it.asPredicate(cb, root) }.toTypedArray())
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.addMatcher(this)
        }
    }

    internal fun <T> getField(root: Root<*>, field: String): Path<T> {
        if (!field.contains('.'))
            return root.get<T>(field)
        val pathSeq = field.splitToSequence('.')
        var path: Path<*> = root
        pathSeq.forEach {
            path = path.get<Any>(it)
        }
        @Suppress("UNCHECKED_CAST")
        return path as Path<T>
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
        if (nestedObj is Iterable<*>) {
            for (it in nestedObj) {
                val result =
                        if (hasNext(path, idx))
                            fieldValueMatch(it, path, idx + 1, match)
                        else match(it)
                if (result) return true
            }
        } else if (nestedObj is Array<*>) {
            for (it in nestedObj) {
                val result =
                        if (hasNext(path, idx))
                            fieldValueMatch(it, path, idx + 1, match)
                        else match(it)
                if (result) return true
            }
        } else {
            if (hasNext(path, idx))
                return fieldValueMatch(nestedObj, path, idx + 1, match)
            return match(nestedObj)
        }
        return false
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
