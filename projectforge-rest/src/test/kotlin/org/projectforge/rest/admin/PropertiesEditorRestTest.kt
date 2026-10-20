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

package org.projectforge.rest.admin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The properties editor shows and writes projectforge.properties (database credentials, security keys): admins
 * only, and saving not for restricted or demo users.
 */
class PropertiesEditorRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var propertiesEditorRest: PropertiesEditorRest

    private val saveRequest = PropertiesEditorRest.SaveRequest(fileVersion = "any", changes = emptyList())

    @Test
    fun `ordinary users are refused on every endpoint`() {
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { propertiesEditorRest.getData() }
        assertThrows(AccessException::class.java) { propertiesEditorRest.getBackups() }
        assertThrows(AccessException::class.java) { propertiesEditorRest.getDiffText(null, true) }
        assertThrows(AccessException::class.java) { propertiesEditorRest.save(saveRequest) }
    }

    @Test
    fun `admins may read and save`() {
        logon(TEST_ADMIN_USER)
        assertNotNull(propertiesEditorRest.getBackups())
        // Nothing to change, so nothing is written, but the access checks are passed:
        assertFalse(propertiesEditorRest.save(saveRequest).success)
    }

    @Test
    fun `restricted admins may not save`() {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "PropertiesEditorRestTest.restricted"
            it.restrictedUser = true
        })
        val group = getGroup(ADMIN_GROUP)
        group.addUser(user)
        groupDao.update(group)
        logon(user)
        assertThrows(AccessException::class.java) { propertiesEditorRest.save(saveRequest) }
    }
}
