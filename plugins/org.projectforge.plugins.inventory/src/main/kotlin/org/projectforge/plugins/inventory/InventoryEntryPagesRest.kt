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

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/inventory")
class InventoryEntryPagesRest() : AbstractDOPagesRest<InventoryEntryDO, InventoryEntryDao>(
        InventoryEntryDao::class.java,
        "plugins.inventory.title",
        cloneSupport = CloneSupport.CLONE) {
    /**
     * Initializes new memos for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): InventoryEntryDO {
        val memo = super.newBaseDO(request)
        memo.owner = ThreadLocalUserContext.getUser()
        return memo
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(lc, "lastUpdate", "item", "owner", "externalOwner", "comment"))

        layout.add(MenuItem("inventory.export",
                i18nKey = "exportAsXls",
                url = InventoryServicesRest.REST_EXCEL_EXPORT_PATH,
                type = MenuItemTargetType.DOWNLOAD))

        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * Sets logged-in-user as owner.
     */
    override fun prepareClone(dto: InventoryEntryDO): InventoryEntryDO {
        val clone = super.prepareClone(dto)
        clone.owner = ThreadLocalUserContext.getUser()
        return clone
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: InventoryEntryDO, userAccess: UILayout.UserAccess): UILayout {
        val item = UIInput("item", lc).enableAutoCompletion(this)
        val layout = super.createEditLayout(dto, userAccess)
                .add(item)
                .add(lc, "owner", "externalOwner", "comment")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
