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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.Constants
import org.projectforge.NextMigration
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.BookEntityRest
import org.projectforge.rest.calendar.CalendarSubscriptionInfoPageRest
import org.projectforge.rest.fibu.kost.Kost1PagesRest
import org.projectforge.web.rest.RestAuthenticationUtils

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
     * rest category; the hand built pages deliberately keep it equal (`book` -> `book`).
     */
    @Test
    fun migratedToNextTest() {
        assertEquals("next/book", PagesResolver.getListPageUrl(BookEntityRest::class.java))
        assertEquals("/next/book", PagesResolver.getListPageUrl(BookEntityRest::class.java, absolute = true))
        assertEquals("next/book?str=test", PagesResolver.getListPageUrl(BookEntityRest::class.java, mapOf("str" to "test")))
        // book is hand built in projectforge-next, so its edit route is /book/<id>, not /book/edit/<id>:
        assertEquals("next/book/42", PagesResolver.getEditPageUrl(BookEntityRest::class.java, 42))
        assertEquals("next/book/new", NextMigration.newEntryUrl("book"))
        assertEquals("next/book/:id", NextMigration.standardEditPage("book"))
        // Not migrated: unchanged, and the category is used as the route.
        assertFalse(NextMigration.isMigrated("address"))
        assertEquals("react/address/edit", NextMigration.newEntryUrl("address"))
        assertEquals("react/address/edit/:id", NextMigration.standardEditPage("address"))
    }

    /**
     * The row click url of a list grid is the *edit* page with the id placeholder - not the *new
     * entry* url with an id appended, which is what it used to be built from. For the generic React
     * shape both happen to be the same (`react/address/edit` + `/id`), so the difference only shows
     * on a hand built page: book answered a row click with `next/book/new/<id>`, the empty add
     * form.
     */
    @Test
    fun rowClickUrlTest() {
        assertEquals("next/book/:id", NextMigration.standardEditPage("book"))
        assertNotEquals(
            "${NextMigration.newEntryUrl("book")}/:id",
            NextMigration.standardEditPage("book"),
            "The new entry url is not a template of the edit url - see AGGridSupport.prepareUIGrid4ListPage."
        )
        assertEquals("react/address/edit/:id", NextMigration.standardEditPage("address"))
        // group has been hand built in projectforge-next since, and follows book's shape.
        assertEquals("next/group/:id", NextMigration.standardEditPage("group"))
    }

    /**
     * As long as a page's layout is still served to the legacy React app (bookmark, browser history),
     * the row click url follows the frontend that asked - a user on `/react/cost1` must not be thrown
     * into projectforge-next by clicking a row. `book` no longer takes part: it extends
     * AbstractDTOEntityRest, which serves no layout at all, so there is no React page to stay on.
     */
    @Test
    fun editPagePerFrontendTest() {
        // Migrated from Wicket: the caller is still the React app, so it gets the React page - the
        // Wicket page renders server side and never asks here for a layout.
        val kost1PagesRest = Kost1PagesRest()
        assertEquals("react/cost1/edit/:id", kost1PagesRest.getEditPage(requestOf(null)))
        assertEquals("next/cost1/:id", kost1PagesRest.getEditPage(requestOf(Constants.NEXT)))
        // The Referer is the fallback of RestAuthenticationUtils.isNextClient (the static export is
        // served under /next/).
        assertEquals(
            "next/cost1/:id",
            kost1PagesRest.getEditPage(requestOf(null, referer = "https://pf/next/cost1")),
        )
        // Not migrated: there is only one frontend, so the caller makes no difference.
        val addressPagesRest = AddressPagesRest()
        assertEquals(
            addressPagesRest.getStandardEditPage(),
            addressPagesRest.getEditPage(requestOf(null)),
        )
        assertEquals(
            addressPagesRest.getStandardEditPage(),
            addressPagesRest.getEditPage(requestOf(Constants.NEXT)),
        )
    }

    private fun requestOf(frontendHeader: String?, referer: String? = null): HttpServletRequest {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getHeader(RestAuthenticationUtils.NEXT_CLIENT_HEADER)).thenReturn(frontendHeader)
        Mockito.`when`(request.getHeader("Referer")).thenReturn(referer)
        return request
    }

    /**
     * The way back to the legacy page, offered by projectforge-next while the migration runs. Not
     * derivable from the next url: the app it leads back to isn't (`cost1` came from Wicket), and a
     * route need not name its category.
     */
    @Test
    fun legacyPageTest() {
        // book has no way back: its React page is gone (BookEntityRest serves no layout), so
        // projectforge-next offers no legacy link for it.
        assertNull(NextMigration.legacyListUrl("book"))
        assertNull(NextMigration.legacyEditPage("book"))
        assertNull(NextMigration.legacyNewEntryUrl("book"))
        // Not migrated: the legacy page is the page itself.
        assertEquals(NextMigration.listUrl("address"), NextMigration.legacyListUrl("address"))
        assertEquals(NextMigration.standardEditPage("address"), NextMigration.legacyEditPage("address"))
    }

    /**
     * Pages the Wicket -> React migration never reached go back to Wicket, whose mount points are
     * `<category>List` and `<category>Edit?id=<id>` (`WebRegistry.addMountPages`) - the id is a query
     * parameter there, so the add url is not the edit url with the placeholder dropped.
     */
    @Test
    fun legacyWicketPageTest() {
        assertEquals("wa/cost1List", NextMigration.legacyListUrl("cost1"))
        assertEquals("wa/cost1Edit?id=:id", NextMigration.legacyEditPage("cost1"))
        assertEquals("wa/cost1Edit", NextMigration.legacyNewEntryUrl("cost1"))
        // The url the menu points at is the next page, so the way back is the only route to Wicket.
        assertEquals("next/cost1", NextMigration.listUrl("cost1"))
        // The React page of the same entity exists as a layout, but is not the way back.
        assertEquals("react/cost1/edit/:id", NextMigration.reactEditPage("cost1"))
    }
}
