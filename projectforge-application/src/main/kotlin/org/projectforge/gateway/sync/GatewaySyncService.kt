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
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.gateway.sync.dto.SyncAddressDto
import org.projectforge.gateway.sync.dto.SyncGroupDto
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewaySyncService(
    private val persistenceService: PfPersistenceService,
    private val userGroupCache: UserGroupCache,
    private val gatewayIcsCache: GatewayIcsCache,
) {

    fun syncUsers(users: List<SyncUserDto>): SyncResult {
        var created = 0
        var updated = 0
        var errors = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            for (dto in users) {
                try {
                    val existing = em.createQuery(
                        "SELECT u FROM PFUserDO u WHERE u.username = :name",
                        PFUserDO::class.java
                    ).setParameter("name", dto.username).resultList.firstOrNull()

                    val user: PFUserDO
                    if (existing == null) {
                        user = PFUserDO()
                        user.username = dto.username
                        user.idpExternalId = dto.idpExternalId
                        user.deactivated = !dto.active
                        em.persist(user)
                        em.flush()
                        created++
                    } else {
                        existing.idpExternalId = dto.idpExternalId
                        existing.deactivated = !dto.active
                        user = em.merge(existing)
                        updated++
                    }

                    // Sync tokens if provided (already encrypted, store 1:1)
                    user.id?.let { userId ->
                        if (dto.davToken != null || dto.calendarRestToken != null) {
                            val auth = em.createQuery(
                                "SELECT a FROM UserAuthenticationsDO a WHERE a.user.id = :userId",
                                UserAuthenticationsDO::class.java
                            ).setParameter("userId", userId).resultList.firstOrNull()
                                ?: UserAuthenticationsDO().also {
                                    it.user = user
                                    it.created = Date()
                                }

                            val now = Date()
                            dto.davToken?.let { auth.davToken = it; auth.davTokenCreationDate = now }
                            dto.calendarRestToken?.let { auth.calendarExportToken = it; auth.calendarExportTokenCreationDate = now }
                            auth.lastUpdate = now

                            if (auth.id == null) em.persist(auth) else em.merge(auth)
                        }
                    }
                } catch (e: Exception) {
                    log.error(e) { "Error syncing user '${dto.username}'" }
                    errors++
                }
            }
        }
        log.info { "User sync complete: created=$created, updated=$updated, errors=$errors" }
        userGroupCache.setExpired()
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncGroups(groups: List<SyncGroupDto>): SyncResult {
        var created = 0
        var updated = 0
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
        }
        log.info { "Group sync complete: created=$created, updated=$updated, errors=$errors" }
        userGroupCache.setExpired()
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncAddresses(addresses: List<SyncAddressDto>): SyncResult {
        var created = 0
        var updated = 0
        var errors = 0
        persistenceService.runInTransaction { context ->
            val em = context.em
            ensureGlobalAddressbookExists(em)
            for (dto in addresses) {
                try {
                    var address = em.createQuery(
                        "SELECT a FROM AddressDO a WHERE a.uid = :uid",
                        AddressDO::class.java
                    ).setParameter("uid", dto.uid).resultList.firstOrNull()

                    if (address == null) {
                        address = AddressDO()
                        address.uid = dto.uid
                        created++
                    } else {
                        updated++
                    }
                    address.firstName = dto.firstName
                    address.name = dto.lastName
                    address.organization = dto.organization
                    address.email = dto.email
                    address.privateEmail = dto.privateEmail
                    address.businessPhone = dto.businessPhone
                    address.mobilePhone = dto.mobilePhone
                    address.privatePhone = dto.privatePhone
                    em.merge(address)
                } catch (e: Exception) {
                    log.error(e) { "Error syncing address uid='${dto.uid}'" }
                    errors++
                }
            }
        }
        log.info { "Address sync complete: created=$created, updated=$updated, errors=$errors" }
        return SyncResult(created = created, updated = updated, errors = errors)
    }

    fun syncIcsEntries(entries: List<SyncIcsEntryDto>): SyncResult {
        var updated = 0
        for (entry in entries) {
            gatewayIcsCache.put(entry.userId, entry.queryParam, entry.icsData)
            updated++
        }
        log.info { "ICS cache sync complete: $updated entries updated" }
        return SyncResult(created = 0, updated = updated, errors = 0)
    }

    private fun ensureGlobalAddressbookExists(em: jakarta.persistence.EntityManager) {
        val exists = em.find(AddressbookDO::class.java, AddressbookDao.GLOBAL_ADDRESSBOOK_ID)
        if (exists == null) {
            log.info { "Creating global addressbook (required for address sync on gateway)." }
            em.createNativeQuery(
                "INSERT INTO t_addressbook (pk, title, description, deleted, created, last_update) VALUES (:id, 'Global', 'Global addressbook', false, NOW(), NOW())"
            ).setParameter("id", AddressbookDao.GLOBAL_ADDRESSBOOK_ID).executeUpdate()
        }
    }

    data class SyncResult(
        val created: Int,
        val updated: Int,
        val errors: Int,
    )
}
