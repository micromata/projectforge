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
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Path
import javax.persistence.criteria.Root

/**
 * Context for building criterias. Holding criteria builder, root path and joinSets.
 */
internal class DBCriteriaContext<O : ExtendedBaseDO<Int>>(
        val cb: CriteriaBuilder,
        val cr: CriteriaQuery<O>,
        val root: Root<O>) {
    internal fun <T> getField(field: String): Path<T> {
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
}
