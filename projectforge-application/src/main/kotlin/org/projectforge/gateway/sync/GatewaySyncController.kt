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

import java.security.MessageDigest
import mu.KotlinLogging
import org.projectforge.framework.integration.SyncStatsRegistry
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncFavoritesDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncResultDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/gateway/sync")
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySyncController(
    private val gatewaySyncService: GatewaySyncService,
    @Value("\${projectforge.gateway.sync.secret:}") private val syncSecret: String,
) {

    @PostMapping("/users")
    fun syncUsers(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody users: List<SyncUserDto>,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: ${users.size} users" }
        val result = measure("users") { gatewaySyncService.syncUsers(users) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/groups")
    fun syncGroups(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody groups: List<SyncGroupDto>,
        @RequestParam(defaultValue = "false") fullSync: Boolean,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: ${groups.size} groups (fullSync=$fullSync)" }
        val result = measure("groups") { gatewaySyncService.syncGroups(groups, fullSync) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/addressbooks")
    fun syncAddressBooks(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody addresses: List<SyncAddressDto>,
        @RequestParam(defaultValue = "false") fullSync: Boolean,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: ${addresses.size} addresses (fullSync=$fullSync)" }
        val result = measure("addresses") { gatewaySyncService.syncAddresses(addresses, fullSync) }
        return ResponseEntity.ok(result)
    }

    /**
     * End of a full address sync sent in batches: all addresses missing in [uids] are marked as deleted.
     */
    @PostMapping("/addressbooks/retain")
    fun retainAddresses(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody uids: List<String>,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: ${uids.size} address uids to retain" }
        val result = measure("addresses-retain") { gatewaySyncService.retainAddresses(uids) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/favorites")
    fun syncFavorites(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody favorites: List<SyncFavoritesDto>,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: favorites of ${favorites.size} users" }
        val result = measure("favorites") { gatewaySyncService.syncFavorites(favorites) }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/ics")
    fun syncIcsEntries(
        @RequestHeader("X-Gateway-Secret") secret: String,
        @RequestBody entries: List<SyncIcsEntryDto>,
    ): ResponseEntity<Any> {
        if (!authenticateSecret(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Receiving sync: ${entries.size} ICS entries" }
        val result = measure("ics") { gatewaySyncService.syncIcsEntries(entries) }
        return ResponseEntity.ok(result)
    }

    /**
     * Timing and statistics per endpoint, shown on the system statistics page (gateway-receive-*).
     */
    private fun measure(name: String, block: () -> SyncResultDto): SyncResultDto {
        return SyncStatsRegistry.get("gateway-receive-$name").execute { run ->
            run.step(name) { counts -> block().also { it.addTo(counts) } }
        }
    }

    private fun authenticateSecret(secret: String): Boolean {
        if (syncSecret.isBlank()) {
            log.error { "Gateway sync secret is not configured!" }
            return false
        }
        // Constant time, so the secret can't be guessed from response times.
        if (!MessageDigest.isEqual(secret.toByteArray(Charsets.UTF_8), syncSecret.toByteArray(Charsets.UTF_8))) {
            log.warn { "Invalid gateway sync secret received" }
            return false
        }
        return true
    }
}
