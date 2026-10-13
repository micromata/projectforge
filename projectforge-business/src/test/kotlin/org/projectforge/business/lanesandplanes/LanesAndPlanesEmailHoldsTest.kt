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

package org.projectforge.business.lanesandplanes

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LanesAndPlanesEmailHoldsTest {
    @Test
    fun apply() {
        val users = listOf(
            user("1", "same@example.org"),
            user("2", "New@example.org"),
            user("3", "confirmed@example.org"),
            user("4", "unknown@example.org"),
            user(null, "additional@example.org"),
            user("5", "taken-new@example.org"),
            user("6", "taken@example.org"),
        )
        val last = mapOf(
            "1" to "Same@Example.org",
            "2" to "old@example.org",
            "3" to "old3@example.org",
            "5" to "taken@example.org",
        )
        val result = LanesAndPlanesEmailHolds.apply(users, last, setOf("confirmed@example.org"))
        Assertions.assertEquals(
            listOf(LanesAndPlanesEmailHolds.Hold("2", "old@example.org", "New@example.org")),
            result.holds,
            "Only an unconfirmed change is held; a different case isn't a change, a used old email isn't held.",
        )
        Assertions.assertEquals(
            mapOf(
                "1" to "same@example.org",
                "2" to "old@example.org",
                "3" to "confirmed@example.org",
                "4" to "unknown@example.org",
                "5" to "taken-new@example.org",
                "6" to "taken@example.org",
                null to "additional@example.org",
            ),
            result.users.associate { it.ident to it.email },
        )
        Assertions.assertEquals(result.users.map { it.email.lowercase() }.sorted(), result.users.map { it.email.lowercase() })
    }

    @Test
    fun loadAndSave(@TempDir dir: File) {
        val file = File(dir, "state.json")
        Assertions.assertNull(LanesAndPlanesEmailHolds.load(file), "Missing file.")
        LanesAndPlanesEmailHolds.save(file, listOf(user("1", "a@example.org"), user(null, "b@example.org")))
        val state = LanesAndPlanesEmailHolds.load(file)
        Assertions.assertEquals(mapOf("1" to "a@example.org"), state?.emailsByIdent)
        Assertions.assertEquals(2, state?.users, "All users are counted, the additional ones included.")
        file.writeText("no json")
        Assertions.assertNull(LanesAndPlanesEmailHolds.load(file), "Unreadable file.")
    }

    private fun user(ident: String?, email: String) = LanesAndPlanesUser(
        ident = ident,
        firstName = "First",
        lastName = "Last",
        email = email,
        accountingInvoiceProfileIds = listOf(1L),
    )
}
