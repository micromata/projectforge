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

package org.projectforge.framework.persistence.api

import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.search.FullTextSession
import org.hibernate.search.Search
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaQuery

/**
 * Generic interface for iterating over database search results (after criteria search as well as after full text query).
 */
internal interface DBResultIterator<O : ExtendedBaseDO<Int>> {
    fun next(): O?
}

/**
 * Usable for empty queries without any result.
 */
internal class DBEmptyResultIterator<O : ExtendedBaseDO<Int>>()
    : DBResultIterator<O> {
    override fun next(): O? {
        return null
    }
}

internal class DBCriteriaResultIterator<O : ExtendedBaseDO<Int>>(
        entityManager: EntityManager,
        criteria: CriteriaQuery<O>)
    : DBResultIterator<O> {
    private val scrollableResults: ScrollableResults

    init {
        val query = entityManager.createQuery(criteria)
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
}

private const val MAX_RESULTS = 100

internal class DBFullTextResultIterator<O : ExtendedBaseDO<Int>>(
        private val baseDao: BaseDao<O>,
        private val fullTextSession: FullTextSession,
        private val query: org.apache.lucene.search.Query) : DBResultIterator<O> {
    private val log = LoggerFactory.getLogger(DBFullTextResultIterator::class.java)
    private var result: List<O>
    private var resultIndex = -1
    private var firstIndex = 0

    init {
        result = nextResultBlock()
    }

    override fun next(): O? {
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
        fullTextQuery.firstResult = firstIndex
        fullTextQuery.maxResults = MAX_RESULTS

        firstIndex += MAX_RESULTS
        @Suppress("UNCHECKED_CAST")
        return fullTextQuery.resultList as List<O> // return a list of managed objects
    }
}

/**
 * Unrecommended mix of full text search and criteria query.
 */
internal class DBFullTextCriteriaResultIterator<O : ExtendedBaseDO<Int>>(
        session: Session,
        private val criteria: Criteria?,
        private val baseDao: BaseDao<O>,
        searchString: String,
        searchFields: Array<String>?) : DBResultIterator<O> {
    private val log = LoggerFactory.getLogger(DBFullTextCriteriaResultIterator::class.java)
    private var result: List<O>
    private var resultIndex = 0
    private var firstIndex = 0
    private var modSearchString = HibernateSearchFilterUtils.modifySearchString(searchString)
    private var usedSearchFields = searchFields ?: baseDao.searchFields
    val fullTextSession = Search.getFullTextSession(session)

    init {
        if (criteria != null) {
            criteria.setCacheable(true)
            if (baseDao.useOwnCriteriaCacheRegion()) {
                criteria.setCacheRegion(baseDao.javaClass.name)
            }
        }
        result = nextResultBlock()
    }

    override fun next(): O? {
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
        val query = HibernateSearchFilterUtils.createFullTextQuery(fullTextSession, usedSearchFields, modSearchString, baseDao.clazz)
        val fullTextQuery = fullTextSession.createFullTextQuery(query, baseDao.clazz)

        if (criteria != null) {
            fullTextQuery.setCriteriaQuery(criteria)
        }
        fullTextQuery.firstResult = firstIndex
        fullTextQuery.maxResults = MAX_RESULTS

        firstIndex += MAX_RESULTS
        @Suppress("UNCHECKED_CAST")
        return fullTextQuery.list() as List<O> // return a list of managed objects
    }
}
