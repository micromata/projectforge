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

package org.projectforge.framework.persistence.user.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PFUserDOEmailsTest {
    @Test
    fun `split emails by comma, semicolon and whitespace without blanks and duplicates`() {
        assertEquals(emptyList<String>(), PFUserDO.splitEmails(null))
        assertEquals(emptyList<String>(), PFUserDO.splitEmails(" , ;"))
        assertEquals(
            listOf("a@example.org", "b@example.org", "c@example.org"),
            PFUserDO.splitEmails(" a@example.org,b@example.org; c@example.org\nA@example.org,"),
        )
    }

    @Test
    fun `normalize emails as comma separated list`() {
        assertNull(PFUserDO.normalizeEmails(" ; "))
        assertEquals("a@example.org, b@example.org", PFUserDO.normalizeEmails("a@example.org;b@example.org "))
    }

    @Test
    fun `normalize email lists without the primary address and alternative addresses in the former ones`() {
        val user = user(1, "Main@example.org", "main@example.org, a@example.org", "A@example.org; old@example.org, MAIN@example.org")
        user.normalizeEmailLists()
        assertEquals("a@example.org", user.alternativeEmails)
        assertEquals("old@example.org", user.formerEmails)

        val empty = user(2, null, " main@example.org ", "")
        empty.normalizeEmailLists()
        assertEquals("main@example.org", empty.alternativeEmails, "Without primary address nothing is removed.")
        assertNull(empty.formerEmails)
    }

    @Test
    fun `all emails of a user`() {
        assertEquals(
            listOf("main@example.org", "a@example.org", "old@example.org"),
            PFUserDO.allEmails(user(1, " main@example.org ", "a@example.org, Main@example.org", "old@example.org")),
        )
    }

    @Test
    fun `find addresses used by other users`() {
        val other = user(2, "primary@example.org", "alt@example.org", "former@example.org")
        val deleted = user(3, "deleted@example.org", null, null).also { it.deleted = true }
        val user = user(
            1, "own@example.org",
            "Primary@example.org, free@example.org, deleted@example.org",
            "former@example.org, alt@example.org",
        )
        val all = listOf(user, other, deleted)
        assertEquals(
            mapOf("Primary@example.org" to other, "former@example.org" to other, "alt@example.org" to other),
            PFUserDO.findForeignEmails(user, all),
        )
        assertTrue(PFUserDO.findForeignEmails(other, listOf(other, deleted)).isEmpty(), "The user itself is skipped.")
        assertEquals(
            mapOf("alt@example.org" to other),
            PFUserDO.findForeignEmails(user(null, null, "alt@example.org", null), listOf(other, deleted)),
            "New user without id.",
        )
    }

    private fun user(id: Long?, email: String?, alternativeEmails: String?, formerEmails: String?) = PFUserDO().also {
        it.id = id
        it.username = "user$id"
        it.email = email
        it.alternativeEmails = alternativeEmails
        it.formerEmails = formerEmails
    }
}
