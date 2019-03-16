package org.projectforge.ui

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.rest.ui.Layout
import java.io.File

class UILayoutTest {
    @Test
    fun testEditBook() {
        val gson = GsonBuilder().setPrettyPrinting().create()

        val book = BookDO()
        var jsonString = gson.toJson(Layout.getEditLayout(book))
        testButtonsPresent(jsonString, "cancel", "create");
        testButtonsAbsent(jsonString, "markAsDeleted", "undelete", "save")

        book.pk = 42
        jsonString = gson.toJson(Layout.getEditLayout(book))
        testButtonsPresent(jsonString, "cancel", "markAsDeleted", "save");
        testButtonsAbsent(jsonString, "undelete", "create")

        book.isDeleted = true
        jsonString = gson.toJson(Layout.getEditLayout(book))
        testButtonsPresent(jsonString, "cancel", "undelete", "save");
        testButtonsAbsent(jsonString, "markAsDeleted", "create")
        val file = File("target", "editBook.json");
        file.writeText(jsonString);
        println("Output written to ${file.absolutePath}")
    }

    private fun testButtonsPresent(jsonString: String, vararg buttons: String) {
        buttons.forEach { assertTrue(jsonString.contains(it), "Button '${it}' not found, but expected.") }
    }

    private fun testButtonsAbsent(jsonString: String, vararg buttons: String) {
        buttons.forEach { assertFalse(jsonString.contains(it), "Button '${it}' found, but not allowed.") }
    }
}