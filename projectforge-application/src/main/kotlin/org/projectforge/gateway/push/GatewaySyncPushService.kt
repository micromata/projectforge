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
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.business.user.UserAuthenticationsDao
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.utils.Crypt
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.beans.factory.annotation.Autowired
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
    private val userAuthenticationsService: UserAuthenticationsService,
    private val teamCalCache: TeamCalCache,
) {
    @Autowired(required = false)
    private var calendarSubscriptionServiceRest: CalendarSubscriptionServiceRest? = null
    private val webClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(config.url)
            .defaultHeader("X-Gateway-Secret", config.secret)
            .build()
    }

    fun pushUsers(): Boolean {
        log.info { "Pushing users to gateway..." }
        val users = userDao.selectAll(checkAccess = false)
        val dtos = users.filter { it.hasSystemAccess() }.map { user ->
            val userId = user.id!!
            SyncUserDto(
                username = user.username!!,
                idpExternalId = user.idpExternalId,
                davToken = userAuthenticationsDao.internalGetToken(userId, UserTokenType.DAV_TOKEN),
                calendarRestToken = userAuthenticationsDao.internalGetToken(userId, UserTokenType.CALENDAR_REST),
                active = user.hasSystemAccess(),
            )
        }
        return postSync("/users", dtos)
    }

    fun pushGroups() {
        log.info { "Pushing groups to gateway..." }
        val groups = userGroupCache.allGroups
        val dtos = groups.map { group ->
            SyncGroupDto(
                name = group.name!!,
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

    fun pushIcsData() {
        val serviceRest = calendarSubscriptionServiceRest
        if (serviceRest == null) {
            log.info { "Skipping ICS push (CalendarSubscriptionServiceRest not available)" }
            return
        }
        log.info { "Pushing ICS calendar data to gateway..." }
        val users = userDao.selectAll(checkAccess = false).filter { it.hasSystemAccess() }
        val icsEntries = mutableListOf<SyncIcsEntryDto>()

        for (user in users) {
            try {
                ThreadLocalUserContext.userContext = UserContext(user)
                val userId = user.id!!
                val token = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST) ?: continue

                // Generate ICS for each accessible team calendar
                val calendars = teamCalCache.allAccessibleCalendars
                for (cal in calendars.orEmpty()) {
                    val calId = cal.id ?: continue
                    generateAndAddIcsEntry(user, userId, token, "teamCals=$calId", icsEntries)
                }

                // Generate Timesheets ICS for this user
                generateAndAddIcsEntry(user, userId, token, "timesheetUser=$userId", icsEntries)

                // Generate Holidays ICS (same for everyone, but cached per user+q)
                generateAndAddIcsEntry(user, userId, token, "holidays=true", icsEntries)

            } catch (e: Exception) {
                log.error(e) { "Error generating ICS for user '${user.username}'" }
            } finally {
                ThreadLocalUserContext.clear()
            }
        }

        if (icsEntries.isNotEmpty()) {
            postSync("/ics", icsEntries)
        }
        log.info { "ICS push complete: ${icsEntries.size} entries" }
    }

    private fun generateAndAddIcsEntry(
        user: org.projectforge.framework.persistence.user.entities.PFUserDO,
        userId: Long,
        token: String,
        additionalParams: String,
        entries: MutableList<SyncIcsEntryDto>,
    ) {
        val serviceRest = calendarSubscriptionServiceRest ?: return
        // Restore user context because exportCalendar() clears it in its finally block
        ThreadLocalUserContext.userContext = UserContext(user)
        val params = "token=$token&$additionalParams"
        val storedToken = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST)
        if (storedToken == null) {
            log.debug { "No CALENDAR_REST token for user $userId, skipping ICS entry." }
            return
        }
        val authenticationToken = storedToken.padEnd(32, 'x')
        val encryptedQ = Crypt.encrypt(authenticationToken, params) ?: return

        try {
            val response = serviceRest.exportCalendar(MockIcsRequest(userId, encryptedQ))
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val body = response.body
                val icsData = when (body) {
                    is ByteArray -> String(body, Charsets.UTF_8)
                    else -> body.toString()
                }
                if (icsData.isNotBlank()) {
                    entries.add(SyncIcsEntryDto(userId = userId, queryParam = encryptedQ, icsData = icsData))
                }
            }
        } catch (e: Exception) {
            log.debug { "Failed to generate ICS for user $userId, params=$additionalParams: ${e.message}" }
        }
    }

    fun pushAll() {
        if (!pushUsers()) return
        pushGroups()
        pushAddressBooks()
        pushIcsData()
    }

    private fun postSync(path: String, body: Any): Boolean {
        try {
            val response = webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block()
            log.info { "Sync push to $path completed: status=${response?.statusCode}" }
            return true
        } catch (e: Exception) {
            if (isConnectionError(e)) {
                log.warn { "Gateway not reachable at ${config.url} (sync skipped): Sync push to $path failed" }
            } else {
                log.error(e) { "Sync push to $path failed" }
            }
            return false
        }
    }

    private fun isConnectionError(e: Exception): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is java.net.ConnectException) return true
            cause = cause.cause
        }
        return false
    }
}
