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

import io.netty.channel.ChannelOption
import jakarta.annotation.PreDestroy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.teamcal.admin.TeamCalCache
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.integration.IntegrationConfig
import org.projectforge.framework.integration.IntegrationErrors
import org.projectforge.framework.integration.SyncCounts
import org.projectforge.framework.integration.SyncStats
import org.projectforge.framework.integration.SyncStatsRegistry
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
import org.projectforge.rest.pub.HeartbeatRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
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
    integrationConfig: IntegrationConfig,
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

    /**
     * Hashes of the last successfully pushed users and groups: a delta sync pushes them only if changed.
     */
    private var lastUsersHash: Int? = null

    private var lastGroupsHash: Int? = null

    private val pushLock = ReentrantLock()

    private val syncStats = SyncStatsRegistry.get("gateway-push")

    private val timeouts = integrationConfig.timeouts(IntegrationConfig.GATEWAY)

    /**
     * Small own pool: the pushes run sequentially, so a few connections are enough. Without the timeouts, a
     * hanging gateway would block the push (and the [pushLock]) forever.
     */
    private val connectionProvider: ConnectionProvider =
        ConnectionProvider.builder("gateway-push")
            .maxConnections(timeouts.maxConnectionsPerRoute)
            .pendingAcquireTimeout(Duration.ofMillis(timeouts.connectionRequestTimeoutMs.takeIf { it > 0 } ?: Long.MAX_VALUE))
            .maxIdleTime(Duration.ofMinutes(1))
            .build()

    private val connector: ReactorClientHttpConnector by lazy {
        var httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeouts.connectTimeoutMs.toInt())
        if (timeouts.responseTimeoutMs > 0) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(timeouts.responseTimeoutMs))
        }
        ReactorClientHttpConnector(httpClient)
    }

    private val webClient: WebClient by lazy {
        WebClient.builder()
            .clientConnector(connector)
            .baseUrl(config.url)
            .defaultHeader("X-Gateway-Secret", config.secret)
            .build()
    }

    /**
     * Without the secret: the heartbeat is public.
     */
    private val heartbeatClient: WebClient by lazy {
        WebClient.builder().clientConnector(connector).build()
    }

    /**
     * Upper bound for [reactor.core.publisher.Mono.block], if the response timeout doesn't fire (e.g. a gateway
     * sending its response very slowly).
     */
    private val blockTimeout: Duration? by lazy {
        timeouts.responseTimeoutMs.takeIf { it > 0 }
            ?.let { Duration.ofMillis(it + timeouts.connectTimeoutMs + BLOCK_TIMEOUT_MARGIN_MS) }
    }

    /**
     * Pushes all users, including deactivated and deleted ones (active = false), so the gateway revokes
     * their access. Tokens are only sent for active users. A delta sync skips the push, if nothing changed since
     * the last push.
     */
    private fun pushUsers(fullSync: Boolean, counts: SyncCounts): Boolean {
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
        }.sortedBy { it.username }
        val hash = dtos.hashCode()
        if (!fullSync && hash == lastUsersHash) {
            log.info { "Users unchanged, skipping user push." }
            counts.unchanged = dtos.size
            return true
        }
        log.info { "Pushing users to gateway..." }
        val result = postSync("/users", dtos, counts) ?: return false
        lastUsersHash = hash.takeIf { result.errors == 0 }
        return true
    }

    /**
     * Groups are always pushed completely (small data volume), but a delta sync skips the push, if nothing
     * changed since the last push. On a full sync, the gateway deletes groups that are missing here.
     */
    private fun pushGroups(fullSync: Boolean, counts: SyncCounts) {
        val groups = userGroupCache.allGroups.filter { !it.deleted && it.name != null }
        val dtos = groups.map { group ->
            SyncGroupDto(
                name = group.name!!,
                memberUsernames = group.assignedUsers?.mapNotNull { it.username }?.sorted() ?: emptyList(),
            )
        }.sortedBy { it.name }
        val hash = dtos.hashCode()
        if (!fullSync && hash == lastGroupsHash) {
            log.info { "Groups unchanged, skipping group push." }
            counts.unchanged = dtos.size
            return
        }
        log.info { "Pushing groups to gateway..." }
        val result = postSync("/groups?fullSync=$fullSync", dtos, counts)
        lastGroupsHash = hash.takeIf { result?.errors == 0 }
    }

    /**
     * Delta sync: only addresses whose data or image changed since the last successful push, including
     * deleted ones. Full sync: all addresses, afterwards the gateway deletes addresses missing here.
     *
     * Sent in batches of [ADDRESS_BATCH_SIZE] (images are Base64 encoded), so neither the main instance nor the
     * gateway has to hold all addresses with their images in memory at once. The batches are always sent as
     * delta: on a full sync, the deletion of missing addresses is triggered after the last batch, so it can't
     * delete the addresses of the batches before.
     */
    private fun pushAddressBooks(fullSync: Boolean, counts: SyncCounts): Boolean {
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
        val errorsBefore = counts.errors
        for (batch in addresses.chunked(ADDRESS_BATCH_SIZE)) {
            postSync("/addressbooks", toAddressDtos(batch), counts)
        }
        if (counts.errors > errorsBefore) {
            // Neither deletions nor the timestamp: the addresses are pushed again with the next sync.
            log.warn { "Address push incomplete, will be repeated on the next sync." }
            return false
        }
        if (full) {
            val uids = addresses.filter { !it.deleted }.map { addressUid(it) }
            if (postSync("/addressbooks/retain", uids, counts) == null) {
                return false
            }
        }
        lastAddressPush = pushStart
        return true
    }

    private fun toAddressDtos(addresses: List<AddressDO>): List<SyncAddressDto> {
        val imagesByAddressId = persistenceService.runReadOnly { context ->
            context.executeQuery(
                "SELECT i FROM AddressImageDO i WHERE i.address.id IN :ids",
                AddressImageDO::class.java,
                Pair("ids", addresses.mapNotNull { it.id }),
            ).mapNotNull { img ->
                val addressId = img.address?.id ?: return@mapNotNull null
                val data = img.image ?: return@mapNotNull null
                addressId to Pair(Base64.getEncoder().encodeToString(data), img.imageType?.name)
            }.toMap()
        }
        return addresses.map { address ->
            val imgPair = imagesByAddressId[address.id]
            SyncAddressDto(
                uid = addressUid(address),
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
    }

    private fun addressUid(address: AddressDO): String = address.uid ?: "pf-${address.id}"

    /**
     * Pushes the complete list of CardDAV favorites (user name and address uid), if changed since the last push.
     * The favorite flag doesn't update the lastUpdate timestamp reliably, so a delta isn't possible here, but
     * the data volume is small.
     */
    private fun pushFavorites(fullSync: Boolean, counts: SyncCounts) {
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
            counts.unchanged = dtos.size
            return
        }
        log.info { "Pushing favorites of ${dtos.size} users to gateway..." }
        if (postSync("/favorites", dtos, counts) != null) {
            lastFavoritesHash = hash
        }
    }

    /**
     * Pushes only calendars whose ICS data changed since the last push. A full sync pushes all calendars.
     * If the gateway reports an empty ICS cache (e.g. after a restart), all calendars are pushed again at once.
     */
    private fun pushIcsData(fullSync: Boolean, counts: SyncCounts, retryOnEmptyGatewayCache: Boolean = true) {
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

        for (user in users) {
            try {
                ThreadLocalUserContext.userContext = UserContext(user)
                val userId = user.id!!
                val token = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST) ?: continue

                // Generate ICS for each accessible team calendar
                val calendars = teamCalCache.allAccessibleCalendars
                for (cal in calendars.orEmpty()) {
                    val calId = cal.id ?: continue
                    if (!addIcsEntry(userId, token, "teamCals=$calId", icsEntries)) skippedUnchanged++
                }

                // Generate Timesheets ICS for this user
                if (!addIcsEntry(userId, token, "timesheetUser=$userId", icsEntries)) skippedUnchanged++

                // Holidays are identical for all users – generate only once, but with each user's query param
                val holidaysQ = encryptQuery(userId, token, HOLIDAYS_PARAMS)
                if (holidaysQ != null && holidaysIcsData == null) {
                    holidaysIcsData = exportIcs(userId, holidaysQ, HOLIDAYS_PARAMS)
                }
                val holidaysAdded = holidaysQ != null && holidaysIcsData != null &&
                        addIfChanged(userId, HOLIDAYS_PARAMS, holidaysQ, holidaysIcsData!!, icsEntries)
                if (!holidaysAdded) skippedUnchanged++

            } catch (e: Exception) {
                log.error(e) { "Error generating ICS for user '${user.username}'" }
            } finally {
                ThreadLocalUserContext.clear()
            }
        }

        // Posted even if empty: the response tells whether the gateway still has its ICS cache.
        var result: SyncResultDto? = null
        try {
            for (batch in icsEntries.chunked(ICS_BATCH_SIZE).ifEmpty { listOf(emptyList()) }) {
                result = postSync("/ics", batch, counts)
                if (result == null) {
                    // The hashes were already updated: forget them, so all calendars are pushed with the next sync.
                    lastPushHashes.clear()
                }
            }
        } catch (e: Exception) {
            lastPushHashes.clear()
            throw e
        }
        counts.unchanged += skippedUnchanged
        log.info { "ICS push complete: ${icsEntries.size} entries pushed, $skippedUnchanged unchanged (skipped)" }
        if (result?.icsCacheSize == 0 && skippedUnchanged > 0 && retryOnEmptyGatewayCache) {
            log.info { "Gateway reports an empty ICS cache (restarted?), pushing all calendars again." }
            pushIcsData(fullSync = true, counts = counts, retryOnEmptyGatewayCache = false)
        }
    }

    /**
     * @return true if the entry was added, false if skipped (unchanged or error).
     */
    private fun addIcsEntry(
        userId: Long,
        token: String,
        additionalParams: String,
        entries: MutableList<SyncIcsEntryDto>,
    ): Boolean {
        val encryptedQ = encryptQuery(userId, token, additionalParams) ?: return false
        val icsData = exportIcs(userId, encryptedQ, additionalParams) ?: return false
        return addIfChanged(userId, additionalParams, encryptedQ, icsData, entries)
    }

    /**
     * @return The encrypted query param of the calendar subscription url or null, if the user has no
     * CALENDAR_REST token.
     */
    private fun encryptQuery(userId: Long, token: String, additionalParams: String): String? {
        val storedToken = userAuthenticationsService.internalGetToken(userId, UserTokenType.CALENDAR_REST)
        if (storedToken == null) {
            log.debug { "No CALENDAR_REST token for user $userId, skipping ICS entry." }
            return null
        }
        return Crypt.encrypt(storedToken.padEnd(32, 'x'), "token=$token&$additionalParams")
    }

    /**
     * Requires the user context of the user (set by the caller).
     * @return The ICS data or null, if empty or failed.
     */
    private fun exportIcs(userId: Long, encryptedQ: String, additionalParams: String): String? {
        val serviceRest = calendarSubscriptionServiceRest ?: return null
        try {
            val response = serviceRest.exportCalendar(MockIcsRequest(userId, encryptedQ))
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val icsData = when (val body = response.body) {
                    is ByteArray -> String(body, Charsets.UTF_8)
                    else -> body.toString()
                }
                return icsData.ifBlank { null }
            }
        } catch (e: Exception) {
            log.debug { "Failed to generate ICS for user $userId, params=$additionalParams: ${e.message}" }
        }
        return null
    }

    /**
     * @return true if the entry was added, false if its content is unchanged since the last push.
     */
    private fun addIfChanged(
        userId: Long,
        additionalParams: String,
        encryptedQ: String,
        icsData: String,
        entries: MutableList<SyncIcsEntryDto>,
    ): Boolean {
        val cacheKey = "$userId:$additionalParams"
        val hash = icsContentHash(icsData)
        if (lastPushHashes[cacheKey] == hash) {
            return false
        }
        lastPushHashes[cacheKey] = hash
        entries.add(SyncIcsEntryDto(userId = userId, queryParam = encryptedQ, icsData = icsData))
        return true
    }

    /**
     * @param fullSync If true, all data is pushed and the gateway removes everything missing in the push
     * (nightly). Otherwise only changed addresses and calendars are pushed. The first sync after a restart of the
     * main instance is always a full sync.
     */
    fun pushAll(fullSync: Boolean = false) {
        pushLock.withLock {
            val full = fullSync || lastAddressPush == null
            val run = syncStats.startRun(if (full) "full" else "delta")
            checkGateway()?.let { problem ->
                run.abort(problem.message!!, problem.timeout)
                return
            }
            log.info { if (full) "Starting full gateway sync..." else "Starting delta gateway sync..." }
            try {
                if (run.step("users") { pushUsers(full, it) }) {
                    run.step("groups") { pushGroups(full, it) }
                    // Favorites reference addresses by uid, so they need the addresses on the gateway first.
                    if (config.syncAddresses && run.step("addresses") { pushAddressBooks(full, it) }) {
                        run.step("favorites") { pushFavorites(full, it) }
                    }
                    if (config.syncCalendar) run.step("ics") { pushIcsData(full, it) }
                }
                run.finish()
            } catch (e: GatewayUnavailableException) {
                log.warn { e.message }
                run.abort(e.message!!, e.timeout)
            } catch (e: Exception) {
                log.error(e) { "Gateway sync failed: ${e.message}" }
                run.abort(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * Calls the gateway's public heartbeat ([HeartbeatRest]), so an unreachable gateway costs one short request
     * and one warning instead of a failing push for every entity.
     * @return null, if the gateway is available, otherwise the problem.
     */
    private fun checkGateway(): GatewayUnavailableException? {
        val url = heartbeatUrl
        try {
            val heartbeat = heartbeatClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(HeartbeatRest.Heartbeat::class.java)
                .block(HEARTBEAT_TIMEOUT)
            if (heartbeat?.mode != HeartbeatRest.MODE_GATEWAY) {
                log.error { "$url isn't a gateway (mode=${heartbeat?.mode}), sync skipped. Check projectforge.gateway.push.url." }
                return GatewayUnavailableException("$url isn't a gateway (mode=${heartbeat?.mode})")
            }
            return null
        } catch (e: Exception) {
            log.warn { "Gateway not reachable at $url, sync skipped: ${e.message}" }
            return GatewayUnavailableException("Gateway not reachable: ${e.message}", IntegrationErrors.isTimeout(e))
        }
    }

    /**
     * The push url points to the sync API (e. g. https://gateway.example.com/api/gateway/sync), the heartbeat is
     * located relative to the root of the gateway (context path included).
     */
    private val heartbeatUrl: String by lazy {
        "${config.url.trimEnd('/').removeSuffix(SYNC_API_PATH)}${HeartbeatRest.URL}"
    }

    /**
     * @param counts The gateway's result is added.
     * @return The gateway's result or null if the push failed.
     * @throws GatewayUnavailableException if the gateway isn't reachable or doesn't answer in time: the whole sync
     * is stopped then, no need to try the other endpoints.
     */
    private fun postSync(path: String, body: Any, counts: SyncCounts): SyncResultDto? {
        try {
            val mono = webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SyncResultDto::class.java)
            val result = (blockTimeout?.let { mono.block(it) } ?: mono.block()) ?: SyncResultDto()
            log.info { "Sync push to $path completed: $result" }
            result.addTo(counts)
            return result
        } catch (e: Exception) {
            if (IntegrationErrors.isConnectionError(e) || isGatewayDown(e)) {
                throw GatewayUnavailableException(
                    "Gateway not reachable at ${config.url} (sync stopped): Sync push to $path failed: ${e.message}",
                    IntegrationErrors.isTimeout(e),
                )
            }
            log.error(e) { "Sync push to $path failed" }
            counts.errors++
            return null
        }
    }

    /**
     * 502, 503 and 504 come from the reverse proxy in front of the gateway: the gateway itself is down.
     */
    private fun isGatewayDown(e: Exception): Boolean {
        return e is WebClientResponseException && e.statusCode.value() in 502..504
    }

    @PreDestroy
    fun shutdown() {
        connectionProvider.dispose()
    }

    private class GatewayUnavailableException(message: String, val timeout: Boolean = false) : RuntimeException(message)

    companion object {
        /**
         * The delta query reaches back this far before the last push, so changes committed while the last push
         * was running aren't lost.
         */
        private const val DELTA_OVERLAP_MS = 5 * 60 * 1000L

        private val HEARTBEAT_TIMEOUT = Duration.ofSeconds(10)

        private const val BLOCK_TIMEOUT_MARGIN_MS = 10_000L

        private const val ADDRESS_BATCH_SIZE = 500

        private const val ICS_BATCH_SIZE = 200

        private const val SYNC_API_PATH = "/api/gateway/sync"

        private const val HOLIDAYS_PARAMS = "holidays=true"

        /**
         * Hash of the ICS data without the DTSTAMP lines: ical4j stamps every generated event with the current
         * time, so the hash of the raw data would change with every export.
         */
        internal fun icsContentHash(icsData: String): Int {
            return icsData.lineSequence().filterNot { it.startsWith("DTSTAMP") }.joinToString("\n").hashCode()
        }
    }
}
