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
import jakarta.persistence.criteria.CriteriaQuery
import mu.KotlinLogging
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.projectforge.framework.persistence.api.ExtendedBaseDO

private val log = KotlinLogging.logger {}

/**
 * Generic interface for iterating over database search results (after criteria search as well as after full text query).
 */
interface DBResultIterator<O : ExtendedBaseDO<Long>> {
    fun next(): O?
    fun sort(list: List<O>): List<O>
}

/**
 * Usable for empty queries without any result.
 */
internal class DBEmptyResultIterator<O : ExtendedBaseDO<Long>>
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

internal class DBCriteriaResultIterator<O : ExtendedBaseDO<Long>>(
    val entityManager: EntityManager,
    criteria: CriteriaQuery<O>,
    val resultPredicates: List<DBPredicate>,
) : DBResultIterator<O> {
    private val scrollableResults: ScrollableResults<O>
    private var counter = 0

    init {
        val query = entityManager.createQuery(criteria)
        val hquery = query.unwrap(org.hibernate.query.Query::class.java)
        @Suppress("UNCHECKED_CAST")
        scrollableResults = hquery.scroll(ScrollMode.FORWARD_ONLY) as ScrollableResults<O>
    }

    override fun next(): O? {
        try {
            if (++counter % 100 == 0) {
                entityManager.clear()  // Flushing and clearing session
            }
            if (!scrollableResults.next()) {
                return null
            }
            return scrollableResults.get()
        } catch (ex: Exception) {
            log.error(ex) { "Error in DBCriteriaResultIterator.next(): " }
            return null
        }
    }

    override fun sort(list: List<O>): List<O> {
        return list
    }
}

