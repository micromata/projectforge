package org.projectforge.ui

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.rest.BookRest
import org.projectforge.rest.json.JsonValidator
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class UILayoutTest : AbstractTestBase() {
    @Autowired
    lateinit var bookRest: BookRest

    @Test
    fun testEditBookActionButtons() {
        val gson = GsonBuilder().create()

        val book = BookDO()
        var jsonString = gson.toJson(bookRest.createEditLayout(book))
        var jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("create", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)

        book.pk = 42
        jsonString = gson.toJson(bookRest.createEditLayout(book))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("markAsDeleted", jsonValidator.get("actions[1].id"))
        assertEquals("update", jsonValidator.get("actions[2].id"))
        assertEquals(3, jsonValidator.getList("actions")?.size)

        book.isDeleted = true
        jsonString = gson.toJson(bookRest.createEditLayout(book))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("undelete", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)
    }

    @Test
    fun testEditBookLayout() {
        val gson = GsonBuilder().create()

        val book = BookDO()
        var jsonString = gson.toJson(bookRest.createEditLayout(book))
        var jsonValidator = JsonValidator(jsonString)
        assertEquals("???book.title.add???", jsonValidator.get("title")) // translations not available in test.
        assertField(jsonValidator.getMap("layout[0]"), "title", 255.0, "string", "??? book.title ???", type = "input", key = "el-1")
        assertEquals(true, jsonValidator.getBoolean("layout[0].focus"))
        assertField(jsonValidator.getMap("layout[1]"), "authors", 1000.0, null, "??? book.authors ???", type = "textarea", key = "el-2")
        assertNull(jsonValidator.getBoolean("layout[1].focus"))
    }

    private fun assertField(element: Map<String, *>?, id: String, maxLength: Double, dataType: String?, label: String, type: String, key: String) {
        assertNotNull(element)
        if (element == null) return // Only for compiler: shouldn't occur due to previous assertNotNull statement.
        assertEquals(id, element.get("id"))
        assertEquals(maxLength, element.get("max-length"))
        if (dataType != null)
            assertEquals(dataType, element.get("data-type"))
        else
            assertNull(dataType)
        assertEquals(type, element.get("type"))
        assertEquals(key, element.get("key"))
    }
}