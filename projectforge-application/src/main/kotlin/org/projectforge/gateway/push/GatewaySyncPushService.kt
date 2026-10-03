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
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.framework.utils.Crypt
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncFavoritesDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncResultDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.Base64
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.push.enabled"], havingValue = "true")
class GatewaySyncPushService(
    private val config: GatewaySyncPushConfig,
    private val userDao: UserDao,
    private val userGroupCache: UserGroupCache,
    private val addressDao: AddressDao,
    private val persistenceService: PfPersistenceService,
    private val userAuthenticationsService: UserAuthenticationsService,
    private val teamCalCache: TeamCalCache,
) {
    @Autowired(required = false)
    private var calendarSubscriptionServiceRest: CalendarSubscriptionServiceRest? = null

    private val lastPushHashes = mutableMapOf<String, Int>()

    /**
     * Start of the last successful address push. Null until the first successful push, so the first sync after
     * a restart of the main instance is a full sync.
     */
    private var lastAddressPush: Date? = null

    /**
     * Hash of the last successfully pushed favorites: they are only pushed again if changed.
     */
    private var lastFavoritesHash: Int? = null

    private val pushLock = ReentrantLock()

    private val webClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(config.url)
            .defaultHeader("X-Gateway-Secret", config.secret)
            .build()
    }

    /**
     * Pushes all users, including deactivated and deleted ones (active = false), so the gateway revokes
     * their access. Tokens are only sent for active users.
     */
    fun pushUsers(): Boolean {
        log.info { "Pushing users to gateway..." }
        val users = userDao.selectAll(checkAccess = false)
        val authByUserId = persistenceService.executeQuery(
            "SELECT a FROM UserAuthenticationsDO a",
            UserAuthenticationsDO::class.java,
        ).associateBy { it.userId }
        val dtos = users.filter { it.username != null }.map { user ->
            val active = user.hasSystemAccess()
            val auth = if (active) authByUserId[user.id] else null
            SyncUserDto(
                username = user.username!!,
                idpExternalId = user.idpExternalId,
                davToken = auth?.davToken,
                calendarRestToken = auth?.calendarExportToken,
                active = active,
            )
        }
        return postSync("/users", dtos) != null
    }

    /**
     * Groups are always pushed completely (small data volume). On a full sync, the gateway deletes groups
     * that are missing here.
     */
    fun pushGroups(fullSync: Boolean) {
        log.info { "Pushing groups to gateway..." }
        val groups = userGroupCache.allGroups.filter { !it.deleted && it.name != null }
        val dtos = groups.map { group ->
            SyncGroupDto(
                name = group.name!!,
                memberUsernames = group.assignedUsers?.mapNotNull { it.username } ?: emptyList(),
            )
        }
        postSync("/groups?fullSync=$fullSync", dtos)
    }

    /**
     * Delta sync: only addresses whose data or image changed since the last successful push, including
     * deleted ones. Full sync: all addresses, the gateway deletes addresses missing here.
     */
    fun pushAddressBooks(fullSync: Boolean): Boolean {
        val pushStart = Date()
        val since = lastAddressPush?.let { Date(it.time - DELTA_OVERLAP_MS) }
        val full = fullSync || since == null
        val addresses = if (full) {
            addressDao.findAll().filterNotNull()
        } else {
            persistenceService.executeQuery(
                "SELECT a FROM AddressDO a WHERE a.lastUpdate >= :since OR a.imageLastUpdate >= :since",
                AddressDO::class.java,
                Pair("since", since),
            )
        }
        if (!full && addresses.isEmpty()) {
            log.info { "No address changes since $since, skipping address push." }
            lastAddressPush = pushStart
            return true
        }
        log.info { "Pushing ${addresses.size} addresses to gateway (fullSync=$full)..." }
        val imagesByAddressId = persistenceService.runReadOnly { context ->
            val images = if (full) {
                context.executeQuery(
                    "SELECT i FROM AddressImageDO i",
                    AddressImageDO::class.java,
                )
            } else {
                addresses.mapNotNull { it.id }.chunked(1000).flatMap { ids ->
                    context.executeQuery(
                        "SELECT i FROM AddressImageDO i WHERE i.address.id IN :ids",
                        AddressImageDO::class.java,
                        Pair("ids", ids),
                    )
                }
            }
            images.mapNotNull { img ->
                val addressId = img.address?.id ?: return@mapNotNull null
                val data = img.image ?: return@mapNotNull null
                addressId to Pair(Base64.getEncoder().encodeToString(data), img.imageType?.name)
            }.toMap()
        }
        val dtos = addresses.map { address ->
            val imgPair = imagesByAddressId[address.id]
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
                imageData = imgPair?.first,
                imageType = imgPair?.second,
                deleted = address.deleted,
            )
        }
        if (postSync("/addressbooks?fullSync=$full", dtos) == null) {
            return false
        }
        lastAddressPush = pushStart
        return true
    }

    /**
     * Pushes the complete list of CardDAV favorites (user name and address uid), if changed since the last push.
     * The favorite flag doesn't update the lastUpdate timestamp reliably, so a delta isn't possible here, but
     * the data volume is small.
     */
    fun pushFavorites(fullSync: Boolean) {
        val rows = persistenceService.executeQuery(
            "SELECT pa.owner.username, pa.address.uid, pa.address.id FROM PersonalAddressDO pa" +
                    " WHERE pa.favoriteCard = true AND pa.deleted = false AND pa.address.deleted = false",
            Array<Any?>::class.java,
        )
        val dtos = rows
            .filter { it[0] != null }
            .groupBy({ it[0] as String }, { (it[1] as String?) ?: "pf-${it[2]}" })
            .map { (username, uids) -> SyncFavoritesDto(username = username, addressUids = uids.sorted()) }
            .sortedBy { it.username }
        val hash = dtos.hashCode()
        if (!fullSync && hash == lastFavoritesHash) {
            log.info { "Favorites unchanged, skipping favorites push." }
            return
        }
        log.info { "Pushing favorites of ${dtos.size} users to gateway..." }
        if (postSync("/favorites", dtos) != null) {
            lastFavoritesHash = hash
        }
    }

    /**
     * Pushes only calendars whose ICS data changed since the last push. A full sync pushes all calendars.
     * If the gateway reports an empty ICS cache (e.g. after a restart), all calendars are pushed again at once.
     */
    fun pushIcsData(fullSync: Boolean, retryOnEmptyGatewayCache: Boolean = true) {
        val serviceRest = calendarSubscriptionServiceRest
        if (serviceRest == null) {
            log.info { "Skipping ICS push (CalendarSubscriptionServiceRest not available)" }
            return
        }
        if (fullSync) {
            lastPushHashes.clear()
        }
        log.info { "Pushing ICS calendar data to gateway..." }
        val users = userDao.selectAll(checkAccess = false).filter { it.hasSystemAccess() && !it.deactivated }
        val icsEntries = mutableListOf<SyncIcsEntryDto>()
        var skippedUnchanged = 0

        // Holidays are identical for all users – generate once and reuse
        var holidaysIcsData: String? = null
        var holidaysEncryptedQ: String? = null
        var holidaysUserId: Long? = null

        for (user in users) {
            try {
                ThreadLocalUserContext.userContext = UserContext(user)
                val userId = user.id!!
                val token = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST) ?: continue

                // Generate ICS for each accessible team calendar
                val calendars = teamCalCache.allAccessibleCalendars
                for (cal in calendars.orEmpty()) {
                    val calId = cal.id ?: continue
                    val count = generateAndAddIcsEntry(user, userId, token, "teamCals=$calId", icsEntries)
                    if (count == 0) skippedUnchanged++
                }

                // Generate Timesheets ICS for this user
                if (generateAndAddIcsEntry(user, userId, token, "timesheetUser=$userId", icsEntries) == 0) {
                    skippedUnchanged++
                }

                // Holidays are identical for all users – generate only for the first user
                if (holidaysIcsData == null) {
                    if (generateAndAddIcsEntry(user, userId, token, "holidays=true", icsEntries) > 0) {
                        val lastEntry = icsEntries.last()
                        holidaysIcsData = lastEntry.icsData
                        holidaysEncryptedQ = lastEntry.queryParam
                        holidaysUserId = lastEntry.userId
                    }
                } else {
                    // Reuse holidays ICS data with this user's encrypted query param
                    val params = "token=$token&holidays=true"
                    val storedToken = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST)
                    if (storedToken != null) {
                        val authenticationToken = storedToken.padEnd(32, 'x')
                        val encryptedQ = Crypt.encrypt(authenticationToken, params)
                        if (encryptedQ != null) {
                            icsEntries.add(SyncIcsEntryDto(userId = userId, queryParam = encryptedQ, icsData = holidaysIcsData))
                        }
                    }
                }

            } catch (e: Exception) {
                log.error(e) { "Error generating ICS for user '${user.username}'" }
            } finally {
                ThreadLocalUserContext.clear()
            }
        }

        // Posted even if empty: the response tells whether the gateway still has its ICS cache.
        val result = postSync("/ics", icsEntries)
        log.info { "ICS push complete: ${icsEntries.size} entries pushed, $skippedUnchanged unchanged (skipped)" }
        if (result?.icsCacheSize == 0 && skippedUnchanged > 0 && retryOnEmptyGatewayCache) {
            log.info { "Gateway reports an empty ICS cache (restarted?), pushing all calendars again." }
            pushIcsData(fullSync = true, retryOnEmptyGatewayCache = false)
        }
    }

    /**
     * @return 1 if entry was added, 0 if skipped (unchanged or error)
     */
    private fun generateAndAddIcsEntry(
        user: org.projectforge.framework.persistence.user.entities.PFUserDO,
        userId: Long,
        token: String,
        additionalParams: String,
        entries: MutableList<SyncIcsEntryDto>,
    ): Int {
        val serviceRest = calendarSubscriptionServiceRest ?: return 0
        ThreadLocalUserContext.userContext = UserContext(user)
        val params = "token=$token&$additionalParams"
        val storedToken = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST)
        if (storedToken == null) {
            log.debug { "No CALENDAR_REST token for user $userId, skipping ICS entry." }
            return 0
        }
        val authenticationToken = storedToken.padEnd(32, 'x')
        val encryptedQ = Crypt.encrypt(authenticationToken, params) ?: return 0

        try {
            val response = serviceRest.exportCalendar(MockIcsRequest(userId, encryptedQ))
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val body = response.body
                val icsData = when (body) {
                    is ByteArray -> String(body, Charsets.UTF_8)
                    else -> body.toString()
                }
                if (icsData.isNotBlank()) {
                    val cacheKey = "$userId:$additionalParams"
                    val hash = icsData.hashCode()
                    if (lastPushHashes[cacheKey] == hash) {
                        return 0
                    }
                    lastPushHashes[cacheKey] = hash
                    entries.add(SyncIcsEntryDto(userId = userId, queryParam = encryptedQ, icsData = icsData))
                    return 1
                }
            }
        } catch (e: Exception) {
            log.debug { "Failed to generate ICS for user $userId, params=$additionalParams: ${e.message}" }
        }
        return 0
    }

    /**
     * @param fullSync If true, all data is pushed and the gateway removes everything missing in the push
     * (nightly). Otherwise only changed addresses and calendars are pushed. The first sync after a restart of the
     * main instance is always a full sync.
     */
    fun pushAll(fullSync: Boolean = false) {
        pushLock.withLock {
            val full = fullSync || lastAddressPush == null
            log.info { if (full) "Starting full gateway sync..." else "Starting delta gateway sync..." }
            if (!pushUsers()) return
            pushGroups(full)
            // Favorites reference addresses by uid, so they need the addresses on the gateway first.
            if (config.syncAddresses && pushAddressBooks(full)) pushFavorites(full)
            if (config.syncCalendar) pushIcsData(full)
        }
    }

    /**
     * @return The gateway's result or null if the push failed.
     */
    private fun postSync(path: String, body: Any): SyncResultDto? {
        try {
            val result = webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SyncResultDto::class.java)
                .block()
            log.info { "Sync push to $path completed: $result" }
            return result ?: SyncResultDto()
        } catch (e: Exception) {
            if (isConnectionError(e)) {
                log.warn { "Gateway not reachable at ${config.url} (sync skipped): Sync push to $path failed" }
            } else {
                log.error(e) { "Sync push to $path failed" }
            }
            return null
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

    companion object {
        /**
         * The delta query reaches back this far before the last push, so changes committed while the last push
         * was running aren't lost.
         */
        private const val DELTA_OVERLAP_MS = 5 * 60 * 1000L
    }
}
