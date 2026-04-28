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

package org.projectforge.idp.handler

import arlut.csd.crypto.SmbEncrypt
import mu.KotlinLogging
import org.projectforge.common.logging.LogDuration
import org.projectforge.business.ldap.LdapMasterLoginHandler
import org.projectforge.business.ldap.LdapService
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.idp.IdpAdminClient
import org.projectforge.idp.IdpConfig
import org.projectforge.idp.converter.IdpGroupConverter
import org.projectforge.idp.converter.IdpUserConverter
import org.projectforge.idp.model.IdpGroup
import org.projectforge.idp.model.IdpUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * LoginHandler for the **Ramp-up Phase**: ProjectForge is master and pushes all data
 * to both IdP and LDAP on every cache refresh.
 *
 * Data flow:
 *   PF database  →  IdP        (syncToIdp, driven by afterUserGroupCacheRefresh)
 *   PF database  →  LDAP       (via LdapMasterLoginHandler delegation)
 *
 * Activation:
 *   projectforge.login.handlerClass=IdpMasterLoginHandler
 */
@Service
open class IdpMasterLoginHandler : LoginHandler {

    @Autowired
    private lateinit var idpAdminClient: IdpAdminClient

    @Autowired
    private lateinit var idpConfig: IdpConfig

    @Autowired
    private lateinit var loginDefaultHandler: LoginDefaultHandler

    @Autowired
    private lateinit var ldapMasterLoginHandler: LdapMasterLoginHandler

    @Autowired
    private lateinit var ldapService: LdapService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var idpUserConverter: IdpUserConverter

    @Autowired
    private lateinit var idpGroupConverter: IdpGroupConverter

    @Autowired
    private lateinit var ldapUserDao: LdapUserDao

    @Volatile
    private var syncInProgress = false

    override fun initialize() {
        val providerName = idpAdminClient.providerName()
        if (!idpAdminClient.isConfigured()) {
            log.warn(
                "$providerName is not fully configured. " +
                "IdpMasterLoginHandler will not push data to $providerName."
            )
        } else {
            log.info(
                "IdpMasterLoginHandler initialized in MASTER mode (provider=$providerName). " +
                "PF is master — all changes will be pushed to $providerName and LDAP."
            )
        }
        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.initialize()
            } catch (ex: Exception) {
                log.warn("LDAP initialization failed (ignored): ${ex.message}")
            }
        } else {
            log.info("LDAP not configured, skipping LDAP initialization.")
        }
    }

    override fun checkLogin(username: String, password: CharArray): LoginResult {
        val result = loginDefaultHandler.checkLogin(username, password)
        if (result.loginResultStatus != LoginResultStatus.SUCCESS) {
            return result
        }
        val user = result.user ?: return result

        if (idpConfig.syncPasswords) {
            when {
                user.localUser  -> log.debug { "Password sync skipped for '${user.username}': localUser=true" }
                user.deleted    -> log.debug { "Password sync skipped for '${user.username}': deleted=true" }
                user.lastIdpPasswordSync != null ->
                    log.debug { "Password sync skipped for '${user.username}': already synced on ${user.lastIdpPasswordSync}" }
                else -> syncPasswordToIdp(user, password)
            }
        }

        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.checkLogin(username, password)
            } catch (ex: Exception) {
                log.error("LDAP master checkLogin failed (ignoring): ${ex.message}", ex)
            }
        }

        return result
    }

    override fun afterUserGroupCacheRefresh(users: Collection<PFUserDO>, groups: Collection<GroupDO>) {
        if (syncInProgress) return
        Thread {
            synchronized(this) {
                if (syncInProgress) return@synchronized
                try {
                    syncInProgress = true
                    syncToIdp(users, groups)
                    if (isLdapConfigured()) {
                        ldapMasterLoginHandler.afterUserGroupCacheRefresh(users, groups)
                    }
                } catch (ex: Exception) {
                    log.error("IdP master sync failed: ${ex.message}", ex)
                } finally {
                    syncInProgress = false
                }
            }
        }.start()
    }

    override fun getAllUsers(): List<PFUserDO> = loginDefaultHandler.getAllUsers()

    override fun getAllGroups(): List<GroupDO> = loginDefaultHandler.getAllGroups()

    override fun isAdminUser(user: PFUserDO): Boolean = loginDefaultHandler.isAdminUser(user)

    override fun checkStayLoggedIn(user: PFUserDO): Boolean = loginDefaultHandler.checkStayLoggedIn(user)

    override fun hasExternalUsermanagementSystem(): Boolean = true

    override fun isPasswordChangeSupported(user: PFUserDO): Boolean = true

    override fun isWlanPasswordChangeSupported(user: PFUserDO): Boolean = true

    override fun passwordChanged(user: PFUserDO, newPassword: CharArray) {
        if (idpConfig.syncPasswords && !user.localUser && !user.deleted) {
            syncPasswordToIdp(user, newPassword)
        }
        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.passwordChanged(user, newPassword)
            } catch (ex: Exception) {
                log.error("LDAP password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
            }
        }
    }

    override fun wlanPasswordChanged(user: PFUserDO, newPassword: CharArray) {
        val wlanAttr = idpConfig.wlanPasswordAttribute
        if (idpAdminClient.isConfigured() && !wlanAttr.isNullOrBlank()
            && !user.localUser && !user.deleted
        ) {
            syncWlanPasswordToIdp(user, newPassword, wlanAttr)
        }
        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.wlanPasswordChanged(user, newPassword)
            } catch (ex: Exception) {
                log.error("LDAP WLAN password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal sync logic: PF → IdP
    // -------------------------------------------------------------------------

    private fun syncToIdp(users: Collection<PFUserDO>, groups: Collection<GroupDO>) {
        if (!idpAdminClient.isConfigured()) {
            log.debug("IdP not configured, skipping push sync.")
            return
        }
        val providerName = idpAdminClient.providerName()
        val duration = LogDuration()
        log.info("Starting PF DB -> $providerName push sync...")

        // --- Sync users ---
        val idpUsers = idpAdminClient.getAllUsers()
        val idpUserByUsername = idpUsers.filter { it.username != null }.associateBy { it.username!! }

        val idpUserIdByUsername = mutableMapOf<String, String>()
        idpUsers.forEach { u -> if (u.username != null && u.id != null) idpUserIdByUsername[u.username!!] = u.id!! }

        var uCreated = 0; var uUpdated = 0; var uDisabled = 0; var uUnmodified = 0; var uErrors = 0

        for (pfUser in users) {
            val username = pfUser.username ?: continue
            try {
                val idpUser = idpUserByUsername[username]
                if (pfUser.deleted || pfUser.localUser) {
                    if (idpUser != null && idpUser.enabled) {
                        idpAdminClient.updateUser(idpUser.id!!, idpUser.copy(enabled = false))
                        uDisabled++
                    }
                } else if (idpUser == null) {
                    val newIdpUser = idpUserConverter.toIdpUser(pfUser)
                    val newId = idpAdminClient.createUser(newIdpUser)
                    idpUserIdByUsername[username] = newId
                    if (pfUser.idpExternalId != newId) {
                        log.debug { "User '$username': storing new idpExternalId '$newId' in PF" }
                        pfUser.idpExternalId = newId
                        userDao.update(pfUser, false)
                    }
                    uCreated++
                } else {
                    val desired = idpUserConverter.toIdpUser(pfUser)
                    if (isUserChanged(username, desired, idpUser)) {
                        idpAdminClient.updateUser(idpUser.id!!, desired.copy(id = idpUser.id))
                        uUpdated++
                    } else {
                        uUnmodified++
                    }
                    if (idpUser.id != null) {
                        idpUserIdByUsername[username] = idpUser.id!!
                        if (pfUser.idpExternalId != idpUser.id) {
                            log.debug { "User '$username': backfilling idpExternalId '${idpUser.id}' into PF" }
                            pfUser.idpExternalId = idpUser.id
                            userDao.update(pfUser, false)
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error("Error syncing user '$username' to $providerName (continuing): ${ex.message}", ex)
                uErrors++
            }
        }
        log.info(
            "$providerName user push: $uCreated created, $uUpdated updated, $uDisabled disabled, " +
            "$uUnmodified unmodified" + (if (uErrors > 0) ", *** $uErrors errors ***" else "")
        )

        // --- Migrate WLAN password hashes from LDAP to IdP (one-time migration) ---
        migrateWlanPasswordsFromLdap(users, idpUserByUsername, idpUserIdByUsername)

        // --- Sync groups ---
        val idpGroups = idpAdminClient.getAllGroups()
        val idpGroupByName = idpGroups.filter { it.name != null }.associateBy { it.name!! }
        val idpGroupIdByName = mutableMapOf<String, String>()
        idpGroups.forEach { g -> if (g.name != null && g.id != null) idpGroupIdByName[g.name!!] = g.id!! }
        // Cache memberIds from the already-fetched groups to avoid per-group API calls during membership sync.
        val idpGroupMemberIds = mutableMapOf<String, Set<String>>()
        idpGroups.forEach { g -> if (g.id != null) idpGroupMemberIds[g.id!!] = g.memberIds ?: emptySet() }

        var gCreated = 0; var gUpdated = 0; var gUnmodified = 0; var gErrors = 0
        val syncGroupAttributes = idpConfig.groupAttributes.isNotEmpty()

        for (group in groups) {
            val groupName = group.name ?: continue
            if (group.deleted || group.localGroup) continue
            try {
                val existing = idpGroupByName[groupName]
                if (existing == null) {
                    val idpGroup = idpGroupConverter.toIdpGroup(group)
                    val newId = idpAdminClient.createGroup(idpGroup)
                    idpGroupIdByName[groupName] = newId
                    idpGroupMemberIds[newId] = emptySet()
                    gCreated++
                } else {
                    if (existing.id != null) idpGroupIdByName[groupName] = existing.id!!
                    if (syncGroupAttributes && existing.id != null) {
                        val desired = idpGroupConverter.toIdpGroup(group)
                        if (isGroupChanged(groupName, desired, existing)) {
                            idpAdminClient.updateGroup(existing.id!!, desired.copy(id = existing.id))
                            gUpdated++
                        } else {
                            gUnmodified++
                        }
                    } else {
                        gUnmodified++
                    }
                }
            } catch (ex: Exception) {
                log.error("Error syncing group '$groupName' to $providerName (continuing): ${ex.message}", ex)
                gErrors++
            }
        }
        log.info(
            "$providerName group push: $gCreated created, $gUpdated updated, $gUnmodified unmodified" +
            (if (gErrors > 0) ", *** $gErrors errors ***" else "")
        )

        // --- Sync memberships ---
        syncMembershipsToIdp(groups, idpUserIdByUsername, idpGroupIdByName, idpGroupMemberIds)

        log.info("PF DB -> $providerName push sync complete in ${duration.toSeconds()}.")
    }

    private fun syncMembershipsToIdp(
        groups: Collection<GroupDO>,
        idpUserIdByUsername: Map<String, String>,
        idpGroupIdByName: Map<String, String>,
        idpGroupMemberIds: Map<String, Set<String>>,
    ) {
        val providerName = idpAdminClient.providerName()
        val usernameByIdpId = idpUserIdByUsername.entries.associate { (k, v) -> v to k }
        var added = 0; var removed = 0; var mErrors = 0

        for (group in groups) {
            if (group.deleted || group.localGroup) continue
            val groupName = group.name ?: continue
            val idpGroupId = idpGroupIdByName[groupName] ?: continue

            val desiredIdpUserIds = group.assignedUsers
                ?.filter { it.hasSystemAccess() && !it.localUser }
                ?.mapNotNull { it.username?.let { u -> idpUserIdByUsername[u] } }
                ?.toSet() ?: emptySet()

            val currentIdpMemberIds = idpGroupMemberIds[idpGroupId] ?: try {
                // Fallback for providers (e.g. Keycloak) that don't include memberIds in getAllGroups().
                idpAdminClient.getGroupMembers(idpGroupId).mapNotNull { it.id }.toSet()
            } catch (ex: Exception) {
                log.error("Error fetching members of IdP group '$groupName' (skipping membership sync): ${ex.message}", ex)
                continue
            }

            for (idpUserId in desiredIdpUserIds - currentIdpMemberIds) {
                val username = usernameByIdpId[idpUserId] ?: idpUserId
                try {
                    idpAdminClient.addUserToGroup(idpUserId, idpGroupId)
                    log.debug { "Added user '$username' (idpId=$idpUserId) to $providerName group '$groupName'" }
                    added++
                } catch (ex: Exception) {
                    log.error("Error adding user '$username' (idpId=$idpUserId) to $providerName group '$groupName': ${ex.message}", ex)
                    mErrors++
                }
            }

            for (idpUserId in currentIdpMemberIds - desiredIdpUserIds) {
                val username = usernameByIdpId[idpUserId] ?: idpUserId
                try {
                    idpAdminClient.removeUserFromGroup(idpUserId, idpGroupId)
                    log.debug { "Removed user '$username' (idpId=$idpUserId) from $providerName group '$groupName'" }
                    removed++
                } catch (ex: Exception) {
                    log.error("Error removing user '$username' (idpId=$idpUserId) from $providerName group '$groupName': ${ex.message}", ex)
                    mErrors++
                }
            }
        }
        log.info(
            "$providerName membership push: $added added, $removed removed" +
            (if (mErrors > 0) ", *** $mErrors errors ***" else "")
        )
    }

    private fun syncPasswordToIdp(user: PFUserDO, password: CharArray) {
        try {
            val idpId = user.idpExternalId?.also {
                log.debug { "Password sync for '${user.username}': using cached idpExternalId '$it'" }
            } ?: run {
                log.debug { "Password sync for '${user.username}': idpExternalId not cached, resolving via username lookup" }
                val found = idpAdminClient.findUserByUsername(user.username ?: return)?.id
                if (found == null) {
                    log.info("IdP user not found for '${user.username}', skipping password sync.")
                    return
                }
                found
            }
            idpAdminClient.resetPassword(idpId, password)
            user.idpExternalId = idpId
            user.lastIdpPasswordSync = Date()
            userDao.update(user, false)
            log.info("Password synced to IdP for user: ${user.username}")
        } catch (ex: Exception) {
            log.error("Failed to sync password to IdP for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    private fun syncWlanPasswordToIdp(user: PFUserDO, password: CharArray, attributeName: String) {
        try {
            val idpId = user.idpExternalId ?: run {
                val found = idpAdminClient.findUserByUsername(user.username ?: return)?.id
                if (found == null) {
                    log.info("IdP user not found for '${user.username}', skipping WLAN password sync.")
                    return
                }
                found
            }
            val ntHash = SmbEncrypt.NTUNICODEHash(password)
            val currentUser = idpAdminClient.getUserById(idpId) ?: run {
                log.warn("IdP user id '$idpId' not found, skipping WLAN password sync.")
                return
            }
            val updatedAttrs = (currentUser.attributes ?: emptyMap()).toMutableMap()
            updatedAttrs[attributeName] = listOf(ntHash)
            idpAdminClient.updateUser(idpId, currentUser.copy(attributes = updatedAttrs))
            log.info("WLAN password (NT hash) synced to IdP attribute '$attributeName' for user: ${user.username}")
        } catch (ex: Exception) {
            log.error("Failed to sync WLAN password to IdP for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    /**
     * One-time migration: copies existing WLAN password hashes (sambaNTPassword) from LDAP
     * to the IdP for users that have set a WLAN password but don't have the hash in IdP yet.
     */
    private fun migrateWlanPasswordsFromLdap(
        users: Collection<PFUserDO>,
        idpUserByUsername: Map<String, IdpUser>,
        idpUserIdByUsername: Map<String, String>,
    ) {
        val wlanAttr = idpConfig.wlanPasswordAttribute
        if (wlanAttr.isNullOrBlank() || !isLdapConfigured()) return

        val candidates = users.filter { pfUser ->
            pfUser.lastWlanPasswordChange != null
                && !pfUser.deleted && !pfUser.localUser
                && pfUser.username != null
                && idpUserIdByUsername.containsKey(pfUser.username)
                && idpUserByUsername[pfUser.username]
                    ?.attributes?.get(wlanAttr)?.firstOrNull().isNullOrBlank()
        }
        if (candidates.isEmpty()) return

        log.info("Migrating WLAN password hashes from LDAP to IdP for ${candidates.size} users...")

        val userBase = ldapService.ldapConfig?.userBase ?: return
        val ldapUsers = try {
            ldapUserDao.findAll(userBase)
        } catch (ex: Exception) {
            log.error("Failed to read LDAP users for WLAN hash migration: ${ex.message}", ex)
            return
        }
        val ldapUserByUid = ldapUsers.associateBy { it.uid }

        var migrated = 0; var skipped = 0; var errors = 0
        for (pfUser in candidates) {
            val username = pfUser.username!!
            val ntHash = ldapUserByUid[username]?.sambaNTPassword
            if (ntHash.isNullOrBlank()) {
                skipped++
                continue
            }
            val idpId = idpUserIdByUsername[username] ?: continue
            try {
                val currentUser = idpAdminClient.getUserById(idpId) ?: continue
                val updatedAttrs = (currentUser.attributes ?: emptyMap()).toMutableMap()
                updatedAttrs[wlanAttr] = listOf(ntHash)
                idpAdminClient.updateUser(idpId, currentUser.copy(attributes = updatedAttrs))
                migrated++
                log.debug { "Migrated WLAN hash from LDAP to IdP for user '$username'" }
            } catch (ex: Exception) {
                log.error("Error migrating WLAN hash for user '$username': ${ex.message}", ex)
                errors++
            }
        }
        log.info(
            "WLAN hash migration: $migrated migrated, $skipped skipped (no hash in LDAP)" +
            (if (errors > 0) ", *** $errors errors ***" else "")
        )
    }

    private fun isLdapConfigured(): Boolean = !ldapService.ldapConfig?.server.isNullOrBlank()

    private fun isUserChanged(username: String, desired: IdpUser, current: IdpUser): Boolean {
        // Compare combined display name rather than split first/last individually.
        // Authentik stores a single "name" field; splitName() can't reliably reconstruct
        // the original first/last boundary (e.g. "Jan Henrik Peters" → "Jan" + "Henrik Peters"
        // vs. PF's "Jan Henrik" + "Peters"). Comparing the combined string avoids
        // false positives that cause unnecessary updates on every sync.
        // Fall back to username when name is blank, matching the createUser/updateUser payload logic.
        val desiredName = "${desired.firstName ?: ""} ${desired.lastName ?: ""}".trim().ifBlank { desired.username ?: "" }
        val currentName = "${current.firstName ?: ""} ${current.lastName ?: ""}".trim().ifBlank { current.username ?: "" }
        if (desiredName != currentName) {
            log.debug { "User '$username': name '$currentName' → '$desiredName'" }
            return true
        }
        val desiredEmail = desired.email?.takeIf { it.isNotBlank() }
        val currentEmail = current.email?.takeIf { it.isNotBlank() }
        if (desiredEmail != currentEmail) {
            log.debug { "User '$username': email '${current.email}' → '${desired.email}'" }
            return true
        }
        if (desired.enabled != current.enabled) {
            log.debug { "User '$username': enabled ${current.enabled} → ${desired.enabled}" }
            return true
        }
        return isAttributesChanged(username, desired.attributes, current.attributes)
    }

    private fun isAttributesChanged(
        username: String,
        desired: Map<String, List<String>>?,
        current: Map<String, List<String>>?
    ): Boolean {
        val managedKeys = idpConfig.userAttributes.values.toSet()
        for (key in managedKeys) {
            if (desired?.get(key) != current?.get(key)) {
                log.debug { "User '$username': attribute '$key' ${current?.get(key)} → ${desired?.get(key)}" }
                return true
            }
        }
        return false
    }

    private fun isGroupChanged(groupName: String, desired: IdpGroup, current: IdpGroup): Boolean {
        val managedKeys = idpConfig.groupAttributes.values.toSet()
        for (key in managedKeys) {
            if (desired.attributes?.get(key) != current.attributes?.get(key)) {
                log.debug { "Group '$groupName': attribute '$key' ${current.attributes?.get(key)} → ${desired.attributes?.get(key)}" }
                return true
            }
        }
        return false
    }
}
