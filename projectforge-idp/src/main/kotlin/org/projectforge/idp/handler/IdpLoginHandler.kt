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
import org.projectforge.idp.IdpAdminClient
import org.projectforge.idp.IdpConfig
import org.projectforge.idp.converter.IdpGroupConverter
import org.projectforge.idp.converter.IdpUserConverter
import org.projectforge.idp.model.IdpUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * LoginHandler that reads users and groups from an external IdP (via Admin REST API) and acts as
 * LDAP master by delegating writes to [LdapMasterLoginHandler].
 *
 * Data flow:
 *  - IdP  →  PF database  (syncFromIdp, called from afterUserGroupCacheRefresh)
 *  - PF database  →  LDAP      (via LdapMasterLoginHandler.afterUserGroupCacheRefresh)
 *
 * Activation: Set projectforge.login.handlerClass=IdpLoginHandler
 *
 * Password sync:
 *  On first successful login the user's password is pushed to the IdP if not yet synced
 *  (tracked via PFUserDO.lastIdpPasswordSync).
 */
@Service
open class IdpLoginHandler : LoginHandler {

    @Autowired
    private lateinit var idpAdminClient: IdpAdminClient

    @Autowired
    private lateinit var idpConfig: IdpConfig

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
    private lateinit var idpUserConverter: IdpUserConverter

    @Autowired
    private lateinit var idpGroupConverter: IdpGroupConverter

    @Volatile
    private var syncInProgress = false

    override fun initialize() {
        val providerName = idpAdminClient.providerName()
        if (!idpAdminClient.isConfigured()) {
            log.warn(
                "$providerName is not fully configured. " +
                "IdpLoginHandler will not be able to sync with $providerName."
            )
        } else {
            log.info("IdpLoginHandler initialized (provider=$providerName)")
        }
        try {
            ldapMasterLoginHandler.initialize()
        } catch (ex: Exception) {
            log.warn("LDAP initialization failed (ignored): ${ex.message}")
        }
    }

    override fun checkLogin(username: String, password: CharArray): LoginResult {
        val result = loginDefaultHandler.checkLogin(username, password)
        if (result.loginResultStatus != LoginResultStatus.SUCCESS) {
            return result
        }
        val user = result.user ?: return result

        if (user.lastIdpPasswordSync == null && !user.localUser && !user.deleted) {
            syncPasswordToIdp(user, password)
        }

        try {
            ldapMasterLoginHandler.checkLogin(username, password)
        } catch (ex: Exception) {
            log.error("LDAP master checkLogin failed (ignoring): ${ex.message}", ex)
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
                    syncFromIdp()
                    val freshUsers = userService.selectAll(false)
                    val freshGroups = groupService.getAllGroups()
                    ldapMasterLoginHandler.afterUserGroupCacheRefresh(freshUsers, freshGroups)
                } catch (ex: Exception) {
                    log.error("IdP sync failed: ${ex.message}", ex)
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
        if (!user.localUser && !user.deleted) {
            syncPasswordToIdp(user, newPassword)
        }
        try {
            ldapMasterLoginHandler.passwordChanged(user, newPassword)
        } catch (ex: Exception) {
            log.error("LDAP password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    override fun wlanPasswordChanged(user: PFUserDO, newPassword: CharArray) {
        try {
            ldapMasterLoginHandler.wlanPasswordChanged(user, newPassword)
        } catch (ex: Exception) {
            log.error("LDAP WLAN password change failed for user '${user.username}' (ignoring): ${ex.message}", ex)
        }
    }

    // -------------------------------------------------------------------------
    // Internal sync logic
    // -------------------------------------------------------------------------

    private fun syncFromIdp() {
        if (!idpAdminClient.isConfigured()) {
            log.debug("IdP not configured, skipping sync.")
            return
        }
        val providerName = idpAdminClient.providerName()
        log.info("Starting $providerName -> PF DB sync...")

        // --- Sync users ---
        val idpUsers = idpAdminClient.getAllUsers()
        val dbUsers = userService.selectAll(false)

        var created = 0; var updated = 0; var deactivated = 0; var errors = 0

        val idpIdToPfUser = mutableMapOf<String, PFUserDO>()

        idpUsers.forEach { idpUser ->
            try {
                val byIdpId = dbUsers.find { it.idpExternalId != null && it.idpExternalId == idpUser.id }
                val existing = byIdpId ?: dbUsers.find { it.username == idpUser.username }
                if (byIdpId != null && byIdpId.username != idpUser.username) {
                    log.debug { "User matched by idpExternalId '${idpUser.id}': PF username '${byIdpId.username}' ≠ IdP username '${idpUser.username}' (username rename detected)" }
                }
                if (existing == null) {
                    val newUser = idpUserConverter.toPFUser(idpUser)
                    newUser.id = null
                    userDao.insert(newUser, false)
                    val persisted = userService.getInternalByUsername(idpUser.username!!)
                    if (persisted != null) {
                        idpUser.id?.let { idpIdToPfUser[it] = persisted }
                    }
                    created++
                } else {
                    val modified = idpUserConverter.copyFields(idpUser, existing)
                    if (modified) {
                        userDao.update(existing, false)
                        updated++
                    }
                    idpUser.id?.let { idpIdToPfUser[it] = existing }
                }
            } catch (ex: Exception) {
                log.error("Error syncing user '${idpUser.username}' from $providerName (continuing): ${ex.message}", ex)
                errors++
            }
        }

        val idpUsernames = idpUsers.mapNotNull { it.username }.toSet()
        dbUsers.filter { !it.localUser && !it.deleted && !idpUsernames.contains(it.username) }.forEach { user ->
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
        log.info("$providerName user sync: $created created, $updated updated, $deactivated deactivated" +
                (if (errors > 0) ", *** $errors errors ***" else ""))

        // --- Sync groups ---
        val idpGroups = idpAdminClient.getAllGroups()
        val dbGroups = groupService.getAllGroups().toMutableList()

        var gCreated = 0; var gUpdated = 0; var gErrors = 0
        val idpIdToPfGroup = mutableMapOf<String, GroupDO>()

        val syncGroupAttributes = idpConfig.groupAttributes.isNotEmpty()

        idpGroups.forEach { idpGroupShallow ->
            try {
                val idpGroup = if (syncGroupAttributes && idpGroupShallow.id != null) {
                    idpAdminClient.getGroup(idpGroupShallow.id!!)
                } else {
                    idpGroupShallow
                }
                val existing = dbGroups.find { it.name == idpGroup.name }
                if (existing == null) {
                    val newGroup = idpGroupConverter.toGroupDO(idpGroup)
                    newGroup.id = null
                    groupDao.insert(newGroup, false)
                    val persisted = groupDao.getByName(idpGroup.name)
                    if (persisted != null) {
                        idpGroup.id?.let { idpIdToPfGroup[it] = persisted }
                        dbGroups.add(persisted)
                    }
                    gCreated++
                } else {
                    val modified = idpGroupConverter.copyFields(idpGroup, existing)
                    if (modified) {
                        groupDao.update(existing, false)
                        gUpdated++
                    }
                    idpGroup.id?.let { idpIdToPfGroup[it] = existing }
                }
            } catch (ex: Exception) {
                log.error("Error syncing group '${idpGroupShallow.name}' from $providerName (continuing): ${ex.message}", ex)
                gErrors++
            }
        }
        log.info("$providerName group sync: $gCreated created, $gUpdated updated" +
                (if (gErrors > 0) ", *** $gErrors errors ***" else ""))

        // --- Sync memberships ---
        syncMemberships(idpUsers, idpIdToPfUser, idpIdToPfGroup)

        userGroupCache.setExpired()
        log.info("$providerName -> PF DB sync complete.")
    }

    private fun syncMemberships(
        idpUsers: List<IdpUser>,
        idpIdToPfUser: Map<String, PFUserDO>,
        idpIdToPfGroup: Map<String, GroupDO>
    ) {
        if (idpIdToPfGroup.isEmpty()) return
        val groupToMembers = mutableMapOf<GroupDO, MutableSet<PFUserDO>>()
        idpIdToPfGroup.values.forEach { groupToMembers[it] = mutableSetOf() }

        idpUsers.forEach { idpUser ->
            val idpUserId = idpUser.id ?: return@forEach
            val pfUser = idpIdToPfUser[idpUserId] ?: return@forEach
            try {
                val userGroups = idpAdminClient.getUserGroups(idpUserId)
                userGroups.forEach { idpGroup ->
                    val pfGroup = idpIdToPfGroup[idpGroup.id] ?: return@forEach
                    groupToMembers.getOrPut(pfGroup) { mutableSetOf() }.add(pfUser)
                }
            } catch (ex: Exception) {
                log.error("Error fetching groups for IdP user '${idpUser.username}' (continuing): ${ex.message}", ex)
            }
        }

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
}
