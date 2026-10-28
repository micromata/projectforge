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

package org.projectforge.rest.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.PageRoutes
import org.projectforge.rest.BookEntityRest
import org.projectforge.rest.orga.PostausgangEntityRest

class PageResolverTest {
    @Test
    fun resolveTest() {
        assertEquals("next/outgoingMail", PagesResolver.getListPageUrl(PostausgangEntityRest::class.java))
        assertEquals("next/outgoingMail?str=test", PagesResolver.getListPageUrl(PostausgangEntityRest::class.java, mapOf("str" to "test")))

        var result = PagesResolver.getListPageUrl(PostausgangEntityRest::class.java, mapOf("str" to "test", "id" to 5))
        assertEquals("next/outgoingMail?str=test&id=5".length, result.length)
        // Order of params may differ:
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("id=5"))
        assertTrue(result.startsWith("next/outgoingMail?"))
        assertTrue(result.contains("&"))

        assertEquals("next/outgoingMail/new", PagesResolver.getEditPageUrl(PostausgangEntityRest::class.java))
        assertEquals("next/outgoingMail/42", PagesResolver.getEditPageUrl(PostausgangEntityRest::class.java, 42))

        result = PagesResolver.getEditPageUrl(PostausgangEntityRest::class.java, 42, mapOf("str" to "test", "p2" to "test2"))
        assertEquals("next/outgoingMail/42?str=test&p2=test2".length, result.length)
        // Order of params may differ:
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("p2=test2"))
        assertTrue(result.startsWith("next/outgoingMail/42?"))
        assertTrue(result.contains("&"))
    }

    /**
     * Pages migrated to projectforge-next must resolve to `next/<route>`, so that server side
     * redirects don't throw the user back into the legacy React app. The route may differ from the
     * rest category; the hand built pages deliberately keep it equal (`book` -> `book`).
     */
    @Test
    fun migratedToNextTest() {
        assertEquals("next/book", PagesResolver.getListPageUrl(BookEntityRest::class.java))
        assertEquals("/next/book", PagesResolver.getListPageUrl(BookEntityRest::class.java, absolute = true))
        assertEquals("next/book?str=test", PagesResolver.getListPageUrl(BookEntityRest::class.java, mapOf("str" to "test")))
        // book is hand built in projectforge-next, so its edit route is /book/<id>, not /book/edit/<id>:
        assertEquals("next/book/42", PagesResolver.getEditPageUrl(BookEntityRest::class.java, 42))
        assertEquals("next/book/new", PageRoutes.newEntryUrl("book"))
        assertEquals("next/book/:id", PageRoutes.standardEditPage("book"))
        // Not migrated (a category of no page, every React list is migrated by now): unchanged, and the category is
        // used as the route.
        assertFalse(PageRoutes.isMigrated(NOT_MIGRATED))
        assertEquals("react/$NOT_MIGRATED/edit", PageRoutes.newEntryUrl(NOT_MIGRATED))
        assertEquals("react/$NOT_MIGRATED/edit/:id", PageRoutes.standardEditPage(NOT_MIGRATED))
    }

    /**
     * The row click url of a list grid is the *edit* page with the id placeholder - not the *new
     * entry* url with an id appended, which is what it used to be built from. For the generic React
     * shape both happen to be the same (`react/<category>/edit` + `/id`), so the difference only shows
     * on a hand built page: book answered a row click with `next/book/new/<id>`, the empty add
     * form.
     */
    @Test
    fun rowClickUrlTest() {
        assertEquals("next/book/:id", PageRoutes.standardEditPage("book"))
        assertNotEquals(
            "${PageRoutes.newEntryUrl("book")}/:id",
            PageRoutes.standardEditPage("book"),
            "The new entry url is not a template of the edit url."
        )
        assertEquals("react/$NOT_MIGRATED/edit/:id", PageRoutes.standardEditPage(NOT_MIGRATED))
        // group has been hand built in projectforge-next since, and follows book's shape.
        assertEquals("next/group/:id", PageRoutes.standardEditPage("group"))
    }

    companion object {
        /** A category no page has: resolved as a page not migrated to projectforge-next. */
        private const val NOT_MIGRATED = "notMigratedCategory"
    }
}
