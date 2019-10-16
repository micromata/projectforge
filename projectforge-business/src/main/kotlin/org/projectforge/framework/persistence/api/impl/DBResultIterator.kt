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

import org.apache.commons.lang3.builder.CompareToBuilder
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.search.FullTextSession
import org.projectforge.common.BeanHelper
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.slf4j.LoggerFactory
import java.text.Collator
import javax.persistence.criteria.CriteriaQuery


/**
 * Generic interface for iterating over database search results (after criteria search as well as after full text query).
 */
internal interface DBResultIterator<O : ExtendedBaseDO<Int>> {
    fun next(): O?
    fun sort(list: List<O>): List<O>
}

/**
 * Usable for empty queries without any result.
 */
internal class DBEmptyResultIterator<O : ExtendedBaseDO<Int>>()
    : DBResultIterator<O> {
    override fun next(): O? {
        return null
    }

    /**
     * Only implemented for sorting list after full text query.
     */
    override fun sort(list: List<O>): List<O> {
        return list
    }
}

internal class DBCriteriaResultIterator<O : ExtendedBaseDO<Int>>(
        session: Session,
        criteria: CriteriaQuery<O>)
    : DBResultIterator<O> {
    private val scrollableResults: ScrollableResults

    init {
        val query = session.createQuery(criteria)
        val hquery = query.unwrap(org.hibernate.query.Query::class.java)
        scrollableResults = hquery.scroll(ScrollMode.FORWARD_ONLY)
    }

    override fun next(): O? {
        if (!scrollableResults.next()) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return scrollableResults.get(0) as O
    }

    override fun sort(list: List<O>): List<O> {
        return list
    }
}

private const val MAX_RESULTS = 100

internal class DBFullTextResultIterator<O : ExtendedBaseDO<Int>>(
        val baseDao: BaseDao<O>,
        val fullTextSession: FullTextSession,
        val query: org.apache.lucene.search.Query,
        val dbResultMatchers: List<DBResultMatcher>,
        val sortBys: Array<SortBy>,
        val criteria: Criteria? = null) // Not recommended
    : DBResultIterator<O> {
    private val log = LoggerFactory.getLogger(DBFullTextResultIterator::class.java)
    private var result: List<O>
    private var resultIndex = -1
    private var firstIndex = 0

    init {
        result = nextResultBlock()
    }

    override fun next(): O? {
        while (true) {
            val next = internalNext() ?: return null
            if (!dbResultMatchers.isNullOrEmpty()) {
                for (matcher in dbResultMatchers) {
                    if (matcher.match(next)) {
                        return next
                    }
                }
                continue // No match.
            }
            return next
        }
    }

    override fun sort(list: List<O>): List<O> {
        val collator = Collator.getInstance(ThreadLocalUserContext.getLocale())
        return list.sortedWith(object : Comparator<O> {
            override fun compare(o1: O, o2: O): Int {
                if (sortBys.isNullOrEmpty()) {
                    return 0
                }
                val ctb = CompareToBuilder()
                for (sortBy in sortBys) {
                    val val1 = BeanHelper.getProperty(o1, sortBy.field)
                    val val2 = BeanHelper.getProperty(o2, sortBy.field)
                    if (val1 is String) {
                        // Strings should be compared by using locale dependent collator (especially for german Umlaute)
                        if (sortBy.ascending) {
                            ctb.append(val1, val2, collator)
                        } else {
                            ctb.append(val2, val1, collator)
                        }
                    } else if (val1 is Comparable<*>) {
                        if (sortBy.ascending) {
                            ctb.append(val1, val2)
                        } else {
                            ctb.append(val2, val1)
                        }
                    } else {
                        if (sortBy.ascending) {
                            ctb.append(val1?.toString(), val2?.toString())
                        } else {
                            ctb.append(val2?.toString(), val1?.toString())
                        }
                    }
                }
                return ctb.toComparison()
            }
        })
    }

    private fun internalNext(): O? {
        if (result.isEmpty()) {
            return null
        }
        if (++resultIndex >= result.size) {
            result = nextResultBlock()
            if (result.isEmpty()) {
                return null
            }
            resultIndex = 0
        }
        return result[resultIndex]
    }

    private fun nextResultBlock(): List<O> {
        val fullTextQuery = fullTextSession.createFullTextQuery(query, baseDao.doClass)
        if (criteria != null) {
            fullTextQuery.setCriteriaQuery(criteria)
        }
        fullTextQuery.firstResult = firstIndex
        fullTextQuery.maxResults = MAX_RESULTS

        firstIndex += MAX_RESULTS
        @Suppress("UNCHECKED_CAST")
        return fullTextQuery.resultList as List<O> // return a list of managed objects
    }
}
