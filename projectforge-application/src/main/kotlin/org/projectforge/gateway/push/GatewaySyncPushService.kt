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

package org.projectforge.gateway.push

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.push.enabled"], havingValue = "true")
class GatewaySyncPushService(
    private val config: GatewaySyncPushConfig,
    private val userDao: UserDao,
    private val userGroupCache: UserGroupCache,
    private val addressDao: AddressDao,
    private val userAuthenticationsDao: UserAuthenticationsDao,
) {
    private val webClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(config.url)
            .defaultHeader("X-Gateway-Secret", config.secret)
            .build()
    }

    fun pushUsers() {
        log.info { "Pushing users to gateway..." }
        val users = userDao.selectAll(checkAccess = false)
        val dtos = users.filter { it.hasSystemAccess() }.map { user ->
            val userId = user.id!!
            SyncUserDto(
                username = user.username!!,
                idpExternalId = user.idpExternalId,
                email = user.email,
                firstname = user.firstname,
                lastname = user.lastname,
                davToken = userAuthenticationsDao.internalGetToken(userId, UserTokenType.DAV_TOKEN),
                calendarRestToken = userAuthenticationsDao.internalGetToken(userId, UserTokenType.CALENDAR_REST),
                active = user.hasSystemAccess(),
            )
        }
        postSync("/users", dtos)
    }

    fun pushGroups() {
        log.info { "Pushing groups to gateway..." }
        val groups = userGroupCache.allGroups
        val dtos = groups.map { group ->
            SyncGroupDto(
                name = group.name!!,
                description = group.description,
                memberUsernames = group.assignedUsers?.mapNotNull { it.username } ?: emptyList(),
            )
        }
        postSync("/groups", dtos)
    }

    fun pushAddressBooks() {
        log.info { "Pushing address books to gateway..." }
        val addresses = addressDao.findAll()
        val dtos = addresses.filterNotNull().map { address ->
            SyncAddressDto(
                uid = address.uid ?: "pf-${address.id}",
                firstName = address.firstName,
                lastName = address.name,
                organization = address.organization,
                email = address.email,
                privateEmail = address.privateEmail,
                businessPhone = address.businessPhone,
                mobilePhone = address.mobilePhone,
                privatePhone = address.privatePhone,
            )
        }
        postSync("/addressbooks", dtos)
    }

    fun pushCalendars() {
        log.info { "Pushing calendars to gateway..." }
        // TODO: Implement calendar push when requirements are defined
    }

    fun pushAll() {
        pushUsers()
        pushGroups()
        pushAddressBooks()
        pushCalendars()
    }

    private fun postSync(path: String, body: Any) {
        try {
            val response = webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block()
            log.info { "Sync push to $path completed: status=${response?.statusCode}" }
        } catch (e: Exception) {
            log.error(e) { "Sync push to $path failed" }
        }
    }
}
