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
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.ldap.LdapMasterLoginHandler
import org.projectforge.business.login.LoginDefaultHandler
import org.projectforge.business.login.LoginHandler
import org.projectforge.business.login.LoginResult
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.keycloak.client.KeycloakAdminClient
import org.projectforge.keycloak.config.KeycloakConfig
import org.projectforge.keycloak.converter.KeycloakGroupConverter
import org.projectforge.keycloak.converter.KeycloakUserConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * LoginHandler that reads users and groups from Keycloak (via Admin REST API) and acts as
 * LDAP master by delegating writes to [LdapMasterLoginHandler].
 *
 * Data flow:
 *  - Keycloak  →  PF database  (syncFromKeycloak, called from afterUserGroupCacheRefresh)
 *  - PF database  →  LDAP      (via LdapMasterLoginHandler.afterUserGroupCacheRefresh)
 *
 * Activation: Set projectforge.login.handlerClass=KeycloakLoginHandler
 *
 * Password sync:
 *  On first successful login the user's password is pushed to Keycloak if not yet synced
 *  (tracked via PFUserDO.lastKeycloakPasswordSync).
 */
@Service
open class KeycloakLoginHandler : LoginHandler {

    @Autowired
    private lateinit var keycloakAdminClient: KeycloakAdminClient

    @Autowired
    private lateinit var keycloakConfig: KeycloakConfig

    @Autowired
    private lateinit var loginDefaultHandler: LoginDefaultHandler

    @Autowired
    private lateinit var ldapMasterLoginHandler: LdapMasterLoginHandler

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

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
                "KeycloakLoginHandler will not be able to sync with Keycloak."
            )
        } else {
            log.info("KeycloakLoginHandler initialized (serverUrl=${keycloakConfig.serverUrl}, realm=${keycloakConfig.realm})")
        }
        // Delegate LDAP OU creation to LdapMasterLoginHandler
        try {
            ldapMasterLoginHandler.initialize()
        } catch (ex: Exception) {
            log.warn("LDAP initialization failed (ignored): ${ex.message}")
        }
    }

    /**
     * Authenticates via the PF database (source of truth for login).
     * On success, syncs the password to Keycloak if not yet done, then delegates LDAP sync.
     * Keycloak and LDAP errors never cause the login to fail.
     */
    override fun checkLogin(username: String, password: CharArray): LoginResult {
        val result = loginDefaultHandler.checkLogin(username, password)
        if (result.loginResultStatus != LoginResultStatus.SUCCESS) {
            return result
        }
        val user = result.user ?: return result

        // Sync password to Keycloak if not yet done
        if (user.lastKeycloakPasswordSync == null && !user.localUser && !user.deleted) {
            syncPasswordToKeycloak(user, password)
        }

        // Delegate LDAP master behavior (updates/creates user in LDAP if credentials differ)
        try {
            ldapMasterLoginHandler.checkLogin(username, password)
        } catch (ex: Exception) {
            log.error("LDAP master checkLogin failed (ignoring): ${ex.message}", ex)
        }

        return result
    }

    /**
     * Syncs all users, groups and memberships from Keycloak into the PF database,
     * then triggers the LDAP master sync.
     * Runs in a background thread (like LdapMasterLoginHandler).
     */
    override fun afterUserGroupCacheRefresh(users: Collection<PFUserDO>, groups: Collection<GroupDO>) {
        if (syncInProgress) return
        Thread {
            synchronized(this) {
                if (syncInProgress) return@synchronized
                try {
                    syncInProgress = true
                    syncFromKeycloak()
                    // After PF DB is up-to-date, push everything to LDAP
                    val freshUsers = userService.selectAll(false)
                    val freshGroups = groupService.getAllGroups()
                    ldapMasterLoginHandler.afterUserGroupCacheRefresh(freshUsers, freshGroups)
                } catch (ex: Exception) {
                    log.error("Keycloak sync failed: ${ex.message}", ex)
                } finally {
                    syncInProgress = false
                }
            }
        }.start()
    }

    /** Returns all users from PF database (already synced from Keycloak). */
    override fun getAllUsers(): List<PFUserDO> = loginDefaultHandler.getAllUsers()

    /** Returns all groups from PF database (already synced from Keycloak). */
    override fun getAllGroups(): List<GroupDO> = loginDefaultHandler.getAllGroups()

    override fun isAdminUser(user: PFUserDO): Boolean = loginDefaultHandler.isAdminUser(user)

    override fun checkStayLoggedIn(user: PFUserDO): Boolean = loginDefaultHandler.checkStayLoggedIn(user)

    override fun hasExternalUsermanagementSystem(): Boolean = true

    /** Password change is supported for now; set to false later when SSO is exclusive. */
    override fun isPasswordChangeSupported(user: PFUserDO): Boolean = true

    override fun isWlanPasswordChangeSupported(user: PFUserDO): Boolean = true

    /**
     * Pushes the new password to both Keycloak and LDAP.
     * Errors are logged but do not interrupt the operation.
     */
    override fun passwordChanged(user: PFUserDO, newPassword: CharArray) {
        if (!user.localUser && !user.deleted) {
            syncPasswordToKeycloak(user, newPassword)
        }
        try {
            ldapMasterLoginHandler.passwordChanged(user, newPassword)
        } catch (ex: Exception) {
            log.error("LDAP password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    override fun wlanPasswordChanged(user: PFUserDO, newPassword: CharArray) {
        // WLAN password is an LDAP-specific concept; delegate to LDAP master only
        try {
            ldapMasterLoginHandler.wlanPasswordChanged(user, newPassword)
        } catch (ex: Exception) {
            log.error("LDAP WLAN password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    // -------------------------------------------------------------------------
    // Internal sync logic
    // -------------------------------------------------------------------------

    /**
     * Syncs users, groups, and memberships from Keycloak into PF's database.
     * - Users missing in Keycloak are deactivated in PF (not deleted).
     * - localUser / localGroup entries are never touched.
     */
    private fun syncFromKeycloak() {
        if (!keycloakConfig.isConfigured()) {
            log.debug("Keycloak not configured, skipping sync.")
            return
        }
        log.info("Starting Keycloak -> PF DB sync...")

        // --- Sync users ---
        val kcUsers = keycloakAdminClient.getAllUsers()
        val dbUsers = userService.selectAll(false)

        var created = 0; var updated = 0; var deactivated = 0; var errors = 0

        // Build a map of Keycloak user ID -> PF user (for membership sync below)
        val kcIdToPfUser = mutableMapOf<String, PFUserDO>()

        kcUsers.forEach { kcUser ->
            try {
                val existing = dbUsers.find { it.username == kcUser.username }
                if (existing == null) {
                    val newUser = keycloakUserConverter.toPFUser(kcUser)
                    newUser.id = null
                    userDao.insert(newUser, false)
                    // Fetch persisted user to get the DB-assigned ID
                    val persisted = userService.getInternalByUsername(kcUser.username!!)
                    if (persisted != null) {
                        kcUser.id?.let { kcIdToPfUser[it] = persisted }
                    }
                    created++
                } else {
                    val modified = keycloakUserConverter.copyFields(kcUser, existing)
                    if (modified) {
                        userDao.update(existing, false)
                        updated++
                    }
                    kcUser.id?.let { kcIdToPfUser[it] = existing }
                }
            } catch (ex: Exception) {
                log.error("Error syncing user '${kcUser.username}' from Keycloak (continuing): ${ex.message}", ex)
                errors++
            }
        }

        // Deactivate PF users that are no longer in Keycloak
        val kcUsernames = kcUsers.mapNotNull { it.username }.toSet()
        dbUsers.filter { !it.localUser && !it.deleted && !kcUsernames.contains(it.username) }.forEach { user ->
            try {
                if (!user.deactivated) {
                    user.deactivated = true
                    userDao.update(user, false)
                    deactivated++
                }
            } catch (ex: Exception) {
                log.error("Error deactivating user '${user.username}' (continuing): ${ex.message}", ex)
                errors++
            }
        }
        log.info("Keycloak user sync: $created created, $updated updated, $deactivated deactivated" +
                (if (errors > 0) ", *** $errors errors ***" else ""))

        // --- Sync groups ---
        val kcGroups = keycloakAdminClient.getAllGroups()
        val dbGroups = groupService.getAllGroups().toMutableList()

        var gCreated = 0; var gUpdated = 0; var gErrors = 0
        val kcIdToPfGroup = mutableMapOf<String, GroupDO>()

        kcGroups.forEach { kcGroup ->
            try {
                val existing = dbGroups.find { it.name == kcGroup.name }
                if (existing == null) {
                    val newGroup = keycloakGroupConverter.toGroupDO(kcGroup)
                    newGroup.id = null
                    groupDao.insert(newGroup, false)
                    val persisted = groupDao.getByName(kcGroup.name)
                    if (persisted != null) {
                        kcGroup.id?.let { kcIdToPfGroup[it] = persisted }
                        dbGroups.add(persisted)
                    }
                    gCreated++
                } else {
                    val modified = keycloakGroupConverter.copyFields(kcGroup, existing)
                    if (modified) {
                        groupDao.update(existing, false)
                        gUpdated++
                    }
                    kcGroup.id?.let { kcIdToPfGroup[it] = existing }
                }
            } catch (ex: Exception) {
                log.error("Error syncing group '${kcGroup.name}' from Keycloak (continuing): ${ex.message}", ex)
                gErrors++
            }
        }
        log.info("Keycloak group sync: $gCreated created, $gUpdated updated" +
                (if (gErrors > 0) ", *** $gErrors errors ***" else ""))

        // --- Sync memberships ---
        syncMemberships(kcUsers, kcIdToPfUser, kcIdToPfGroup)

        // Force UserGroupCache to refresh with the new data
        userGroupCache.setExpired()
        log.info("Keycloak -> PF DB sync complete.")
    }

    private fun syncMemberships(
        kcUsers: List<org.projectforge.keycloak.model.KeycloakUser>,
        kcIdToPfUser: Map<String, PFUserDO>,
        kcIdToPfGroup: Map<String, GroupDO>
    ) {
        if (kcIdToPfGroup.isEmpty()) return
        // Build desired group → members map from Keycloak
        val groupToMembers = mutableMapOf<GroupDO, MutableSet<PFUserDO>>()
        kcIdToPfGroup.values.forEach { groupToMembers[it] = mutableSetOf() }

        kcUsers.forEach { kcUser ->
            val kcUserId = kcUser.id ?: return@forEach
            val pfUser = kcIdToPfUser[kcUserId] ?: return@forEach
            try {
                val userGroups = keycloakAdminClient.getUserGroups(kcUserId)
                userGroups.forEach { kcGroup ->
                    val pfGroup = kcIdToPfGroup[kcGroup.id] ?: return@forEach
                    groupToMembers.getOrPut(pfGroup) { mutableSetOf() }.add(pfUser)
                }
            } catch (ex: Exception) {
                log.error("Error fetching groups for Keycloak user '${kcUser.username}' (continuing): ${ex.message}", ex)
            }
        }

        // Apply membership changes via GroupDao
        var mErrors = 0
        groupToMembers.forEach { (pfGroup, members) ->
            try {
                groupDao.setAssignedUsers(pfGroup, members)
            } catch (ex: Exception) {
                log.error("Error updating members for group '${pfGroup.name}' (continuing): ${ex.message}", ex)
                mErrors++
            }
        }
        if (mErrors > 0) {
            log.error("*** $mErrors errors during membership sync ***")
        }
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
}
