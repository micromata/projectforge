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

package org.projectforge.gateway.sync

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressDO
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncCalendarDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySyncService(
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val addressDao: AddressDao,
    private val userAuthenticationsDao: UserAuthenticationsDao,
) {

    fun syncUsers(users: List<SyncUserDto>): SyncResult {
        var created = 0
        var updated = 0
        var errors = 0
        for (dto in users) {
            try {
                var user = userDao.getInternalByName(dto.username)
                if (user == null) {
                    user = PFUserDO()
                    user.username = dto.username
                    user.firstname = dto.firstname
                    user.lastname = dto.lastname
                    user.email = dto.email
                    user.idpExternalId = dto.idpExternalId
                    user.deactivated = !dto.active
                    userDao.insert(user, checkAccess = false)
                    created++
                } else {
                    user.firstname = dto.firstname
                    user.lastname = dto.lastname
                    user.email = dto.email
                    user.idpExternalId = dto.idpExternalId
                    user.deactivated = !dto.active
                    userDao.update(user, checkAccess = false)
                    updated++
                }
                // Sync tokens if provided
                user.id?.let { userId ->
                    dto.davToken?.let { token ->
                        userAuthenticationsDao.internalSetToken(userId, UserTokenType.DAV_TOKEN, token)
                    }
                    dto.calendarRestToken?.let { token ->
                        userAuthenticationsDao.internalSetToken(userId, UserTokenType.CALENDAR_REST, token)
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Error syncing user '${dto.username}'" }
                errors++
            }
        }
        log.info { "User sync complete: created=$created, updated=$updated, errors=$errors" }
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncGroups(groups: List<SyncGroupDto>): SyncResult {
        var created = 0
        var updated = 0
        var errors = 0
        for (dto in groups) {
            try {
                var group = groupDao.getByName(dto.name)
                if (group == null) {
                    group = GroupDO()
                    group.name = dto.name
                    group.description = dto.description
                    groupDao.insert(group, checkAccess = false)
                    created++
                } else {
                    group.description = dto.description
                    groupDao.update(group, checkAccess = false)
                    updated++
                }
                // Update member assignments
                val memberUsers = dto.memberUsernames.mapNotNull { username ->
                    userDao.getInternalByName(username)
                }
                if (memberUsers.isNotEmpty()) {
                    groupDao.setAssignedUsers(group, memberUsers)
                }
            } catch (e: Exception) {
                log.error(e) { "Error syncing group '${dto.name}'" }
                errors++
            }
        }
        log.info { "Group sync complete: created=$created, updated=$updated, errors=$errors" }
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncAddresses(addresses: List<SyncAddressDto>): SyncResult {
        var created = 0
        var updated = 0
        var errors = 0
        for (dto in addresses) {
            try {
                var address = addressDao.findByUid(dto.uid)
                if (address == null) {
                    address = AddressDO()
                    address.uid = dto.uid
                    address.firstName = dto.firstName
                    address.name = dto.lastName
                    address.organization = dto.organization
                    address.email = dto.email
                    address.privateEmail = dto.privateEmail
                    address.businessPhone = dto.businessPhone
                    address.mobilePhone = dto.mobilePhone
                    address.privatePhone = dto.privatePhone
                    addressDao.insert(address, checkAccess = false)
                    created++
                } else {
                    address.firstName = dto.firstName
                    address.name = dto.lastName
                    address.organization = dto.organization
                    address.email = dto.email
                    address.privateEmail = dto.privateEmail
                    address.businessPhone = dto.businessPhone
                    address.mobilePhone = dto.mobilePhone
                    address.privatePhone = dto.privatePhone
                    addressDao.update(address, checkAccess = false)
                    updated++
                }
            } catch (e: Exception) {
                log.error(e) { "Error syncing address uid='${dto.uid}'" }
                errors++
            }
        }
        log.info { "Address sync complete: created=$created, updated=$updated, errors=$errors" }
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncCalendars(calendars: List<SyncCalendarDto>): SyncResult {
        // Calendar sync stores ICS data for the ICS subscription export.
        // The actual calendar events are served by CalendarSubscriptionServiceRest
        // which generates ICS on-the-fly from the database.
        // TODO: Implement when calendar sync requirements are fully defined
        log.info { "Calendar sync received: ${calendars.size} calendars (implementation pending)" }
        return SyncResult(created = 0, updated = 0, errors = 0)
    }

    data class SyncResult(
        val created: Int,
        val updated: Int,
        val errors: Int,
    )
}
