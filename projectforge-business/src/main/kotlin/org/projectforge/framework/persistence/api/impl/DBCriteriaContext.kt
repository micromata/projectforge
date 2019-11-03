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
        val root: Root<O>) {
    private val log = LoggerFactory.getLogger(DBCriteriaContext::class.java)
    private val aliasMap = mutableMapOf<String, Join<Any, Any>>()

    fun addAlias(dbAlias: DBAlias) {
        if (dbAlias.parent != null) {
            log.error("Chained joins are not yet supported: $dbAlias")
            return
        }
        //var parent = if (dbAlias.parent != null) getField<Any>(dbAlias.parent) else root
        // chained?
        //val join = root.fetch<Any, Any>(dbAlias.attribute, dbAlias.joinType) as Join<Any, Any>

        val join = root.join<Any, Any>(dbAlias.attribute, dbAlias.joinType)
        aliasMap[dbAlias.alias] = join
        // CriteriaQuery<Parent> criteria = cb.createQuery((Class<Parent>) Parent.class);
        //Root<Parent> parent = criteria.from(Parent.class);
        //
        //criteria.select((Selection<T>) parent);
        //SetJoin<Parent, Children> children = parent.joinSet("children", JoinType.LEFT);
        //
        //val join = parent.fetch(Parent_.children) as Join<Parent, Children>

        //Predicate sexPredicate = cb.equal(children.get("sex"), "MALE");
        //parent.fetch(children);
        ////parent.fetch("children");//try also this
        //
        //criteria.where(sexPredicate);
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
            if (path == parent) {
                // First loop, use alias, if any:
                path = aliasMap[it] ?: path.get<Any>(it)
            } else {
                path = path.get<Any>(it)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return path as Path<T>
    }
}
