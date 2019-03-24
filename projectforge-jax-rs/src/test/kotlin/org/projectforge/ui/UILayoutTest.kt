package org.projectforge.ui

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.rest.LayoutRegistry
import org.projectforge.rest.json.JsonValidator
import org.projectforge.test.AbstractTestBase

class UILayoutTest : AbstractTestBase() {
    @Test
    fun testEditBookActionButtons() {
        val gson = GsonBuilder().create()

        val book = BookDO()
        var jsonString = gson.toJson(LayoutRegistry.getEditLayout(book))
        var jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("create", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)

        book.pk = 42
        jsonString = gson.toJson(LayoutRegistry.getEditLayout(book))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("markAsDeleted", jsonValidator.get("actions[1].id"))
        assertEquals("update", jsonValidator.get("actions[2].id"))
        assertEquals(3, jsonValidator.getList("actions")?.size)

        book.isDeleted = true
        jsonString = gson.toJson(LayoutRegistry.getEditLayout(book))
        jsonValidator = JsonValidator(jsonString)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("undelete", jsonValidator.get("actions[1].id"))
        assertEquals(2, jsonValidator.getList("actions")?.size)
    }
}