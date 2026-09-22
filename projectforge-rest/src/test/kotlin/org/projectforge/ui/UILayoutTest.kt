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

package org.projectforge.ui

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.json.JsonValidator
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.dto.Address
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import jakarta.servlet.http.HttpServletRequest

class UILayoutTest : AbstractTestBase() {
    @Autowired
    lateinit var addressRest: AddressPagesRest

    @Test
    fun testAddressEditLayout() {
        logon(TEST_ADMIN_USER) // Needed for getting address books.
        val gson = GsonBuilder().create()
        val address = Address()
        val jsonString = gson.toJson(addressRest.createEditLayout(address, UILayout.UserAccess(true, true, true, true)))
        val jsonValidator = JsonValidator(jsonString)

        var map = jsonValidator.findParentMap("id", "addressStatus")
        assertEquals(true, map!!["required"] as Boolean)

        map = jsonValidator.findParentMap("id", "form")
        assertEquals(true, map!!["required"] as Boolean)
    }

    /**
     * The action buttons an edit layout offers follow the state of the entry, and are added by
     * AbstractPagesRest.createEditLayout for every entity alike (see LayoutUtils.processEditPage).
     *
     * `forceDelete` is the exception: AddressDao is one of the two DAOs that allow it
     * (`isForceDeletionSupport`), so the button appears next to markAsDeleted / undelete here.
     */
    @Test
    fun testEditActionButtons() {
        logon(TEST_ADMIN_USER) // Needed for getting address books.
        val gson = GsonBuilder().create()
        val userAccess = UILayout.UserAccess(true, true, true, true)
        val address = Address()
        var jsonString = gson.toJson(addressRest.createEditLayout(address, userAccess))
        var jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("create", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)

        address.id = 42
        jsonString = gson.toJson(addressRest.createEditLayout(address, userAccess))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("forceDelete", jsonValidator.get("actions[1].id"))
        assertEquals("markAsDeleted", jsonValidator.get("actions[2].id"))
        // AddressPagesRest declares CloneSupport, so cloning is offered before saving.
        assertEquals("clone", jsonValidator.get("actions[3].id"))
        assertEquals("update", jsonValidator.get("actions[4].id"))
        assertEquals(5, jsonValidator.getList("actions")?.size)

        address.deleted = true
        jsonString = gson.toJson(addressRest.createEditLayout(address, userAccess))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("undelete", jsonValidator.get("actions[1].id"))
        assertEquals("forceDelete", jsonValidator.get("actions[2].id"))
        assertEquals("clone", jsonValidator.get("actions[3].id"))
        assertEquals(4, jsonValidator.getList("actions")?.size)
    }

    /**
     * A list layout is one AG_GRID_LIST_PAGE element plus the reset and search actions, whatever the
     * entity - the column defs are the entity's own (see AGGridSupport.prepareUIGrid4ListPage).
     */
    @Test
    fun testListLayout() {
        logon(TEST_USER)
        val gson = GsonBuilder().create()
        val jsonString =
            gson.toJson(addressRest.createListLayout(Mockito.mock(HttpServletRequest::class.java), MagicFilter()))
        val jsonValidator = JsonValidator(jsonString)

        assertEquals("resultSet", jsonValidator.get("layout[0].id"))
        assertEquals("AG_GRID_LIST_PAGE", jsonValidator.get("layout[0].type"))
        assertEquals("el-1", jsonValidator.get("layout[0].key"))
        assertTrue((jsonValidator.getList("layout[0].columnDefs")?.size ?: 0) > 0)

        assertEquals(2, jsonValidator.getList("actions")?.size)
        assertEquals("reset", jsonValidator.get("actions[0].id"))
        assertEquals(translate("reset"), jsonValidator.get("actions[0].title"))
        assertEquals("SECONDARY", jsonValidator.get("actions[0].color")) // Gson doesn't know JsonProperty of Jackson (DANGER -> danger.)
        assertEquals("BUTTON", jsonValidator.get("actions[0].type"))

        assertEquals("PRIMARY", jsonValidator.get("actions[1].color")) // Gson doesn't know JsonProperty of Jackson.
    }

}
