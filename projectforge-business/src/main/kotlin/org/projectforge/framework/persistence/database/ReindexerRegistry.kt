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

import de.micromata.genome.jpa.StdRecord
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ReindexerRegistry {
    private val registryMap = mutableMapOf<Class<*>, ReindexerStrategy>()
    private val standardAbstractBaseDOStrategy = ReindexerStrategy("", "id", "lastUpdate")
    private val standardStandardRecordStrategy = ReindexerStrategy("", "id", "modifiedAt")
    private val standardUnknownStrategy = ReindexerStrategy("", "id", null)

    init {
        add(PfHistoryMasterDO::class.java, ReindexerStrategy("left join fetch t.attributes", "pk", "modifiedAt"))
    }

    fun add(clazz: Class<*>, strategy: ReindexerStrategy) {
        registryMap[clazz] = strategy
    }

    internal fun get(clazz: Class<*>): ReindexerStrategy {
        return registryMap[clazz] ?: when {
            AbstractBaseDO::class.java.isAssignableFrom(clazz) -> standardAbstractBaseDOStrategy
            StdRecord::class.java.isAssignableFrom(clazz) -> standardStandardRecordStrategy
            else -> standardUnknownStrategy
        }
    }
}
