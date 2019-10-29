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

package org.projectforge.menu.builder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessChecker
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class FavoritesMenuTest : AbstractTestBase() {

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @Autowired
    private val accessChecker: AccessChecker? = null

    @Test
    fun setFavoritesMenuTest() {
        logon(ADMIN)
        val menu = favoritesMenuCreator.getFavoriteMenu(XML)
        assertEquals("Administration", menu.menuItems[0].title)
        assertEquals("ACCESS_LIST", menu.menuItems[0].subMenu!![0].id)
    }

    companion object {

        private const val XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<root>\n" +
                "    <item>Administration\n" +
                "        <item id=\"ACCESS_LIST\">Zugriffsverwaltung</item>\n" +
                "        <item id=\"GROUP_LIST\">Gruppen</item>\n" +
                "        <item id=\"USER_LIST\">Benutzer</item>\n" +
                "        <item id=\"SYSTEM\">System</item>\n" +
                "        <item id=\"licenseManagement\">Lizenzen</item>\n" +
                "    </item>\n" +
                "    <item>Orga\n" +
                "        <item id=\"INBOX_LIST\">Posteingang</item>\n" +
                "        <item id=\"OUTBOX_LIST\">Postausgang</item>\n" +
                "        <item id=\"CONTRACTS\">Verträge</item>\n" +
                "        <item id=\"BOOK_LIST\">Bücher</item>\n" +
                "    </item>\n" +
                "    <item>FiBu\n" +
                "        <item id=\"OUTGOING_INVOICE_LIST\">Debitorenrechnungen</item>\n" +
                "        <item id=\"INCOMING_INVOICE_LIST\">Kreditorenrechnungen</item>\n" +
                "        <item id=\"SCRIPT_LIST\">Scriptliste</item>\n" +
                "        <item id=\"ORDER_LIST\">Auftragsbuch</item>\n" +
                "    </item>\n" +
                "    <item id=\"TASK_TREE\">Strukturbaum</item>\n" +
                "    <item id=\"CALENDAR\">Kalender</item>\n" +
                "</root>\n"
    }
}
