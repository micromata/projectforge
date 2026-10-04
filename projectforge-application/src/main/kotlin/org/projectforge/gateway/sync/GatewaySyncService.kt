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
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressImageCache
import org.projectforge.business.address.PersonalAddressCache
import org.projectforge.business.address.PersonalAddressDO
import org.projectforge.business.address.ImageType
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.carddav.service.AddressDAVCache
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncFavoritesDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncResultDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySyncService(
    private val persistenceService: PfPersistenceService,
    private val userGroupCache: UserGroupCache,
    private val gatewayIcsCache: GatewayIcsCache,
    private val addressImageCache: AddressImageCache,
    private val personalAddressCache: PersonalAddressCache,
) {
    /** Optional: not available if the CardDAV server is disabled. */
    @Autowired(required = false)
    private var addressDAVCache: AddressDAVCache? = null

    /**
     * Users are never deleted here: the main instance pushes all of its users, inactive ones with
     * [SyncUserDto.active] = false. Their tokens are revoked.
     *
     * Only actual changes are written: the main instance pushes all users, so writing them unconditionally
     * would reset the token creation dates with every sync.
     */
    fun syncUsers(users: List<SyncUserDto>): SyncResultDto {
        var created = 0
        var updated = 0
        var unchanged = 0
        var errors = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            for (dto in users) {
                try {
                    val existing = em.createQuery(
                        "SELECT u FROM PFUserDO u WHERE u.username = :name",
                        PFUserDO::class.java
                    ).setParameter("name", dto.username).resultList.firstOrNull()

                    var changed = false
                    val user: PFUserDO
                    if (existing == null) {
                        user = PFUserDO()
                        user.username = dto.username
                        user.idpExternalId = dto.idpExternalId
                        user.deactivated = !dto.active
                        em.persist(user)
                        em.flush()
                    } else {
                        if (existing.idpExternalId != dto.idpExternalId || existing.deactivated != !dto.active) {
                            existing.idpExternalId = dto.idpExternalId
                            existing.deactivated = !dto.active
                            changed = true
                        }
                        user = existing
                    }

                    // Sync tokens if provided (already encrypted, store 1:1)
                    user.id?.let { userId ->
                        val auth = em.createQuery(
                            "SELECT a FROM UserAuthenticationsDO a WHERE a.user.id = :userId",
                            UserAuthenticationsDO::class.java
                        ).setParameter("userId", userId).resultList.firstOrNull()
                        if (!dto.active) {
                            // Deactivated or deleted on the main instance: revoke DAV and calendar access.
                            if (auth != null && (auth.davToken != null || auth.calendarExportToken != null)) {
                                auth.davToken = null
                                auth.davTokenCreationDate = null
                                auth.calendarExportToken = null
                                auth.calendarExportTokenCreationDate = null
                                auth.lastUpdate = Date()
                                em.merge(auth)
                                log.info { "Revoked gateway tokens of inactive user '${dto.username}'." }
                                changed = true
                            }
                        } else if (dto.davToken != null || dto.calendarRestToken != null) {
                            val target = auth ?: UserAuthenticationsDO().also {
                                it.user = user
                                it.created = Date()
                            }

                            val now = Date()
                            var tokenChanged = false
                            dto.davToken?.takeIf { it != target.davToken }?.let {
                                target.davToken = it
                                target.davTokenCreationDate = now
                                tokenChanged = true
                            }
                            dto.calendarRestToken?.takeIf { it != target.calendarExportToken }?.let {
                                target.calendarExportToken = it
                                target.calendarExportTokenCreationDate = now
                                tokenChanged = true
                            }
                            if (tokenChanged) {
                                target.lastUpdate = now
                                if (target.id == null) em.persist(target) else em.merge(target)
                                changed = true
                            }
                        }
                    }
                    when {
                        existing == null -> created++
                        changed -> updated++
                        else -> unchanged++
                    }
                } catch (e: Exception) {
                    log.error(e) { "Error syncing user '${dto.username}'" }
                    errors++
                }
            }
        }
        log.info { "User sync complete: created=$created, updated=$updated, unchanged=$unchanged, errors=$errors" }
        if (created + updated > 0) {
            userGroupCache.setExpired()
        }
        return SyncResultDto(created = created, updated = updated, unchanged = unchanged, errors = errors)
    }

    /**
     * @param fullSync If true, [groups] contains all groups of the main instance, so groups missing here are
     * marked as deleted and lose their members.
     */
    fun syncGroups(groups: List<SyncGroupDto>, fullSync: Boolean = false): SyncResultDto {
        var created = 0
        var updated = 0
        var deleted = 0
        var unchanged = 0
        var errors = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            for (dto in groups) {
                try {
                    var group = em.createQuery(
                        "SELECT g FROM GroupDO g WHERE g.name = :name",
                        GroupDO::class.java
                    ).setParameter("name", dto.name).resultList.firstOrNull()

                    if (group == null) {
                        group = GroupDO()
                        group.name = dto.name
                        em.persist(group)
                        em.flush()
                        created++
                    } else {
                        val restored = group.deleted
                        if (restored) {
                            group.deleted = false
                            group.lastUpdate = Date()
                        }
                        val members = em.createNativeQuery(
                            "SELECT u.username FROM t_group_user gu JOIN t_pf_user u ON u.pk = gu.user_id WHERE gu.group_id = :gid"
                        ).setParameter("gid", group.id).resultList.map { it as String }.toSet()
                        if (!restored && members == dto.memberUsernames.toSet()) {
                            unchanged++
                            continue
                        }
                        updated++
                    }
                    val groupId = group.id!!

                    // Clear and re-populate t_group_user
                    em.createNativeQuery("DELETE FROM t_group_user WHERE group_id = :gid")
                        .setParameter("gid", groupId).executeUpdate()

                    for (username in dto.memberUsernames) {
                        em.createNativeQuery(
                            "INSERT INTO t_group_user (group_id, user_id) SELECT :gid, pk FROM t_pf_user WHERE username = :uname"
                        ).setParameter("gid", groupId).setParameter("uname", username).executeUpdate()
                    }
                } catch (e: Exception) {
                    log.error(e) { "Error syncing group '${dto.name}'" }
                    errors++
                }
            }
            if (fullSync && groups.isNotEmpty()) {
                val names = groups.map { it.name }.toSet()
                em.createQuery("SELECT g FROM GroupDO g WHERE g.deleted = false", GroupDO::class.java)
                    .resultList
                    .filter { it.name !in names }
                    .forEach { group ->
                        em.createNativeQuery("DELETE FROM t_group_user WHERE group_id = :gid")
                            .setParameter("gid", group.id).executeUpdate()
                        group.deleted = true
                        group.lastUpdate = Date()
                        log.info { "Group '${group.name}' no longer exists on the main instance, marked as deleted." }
                        deleted++
                    }
            }
        }
        log.info { "Group sync complete: created=$created, updated=$updated, deleted=$deleted, unchanged=$unchanged, errors=$errors" }
        if (created + updated + deleted > 0) {
            userGroupCache.setExpired()
        }
        return SyncResultDto(created = created, updated = updated, deleted = deleted, unchanged = unchanged, errors = errors)
    }

    /**
     * Processed in chunks of [ADDRESS_CHUNK_SIZE], each in its own transaction with one lookup query for the
     * addresses and one for the images, so a full sync of thousands of addresses doesn't hold a single
     * connection for minutes. Images are only rewritten if changed, so CardDAV clients don't reload all photos
     * after each full sync.
     * @param addresses New, modified and deleted addresses (delta) or all addresses of the main instance (full sync).
     * @param fullSync If true, addresses missing in [addresses] are marked as deleted.
     */
    fun syncAddresses(addresses: List<SyncAddressDto>, fullSync: Boolean = false): SyncResultDto {
        var created = 0
        var updated = 0
        var deleted = 0
        var errors = 0
        for (chunk in addresses.chunked(ADDRESS_CHUNK_SIZE)) {
            try {
                val result = syncAddressChunk(chunk)
                created += result.created
                updated += result.updated
                deleted += result.deleted
            } catch (e: Exception) {
                // The chunk's transaction is rolled back completely.
                log.error(e) { "Error syncing ${chunk.size} addresses (uids ${chunk.first().uid}..${chunk.last().uid})" }
                errors += chunk.size
            }
        }
        if (fullSync && addresses.isNotEmpty() && errors == 0) {
            deleted += markMissingAddressesAsDeleted(addresses.map { it.uid }.toSet())
        }
        log.info { "Address sync complete: created=$created, updated=$updated, deleted=$deleted, errors=$errors" }
        if (created + updated + deleted > 0) {
            expireAddressCaches()
        }
        return SyncResultDto(created = created, updated = updated, deleted = deleted, errors = errors)
    }

    /**
     * End of a full sync sent in batches (each batch as delta): marks all addresses as deleted, which aren't
     * part of [uids] (the not deleted addresses of the main instance).
     */
    fun retainAddresses(uids: List<String>): SyncResultDto {
        if (uids.isEmpty()) {
            // Protection: an empty address list of the main instance is most likely an error.
            log.warn { "Empty uid list received, no addresses deleted." }
            return SyncResultDto()
        }
        val deleted = markMissingAddressesAsDeleted(uids.toSet())
        log.info { "Address retain complete: ${uids.size} addresses retained, deleted=$deleted" }
        if (deleted > 0) {
            expireAddressCaches()
        }
        return SyncResultDto(deleted = deleted)
    }

    private fun markMissingAddressesAsDeleted(uids: Set<String>): Int {
        var deleted = 0
        persistenceService.runInTransaction { context ->
            context.em.createQuery("SELECT a FROM AddressDO a WHERE a.deleted = false", AddressDO::class.java)
                .resultList
                .filter { it.uid !in uids }
                .forEach {
                    it.deleted = true
                    it.lastUpdate = Date()
                    deleted++
                }
        }
        return deleted
    }

    private fun syncAddressChunk(chunk: List<SyncAddressDto>): SyncResultDto {
        var created = 0
        var updated = 0
        var deleted = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            val globalAddressbook = ensureGlobalAddressbookExists(em)
            val addressByUid = em.createQuery("SELECT a FROM AddressDO a WHERE a.uid IN :uids", AddressDO::class.java)
                .setParameter("uids", chunk.map { it.uid })
                .resultList.associateBy { it.uid }
            val imageByAddressId = if (addressByUid.isEmpty()) emptyMap() else {
                em.createQuery("SELECT i FROM AddressImageDO i WHERE i.address.id IN :ids", AddressImageDO::class.java)
                    .setParameter("ids", addressByUid.values.map { it.id })
                    .resultList.associateBy { it.address?.id }
            }
            for (dto in chunk) {
                var address = addressByUid[dto.uid]
                if (address == null) {
                    if (dto.deleted) {
                        continue // Deleted on the main instance before it was ever synced.
                    }
                    address = AddressDO()
                    address.uid = dto.uid
                    address.add(globalAddressbook)
                    created++
                } else if (dto.deleted) {
                    if (!address.deleted) {
                        deleted++
                    }
                } else {
                    updated++
                }
                address.deleted = dto.deleted
                address.firstName = dto.firstName
                address.name = dto.lastName
                address.organization = dto.organization
                address.email = dto.email
                address.privateEmail = dto.privateEmail
                address.businessPhone = dto.businessPhone
                address.mobilePhone = dto.mobilePhone
                address.privatePhone = dto.privatePhone
                if (address.id == null) {
                    em.persist(address) // Existing addresses are managed: changes are written on commit.
                }

                // Sync image
                val img = address.id?.let { imageByAddressId[it] }
                if (dto.imageData != null) {
                    val imageBytes = Base64.getDecoder().decode(dto.imageData)
                    val imageType = try { ImageType.valueOf(dto.imageType ?: "JPEG") } catch (_: Exception) { ImageType.JPEG }
                    if (img != null && img.image?.contentEquals(imageBytes) == true && img.imageType == imageType) {
                        continue // Image unchanged.
                    }
                    val image = img ?: AddressImageDO().also { it.address = address }
                    image.image = imageBytes
                    image.imageType = imageType
                    image.lastUpdate = Date()
                    if (image.id == null) em.persist(image)
                    address.imageLastUpdate = image.lastUpdate
                } else if (img != null) {
                    em.remove(img)
                    address.imageLastUpdate = null
                }
            }
        }
        return SyncResultDto(created = created, updated = updated, deleted = deleted)
    }

    /**
     * Replaces the CardDAV favorites of all users: [favorites] is the complete list of the main instance, so
     * favorites missing here are removed. Users and addresses unknown on the gateway are ignored.
     */
    fun syncFavorites(favorites: List<SyncFavoritesDto>): SyncResultDto {
        var created = 0
        var updated = 0
        var deleted = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            val userIdByName = em.createQuery("SELECT u FROM PFUserDO u", PFUserDO::class.java)
                .resultList.associate { it.username to it.id!! }
            val addressIdByUid = em.createQuery(
                "SELECT a FROM AddressDO a WHERE a.deleted = false", AddressDO::class.java
            ).resultList.associate { it.uid to it.id!! }
            // Owner id -> ids of the wanted favorite addresses. Entries are removed when found in the db.
            val wanted = mutableMapOf<Long, MutableSet<Long>>()
            for (dto in favorites) {
                val ownerId = userIdByName[dto.username] ?: continue
                wanted[ownerId] = dto.addressUids.mapNotNull { addressIdByUid[it] }.toMutableSet()
            }
            em.createQuery("SELECT pa FROM PersonalAddressDO pa", PersonalAddressDO::class.java).resultList
                .forEach { pa ->
                    val ownerId = pa.owner?.id
                    val addressId = pa.address?.id
                    val favorite = ownerId != null && addressId != null && wanted[ownerId]?.remove(addressId) == true
                    if (pa.isFavoriteCard != favorite || (favorite && pa.deleted)) {
                        pa.isFavoriteCard = favorite
                        pa.deleted = false
                        pa.setLastUpdate()
                        if (favorite) updated++ else deleted++
                    }
                }
            wanted.forEach { (ownerId, addressIds) ->
                addressIds.forEach { addressId ->
                    val pa = PersonalAddressDO()
                    pa.owner = em.getReference(PFUserDO::class.java, ownerId)
                    pa.address = em.getReference(AddressDO::class.java, addressId)
                    pa.isFavoriteCard = true
                    pa.setCreated()
                    pa.setLastUpdate()
                    em.persist(pa)
                    created++
                }
            }
        }
        log.info { "Favorites sync complete: created=$created, updated=$updated, removed=$deleted" }
        if (created + updated + deleted > 0) {
            expireAddressCaches()
        }
        return SyncResultDto(created = created, updated = updated, deleted = deleted)
    }

    /**
     * The sync writes directly via JPA, so the address caches aren't notified by the DAOs: CardDAV would serve
     * stale contacts and images.
     */
    private fun expireAddressCaches() {
        addressDAVCache?.forceReload()
        addressImageCache.setExpired()
        personalAddressCache.forceReload()
    }

    fun syncIcsEntries(entries: List<SyncIcsEntryDto>): SyncResultDto {
        var updated = 0
        for (entry in entries) {
            gatewayIcsCache.put(entry.userId, entry.queryParam, entry.icsData)
            updated++
        }
        log.info { "ICS cache sync complete: $updated entries updated" }
        return SyncResultDto(updated = updated, icsCacheSize = gatewayIcsCache.size())
    }

    private fun ensureGlobalAddressbookExists(em: jakarta.persistence.EntityManager): AddressbookDO {
        em.find(AddressbookDO::class.java, AddressbookDao.GLOBAL_ADDRESSBOOK_ID)?.let { return it }
        log.info { "Creating global addressbook (required for address sync on gateway)." }
        em.createNativeQuery(
            "INSERT INTO t_addressbook (pk, title, description, deleted, created, last_update) VALUES (:id, 'Global', 'Global addressbook', false, NOW(), NOW())"
        ).setParameter("id", AddressbookDao.GLOBAL_ADDRESSBOOK_ID).executeUpdate()
        return em.find(AddressbookDO::class.java, AddressbookDao.GLOBAL_ADDRESSBOOK_ID)!!
    }

    companion object {
        /**
         * Addresses per transaction (and per IN clause) of [syncAddresses].
         */
        private const val ADDRESS_CHUNK_SIZE = 500
    }
}
