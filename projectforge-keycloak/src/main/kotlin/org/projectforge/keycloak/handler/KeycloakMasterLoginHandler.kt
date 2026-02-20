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

package org.projectforge.keycloak.handler

import mu.KotlinLogging
import org.projectforge.business.ldap.LdapConfig
import org.projectforge.business.ldap.LdapMasterLoginHandler
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.keycloak.client.KeycloakAdminClient
import org.projectforge.keycloak.config.KeycloakConfig
import org.projectforge.keycloak.converter.KeycloakGroupConverter
import org.projectforge.keycloak.converter.KeycloakUserConverter
import org.projectforge.keycloak.model.KeycloakUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * LoginHandler for the **Ramp-up Phase**: ProjectForge is master and pushes all data
 * to both Keycloak and LDAP on every cache refresh.
 *
 * Data flow:
 *   PF database  →  Keycloak   (syncToKeycloak, driven by afterUserGroupCacheRefresh)
 *   PF database  →  LDAP       (via LdapMasterLoginHandler delegation)
 *
 * This mode bridges the gap between PF-as-master and the final state where
 * Keycloak becomes master ([KeycloakLoginHandler]).
 * Switch to KeycloakMasterLoginHandler while populating Keycloak, then flip to
 * KeycloakLoginHandler once Keycloak is the authoritative source.
 *
 * Activation:
 *   projectforge.login.handlerClass=KeycloakMasterLoginHandler
 */
@Service
open class KeycloakMasterLoginHandler : LoginHandler {

    @Autowired
    private lateinit var keycloakAdminClient: KeycloakAdminClient

    @Autowired
    private lateinit var keycloakConfig: KeycloakConfig

    @Autowired
    private lateinit var loginDefaultHandler: LoginDefaultHandler

    @Autowired
    private lateinit var ldapMasterLoginHandler: LdapMasterLoginHandler

    @Autowired
    private lateinit var ldapConfig: LdapConfig

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var keycloakUserConverter: KeycloakUserConverter

    @Autowired
    private lateinit var keycloakGroupConverter: KeycloakGroupConverter

    @Volatile
    private var syncInProgress = false

    override fun initialize() {
        if (!keycloakConfig.isConfigured()) {
            log.warn(
                "Keycloak is not fully configured (serverUrl/realm/clientId/clientSecret missing). " +
                "KeycloakMasterLoginHandler will not push data to Keycloak."
            )
        } else {
            log.info(
                "KeycloakMasterLoginHandler initialized in MASTER mode " +
                "(serverUrl=${keycloakConfig.serverUrl}, realm=${keycloakConfig.realm}). " +
                "PF is master — all changes will be pushed to Keycloak and LDAP."
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

    /**
     * Authenticates via PF database.
     * On success syncs the password to Keycloak if not yet done, then delegates LDAP sync.
     * Keycloak/LDAP errors never cause the login to fail.
     */
    override fun checkLogin(username: String, password: CharArray): LoginResult {
        val result = loginDefaultHandler.checkLogin(username, password)
        if (result.loginResultStatus != LoginResultStatus.SUCCESS) {
            return result
        }
        val user = result.user ?: return result

        // Phase 2: sync password to Keycloak on first login if syncPasswords is enabled
        if (keycloakConfig.syncPasswords && user.lastKeycloakPasswordSync == null
            && !user.localUser && !user.deleted
        ) {
            syncPasswordToKeycloak(user, password)
        }

        // LDAP master behavior: create/update user in LDAP if credentials differ
        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.checkLogin(username, password)
            } catch (ex: Exception) {
                log.error("LDAP master checkLogin failed (ignoring): ${ex.message}", ex)
            }
        }

        return result
    }

    /**
     * Pushes all PF users, groups and memberships to Keycloak, then triggers the
     * existing LDAP master sync. Runs in a background thread.
     */
    override fun afterUserGroupCacheRefresh(users: Collection<PFUserDO>, groups: Collection<GroupDO>) {
        if (syncInProgress) return
        Thread {
            synchronized(this) {
                if (syncInProgress) return@synchronized
                try {
                    syncInProgress = true
                    syncToKeycloak(users, groups)
                    // After Keycloak is updated, also push to LDAP (if configured)
                    if (isLdapConfigured()) {
                        ldapMasterLoginHandler.afterUserGroupCacheRefresh(users, groups)
                    }
                } catch (ex: Exception) {
                    log.error("Keycloak master sync failed: ${ex.message}", ex)
                } finally {
                    syncInProgress = false
                }
            }
        }.start()
    }

    /** PF database is source of truth — return users from DB. */
    override fun getAllUsers(): List<PFUserDO> = loginDefaultHandler.getAllUsers()

    /** PF database is source of truth — return groups from DB. */
    override fun getAllGroups(): List<GroupDO> = loginDefaultHandler.getAllGroups()

    override fun isAdminUser(user: PFUserDO): Boolean = loginDefaultHandler.isAdminUser(user)

    override fun checkStayLoggedIn(user: PFUserDO): Boolean = loginDefaultHandler.checkStayLoggedIn(user)

    override fun hasExternalUsermanagementSystem(): Boolean = true

    /** Password change is supported (PF is master). */
    override fun isPasswordChangeSupported(user: PFUserDO): Boolean = true

    override fun isWlanPasswordChangeSupported(user: PFUserDO): Boolean = true

    /**
     * Pushes the new password to Keycloak and LDAP.
     */
    override fun passwordChanged(user: PFUserDO, newPassword: CharArray) {
        if (keycloakConfig.syncPasswords && !user.localUser && !user.deleted) {
            syncPasswordToKeycloak(user, newPassword)
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
        if (isLdapConfigured()) {
            try {
                ldapMasterLoginHandler.wlanPasswordChanged(user, newPassword)
            } catch (ex: Exception) {
                log.error("LDAP WLAN password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal sync logic: PF → Keycloak
    // -------------------------------------------------------------------------

    /**
     * Pushes all PF users, groups and memberships to Keycloak.
     * Follows the same pattern as LdapMasterLoginHandler.updateLdap():
     * - Creates missing entries
     * - Updates changed entries
     * - Disables (not deletes) removed or deleted users
     * - Respects localUser / localGroup flags
     */
    private fun syncToKeycloak(users: Collection<PFUserDO>, groups: Collection<GroupDO>) {
        if (!keycloakConfig.isConfigured()) {
            log.debug("Keycloak not configured, skipping push sync.")
            return
        }
        log.info("Starting PF DB -> Keycloak push sync...")

        // --- Sync users ---
        val kcUsers = keycloakAdminClient.getAllUsers()
        val kcUserByUsername = kcUsers.filter { it.username != null }.associateBy { it.username!! }

        // Build KC-ID map for membership sync later
        val kcUserIdByUsername = mutableMapOf<String, String>()
        kcUsers.forEach { u -> if (u.username != null && u.id != null) kcUserIdByUsername[u.username!!] = u.id!! }

        var uCreated = 0; var uUpdated = 0; var uDisabled = 0; var uUnmodified = 0; var uErrors = 0

        for (pfUser in users) {
            val username = pfUser.username ?: continue
            try {
                val kcUser = kcUserByUsername[username]
                if (pfUser.deleted || pfUser.localUser) {
                    // Deleted / local users: disable in KC if they exist there
                    if (kcUser != null && kcUser.enabled) {
                        keycloakAdminClient.updateUser(kcUser.id!!, kcUser.copy(enabled = false))
                        uDisabled++
                    }
                } else if (kcUser == null) {
                    // New user → create in Keycloak
                    val newKcUser = keycloakUserConverter.toKeycloakUser(pfUser)
                    val newId = keycloakAdminClient.createUser(newKcUser)
                    kcUserIdByUsername[username] = newId
                    uCreated++
                } else {
                    // Existing user → update if fields changed
                    val desired = keycloakUserConverter.toKeycloakUser(pfUser)
                    if (isUserChanged(desired, kcUser)) {
                        keycloakAdminClient.updateUser(kcUser.id!!, desired.copy(id = kcUser.id))
                        uUpdated++
                    } else {
                        uUnmodified++
                    }
                    // Ensure ID is in map (may have been fetched before create)
                    if (kcUser.id != null) kcUserIdByUsername[username] = kcUser.id!!
                }
            } catch (ex: Exception) {
                log.error("Error syncing user '$username' to Keycloak (continuing): ${ex.message}", ex)
                uErrors++
            }
        }
        log.info(
            "Keycloak user push: $uCreated created, $uUpdated updated, $uDisabled disabled, " +
            "$uUnmodified unmodified" + (if (uErrors > 0) ", *** $uErrors errors ***" else "")
        )

        // --- Sync groups ---
        val kcGroups = keycloakAdminClient.getAllGroups()
        val kcGroupByName = kcGroups.filter { it.name != null }.associateBy { it.name!! }
        val kcGroupIdByName = mutableMapOf<String, String>()
        kcGroups.forEach { g -> if (g.name != null && g.id != null) kcGroupIdByName[g.name!!] = g.id!! }

        var gCreated = 0; var gErrors = 0

        for (group in groups) {
            val groupName = group.name ?: continue
            if (group.deleted || group.localGroup) continue
            try {
                if (!kcGroupByName.containsKey(groupName)) {
                    val kcGroup = keycloakGroupConverter.toKeycloakGroup(group)
                    val newId = keycloakAdminClient.createGroup(kcGroup)
                    kcGroupIdByName[groupName] = newId
                    gCreated++
                } else {
                    val existing = kcGroupByName[groupName]!!
                    if (existing.id != null) kcGroupIdByName[groupName] = existing.id!!
                }
            } catch (ex: Exception) {
                log.error("Error syncing group '$groupName' to Keycloak (continuing): ${ex.message}", ex)
                gErrors++
            }
        }
        log.info(
            "Keycloak group push: $gCreated created" + (if (gErrors > 0) ", *** $gErrors errors ***" else "")
        )

        // --- Sync memberships ---
        syncMembershipsToKeycloak(groups, kcUserIdByUsername, kcGroupIdByName)

        log.info("PF DB -> Keycloak push sync complete.")
    }

    private fun syncMembershipsToKeycloak(
        groups: Collection<GroupDO>,
        kcUserIdByUsername: Map<String, String>,
        kcGroupIdByName: Map<String, String>
    ) {
        var added = 0; var removed = 0; var mErrors = 0

        for (group in groups) {
            if (group.deleted || group.localGroup) continue
            val groupName = group.name ?: continue
            val kcGroupId = kcGroupIdByName[groupName] ?: continue

            // Desired members: only active PF users that have system access
            val desiredUsernames = group.assignedUsers
                ?.filter { it.hasSystemAccess() && !it.localUser }
                ?.mapNotNull { it.username }
                ?.toSet() ?: emptySet()

            // Current members in Keycloak
            val currentKcMembers: Set<String> = try {
                keycloakAdminClient.getGroupMembers(kcGroupId)
                    .mapNotNull { it.username }
                    .toSet()
            } catch (ex: Exception) {
                log.error("Error fetching members of KC group '$groupName' (skipping membership sync): ${ex.message}", ex)
                continue
            }

            // Add missing members
            for (username in desiredUsernames - currentKcMembers) {
                val kcUserId = kcUserIdByUsername[username] ?: continue
                try {
                    keycloakAdminClient.addUserToGroup(kcUserId, kcGroupId)
                    added++
                } catch (ex: Exception) {
                    log.error("Error adding user '$username' to KC group '$groupName': ${ex.message}", ex)
                    mErrors++
                }
            }

            // Remove surplus members
            for (username in currentKcMembers - desiredUsernames) {
                val kcUserId = kcUserIdByUsername[username] ?: continue
                try {
                    keycloakAdminClient.removeUserFromGroup(kcUserId, kcGroupId)
                    removed++
                } catch (ex: Exception) {
                    log.error("Error removing user '$username' from KC group '$groupName': ${ex.message}", ex)
                    mErrors++
                }
            }
        }
        log.info(
            "Keycloak membership push: $added added, $removed removed" +
            (if (mErrors > 0) ", *** $mErrors errors ***" else "")
        )
    }

    private fun syncPasswordToKeycloak(user: PFUserDO, password: CharArray) {
        try {
            val kcUser = keycloakAdminClient.findUserByUsername(user.username ?: return)
            val kcId = kcUser?.id ?: run {
                log.debug("Keycloak user not found for '${user.username}', skipping password sync.")
                return
            }
            keycloakAdminClient.resetPassword(kcId, password)
            user.lastKeycloakPasswordSync = Date()
            userDao.update(user, false)
            log.info("Password synced to Keycloak for user: ${user.username}")
        } catch (ex: Exception) {
            log.error("Failed to sync password to Keycloak for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    /** Returns true if LDAP is configured (projectforge.ldap.server is set). */
    private fun isLdapConfigured(): Boolean = !ldapConfig.server.isNullOrBlank()

    /**
     * Checks whether the desired KC user representation differs from the current KC user.
     * Only compares fields that PF manages (firstName, lastName, email, enabled).
     */
    private fun isUserChanged(desired: KeycloakUser, current: KeycloakUser): Boolean =
        desired.firstName != current.firstName ||
        desired.lastName != current.lastName ||
        desired.email != current.email ||
        desired.enabled != current.enabled
}
