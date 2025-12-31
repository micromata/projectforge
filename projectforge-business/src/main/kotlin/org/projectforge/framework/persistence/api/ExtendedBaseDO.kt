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

package org.projectforge.framework.persistence.api

import jakarta.persistence.Column
import org.projectforge.common.anots.PropertyInfo
import java.io.Serializable
import java.util.*
import jakarta.persistence.Basic

/**
 * Extends BaseDO: Supports extended functionalities: deleted, created and lastUpdate.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
interface ExtendedBaseDO<I : Serializable> : BaseDO<I>, MarkDeletableRecord<I> {
    /**
     * If any re-calculations have to be done before displaying, indexing etc. Such re-calculations are use-full for e. g.
     * transient fields calculated from persistent fields.
     */
    fun recalculate()

    override var deleted: Boolean

    var created: Date?

    fun setCreated() {
        this.created = Date()
    }


    /**
     * Last update will be modified automatically for every update of the database object.
     *
     * @return
     */
    var lastUpdate: Date?

    fun setLastUpdate() {
        this.lastUpdate = Date()
    }
}
