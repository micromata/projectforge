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
    fun applyWithExistingEmails() {
        val users = listOf(
            user("1", "known@example.org"),
            user("2", "new@example.org"),
            user("3", "unknown@example.org"),
            user("4", "taken-new@example.org"),
            user("5", "Confirmed@example.org"),
            user("6", "pushed@example.org"),
            user(null, "additional@example.org"),
        )
        val existing = setOf(
            "known@example.org", "alt2@example.org", "known-former@example.org", "known@example.org",
            "taken@example.org", "old5@example.org", "old6@example.org",
        )
        val others = mapOf(
            "1" to listOf("alt1@example.org"),
            "2" to listOf("unknown-alt@example.org", "Alt2@example.org", "known-former@example.org"),
            "3" to listOf("other@example.org"),
            "4" to listOf("known@example.org"),
            "5" to listOf("old5@example.org"),
            "6" to listOf("old6@example.org"),
        )
        val result = LanesAndPlanesEmailHolds.apply(
            users, mapOf("6" to "pushed@example.org"), setOf("confirmed@example.org"), existing, others,
        )
        Assertions.assertEquals(
            listOf(LanesAndPlanesEmailHolds.Hold("2", "Alt2@example.org", "new@example.org")),
            result.holds,
            "The first known other email is sent; known primary, used other email, confirmed email and a pushed " +
                    "user aren't changed.",
        )
        Assertions.assertEquals(
            mapOf(
                "1" to "known@example.org",
                "2" to "Alt2@example.org",
                "3" to "unknown@example.org",
                "4" to "taken-new@example.org",
                "5" to "Confirmed@example.org",
                "6" to "pushed@example.org",
                null to "additional@example.org",
            ),
            result.users.associate { it.ident to it.email },
        )
        Assertions.assertEquals(listOf("3", "4", "5", "6"), result.unknownIdents.sorted())
        Assertions.assertTrue(
            LanesAndPlanesEmailHolds.apply(users, emptyMap(), emptySet(), emptySet(), others).let {
                it.holds.isEmpty() && it.unknownIdents.isEmpty()
            },
            "Without existing emails nothing is checked.",
        )
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
