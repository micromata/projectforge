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
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.DaoConst
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

/** The data search of the menu ([SearchRest]). */
class SearchRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var searchRest: SearchRest

    /** Deactivated and deleted users aren't found, not even by an admin: the search is for whom to reach. */
    @Test
    fun inactiveUsersNotFoundTest() {
        logon(ADMIN_USER)
        val word = "searchrest${System.currentTimeMillis()}"
        val active = initTestDB.addUser(PFUserDO().also {
            it.username = "active-$word"
            it.lastname = word
        })
        val deactivated = initTestDB.addUser(PFUserDO().also {
            it.username = "deactivated-$word"
            it.lastname = word
            it.deactivated = true
        })
        val deleted = initTestDB.addUser(PFUserDO().also {
            it.username = "deleted-$word"
            it.lastname = word
            it.deleted = true
        })
        val areas = searchRest.query(word, listOf(DaoConst.USER), 10).areas
        val ids = areas.flatMap { it.hits }.map { it.id }
        Assertions.assertTrue(active.id in ids, "$ids")
        Assertions.assertFalse(deactivated.id in ids, "$ids")
        Assertions.assertFalse(deleted.id in ids, "$ids")
    }

    /**
     * An ordinary user finds the users of the user selection (same group) as contacts, without their pages; only in
     * the menu search (`contacts`), and not the user self. An admin gets their pages.
     */
    @Test
    fun contactsForOrdinaryUsersTest() {
        logon(ADMIN_USER)
        val word = "searchcontact${System.currentTimeMillis()}"
        val colleague = initTestDB.addUser(PFUserDO().also {
            it.username = "colleague-$word"
            it.lastname = word
        })
        // In no group of the test user: not found by it.
        initTestDB.addUser(PFUserDO().also {
            it.username = "stranger-$word"
            it.lastname = word
        })
        initTestDB.addGroup("group-$word", TEST_USER, colleague.username!!)
        // The test context registers the user area twice: the hits of all areas.
        val admin = searchRest.query(word, listOf(DaoConst.USER), 10, contacts = true).areas
        Assertions.assertEquals(2, admin.flatMap { it.hits }.map { it.id }.toSet().size)
        Assertions.assertTrue(admin.flatMap { it.hits }.all { it.viewUrl != null })
        Assertions.assertTrue(admin.all { it.listUrl != null })

        logon(TEST_USER)
        Assertions.assertTrue(searchRest.query(word, listOf(DaoConst.USER), 10).areas.isEmpty())
        val contacts = searchRest.query(word, listOf(DaoConst.USER), 10, contacts = true).areas
        Assertions.assertEquals(setOf(colleague.id), contacts.flatMap { it.hits }.map { it.id }.toSet())
        Assertions.assertTrue(contacts.flatMap { it.hits }.all { it.viewUrl == null })
        Assertions.assertTrue(contacts.all { it.listUrl == null })
        val self = searchRest.query(TEST_USER, listOf(DaoConst.USER), 10, contacts = true).areas
        Assertions.assertTrue(self.flatMap { it.hits }.none { it.id == getUser(TEST_USER).id }, "$self")
    }
}
