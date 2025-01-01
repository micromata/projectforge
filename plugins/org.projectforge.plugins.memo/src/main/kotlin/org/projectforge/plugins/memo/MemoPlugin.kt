/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.memo

import org.projectforge.Constants
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.core.AbstractPlugin
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.web.WicketSupport

/**
 * Your plugin initialization. Register all your components such as i18n files, data-access object etc.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MemoPlugin : AbstractPlugin(ID, "Memo", "Personal text memos of users.") {


    override fun initialize() {
        val memoDao = WicketSupport.get(MemoDao::class.java)
        val menuCreator = WicketSupport.get(MenuCreator::class.java)

        // DatabaseUpdateDao is needed by the updater:
        // Register it:
        register(MemoDao::class.java, memoDao, "plugins.memo")

        // Register the menu entry as sub menu entry of the misc menu:
        menuCreator.register(
            MenuItemDefId.MISC,
            MenuItemDef(info.id, "plugins.memo.menu", "${Constants.REACT_APP_PATH}memo")
        )
        // Later: React only:
        // menuCreator.add(parentId, menuItemDef);


        // Define the access management:
        registerRight(MemoRight())

        // All the i18n stuff:
        addResourceBundle(RESOURCE_BUNDLE_NAME)
    }

    companion object {
        const val ID = PluginAdminService.PLUGIN_MEMO_ID
        const val RESOURCE_BUNDLE_NAME = "MemoI18nResources"

        // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
        // each test).
        // The entities are inserted in ascending order and deleted in descending order.
        private val PERSISTENT_ENTITIES = arrayOf<Class<*>>(MemoDO::class.java)
    }
}
