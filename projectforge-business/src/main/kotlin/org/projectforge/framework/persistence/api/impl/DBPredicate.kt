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
interface DBPredicate {
    fun match(obj: Any): Boolean
    fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate
    fun addTo(qb: DBQueryBuilder<*>)

    companion object {
        private val log = LoggerFactory.getLogger(DBPredicate::class.java)
    }

    class Equals(
            val field: String,
            val expectedValue: Any)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(field, obj) { Objects.equals(expectedValue, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.equal(getField<Any>(root, field), expectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.equal(field, expectedValue)
        }
    }

    class NotEquals(
            val field: String,
            val notExpectedValue: Any)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            return fieldValueMatch(field, obj) { !Objects.equals(notExpectedValue, it) }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.notEqual(getField<Any>(root, field), notExpectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.notEqual(field, notExpectedValue)
        }
    }

    class AnyOf<O>(
            val field: String,
            vararg val values: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
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
            val predicate = getField<Any>(root, field).`in`(values)
            return cb.`in`(predicate)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.anyOf(field, values)
        }
    }

    class Between<O : Comparable<O>>(
            val field: String,
            val from: O,
            val to: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
            if (from::class.java.isAssignableFrom(value::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return from <= value as O && value <= to
            }
            log.warn("Between operator fails, because value isn't of type ${from::class.java}: $value")
            return false
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.between(getField<O>(root, field), from, to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.between(field, from, to)
        }
    }

    class Greater<O : Comparable<O>>(
            val field: String,
            val from: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
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
            return cb.greaterThan(getField<O>(root, field), from)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.greater(field, from)
        }
    }

    class GreaterEqual<O : Comparable<O>>(
            val field: String,
            val from: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
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
            return cb.greaterThanOrEqualTo(getField<O>(root, field), from)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.greaterEqual(field, from)
        }
    }

    class Less<O : Comparable<O>>(
            val field: String,
            val to: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
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
            return cb.lessThan(getField<O>(root, field), to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.less(field, to)
        }
    }

    class LessEqual<O : Comparable<O>>(
            val field: String,
            val to: O)
        : DBPredicate {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field) ?: return false
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
            return cb.lessThanOrEqualTo(getField<O>(root, field), to)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.lessEqual(field, to)
        }
    }

    class Like(
            val field: String,
            val expectedValue: String)
        : DBPredicate {
        private var plainString: String
        private val matchType: MatchType

        init {
            plainString = expectedValue.trim().replace('%', '*')
            if (plainString.startsWith("*")) {
                plainString = plainString.substring(1)
                if (plainString.endsWith('*')) {
                    matchType = MatchType.CONTAINS
                    plainString = plainString.substring(0, plainString.length - 1)
                } else {
                    matchType = MatchType.STARTS_WITH
                }
            } else if (plainString.endsWith('*')) {
                matchType = MatchType.ENDS_WITH
                plainString = plainString.substring(0, plainString.length - 1)
            } else {
                matchType = MatchType.EXACT
            }
        }

        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field)?.toString() ?: return false
            return when (matchType) {
                MatchType.CONTAINS -> value.contains(plainString)
                MatchType.EXACT -> value.equals(plainString)
                MatchType.STARTS_WITH -> value.startsWith(plainString)
                MatchType.ENDS_WITH -> value.endsWith(plainString)
            }
        }

        /**
         * Convert this predicate to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.like(cb.lower(getField<String>(root, field)), expectedValue)
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.ilike(field, expectedValue)
        }
    }

    class IsNull(val field: String) : DBPredicate {
        override fun match(obj: Any): Boolean {
            return BeanHelper.getProperty(obj, field) == null
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.isNull(getField<Any>(root, field))
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.isNull(field)
        }
    }

    class IsNotNull(val field: String) : DBPredicate {
        override fun match(obj: Any): Boolean {
            return BeanHelper.getProperty(obj, field) != null
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.isNotNull(getField<Any>(root, field))
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.isNotNull(field)
        }
    }

    class Not(val predicate: DBPredicate) : DBPredicate {
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

    class And(vararg val predicates: DBPredicate) : DBPredicate {

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

    class Or(vararg val predicates: DBPredicate) : DBPredicate {

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

    class In(val field: String,
             vararg val values: Any
    ) : DBPredicate {

        override fun match(obj: Any): Boolean {
            if (values.isNullOrEmpty()) {
                return false
            }
            for (value in values) {
                if (value == obj) {
                    return true
                }
            }
            return false
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            val inClause = cb.`in`(getField<Any>(root, field))
            for (value in values) {
                inClause.value(value)
            }
            return inClause
        }

        override fun addTo(qb: DBQueryBuilder<*>) {
            qb.addMatcher(this)
        }
    }

    fun <T> getField(root: Root<*>, field: String): Path<T> {
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

    fun fieldValueMatch(field: String, obj: Any, match: (value: Any) -> Boolean): Boolean {
        if (!field.contains('.')) {
            return match(BeanHelper.getProperty(obj, field))
        }
        log.warn("Matching of nested properties not yet supported: '$field'.")
        return false
    }
}
