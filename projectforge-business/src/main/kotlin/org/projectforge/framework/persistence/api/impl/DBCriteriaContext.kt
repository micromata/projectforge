/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.slf4j.LoggerFactory
import jakarta.persistence.criteria.*


/**
 * Context for building criterias. Holding criteria builder, root path and joinSets.
 */
internal class DBCriteriaContext<O : ExtendedBaseDO<Long>>(
        val cb: CriteriaBuilder,
        val cr: CriteriaQuery<O>,
        val root: Root<O>,
        /**
         * For logging purposes.
         */
        val entityClass: Class<O>) {
    private val log = LoggerFactory.getLogger(DBCriteriaContext::class.java)
    private val joinMap = mutableMapOf<String, Join<Any, Any>>()

    /**
     * Joins created on demand for nested sort paths, keyed by the path they reach (`projekt`,
     * `projekt.kunde`). Kept apart from [joinMap], which the query's own [DBJoin]s own: those are keyed
     * by attribute name only, so writing a nested path into it could shadow one of them.
     */
    private val sortJoinMap = mutableMapOf<String, Join<Any, Any>>()

    val entityName
        get() = entityClass.simpleName

    fun addJoin(dbAlias: DBJoin) {
        @Suppress("UNCHECKED_CAST")
        var parent = root as From<Any, Any>
        if (dbAlias.parent != null) {
            val parentJoin = joinMap[dbAlias.parent]
            if (parentJoin == null) {
                log.error("Parent '${dbAlias.parent}' not yet registered as join: $dbAlias")
                return
            }
            parent = parentJoin
        }

        @Suppress("UNCHECKED_CAST")
        val join = if (dbAlias.fetch)
            parent.fetch<Any, Any>(dbAlias.attribute, dbAlias.joinType) as Join<Any, Any>
        else
            parent.join<Any, Any>(dbAlias.attribute, dbAlias.joinType)
        joinMap[dbAlias.attribute] = join
    }

    fun <T> getField(field: String): Path<T> {
        return getField(root, field)
    }

    /**
     * The path of a field to order by, joining what a nested path needs.
     *
     * [getField] cannot serve an `order by` over a nested path on its own: it walks the path with
     * `get()`, and dereferencing an association that way makes Hibernate add an **inner** join. Ordering
     * would then drop rows — sorting the order book by customer would hide every order that has no
     * customer, which is a filter, not a sort. So each segment gets an explicit [JoinType.LEFT] join
     * instead, reused across sort properties (`projekt.kunde.name` and `projekt.name` share the
     * `projekt` join) and never mixed with the query's own joins in [joinMap].
     *
     * A path the query already joined keeps that join, so an `order by` over a filtered association
     * orders by the same rows the filter matched.
     */
    fun <T> getOrderField(field: String): Path<T> {
        if (!field.contains('.')) {
            return root.get<T>(field)
        }
        val segments = field.split('.')
        var from: From<Any, Any> = @Suppress("UNCHECKED_CAST") (root as From<Any, Any>)
        // All but the last segment are associations to join; the last one is the property to order by.
        segments.dropLast(1).forEachIndexed { index, segment ->
            val path = segments.take(index + 1).joinToString(".")
            // [joinMap] is keyed by attribute name alone, so it is only asked for the first segment —
            // as [getField] does — where the key is unambiguous.
            val existing = if (index == 0) joinMap[segment] else null
            from = existing ?: sortJoinMap.getOrPut(path) { from.join(segment, JoinType.LEFT) }
        }
        return from.get<T>(segments.last())
    }

    private fun <T> getField(parent: Path<*>, field: String): Path<T> {
        if (!field.contains('.'))
            return parent.get<T>(field)
        val pathSeq = field.splitToSequence('.')
        var path: Path<*> = parent
        pathSeq.forEach {
            path = if (path == parent) {
                // First loop, use alias, if any:
                joinMap[it] ?: path.get<Any>(it)
            } else {
                path.get<Any>(it)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return path as Path<T>
    }
}
