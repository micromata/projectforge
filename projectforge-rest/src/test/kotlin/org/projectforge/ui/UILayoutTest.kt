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

package org.projectforge.ui

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.json.JsonValidator
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.BookPagesRest
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.Book
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import jakarta.servlet.http.HttpServletRequest

class UILayoutTest : AbstractTestBase() {
    @Autowired
    lateinit var bookRest: BookPagesRest

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

    @Test
    fun testEditBookActionButtons() {
        val gson = GsonBuilder().create()
        val userAccess = UILayout.UserAccess(true, true, true, true)
        val book = Book()
        var jsonString = gson.toJson(bookRest.createEditLayout(book, userAccess))
        var jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("create", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)

        book.id = 42
        jsonString = gson.toJson(bookRest.createEditLayout(book, userAccess))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("markAsDeleted", jsonValidator.get("actions[1].id"))
        assertEquals("update", jsonValidator.get("actions[2].id"))
        assertEquals(3, jsonValidator.getList("actions")?.size)

        book.deleted = true
        jsonString = gson.toJson(bookRest.createEditLayout(book, userAccess))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("undelete", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)
    }

    @Test
    fun testEditBookLayout() {
        val gson = GsonBuilder().create()
        val book = Book()
        val userAccess = UILayout.UserAccess(true, true, true, true)
        book.id = 42 // So lend-out component will be visible (only in edit mode)
        val jsonString = gson.toJson(bookRest.createEditLayout(book, userAccess))
        val jsonValidator = JsonValidator(jsonString)

        assertEquals(translate("book.title.edit"), jsonValidator.get("title")) // translations not available in test.
        val title = jsonValidator.getMap("layout[0]")
        assertField(title, "title", 255.0, "STRING", translate("book.title"), type = "INPUT", key = "el-1")
        assertEquals(true, title!!["focus"])

        val authors = jsonValidator.getMap("layout[1]")
        assertField(authors, "authors", 1000.0, null, translate("book.authors"), type = "TEXTAREA", key = "el-2")
        assertNull(jsonValidator.getBoolean("layout[1].focus"))

        assertEquals("ROW", jsonValidator.get("layout[2].type"))
        assertEquals("el-3", jsonValidator.get("layout[2].key"))

        assertEquals(6.0, jsonValidator.getDouble("layout[2].content[0].length.xs"))
        assertEquals("COL", jsonValidator.get("layout[2].content[0].type"))
        assertEquals("el-4", jsonValidator.get("layout[2].content[0].key"))
    }

    @Test
    fun testBookListLayout() {
        logon(TEST_USER)
        val gson = GsonBuilder().create()
        val jsonString = gson.toJson(bookRest.createListLayout(Mockito.mock(HttpServletRequest::class.java), MagicFilter()))
        val jsonValidator = JsonValidator(jsonString)

        assertEquals("resultSet", jsonValidator.get("layout[0].id"))
        assertEquals("AG_GRID_LIST_PAGE", jsonValidator.get("layout[0].type"))
        assertEquals("el-1", jsonValidator.get("layout[0].key"))

        assertEquals(8, jsonValidator.getList("layout[0].columnDefs")?.size)

        assertEquals(2, jsonValidator.getList("actions")?.size)
        assertEquals("reset", jsonValidator.get("actions[0].id"))
        assertEquals(translate("reset"), jsonValidator.get("actions[0].title"))
        assertEquals("SECONDARY", jsonValidator.get("actions[0].color")) // Gson doesn't know JsonProperty of Jackson (DANGER -> danger.)
        assertEquals("BUTTON", jsonValidator.get("actions[0].type"))

        assertEquals("PRIMARY", jsonValidator.get("actions[1].color")) // Gson doesn't know JsonProperty of Jackson.
    }

    private fun findIndexed(jsonValidator: JsonValidator, field: String, path: String): Int {
        for (i in 0..7) {
            if (jsonValidator.get(path.replace("#idx#", "$i")) == field) {
                return i
            }
        }
        org.junit.jupiter.api.fail("Indexed element '$field' not found in path '$path'.")
    }

    private fun assertField(element: Map<String, *>?, id: String, maxLength: Double, dataType: String?, label: String, type: String, key: String) {
        assertNotNull(element)
        assertEquals(id, element!!.get("id"))
        assertEquals(maxLength, element.get("maxLength"))
        if (dataType != null)
            assertEquals(dataType, element.get("dataType"))
        else
            assertNull(dataType)
        assertEquals(label, element.get("label"))
        assertEquals(type, element.get("type"))
        assertEquals(key, element.get("key"))
    }
}
