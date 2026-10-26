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

package org.projectforge.rest.dto

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.ContactUserIds
import org.projectforge.business.ldap.PFUserDOConverter
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.TimeNotation
import java.util.*

class User(
    id: Long? = null,
    displayName: String? = null,
    var username: String? = null,
    var firstname: String? = null,
    var nickname: String? = null,
    var jiraUsername: String? = null,
    var lastname: String? = null,
    var gender: Gender? = null,
    var mobilePhone: String? = null,
    var description: String? = null,
    var organization: String? = null,
    var email: String? = null,
    var alternativeEmails: String? = null,
    var formerEmails: String? = null,
    override var deactivated: Boolean = false,
    var timeZone: String? = null,
    var locale: Locale? = null,
    var dateFormat: String? = null,
    var excelDateFormat: String? = null,
    var timeNotation: TimeNotation? = null,
    var personalPhoneIdentifiers: String? = null,
    var assignedGroups: MutableList<Group>? = null,
    var lastLogin: Date? = null,
    var lastLoginTimeAgo: String? = null,
    /**
     * Timestamp and time ago: 2022-16-15 18:49 (5 minutes ago)
     */
    var lastLoginFormatted: String? = null,
    var sshPublicKey: String? = null,
    var gpgPublicKey: String? = null,
    var rightsAsString: String? = null,
    var ldapValues: UserLdapValues? = null,
    var localUser: Boolean? = false,
    var restrictedUser: Boolean? = false,
    var hrPlanning: Boolean? = false,
) : BaseDTODisplayObject<PFUserDO>(id = id, displayName = displayName) {

    /**
     * For admin's user page for setting initial password while creating new users.
     */
    var password: String? = null

    /**
     * For admin's user page for setting initial wlan password while creating new users.
     */
    var wlanPassword: String? = null

    var calendarExportTokenCreationDate: Date? = null
    var davTokenCreationDate: Date? = null
    var restClientTokenCreationDate: Date? = null

    /**
     * Number of devices with a valid stay-logged-in token (one row per device, see
     * [org.projectforge.business.user.StayLoggedInTokenDO]). No token is ever sent to the client.
     */
    var stayLoggedInDevices: Int? = null

    /**
     * When the most recently used of those devices was last seen.
     */
    var stayLoggedInLastAccessTimeAgo: String? = null

    var calendarExportTokenCreationTimeAgo: String? = null
    var davTokenCreationTimeAgo: String? = null
    var restClientTokenCreationTimeAgo: String? = null

    var lastPasswordChange: Date? = null
    var lastPasswordChangeFormatted: String? = null
    var lastWlanPasswordChange: Date? = null
    var lastWlanPasswordChangeFormatted: String? = null

    var userRights: MutableList<UserRightDto>? = null

    /*
     * Read-only flags of the request for the user edit page (projectforge-next), set in edit mode for
     * administrators only (UserEntityRest.transformFromDB). No properties of the user: copyTo finds no PFUserDO
     * counterpart and ignores them.
     */
    /** Posix accounts are in use: the LDAP fieldset shows the uid/gid fields. */
    var posixConfigured: Boolean? = null

    /** Samba accounts are in use: the LDAP fieldset shows the samba SID fields. */
    var sambaConfigured: Boolean? = null

    /** An external user management system (LDAP) is in use: the local user flag means something. */
    var externalUserManagementSystem: Boolean? = null

    /** The WLAN password can be set: the initial WLAN password field and the change link are offered. */
    var wlanPasswordSupported: Boolean? = null

    /** Sipgate is configured: the personal phone identifiers are of use. */
    var phoneIdentifiersSupported: Boolean? = null

    /**
     * @see copyFromMinimal
     */
    constructor(src: PFUserDO) : this() {
        copyFromMinimal(src)
    }

    fun add(userRightDto: UserRightDto) {
        if (userRights == null) {
            userRights = mutableListOf()
        }
        userRights!!.add(userRightDto)
    }

    override fun copyFromMinimal(src: PFUserDO) {
        this.displayName = src.displayName
        this.username = src.username
        // id, deleted and deactivated, for every user (not only admins): a no longer active user is struck through
        // wherever shown as a reference.
        super.copyFromMinimal(src)
    }

    override fun copyFrom(src: PFUserDO) {
        val user = PfCaches.instance.getUserIfNotInitialized(src)!!
        // Visible for non-admin user's (if in any same group)
        copyFromMinimal(user)
        timeZone = user.timeZoneString
        gpgPublicKey = user.gpgPublicKey
        sshPublicKey = user.sshPublicKey
        firstname = user.firstname
        lastname = user.lastname
        if (userGroupCache.isUserMemberOfAdminGroup) {
            super.copyFrom(user)
            lastLogin?.let { date ->
                lastLoginTimeAgo = TimeAgo.getMessage(date)
                lastLoginFormatted = TimeAgo.getDateAndMessage(date)
            }
            lastPasswordChange?.let { date ->
                lastPasswordChangeFormatted = TimeAgo.getDateAndMessage(date)
            }
            lastWlanPasswordChange?.let { date ->
                lastWlanPasswordChangeFormatted = TimeAgo.getDateAndMessage(date)
            }
            if (accessChecker.isLoggedInUserMemberOfAdminGroup) {
                // Rights
                val sb = StringBuilder()
                userDao.getUserRights(user.id)?.forEachIndexed { index, rightDO ->
                    if (index > 0) {
                        sb.append(", ")
                    }
                    sb.append(translate(userRightService.getRightId(rightDO.rightIdString).i18nKey))
                    sb.append(
                        when (rightDO.value) {
                            UserRightValue.READONLY -> " (ro)"
                            UserRightValue.PARTLYREADWRITE -> " (prw)"
                            UserRightValue.READWRITE -> " (rw)"
                            else -> ""
                        }
                    )
                }
                rightsAsString = sb.toString()
                val newAssignedGroups = mutableSetOf<Group>()
                userGroupCache.getUserGroups(user)?.forEach { groupId ->
                    userGroupCache.getGroup(groupId)?.let { groupDO ->
                        val group = Group()
                        group.copyFromMinimal(groupDO)
                        if (!newAssignedGroups.any { it.id == groupDO.id }) {
                            newAssignedGroups.add(group)
                        }
                    }
                }
                assignedGroups = newAssignedGroups.sortedBy { it.displayName?.lowercase() }.toMutableList()
                PFUserDOConverter.readLdapUserValues(user.ldapValues)?.let { srcValues ->
                    ldapValues = UserLdapValues.create(srcValues)
                }
            }
        }
    }

    override fun copyTo(dest: PFUserDO) {
        super.copyTo(dest)
        dest.timeZoneString = timeZone
        ldapValues?.let {
            val ldapUserValues = it.convert()
            dest.ldapValues = PFUserDOConverter.getLdapValuesAsXml(ldapUserValues)
        }
    }

    fun ensureLdapValues(): UserLdapValues {
        ldapValues?.let { return it }
        UserLdapValues().let {
            ldapValues = it
            return it
        }
    }


    companion object {
        private val accessChecker =
            ApplicationContextProvider.getApplicationContext().getBean(AccessChecker::class.java)
        private val userDao = ApplicationContextProvider.getApplicationContext().getBean(UserDao::class.java)
        private val userGroupCache: UserGroupCache by lazy { UserGroupCache.getInstance() }
        private val userRightService =
            ApplicationContextProvider.getApplicationContext().getBean(UserRightService::class.java)

        fun getUser(userId: Long?, minimal: Boolean = true): User? {
            userId ?: return null
            val userDO = userDao.findOrLoad(userId) ?: return null
            val user = User()
            if (minimal) {
                user.copyFromMinimal(userDO)
            } else {
                user.copyFrom(userDO)
            }
            return user
        }

        /**
         * The user as a reference of a list row: id, display name, deleted and deactivated (struck through when no
         * longer active), without the username and the flags of [copyFromMinimal] that a row of a long list would
         * carry for nothing.
         */
        fun ref(user: PFUserDO): User {
            return User(user.id, user.displayName, localUser = null, restrictedUser = null, hrPlanning = null).also {
                it.deleted = user.deleted
                it.deactivated = user.deactivated
            }
        }

        /**
         * The contact person and the further contacts of an order or project (main contact first), each with its name
         * and whether it is still active, from the cache: a no longer active one is struck through. Unknown ids are
         * skipped; null if there is nobody.
         */
        fun toContactList(contactPersonId: Long?, additionalContactUserIds: String?): List<User>? {
            val caches = PfCaches.instance
            return (listOfNotNull(contactPersonId) + ContactUserIds.toList(additionalContactUserIds))
                .distinct()
                .mapNotNull { caches.getUser(it)?.let { user -> ref(user) } }
                .takeIf { it.isNotEmpty() }
        }

        /**
         * Converts csv of user ids to list of user (only with id and displayName = "???", no other content).
         */
        fun toUserList(str: String?): List<User>? {
            if (str.isNullOrBlank()) return null
            return toLongArray(str)?.map { User(it, "???") }
        }

        /**
         * Converts csv of user ids to list of user id's.
         */
        fun toLongArray(str: String?): LongArray? {
            if (str.isNullOrBlank()) return null
            return StringHelper.splitToLongs(str, ",", false)
        }

        /**
         * Converts user list to list of long values (of format supported by [toUserList]).
         */
        fun toLongList(users: List<User>?): String? {
            return users?.filter { it.id != null }?.joinToString { "${it.id}" }
        }

        /**
         * Set display names of any existing user in the given list, and whether it is still active (deleted,
         * deactivated): a no longer active user is struck through wherever shown as a reference.
         * @see UserService.getUser
         */
        fun restoreDisplayNames(users: List<User>?) {
            val caches = PfCaches.instance
            users?.forEach { user ->
                val userDO = caches.getUser(user.id)
                user.displayName = userDO?.displayName ?: "???"
                user.deleted = userDO?.deleted == true
                user.deactivated = userDO?.deactivated == true
            }
        }

        fun restoreEmails(users: List<User>?) {
            val caches = PfCaches.instance
            users?.forEach { it.email = caches.getUser(it.id)?.email }
        }

        /**
         * Converts csv of user ids to list of user.
         */
        fun toUserNames(userIds: String?): String {
            val users = toUserList(userIds)
            restoreDisplayNames(users)
            return users?.joinToString { it.displayName ?: "???" } ?: ""
        }

        fun getAssignedGroupDOs(user: User): List<GroupDO>? {
            return user.assignedGroups?.map {
                val groupDO = GroupDO()
                it.copyTo(groupDO)
                groupDO
            }
        }
    }
}
