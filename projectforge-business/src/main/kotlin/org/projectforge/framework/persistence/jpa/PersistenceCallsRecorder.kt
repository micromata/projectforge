/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.stat.spi.StatisticsImplementor

/**
 * For performance measurement of persistence calls. Each call of persistenceService (CRUD) is counted.
 * Mustn't be thread-safe, because it is used in a thread-local context.
 * Multiple calls stats aren't supported, the last one wins and overwrites any exiting stats.
 * If you want to use it in a multithreaded context, you have to synchronize it.
 * Please note, that the indirect database calls e.g. on lazy loading aren't counted.
 *
 * Lazy-Breakpoint: AbstractLazyInitializer.initialize (#134)
 * org.hibernate.persister.entity.AbstractEntityPersister#generateSelectLazy:
 *
 */
internal class PersistenceCallsRecorder(val entityManager: EntityManager, val extended: Boolean) {
    enum class CallType { CRITERIA_UPDATE, FIND, GET_REFERENCE, MERGE, PERSIST, REMOVE, UPDATE, QUERY, SELECT }
    data class Call(val method: CallType, val sql: String, val detail: String? = null) : Comparable<Call> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Call) return false

            return method == other.method && sql == other.sql
        }

        override fun hashCode(): Int {
            var result = method.hashCode()
            result = 31 * result + sql.hashCode()
            return result
        }

        override fun compareTo(other: Call): Int {
            val res = method.compareTo(other.method)
            if (res != 0) {
                return res
            }
            return sql.compareTo(other.sql)
        }
    }

    val usage = mutableListOf<Call>()
    val countMap = mutableMapOf<Call, Int>()
    val statistics: StatisticsImplementor
    val callStats: PersistenceCallsStats

    init {
        val session = entityManager.unwrap(Session::class.java)
        statistics = (session.factory as SessionFactoryImplementor).statistics
        callStats = PersistenceCallsStats(statistics)
    }

    internal fun add(method: CallType, entity: String, detail: PersistenceCallsStatsBuilder) {
        add(method, entity, detail.toString())
    }

    internal fun add(method: CallType, entity: String, detail: String? = null) {
        val call = Call(method, entity, detail)
        if (extended) {
            usage.add(call)
        }
        countMap[call] = countMap.getOrDefault(call, 0) + 1
    }

    fun getStats(): PersistenceCallsStats {
        return callStats.getStats(statistics)
    }

    fun toString(extended: Boolean = false): String {
        val sb = StringBuilder()
        sb.append("PersistenceCallsStats(direct calls)={")
        sb.append("total=").append(countMap.values.sum()).append(",")
        sb.append("read=").append(countMap.filter { readOperations.contains(it.key.method) }.values.sum()).append(",")
        sb.append("write=").append(countMap.filter { writeOperations.contains(it.key.method) }.values.sum()).append("}")
        sb.append(", ")
        callStats.append(sb, statistics)
        if (extended) {
            sb.appendLine()
            countMap.toSortedMap().forEach { (call, count) ->
                sb.append("method=").append(call.method).append(",")
                    .append("call=").append(call.sql).append(",")
                    .append("count=").append(count).append(",")
                    .append("}")
                    .appendLine()
            }
        }
        return sb.toString()
    }

    companion object {
        val readOperations = arrayOf(CallType.FIND, CallType.GET_REFERENCE, CallType.QUERY, CallType.SELECT)
        val writeOperations =
            arrayOf(CallType.CRITERIA_UPDATE, CallType.MERGE, CallType.PERSIST, CallType.REMOVE, CallType.UPDATE)
    }
}
