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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure number helpers of [PhoneCallRest] (see [SendTextMessageRest] for the sibling SMS
 * page). The call itself goes through [org.projectforge.rest.sipgate.SipgateDirectCallService], covered by its
 * own test; here only the input validation and the telephone-system-number stripping are checked.
 */
class PhoneCallRestTest {
    @Test
    fun isValidDialNumber() {
        Assertions.assertTrue(PhoneCallRest.isValidDialNumber("0123456789"))
        Assertions.assertTrue(PhoneCallRest.isValidDialNumber("+49 561 / 316 793-0"))
        Assertions.assertTrue(PhoneCallRest.isValidDialNumber("(0561) 316793"))
        Assertions.assertTrue(PhoneCallRest.isValidDialNumber(""), "The emptiness is guarded before this check.")

        Assertions.assertFalse(PhoneCallRest.isValidDialNumber("0561abc"))
        Assertions.assertFalse(PhoneCallRest.isValidDialNumber("call me"))
        Assertions.assertFalse(PhoneCallRest.isValidDialNumber("0561*99"))
    }

    @Test
    fun stripSystemNumber() {
        Assertions.assertNull(PhoneCallRest.stripSystemNumber(null, "0"))

        // No system number configured: the extracted number is returned unchanged.
        Assertions.assertEquals("0561316793", PhoneCallRest.stripSystemNumber("0561316793", null))
        Assertions.assertEquals("0561316793", PhoneCallRest.stripSystemNumber("0561316793", ""))

        // Leading system number is stripped, otherwise the number is left as it is.
        Assertions.assertEquals("561316793", PhoneCallRest.stripSystemNumber("0561316793", "0"))
        Assertions.assertEquals("316793", PhoneCallRest.stripSystemNumber("0561316793", "0561"))
        Assertions.assertEquals("0561316793", PhoneCallRest.stripSystemNumber("0561316793", "99"))
    }
}
