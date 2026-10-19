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

package org.projectforge.plugins.datatransfer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * The gateway has only the areas pushed by the main instance (with their ids): it must neither change nor create
 * any area itself, personal boxes included.
 */
class DataTransferAreaDaoGatewayTest {
    private val dao = DataTransferAreaDao().also {
        DataTransferAreaDao::class.java.getDeclaredField("externalAccessConfig").apply {
            isAccessible = true
            set(it, DataTransferExternalAccessConfig(gatewayMode = true))
        }
    }

    private val admin = PFUserDO().also { it.id = 1 }

    @Test
    fun `areas are read-only on the gateway, also for their admins`() {
        val area = DataTransferAreaDO().also { it.id = 5; it.areaName = "Area"; it.adminIds = "1" }
        for (type in listOf(OperationType.INSERT, OperationType.UPDATE, OperationType.DELETE)) {
            assertFalse(dao.hasAccess(admin, area, area, type, false), "$type")
        }
        assertFalse(dao.hasInsertAccess(admin), "No add button.")
    }

    @Test
    fun `no personal boxes on the gateway`() {
        assertNull(dao.ensurePersonalBox(1))
    }
}
