/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.user

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.login.Login
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The group user relations will be cached with this class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
// Open for mocking in test cases.
@Service
open class UserGroupCache : AbstractCache() {

    @Autowired
    private lateinit var userRights: UserRightService

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @PostConstruct
    private fun postConstruct() {
        if (INSTANCE != null) {
            log.warn { "Oups, shouldn't instantiate UserGroupCache twice!" }
            return
        }
        INSTANCE = this
    }

    /**
     * The key is the user id and the value is a list of assigned groups.
     * Mustn't be synchronized because it is only read by the cache.
     */
    private var userGroupIdMap = mapOf<Long, Set<Long>>()

    /**
     * The key is the group id.
     * Mustn't be synchronized because it is only read by the cache.
     */
    private var groupMap = mapOf<Long, GroupDO>()

    /**
     * List of all rights (value) defined for the user ids (key).
     * Mustn't be synchronized because it is only read by the cache.
     */
    private var rightMap = mapOf<Long, List<UserRightDO>>()

    /**
     * Must be synchronized because it is mutable.
     */
    private var userMap = mutableMapOf<Long, PFUserDO>()

    /**
     * Key is user id.
     */
    private var employeeMap = mapOf<Long, EmployeeDO>()
    private var adminUsers = setOf<Long>()
    private var financeUsers = setOf<Long>()
    private var controllingUsers = setOf<Long>()
    private var projectManagers = setOf<Long>()
    private var projectAssistants = setOf<Long>()
    private var marketingUsers = setOf<Long>()
    private var orgaUsers = setOf<Long>()
    private var hrUsers = setOf<Long>()

    fun getGroup(group: ProjectForgeGroup): GroupDO? {
        checkRefresh()
        return groupMap.values.find { group.matches(it.name) }
    }

    fun getGroup(groupId: Long?): GroupDO? {
        checkRefresh()
        return groupMap[groupId]
    }

    fun getUserById(userId: String?): PFUserDO? {
        userId ?: return null
        try {
            return getUser(userId.toLongOrNull())
        } catch (e: NumberFormatException) {
            // Ignore error.
        }
        return null
    }

    fun getUser(userId: Long?): PFUserDO? {
        userId ?: return null
        checkRefresh()
        synchronized(userMap) {
            return userMap[userId]
        }
    }

    fun getUser(username: String): PFUserDO? {
        if (username.isBlank()) {
            return null
        }
        checkRefresh()
        synchronized(userMap) {
            return userMap.values.find { username == it.username }
        }
    }

    fun getUserByFullname(fullname: String): PFUserDO? {
        if (fullname.isBlank()) {
            return null
        }
        checkRefresh()
        synchronized(userMap) {
            return userMap.values.find { fullname == it.getFullname() }
        }
    }

    /**
     * @return all users (also deleted users).
     */
    val allUsers: Collection<PFUserDO>
        get() {
            checkRefresh()
            synchronized(userMap) {
                return userMap.values
            }
        }

    /**
     * @return all groups (also deleted groups).
     */
    val allGroups: Collection<GroupDO>
        get() {
            checkRefresh()
            return groupMap.values
        }

    /**
     * Only for internal use.
     */
    fun internalGetNumberOfUsers(): Int {
        checkRefresh()
        synchronized(userMap) {
            return userMap.size
        }
    }

    fun getUsername(userId: Long?): String? { // checkRefresh(); Done by getUserMap().
        userId ?: return null
        checkRefresh()
        synchronized(userMap) {
            val user = userMap[userId] ?: return userId.toString()
            return user.username
        }
    }

    /**
     * Check for current logged-in user.
     */
    fun isLoggedInUserMemberOfGroup(groupId: Long?): Boolean {
        return isUserMemberOfGroup(ThreadLocalUserContext.loggedInUserId, groupId)
    }

    fun isUserMemberOfGroup(user: PFUserDO?, groupId: Long?): Boolean {
        return if (user == null) {
            false
        } else isUserMemberOfGroup(user.id, groupId)
    }

    fun isUserMemberOfGroup(userId: Long?, groupId: Long?): Boolean {
        groupId ?: return false
        checkRefresh()
        return userGroupIdMap[userId]?.contains(groupId) == true
    }

    fun isLoggedInUserMemberOfAtLeastOneGroup(vararg groupIds: Long?): Boolean {
        return isUserMemberOfAtLeastOneGroup(ThreadLocalUserContext.loggedInUserId, *groupIds)
    }

    fun isUserMemberOfAtLeastOneGroup(userId: Long?, vararg groupIds: Long?): Boolean {
        userId ?: return false
        if (groupIds.isEmpty()) {
            return false
        }
        checkRefresh()
        val groupSet = userGroupIdMap[userId] ?: return false
        return groupIds.any { groupSet.contains(it) }
    }

    val isUserMemberOfAdminGroup: Boolean
        get() = isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfAdminGroup(userId: Long?): Boolean {
        checkRefresh()
        return adminUsers.contains(userId)
    }

    val isUserMemberOfFinanceGroup: Boolean
        get() = isUserMemberOfFinanceGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfFinanceGroup(userId: Long?): Boolean {
        checkRefresh()
        return financeUsers.contains(userId)
    }

    val isUserMemberOfProjectManagers: Boolean
        get() = isUserMemberOfProjectManagers(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfProjectManagers(userId: Long?): Boolean {
        checkRefresh()
        return projectManagers.contains(userId)
    }

    val isUserMemberOfProjectAssistant: Boolean
        get() = isUserMemberOfProjectAssistant(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfProjectAssistant(userId: Long?): Boolean {
        checkRefresh()
        return projectAssistants.contains(userId)
    }

    fun isUserProjectManagerOrAssistantForProject(projekt: ProjektDO?): Boolean {
        if (projekt?.projektManagerGroupId == null) {
            return false
        }
        val userId = ThreadLocalUserContext.loggedInUserId
        return if (!isUserMemberOfProjectAssistant(userId) && !isUserMemberOfProjectManagers(userId)) {
            false
        } else isUserMemberOfGroup(userId, projekt.projektManagerGroupId)
    }

    val isUserMemberOfControllingGroup: Boolean
        get() = isUserMemberOfControllingGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfControllingGroup(userId: Long?): Boolean {
        checkRefresh()
        return controllingUsers.contains(userId)
    }

    val isUserMemberOfMarketingGroup: Boolean
        get() = isUserMemberOfMarketingGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfMarketingGroup(userId: Long?): Boolean {
        checkRefresh()
        return marketingUsers.contains(userId)
    }

    val isUserMemberOfOrgaGroup: Boolean
        get() = isUserMemberOfOrgaGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfOrgaGroup(userId: Long?): Boolean {
        checkRefresh()
        return orgaUsers.contains(userId)
    }

    fun isUserMemberOfHRGroup(userId: Long?): Boolean {
        checkRefresh()
        return hrUsers.contains(userId)
    }

    /**
     * Checks if the given user is at least member of one of the given groups.
     */
    fun isLoggedInUserMemberOfGroup(vararg groups: ProjectForgeGroup): Boolean {
        return isUserMemberOfGroup(ThreadLocalUserContext.loggedInUser, *groups)
    }

    /**
     * Checks if the given user is at least member of one of the given groups.
     */
    fun isUserMemberOfGroup(user: PFUserDO?, vararg groups: ProjectForgeGroup): Boolean {
        user ?: return false
        require(groups.isNotEmpty())
        groups.forEach { group ->
            val result = when (group) {
                ProjectForgeGroup.ADMIN_GROUP -> isUserMemberOfAdminGroup(user.id)
                ProjectForgeGroup.FINANCE_GROUP -> isUserMemberOfFinanceGroup(user.id)
                ProjectForgeGroup.PROJECT_MANAGER -> isUserMemberOfProjectManagers(user.id)
                ProjectForgeGroup.PROJECT_ASSISTANT -> isUserMemberOfProjectAssistant(user.id)
                ProjectForgeGroup.CONTROLLING_GROUP -> isUserMemberOfControllingGroup(user.id)
                ProjectForgeGroup.MARKETING_GROUP -> isUserMemberOfMarketingGroup(user.id)
                ProjectForgeGroup.ORGA_TEAM -> isUserMemberOfOrgaGroup(user.id)
                ProjectForgeGroup.HR_GROUP -> isUserMemberOfHRGroup(user.id)
            }
            if (result) {
                return true
            }
        }
        return false
    }

    fun getUserRights(userId: Long?): List<UserRightDO>? {
        checkRefresh()
        return rightMap[userId]
    }

    /**
     * Returns a collection of group id's to which the user is assigned to.
     *
     * @return collection if found, otherwise null.
     */
    fun getUserGroups(user: PFUserDO?): Collection<Long>? {
        user ?: return null
        checkRefresh()
        return userGroupIdMap[user.id]
    }

    /**
     * Returns a collection of group id's to which the user is assigned to.
     *
     * @return collection if found, otherwise null.
     */
    fun getUserGroupDOs(user: PFUserDO?): Collection<GroupDO>? {
        user ?: return null
        return getUserGroups(user)?.map { groupId ->
            getGroup(groupId)
        }?.filterNotNull()
    }

    fun getUserGroupIdMap(): Map<Long, Set<Long>> {
        checkRefresh()
        return userGroupIdMap
    }

    fun getEmployeeId(userId: Long?): Long? {
        userId ?: return null
        checkRefresh()
        return employeeMap[userId]?.id
    }

    fun getEmployeeByUser(userId: Long?): EmployeeDO? {
        userId ?: return null
        checkRefresh()
        return employeeMap[userId]
    }

    fun getUser(employeeDO: EmployeeDO?): PFUserDO? {
        employeeDO ?: return null
        checkRefresh()
        val userId = employeeMap.values.find { it.id == employeeDO.id }?.userId
        return getUser(userId)
    }

    /**
     * Should be called after user modifications.
     */
    fun updateUser(user: PFUserDO) {
        checkRefresh()
        synchronized(userMap) {
            user.id?.let { userMap[it] = user }
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized.
     */
    override fun refresh() {
        try {
            PfPersistenceService.startCallsStatsRecording()
            log.info("Initializing UserGroupCache...")
            val saved = persistenceService.saveStatsState()
            // This method must not be synchronized because it works with a new copy of maps.
            val uMap: MutableMap<Long, PFUserDO> = HashMap()
            // Could not autowire UserDao because of cyclic reference with AccessChecker.
            log.info("Loading all users ...")
            persistenceService.runIsolatedReadOnly { _ ->
                val users = Login.getInstance().allUsers
                users.forEach { user ->
                    user.id?.let { userId ->
                        uMap[userId] = user
                    }
                }
                if (users.size != uMap.size) {
                    log.warn("********** Load ${users.size} from the backend, but added only ${uMap.size} users to cache!")
                    log.info("For debugging UserCache fuck-up: " + ToStringUtil.toJsonString(users))
                    return@runIsolatedReadOnly
                }
                log.info("Loading all groups ...")
                val groups = Login.getInstance().allGroups
                val gMap = mutableMapOf<Long, GroupDO>()
                val ugIdMap = mutableMapOf<Long, MutableSet<Long>>()
                val nAdminUsers = mutableSetOf<Long>()
                val nFinanceUser = mutableSetOf<Long>()
                val nControllingUsers = mutableSetOf<Long>()
                val nProjectManagers = mutableSetOf<Long>()
                val nProjectAssistants = mutableSetOf<Long>()
                val nMarketingUsers = mutableSetOf<Long>()
                val nOrgaUsers = mutableSetOf<Long>()
                val nhrUsers = mutableSetOf<Long>()
                for (group in groups) {
                    val groupId = group.id ?: continue
                    gMap[groupId] = group
                    group.assignedUsers?.forEach { user ->
                        val userId = user.id ?: return@forEach
                        val groupIdSet = ugIdMap.getOrPut(userId) { mutableSetOf() }
                        groupIdSet.add(groupId)
                        when {
                            ProjectForgeGroup.ADMIN_GROUP.matches(group.name) -> nAdminUsers.add(userId)
                            ProjectForgeGroup.FINANCE_GROUP.matches(group.name) -> nFinanceUser.add(userId)
                            ProjectForgeGroup.CONTROLLING_GROUP.matches(group.name) -> nControllingUsers.add(userId)
                            ProjectForgeGroup.PROJECT_MANAGER.matches(group.name) -> nProjectManagers.add(userId)
                            ProjectForgeGroup.PROJECT_ASSISTANT.matches(group.name) -> nProjectAssistants.add(userId)
                            ProjectForgeGroup.MARKETING_GROUP.matches(group.name) -> nMarketingUsers.add(userId)
                            ProjectForgeGroup.ORGA_TEAM.matches(group.name) -> nOrgaUsers.add(userId)
                            ProjectForgeGroup.HR_GROUP.matches(group.name) -> nhrUsers.add(userId)
                        }
                    }
                }
                this.userMap = uMap
                this.groupMap = gMap
                val nEmployeeMap = mutableMapOf<Long, EmployeeDO>()
                employeeDao.selectAllNotDeleted(checkAccess = false).forEach { employeeDO ->
                    employeeDO.userId?.let { userId ->
                        nEmployeeMap[userId] = employeeDO
                        employeeDao.setEmployeeStatus(employeeDO)
                    }
                }
                this.employeeMap = nEmployeeMap
                this.adminUsers = nAdminUsers
                this.financeUsers = nFinanceUser
                this.controllingUsers = nControllingUsers
                this.projectManagers = nProjectManagers
                this.projectAssistants = nProjectAssistants
                this.marketingUsers = nMarketingUsers
                this.orgaUsers = nOrgaUsers
                this.hrUsers = nhrUsers
                this.userGroupIdMap = ugIdMap
                val rMap = mutableMapOf<Long, List<UserRightDO>>()
                val rights = try {
                    userRightDao.internalGetAllOrdered()
                } catch (ex: Exception) {
                    log.error(
                        "******* Exception while getting user rights from data-base (only OK for migration from older versions): "
                                + ex.message,
                        ex
                    )
                    ArrayList()
                }
                var list: MutableList<UserRightDO>? = null
                var userId: Long? = null
                for (right in rights) {
                    if (right.userId == null) {
                        log.warn("Oups, userId = null: $right")
                        continue
                    }
                    if (right.userId != userId) {
                        list = ArrayList()
                        userId = right.userId
                        if (userId != null) {
                            rMap[userId] = list
                        }
                    }
                    if (userRights.getRight(right.rightIdString) != null
                        && userRights.getRight(right.rightIdString).isAvailable(right.user, getUserGroupDOs(right.user))
                    ) {
                        list!!.add(right)
                    }
                }
                this.rightMap = rMap
                log.info("Initializing of UserGroupCache done. Found ${uMap.size} entries.")
                Login.getInstance().afterUserGroupCacheRefresh(users, groups)
            }
            log.info(
                "UserGroupCache.refresh done. stats=${persistenceService.formatStats(saved)}, callsStats=${
                    PfPersistenceService.showCallsStatsRecording()
                }"
            )
        } finally {
            PfPersistenceService.stopCallsStatsRecording()
        }
        Thread {
            jobHandler.checkStatus()
        }.start()
    }

    companion object {
        private const val serialVersionUID = -6501106088529363341L
        private var INSTANCE: UserGroupCache? = null

        @JvmStatic
        fun getInstance(): UserGroupCache {
            return INSTANCE!!
        }

        @JvmStatic
        fun isUserMemberOfGroup(userGroups: Collection<GroupDO?>?, vararg groups: ProjectForgeGroup): Boolean {
            if (userGroups.isNullOrEmpty()) {
                return false
            }
            for (group in groups) {
                val dbGroup = getInstance().getGroup(group) ?: continue
                if (userGroups.any { it?.id == dbGroup.id }) {
                    return true
                }
            }
            return false
        }
    }

    init {
        setExpireTimeInHours(1)
    }
}
