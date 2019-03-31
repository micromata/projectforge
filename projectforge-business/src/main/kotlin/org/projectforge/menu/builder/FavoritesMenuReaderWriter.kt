/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.menu.builder

import com.google.gson.GsonBuilder
import org.apache.commons.lang3.StringUtils
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import java.util.*


/**
 * Reads and writes favorite menus from and to the user's preferences.
 */
class FavoritesMenuReaderWriter {
    internal class FavoriteMenu(val childs: MutableList<FavoriteMenuItem> = mutableListOf())

    internal class FavoriteMenuItem(var id: String? = null,
                                    var title: String? = null,
                                    var childs: MutableList<FavoriteMenuItem>? = null)

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(FavoritesMenuReaderWriter::class.java)

        internal fun read(menuCreator: MenuCreator, favMenuAsString: String?)
                : Menu {
            if (favMenuAsString.isNullOrBlank())
                return Menu()
            if (favMenuAsString.trim().startsWith("{")) {
                // Current json format (since 2019):
                return Companion.readFromJson(menuCreator, favMenuAsString)
            } else if (favMenuAsString.contains("<root>") == false) {
                // CSV format (oldest)
                return FavoritesMenuReaderWriter.buildFromOldUserPrefFormat(menuCreator, favMenuAsString);
            } else {
                // XML format (until 2019)
                return FavoritesMenuReaderWriter.readFromXml(menuCreator, favMenuAsString)
            }
        }

        /**
         * New json format until 2019.
         */
        private fun readFromJson(menuCreator: MenuCreator, menuAsJson: String):Menu {
            if (log.isDebugEnabled == true) {
                log.debug("readFromXml: $menuAsJson")
            }
            val menu = Menu()
            val favMenuDef = GsonBuilder().create().fromJson(menuAsJson, FavoriteMenu::class.java)
            if (favMenuDef == null) {
                log.error("Can't parse user's favorite menu: $menuAsJson.")
                return menu
            }
            favMenuDef.childs?.forEach {
                val menuItem = readFromJson(menuCreator, it)
                if (menuItem != null)
                    menu.add(menuItem)
            }
            return menu
        }

        /**
         * New json format until 2019.
         */
        private fun readFromJson(menuCreator: MenuCreator, favMenuItem: FavoriteMenuItem): MenuItem? {
            val menuItem = buildMenuItem(menuCreator, favMenuItem)
            favMenuItem.childs?.forEach {
                val child = buildMenuItem(menuCreator, it)
                menuItem.add(child)
            }
            return menuItem
        }

        private fun buildMenuItem(menuCreator: MenuCreator, favMenuItem: FavoriteMenuItem): MenuItem {
            var menuItemDef: MenuItemDef? = null
            if (favMenuItem.id != null) {
                menuItemDef = menuCreator.findById(favMenuItem.id!!)
            }
            var menuItem: MenuItem?
            if (menuItemDef != null) {
                menuItem = MenuItem(menuItemDef)
            } else {
                menuItem = MenuItem()
                if (favMenuItem.title.isNullOrBlank())
                    menuItem.title = "???"
                else
                    menuItem.title = favMenuItem.title
            }
            return menuItem
        }

        /**
         * Old XML format until 2019.
         */
        fun readFromXml(menuCreator: MenuCreator, menuAsXml: String): Menu {
            if (log.isDebugEnabled == true) {
                log.debug("readFromXml: $menuAsXml")
            }
            val menu = Menu()
            var document: Document?
            try {
                document = DocumentHelper.parseText(menuAsXml)
            } catch (ex: DocumentException) {
                log.error("Exception encountered $ex", ex)
                return menu
            }
            val root = document!!.rootElement
            val it = root.elementIterator("item")
            while (it.hasNext()) {
                val item = it.next() as Element
                val menuItem = readFromXml(menuCreator, item)
                if (menuItem != null)
                    menu.add(menuItem)
            }
            return menu
        }

        /**
         * Old XML format until 2019.
         */
        private fun readFromXml(menuCreator: MenuCreator, item: Element): MenuItem? {
            if (item.name != "item") {
                log.error("Tag 'item' expected instead of '" + item.name + "'. Ignoring this tag.")
                return null
            }
            var id: String? = item.attributeValue("id")
            var menuItemDef: MenuItemDef? = null
            if (id != null && id.startsWith("c-") == true) {
                id = id.substring(2)
            }
            if (id != null) {
                menuItemDef = menuCreator.findById(id)
            }
            var menuItem: MenuItem?
            if (menuItemDef != null) {
                menuItem = MenuItem(menuItemDef)
            } else {
                menuItem = MenuItem()
                val trimmedTitle = item.textTrim
                if (trimmedTitle != null) {
                    // menuEntry.setName(StringEscapeUtils.escapeXml(trimmedTitle));
                    if (StringUtils.isBlank(trimmedTitle) == true)
                        menuItem.title = "???"
                    else
                        menuItem.title = trimmedTitle
                }
            }
            val it = item.elementIterator("item")
            while (it.hasNext()) {
                if (menuItemDef != null) {
                    log.warn("Menu entry shouldn't have children, because it's a leaf node.")
                }
                val child = it.next() as Element
                val childMenuEntry = readFromXml(menuCreator, child)
                if (childMenuEntry != null) {
                    menuItem.add(childMenuEntry)
                }
            }
            return menuItem
        }

        /**
         * Oldes CSV format until 2019.
         * @param menuCreator
         * @param userPrefEntry coma separated list of MenuItemDefs.
         */
        private fun buildFromOldUserPrefFormat(menuCreator: MenuCreator, userPrefEntry: String): Menu {
            val menu = Menu()
            val tokenizer = StringTokenizer(userPrefEntry, ",")
            while (tokenizer.hasMoreTokens()) {
                var token = tokenizer.nextToken()
                if (token.startsWith("M_") == true) {
                    token = token.substring(2)
                }
                try {
                    val menuItemDef = menuCreator.findById(token) ?: continue
                    menu.add(MenuItem(menuItemDef))
                } catch (ex: Exception) {
                    log.info("Menu '" + token + "' not found: " + ex.message, ex)
                }

            }
            return menu
        }

    }
}
