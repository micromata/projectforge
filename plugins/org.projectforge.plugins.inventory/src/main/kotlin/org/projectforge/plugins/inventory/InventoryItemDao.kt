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

package org.projectforge.plugins.inventory

import org.projectforge.framework.persistence.api.*
import org.springframework.stereotype.Repository

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save,
 * delete etc. is implemented by the super class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class InventoryItemDao : BaseDao<InventoryItemDO>(InventoryItemDO::class.java) {

    init {
        userRightId = InventoryRightId.PLUGIN_INVENTORY
    }

    private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf("externalOwners", "item")

    override fun isAutocompletionPropertyEnabled(property: String): Boolean {
        return ENABLED_AUTOCOMPLETION_PROPERTIES.contains(property)
    }

    override fun newInstance(): InventoryItemDO {
        return InventoryItemDO()
    }
}
