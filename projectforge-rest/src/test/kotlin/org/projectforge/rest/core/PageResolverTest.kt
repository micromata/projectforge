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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.NextMigration
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.BookPagesRest
import org.projectforge.rest.calendar.CalendarSubscriptionInfoPageRest

class PageResolverTest {
    @Test
    fun resolveTest() {
        assertEquals("react/address", PagesResolver.getListPageUrl(AddressPagesRest::class.java))
        assertEquals("react/address?str=test", PagesResolver.getListPageUrl(AddressPagesRest::class.java, mapOf("str" to "test")))

        var result = PagesResolver.getListPageUrl(AddressPagesRest::class.java, mapOf("str" to "test", "id" to 5))
        assertEquals("react/address?str=test&id=5".length, result.length)
        // Order of params may differ:
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("id=5"))
        assertTrue(result.startsWith("react/address?"))
        assertTrue(result.contains("&"))

        assertEquals("react/address/edit", PagesResolver.getEditPageUrl(AddressPagesRest::class.java))
        assertEquals("react/address/edit/42", PagesResolver.getEditPageUrl(AddressPagesRest::class.java, 42))

        result = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, 42, mapOf("str" to "test", "p2" to "test2"))
        assertEquals("react/address/edit/42?str=test&p2=test2".length, result.length)
        // Order of params may differ:
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("p2=test2"))
        assertTrue(result.startsWith("react/address/edit/42?"))
        assertTrue(result.contains("&"))

        assertEquals("react/calendarSubscription/dynamic", PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java, trailingSlash = false))
        assertEquals("react/calendarSubscription/dynamic/", PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java))
        assertEquals("react/calendarSubscription/dynamic/123", PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java, id = 123))
    }

    /**
     * Pages migrated to projectforge-next must resolve to `next/<route>`, so that server side
     * redirects don't throw the user back into the legacy React app. The route may differ from the
     * rest category: `book` -> `books`.
     */
    @Test
    fun migratedToNextTest() {
        assertEquals("next/books", PagesResolver.getListPageUrl(BookPagesRest::class.java))
        assertEquals("/next/books", PagesResolver.getListPageUrl(BookPagesRest::class.java, absolute = true))
        assertEquals("next/books?str=test", PagesResolver.getListPageUrl(BookPagesRest::class.java, mapOf("str" to "test")))
        // books is hand built in projectforge-next, so its edit route is /books/<id>, not /books/edit/<id>:
        assertEquals("next/books/42", PagesResolver.getEditPageUrl(BookPagesRest::class.java, 42))
        assertEquals("next/books/new", NextMigration.newEntryUrl("book"))
        assertEquals("next/books/:id", NextMigration.standardEditPage("book"))
        // Not migrated: unchanged, and the category is used as the route.
        assertFalse(NextMigration.isMigrated("address"))
        assertEquals("react/address/edit", NextMigration.newEntryUrl("address"))
        assertEquals("react/address/edit/:id", NextMigration.standardEditPage("address"))
    }

    /**
     * The row click url of a list grid is the *edit* page with the id placeholder - not the *new
     * entry* url with an id appended, which is what it used to be built from. For the generic React
     * shape both happened to be the same (`react/group/edit` + `/id`), so the difference only showed
     * on a hand built page: books answered a row click with `next/books/new/<id>`, the empty add
     * form.
     */
    @Test
    fun rowClickUrlTest() {
        assertEquals("next/books/:id", NextMigration.standardEditPage("book"))
        assertNotEquals(
            "${NextMigration.newEntryUrl("book")}/:id",
            NextMigration.standardEditPage("book"),
            "The new entry url is not a template of the edit url - see AGGridSupport.prepareUIGrid4ListPage."
        )
        assertEquals("react/group/edit/:id", NextMigration.standardEditPage("group"))
    }

    /**
     * The way back to the legacy page, offered by projectforge-next while the migration runs. Not
     * derivable from the next url: `books` is not `book`.
     */
    @Test
    fun legacyPageTest() {
        assertEquals("react/book", NextMigration.legacyListUrl("book"))
        assertEquals("react/book/edit/:id", NextMigration.legacyEditPage("book"))
        assertEquals("react/book/edit", NextMigration.legacyNewEntryUrl("book"))
        // Not migrated: the legacy page is the page itself.
        assertEquals(NextMigration.listUrl("address"), NextMigration.legacyListUrl("address"))
        assertEquals(NextMigration.standardEditPage("address"), NextMigration.legacyEditPage("address"))
    }
}
