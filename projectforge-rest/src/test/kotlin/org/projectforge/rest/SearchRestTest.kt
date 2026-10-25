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
}
