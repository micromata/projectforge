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
package org.projectforge.plugins.merlin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Who may change or delete a template is decided by its stored admins, not by the admins of the posted object.
 */
class MerlinTemplateDaoAccessTest {
    private val dao = MerlinTemplateDao()

    private val admin = PFUserDO().also { it.id = 1 }
    private val accessUser = PFUserDO().also { it.id = 2 }
    private val otherUser = PFUserDO().also { it.id = 3 }

    private fun storedTemplate() = MerlinTemplateDO().also {
        it.id = 5
        it.name = "Template"
        it.adminIds = "1"
        it.accessUserIds = "2"
    }

    @Test
    fun `nobody becomes admin by posting the own id`() {
        val dbObj = storedTemplate()
        for (user in listOf(accessUser, otherUser)) {
            val posted = storedTemplate().also { it.adminIds = "1,${user.id}" }
            for (type in listOf(OperationType.UPDATE, OperationType.DELETE)) {
                assertFalse(dao.hasAccess(user, posted, dbObj, type, false), "user ${user.id}, $type")
            }
        }
    }

    @Test
    fun `the stored admin may update and delete`() {
        val dbObj = storedTemplate()
        assertTrue(dao.hasAccess(admin, storedTemplate().also { it.adminIds = "3" }, dbObj, OperationType.UPDATE, false))
        assertTrue(dao.hasAccess(admin, dbObj, dbObj, OperationType.DELETE, false))
        assertTrue(dao.hasAccess(admin, storedTemplate().also { it.id = null }, null, OperationType.INSERT, false))
    }
}
