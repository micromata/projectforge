package org.projectforge.ui

import com.google.gson.GsonBuilder
import de.micromata.genome.db.jpa.history.api.HistoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.rest.BookRest
import org.projectforge.rest.core.ListFilterService
import org.projectforge.rest.json.JsonValidator
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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
}