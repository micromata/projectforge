/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import java.util.*

/**
 * BaseHistorizableDTO is a DTO representation of a AbstractHistorizableBaseDO<Int>. It copies most fields automatically by name and type from
 * DTO to  AbstractHistorizableBaseDO<Int> and vice versa.
 */
open class BaseDTODisplayObject<T : ExtendedBaseDO<Int>>(id: Int? = null,
                                                         /**
                                                          * Only for displaying purposes. Will be ignored on save or update.
                                                          */
                                                         override var displayName: String? = null,
                                                         deleted: Boolean = false,
                                                         created: Date? = null,
                                                         lastUpdate: Date? = null,
                                                         tenant: Tenant? = null)
    : BaseDTO<T>(id, deleted, created, lastUpdate, tenant), DisplayNameCapable {
    override fun copyFromMinimal(src: T) {
        super.copyFromMinimal(src)
        if (src is DisplayNameCapable)
            this.displayName = src.displayName
    }
}
