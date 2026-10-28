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

package org.projectforge.gateway

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.gateway.sync.GatewaySyncService
import org.projectforge.gateway.sync.dto.SyncDataTransferAreaDto
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao

class GatewaySyncDataTransferAreasTest {
    private val persistenceService: PfPersistenceService = mock()
    private val dataTransferAreaDao: DataTransferAreaDao = mock()
    private val service = GatewaySyncService(
        persistenceService = persistenceService,
        userGroupCache = mock(),
        gatewayIcsCache = mock(),
        addressImageCache = mock(),
        personalAddressCache = mock(),
        dataTransferAreaDao = dataTransferAreaDao,
    )

    init {
        whenever(persistenceService.executeQuery("SELECT u FROM PFUserDO u", PFUserDO::class.java)).thenReturn(
            listOf(user(11, "kai"), user(12, "anna"))
        )
        whenever(
            persistenceService.executeQuery("SELECT g FROM GroupDO g WHERE g.deleted = false", GroupDO::class.java)
        ).thenReturn(listOf(GroupDO().also { it.id = 21; it.name = "staff" }))
    }

    @Test
    fun `new area is created with the id of the main instance, local user ids, unknown names are dropped`() {
        existingAreas()
        // The row inserted natively with the given id, as the DAO reads it afterwards.
        whenever(dataTransferAreaDao.find(5L, false)).thenReturn(DataTransferAreaDO().also { it.id = 5 })
        val result = service.syncDataTransferAreas(
            listOf(dto(id = 5, admins = listOf("kai", "unknown"), groups = listOf("staff", "gone")))
        )
        assertEquals(1, result.created)
        val captor = argumentCaptor<DataTransferAreaDO>()
        verify(dataTransferAreaDao).update(captor.capture(), eq(false))
        val area = captor.firstValue
        assertEquals(5L, area.id)
        assertEquals("Area 5", area.areaName)
        assertEquals("11", area.adminIds)
        assertEquals("12", area.accessUserIds)
        assertEquals("21", area.accessGroupIds)
        assertNull(area.observerIds)
        assertEquals(true, area.externalDownloadEnabled)
        assertEquals("token", area.externalAccessToken)
    }

    @Test
    fun `unchanged area isn't written, changed one is updated`() {
        val unchanged = area(1, dto(id = 1))
        val changed = area(2, dto(id = 2))
        existingAreas(unchanged, changed)
        val result = service.syncDataTransferAreas(listOf(dto(id = 1), dto(id = 2, name = "Renamed")))
        assertEquals(1, result.unchanged)
        assertEquals(1, result.updated)
        verify(dataTransferAreaDao).update(changed, false)
        verify(dataTransferAreaDao, never()).update(unchanged, false)
        assertEquals("Renamed", changed.areaName)
    }

    @Test
    fun `area missing in the list is marked as deleted, a returning one restored`() {
        val missing = area(1, dto(id = 1))
        val returning = area(2, dto(id = 2)).also { it.deleted = true }
        existingAreas(missing, returning)
        val result = service.syncDataTransferAreas(listOf(dto(id = 2)))
        assertEquals(1, result.deleted)
        assertEquals(1, result.updated)
        verify(dataTransferAreaDao).markAsDeleted(missing, false)
        assertNull(missing.externalAccessToken, "No external access to the files kept on the gateway.")
        assertNull(missing.externalPassword)
        assertFalse(missing.externalAccessEnabled)
        verify(dataTransferAreaDao).undelete(returning, false)
        assertEquals("token", returning.externalAccessToken, "Restored with the external access of the main instance.")
    }

    @Test
    fun `empty list deletes all areas`() {
        val area = area(1, dto(id = 1))
        existingAreas(area)
        assertEquals(1, service.syncDataTransferAreas(emptyList()).deleted)
        verify(dataTransferAreaDao).markAsDeleted(area, false)
        verify(dataTransferAreaDao, never()).update(any<DataTransferAreaDO>(), any())
    }

    private fun existingAreas(vararg areas: DataTransferAreaDO) {
        whenever(
            persistenceService.executeQuery(
                "SELECT a FROM DataTransferAreaDO a",
                DataTransferAreaDO::class.java,
            )
        ).thenReturn(areas.toList())
    }

    private fun user(id: Long, username: String) = PFUserDO().also { it.id = id; it.username = username }

    private fun dto(
        id: Long,
        name: String = "Area $id",
        admins: List<String> = listOf("kai"),
        groups: List<String> = emptyList(),
    ) = SyncDataTransferAreaDto(
        id = id,
        areaName = name,
        adminUsernames = admins,
        accessUsernames = listOf("anna"),
        accessGroupNames = groups,
        externalDownloadEnabled = true,
        externalAccessToken = "token",
        externalPassword = "password",
        expiryDays = 7,
    )

    /** An area on the gateway as the sync of [dto] would have written it. */
    private fun area(id: Long, dto: SyncDataTransferAreaDto) = DataTransferAreaDO().also {
        it.id = id
        it.areaName = dto.areaName
        it.adminIds = "11"
        it.accessUserIds = "12"
        it.externalDownloadEnabled = true
        it.externalUploadEnabled = false
        it.externalAccessToken = "token"
        it.externalPassword = "password"
        it.expiryDays = 7
    }
}
