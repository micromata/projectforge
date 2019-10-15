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

import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Restrictions
import org.projectforge.common.BeanHelper
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

/**
 * After querying, every result entry is matched against matchers (for fields not supported by the full text query).
 */
internal interface DBResultMatcher {
    fun match(obj: Any): Boolean
    fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate
    fun asHibernateCriterion(): Criterion

    class Equals(
            val field: String,
            val expectedValue: Any)
        : DBResultMatcher {
        override fun match(obj: Any): Boolean {
            val value = BeanHelper.getProperty(obj, field)
            return Objects.equals(expectedValue, value)
        }

        /**
         * Convert this matcher to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.equal(root.get<Any>(field), expectedValue)
        }

        override fun asHibernateCriterion(): Criterion {
            return Restrictions.eq(field, expectedValue)
        }
    }

    class Like(
            val field: String,
            val expectedValue: String)
        : DBResultMatcher {
        var plainString: String
        val matchType: MatchType

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
         * Convert this matcher to JPA criteria for where clause in select.
         */
        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.like(cb.lower(root.get<String>(field)), expectedValue)
        }

        override fun asHibernateCriterion(): Criterion {
            return Restrictions.ilike(field, expectedValue)
        }
    }

    class IsNull(
            val field: String)
        : DBResultMatcher {
        override fun match(obj: Any): Boolean {
            return BeanHelper.getProperty(obj, field) == null
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.isNull(root.get<Any>(field))
        }

        override fun asHibernateCriterion(): Criterion {
            return Restrictions.isNull(field)
        }
    }

    class Or(
            vararg matcher: DBResultMatcher
    ) : DBResultMatcher {
        val matcherList = matcher

        override fun match(obj: Any): Boolean {
            if (matcherList.isNullOrEmpty()) {
                return false
            }
            for (matcher in matcherList) {
                if (matcher.match(obj)) {
                    return true
                }
            }
            return false
        }

        override fun asPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate {
            return cb.or(*matcherList.map { it.asPredicate(cb, root) }.toTypedArray())
        }

        override fun asHibernateCriterion(): Criterion {
            return Restrictions.or(*matcherList.map { it.asHibernateCriterion() }.toTypedArray())
        }
    }
}
