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

package org.projectforge.framework.persistence.candh

import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PfHistoryMasterDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext

/**
 * Wrapper for PfHistoryMasterDO with additional functionalities.
 */
class CandHHistoryMasterWrapper(internal var master: PfHistoryMasterDO) {
    internal var attributes: MutableSet<CandHHistoryAttrWrapper>? = null

    fun internalPrepareForPersist() {
        master.attributes = mutableSetOf()
        attributes?.forEach { attrs ->
            master.attributes!!.add(attrs.attr)
            attrs.attr.internalSerializeValueObjects()
        }
    }

    companion object {
        @JvmOverloads
        fun create(
            entity: IdObject<Long>,
            entityOpType: EntityOpType,
            entityName: String? = HibernateUtils.getRealClass(entity).name,
            modifiedBy: String? = ThreadLocalUserContext.userId?.toString(),
        ): CandHHistoryMasterWrapper {
            PfHistoryMasterDO.create(entity, entityOpType, entityName = entityName, modifiedBy = modifiedBy)
                .let { master ->
                    return CandHHistoryMasterWrapper(master)
                }
        }
    }
}
