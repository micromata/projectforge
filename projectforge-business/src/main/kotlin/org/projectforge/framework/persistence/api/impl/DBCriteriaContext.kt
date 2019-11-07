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

import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.slf4j.LoggerFactory
import javax.persistence.criteria.*


/**
 * Context for building criterias. Holding criteria builder, root path and joinSets.
 */
internal class DBCriteriaContext<O : ExtendedBaseDO<Int>>(
        val cb: CriteriaBuilder,
        val cr: CriteriaQuery<O>,
        val root: Root<O>,
        /**
         * For logging purposes.
         */
        val entityClass: Class<O>) {
    private val log = LoggerFactory.getLogger(DBCriteriaContext::class.java)
    private val joinMap = mutableMapOf<String, Join<Any, Any>>()

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
