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

package src.test.kotlin.org.projectforge.caldav.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.caldav.config.PFMiltonFilter

class PFMiltonFilterTest {
    @Test
    fun checkUserAgentTest() {
        val filter = PFMiltonFilter()
        Assertions.assertFalse(filter.checkUserAgent(null))
        Assertions.assertFalse(filter.checkUserAgent(""))
        Assertions.assertFalse(filter.checkUserAgent("  "))
        Assertions.assertFalse(filter.checkUserAgent("Safari"))
        Assertions.assertFalse(filter.checkUserAgent(".iOS."))
        Assertions.assertFalse(filter.checkUserAgent("ios"), "case is not ignored.")
        Assertions.assertFalse(filter.checkUserAgent("CriOS"))


        Assertions.assertTrue(filter.checkUserAgent("iOS"))
        Assertions.assertTrue(filter.checkUserAgent("Adressbuch"))

        Assertions.assertTrue(filter.checkUserAgent("AddressBook"))
        Assertions.assertTrue(filter.checkUserAgent("Address Book"))
        Assertions.assertTrue(filter.checkUserAgent("Address - Book"))
        Assertions.assertTrue(filter.checkUserAgent("eMClient"))
        Assertions.assertTrue(filter.checkUserAgent("eM Client"))
        Assertions.assertTrue(filter.checkUserAgent("eM-Client"))
    }
}
