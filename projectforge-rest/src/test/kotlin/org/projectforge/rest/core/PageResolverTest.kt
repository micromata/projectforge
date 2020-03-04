/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.calendar.CalendarSubscriptionInfoPageRest

class PageResolverTest {
    @Test
    fun resolveTest() {
        assertEquals("react/address", PagesResolver.getListPageUrl(AddressPagesRest::class.java))
        assertEquals("react/address?str=test", PagesResolver.getListPageUrl(AddressPagesRest::class.java, mapOf("str" to "test")))

        var result = PagesResolver.getListPageUrl(AddressPagesRest::class.java, mapOf("str" to "test", "id" to 5))
        assertEquals("react/address?str=test&id=5".length, result.length)
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("id=5"))
        assertTrue(result.startsWith("react/address?"))
        assertTrue(result.contains("&"))

        assertEquals("react/address/edit", PagesResolver.getEditPageUrl(AddressPagesRest::class.java))
        assertEquals("react/address/edit?id=42", PagesResolver.getEditPageUrl(AddressPagesRest::class.java, 42))

        result = PagesResolver.getEditPageUrl(AddressPagesRest::class.java, 42, mapOf("str" to "test"))
        assertEquals("react/address/edit?id=42&str=test".length, result.length)
        assertTrue(result.contains("str=test"))
        assertTrue(result.contains("id=42"))
        assertTrue(result.startsWith("react/address/edit?"))
        assertTrue(result.contains("&"))

        assertEquals("react/calendarSubscription/dynamic", PagesResolver.getDynamicPageUrl(CalendarSubscriptionInfoPageRest::class.java))
    }
}
