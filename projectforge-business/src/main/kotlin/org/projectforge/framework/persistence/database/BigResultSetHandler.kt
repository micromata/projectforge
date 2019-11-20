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

package org.projectforge.framework.persistence.database

import org.hibernate.*
import org.hibernate.query.Query
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.TypedQuery

// HSQLDB: unlimited
// PostgreSQL: 32767 (JDBC driver 9.1, Short.MAX_VALUE)
private const val BLOCK_SIZE = 200 // Is limited by database (supported number of in values).
// BLOCK_SIZE performance tests:
// 1.000: 2,000/s
//   750: 7,000/s org.hibernate.TransientObjectException: The instance was not associated with this session
//   500: 8.300/s
//   300: 7.400/s
//   200: 7.400/s
//   100: 7.500/s
//    10: <2,000/s

/**
 * Load ids of result set as Scrollable and loads objects by their ids in blocks on demand.
 */
class BigResultSetHandler<T>(val em: EntityManager, val clazz: Class<T>, idsQuery: TypedQuery<Number>) {
    var totalRead = 0L
        private set

    private var sessionClearCounter = 100

    val select: String
    val strategy = ReindexerRegistry.get(clazz)

    private var currentObjectsBlock: List<T>? = null
    private var currentObjectsBlockIterator: Iterator<T>? = null
    private var offset = 0
    private val scrollableResults: ScrollableResults

    init {
        select = "select distinct t from ${clazz.simpleName} as t${strategy.join} where t.${strategy.idProperty} in :ids"
        scrollableResults = idsQuery.unwrap(Query::class.java)
                .setCacheMode(CacheMode.IGNORE)
                .setHibernateFlushMode(FlushMode.MANUAL)
                .setLockMode(LockModeType.NONE)
                .setFetchSize(BLOCK_SIZE)
                .setReadOnly(true)
                .scroll(ScrollMode.FORWARD_ONLY)
        readNextBlock()
    }

    fun hasNext(): Boolean {
        if (currentObjectsBlockIterator == null) {
            return false
        }
        if (currentObjectsBlockIterator?.hasNext() == true) {
            return true
        }
        return readNextBlock()
    }

    fun next(): T? {
        if (!hasNext())
            return null
        ++totalRead
        return currentObjectsBlockIterator!!.next()
    }

    private fun readNextBlock(): Boolean {
        /*val ids = idsQuery.unwrap(Query::class.java)
                .setCacheMode(CacheMode.IGNORE)
                .setHibernateFlushMode(FlushMode.MANUAL)
                .setLockMode(LockModeType.NONE)
                .setReadOnly(true)
                .setFirstResult(offset)
                .setMaxResults(BLOCK_SIZE)
                .resultList
        offset += BLOCK_SIZE*/
        val ids = mutableListOf<Number>()
        var counter = 0
        while (counter++ < BLOCK_SIZE && scrollableResults.next()) {
            ids.add(scrollableResults[0] as Number)
        }
        if (ids.size > 0) {
            if (sessionClearCounter-- <= 0) {
                val session = em.unwrap(Session::class.java)//.delegate as Session
                session.flush()
                session.clear()
                sessionClearCounter = 100
            }
            val query = em.createQuery(select, clazz).setParameter("ids", ids)
            query.unwrap(Query::class.java)
                    .setCacheMode(CacheMode.IGNORE)
                    .setHibernateFlushMode(FlushMode.MANUAL)
                    .setLockMode(LockModeType.NONE)
                    .setFetchSize(BLOCK_SIZE)
                    .setReadOnly(true)
            val block = query.resultList
            currentObjectsBlock = block
            currentObjectsBlockIterator = block.iterator()
            return true
        } else {
            currentObjectsBlock = null
            currentObjectsBlockIterator = null
            return false // Finished
        }
    }
}
