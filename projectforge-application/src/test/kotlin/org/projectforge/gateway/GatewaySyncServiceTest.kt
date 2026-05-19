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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressDO
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.gateway.sync.GatewayIcsCache
import org.projectforge.gateway.sync.GatewaySyncService
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncUserDto

@ExtendWith(MockitoExtension::class)
class GatewaySyncServiceTest {

    private val userDao = mock<UserDao>()
    private val groupDao = mock<GroupDao>()
    private val addressDao = mock<AddressDao>()
    private val userAuthenticationsDao = mock<UserAuthenticationsDao>()
    private val icsCache = GatewayIcsCache()

    private val service = GatewaySyncService(userDao, groupDao, addressDao, userAuthenticationsDao, icsCache)

    @Test
    fun syncUsersCreatesNewUser() {
        whenever(userDao.getInternalByName("newuser")).thenReturn(null)

        val result = service.syncUsers(
            listOf(
                SyncUserDto(
                    username = "newuser",
                    idpExternalId = "sub-1",
                    email = "new@example.com",
                    firstname = "New",
                    lastname = "User",
                    davToken = "dav-tok",
                    calendarRestToken = "cal-tok",
                    active = true,
                )
            )
        )

        assertEquals(1, result.created)
        assertEquals(0, result.updated)
        assertEquals(0, result.errors)
        verify(userDao).insert(any<PFUserDO>(), eq(false))
    }

    @Test
    fun syncUsersUpdatesExistingUser() {
        val existing = PFUserDO()
        existing.id = 42L
        existing.username = "existuser"
        whenever(userDao.getInternalByName("existuser")).thenReturn(existing)

        val result = service.syncUsers(
            listOf(
                SyncUserDto(
                    username = "existuser",
                    idpExternalId = "sub-2",
                    email = "exist@example.com",
                    firstname = "Exist",
                    lastname = "User",
                    davToken = "new-dav",
                    calendarRestToken = null,
                    active = true,
                )
            )
        )

        assertEquals(0, result.created)
        assertEquals(1, result.updated)
        verify(userDao).update(any<PFUserDO>(), eq(false))
        verify(userAuthenticationsDao).internalSetToken(42L, UserTokenType.DAV_TOKEN, "new-dav")
        verify(userAuthenticationsDao, never()).internalSetToken(eq(42L), eq(UserTokenType.CALENDAR_REST), any())
    }

    @Test
    fun syncGroupsCreatesNewGroup() {
        whenever(groupDao.getByName("devs")).thenReturn(null)

        val result = service.syncGroups(
            listOf(SyncGroupDto(name = "devs", description = "Developers", memberUsernames = listOf()))
        )

        assertEquals(1, result.created)
        verify(groupDao).insert(any<GroupDO>(), eq(false))
    }

    @Test
    fun syncGroupsUpdatesAndAssignsMembers() {
        val existingGroup = GroupDO()
        existingGroup.id = 10L
        existingGroup.name = "team"
        whenever(groupDao.getByName("team")).thenReturn(existingGroup)

        val user1 = PFUserDO().apply { id = 1L; username = "alice" }
        val user2 = PFUserDO().apply { id = 2L; username = "bob" }
        whenever(userDao.getInternalByName("alice")).thenReturn(user1)
        whenever(userDao.getInternalByName("bob")).thenReturn(user2)
        whenever(userDao.getInternalByName("unknown")).thenReturn(null)

        val result = service.syncGroups(
            listOf(SyncGroupDto(name = "team", description = "Updated", memberUsernames = listOf("alice", "bob", "unknown")))
        )

        assertEquals(0, result.created)
        assertEquals(1, result.updated)
        verify(groupDao).setAssignedUsers(eq(existingGroup), argThat<Collection<PFUserDO>> { size == 2 })
    }

    @Test
    fun syncAddressesCreatesNew() {
        whenever(addressDao.findByUid("pf-100")).thenReturn(null)

        val result = service.syncAddresses(
            listOf(
                SyncAddressDto(
                    uid = "pf-100",
                    firstName = "Max",
                    lastName = "Mustermann",
                    organization = "Micromata",
                    email = "max@example.com",
                    privateEmail = null,
                    businessPhone = "+49123456",
                    mobilePhone = null,
                    privatePhone = null,
                )
            )
        )

        assertEquals(1, result.created)
        verify(addressDao).insert(any<AddressDO>(), eq(false))
    }

    @Test
    fun syncAddressesUpdatesExisting() {
        val existing = AddressDO()
        existing.id = 50L
        existing.uid = "pf-50"
        whenever(addressDao.findByUid("pf-50")).thenReturn(existing)

        val result = service.syncAddresses(
            listOf(
                SyncAddressDto(
                    uid = "pf-50",
                    firstName = "Updated",
                    lastName = "Name",
                    organization = null,
                    email = "updated@example.com",
                    privateEmail = null,
                    businessPhone = null,
                    mobilePhone = null,
                    privatePhone = null,
                )
            )
        )

        assertEquals(0, result.created)
        assertEquals(1, result.updated)
        verify(addressDao).update(any<AddressDO>(), eq(false))
    }

    @Test
    fun syncIcsEntriesPutsIntoCache() {
        val entries = listOf(
            SyncIcsEntryDto(userId = 1L, queryParam = "q1", icsData = "ics-content-1"),
            SyncIcsEntryDto(userId = 2L, queryParam = "q2", icsData = "ics-content-2"),
        )

        val result = service.syncIcsEntries(entries)

        assertEquals(0, result.created)
        assertEquals(2, result.updated)
        assertEquals("ics-content-1", icsCache.get(1L, "q1"))
        assertEquals("ics-content-2", icsCache.get(2L, "q2"))
    }
}
