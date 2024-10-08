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
open class UserGroupCache() : AbstractCache() {

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
     */
    private var userGroupIdMap: Map<Long, MutableSet<Long>>? = null

    /**
     * The key is the group id.
     */
    private var groupMap: Map<Long, GroupDO>? = null

    /**
     * List of all rights (value) defined for the user ids (key).
     */
    private var rightMap: Map<Long, List<UserRightDO>>? = null
    private var userMap: MutableMap<Long, PFUserDO?>? = null

    /**
     * Key is user id.
     */
    private var employeeMap = mapOf<Long?, EmployeeDO>()
    private var adminUsers: MutableSet<Long>? = null
    private var financeUsers: Set<Long>? = null
    private var controllingUsers: Set<Long>? = null
    private var projectManagers: Set<Long>? = null
    private var projectAssistants: Set<Long>? = null
    private var marketingUsers: Set<Long>? = null
    private var orgaUsers: Set<Long>? = null
    private var hrUsers: Set<Long>? = null

    fun getGroup(group: ProjectForgeGroup): GroupDO? {
        checkRefresh()
        return groupMap?.values?.find { group.matches(it.name) }
    }

    fun getGroup(groupId: Long?): GroupDO? {
        checkRefresh()
        return groupMap!![groupId]
    }

    fun getUser(userId: Long?): PFUserDO? {
        if (userId == null) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.let { it[userId] }
        return user
    }

    fun getUser(username: String): PFUserDO? {
        if (username.isBlank()) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.values?.find { username == it?.username }
        return user
    }

    fun getUserByFullname(fullname: String): PFUserDO? {
        if (fullname.isBlank()) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.values?.find { fullname == it?.getFullname() }
        return user
    }

    /**
     * @return all users (also deleted users).
     */
    val allUsers: Collection<PFUserDO?>
        get() =// checkRefresh(); Done by getUserMap().
            getUserMap()!!.values// checkRefresh(); Done by getGMap().

    /**
     * @return all groups (also deleted groups).
     */
    val allGroups: Collection<GroupDO>
        get() =// checkRefresh(); Done by getGMap().
            getGroupMap()!!.values

    /**
     * Only for internal use.
     */
    fun internalGetNumberOfUsers(): Int {
        return if (userMap == null) {
            0
        } else { // checkRefresh(); Done by getUserMap().
            getUserMap()!!.size
        }
    }

    fun getUsername(userId: Long?): String? { // checkRefresh(); Done by getUserMap().
        userId ?: return null
        val user = getUserMap()!![userId] ?: return userId.toString()
        return user.username
    }

    /**
     * Check for current logged in user.
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
        if (groupId == null) {
            return false
        }
        checkRefresh()
        val groupSet: Set<Long>? = getUserGroupIdMap()!![userId]
        return groupSet != null && groupSet.contains(groupId)
    }

    fun isLoggedInUserMemberOfAtLeastOneGroup(vararg groupIds: Long?): Boolean {
        return isUserMemberOfAtLeastOneGroup(ThreadLocalUserContext.loggedInUserId, *groupIds)
    }

    fun isUserMemberOfAtLeastOneGroup(userId: Long?, vararg groupIds: Long?): Boolean {
        if (groupIds.isEmpty()) {
            return false
        }
        checkRefresh()
        val groupSet = getUserGroupIdMap()?.let { it[userId] } ?: return false
        return groupIds.any { groupSet.contains(it) }
    }

    val isUserMemberOfAdminGroup: Boolean
        get() = isUserMemberOfAdminGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfAdminGroup(userId: Long?): Boolean {
        checkRefresh()
        // adminUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return adminUsers != null && adminUsers!!.contains(userId)
    }

    val isUserMemberOfFinanceGroup: Boolean
        get() = isUserMemberOfFinanceGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfFinanceGroup(userId: Long?): Boolean {
        checkRefresh()
        // financeUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return financeUsers != null && financeUsers!!.contains(userId)
    }

    val isUserMemberOfProjectManagers: Boolean
        get() = isUserMemberOfProjectManagers(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfProjectManagers(userId: Long?): Boolean {
        checkRefresh()
        // projectManagers should only be null in maintenance mode (e. g. if user table isn't readable).
        return projectManagers != null && projectManagers!!.contains(userId)
    }

    val isUserMemberOfProjectAssistant: Boolean
        get() = isUserMemberOfProjectAssistant(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfProjectAssistant(userId: Long?): Boolean {
        checkRefresh()
        // projectAssistants should only be null in maintenance mode (e. g. if user table isn't readable).
        return projectAssistants != null && projectAssistants!!.contains(userId)
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
        // controllingUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return controllingUsers != null && controllingUsers!!.contains(userId)
    }

    val isUserMemberOfMarketingGroup: Boolean
        get() = isUserMemberOfMarketingGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfMarketingGroup(userId: Long?): Boolean {
        checkRefresh()
        // marketingUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return marketingUsers!!.contains(userId)
    }

    val isUserMemberOfOrgaGroup: Boolean
        get() = isUserMemberOfOrgaGroup(ThreadLocalUserContext.loggedInUserId)

    fun isUserMemberOfOrgaGroup(userId: Long?): Boolean {
        checkRefresh()
        // orgaUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return orgaUsers != null && orgaUsers!!.contains(userId)
    }

    fun isUserMemberOfHRGroup(userId: Long?): Boolean {
        checkRefresh()
        // hrUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return hrUsers != null && hrUsers!!.contains(userId)
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
        if (user == null) {
            return false
        }
        require(groups.isNotEmpty())
        for (group in groups) {
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
        return userRightMap!![userId]
    }

    fun getUserRight(userId: Long?, rightId: UserRightId): UserRightDO? {
        val rights = getUserRights(userId) ?: return null
        return rights.find { it.rightIdString == rightId.id }
    }

    private val userRightMap: Map<Long, List<UserRightDO>>?
        get() {
            checkRefresh()
            return rightMap
        }

    /**
     * Returns a collection of group id's to which the user is assigned to.
     *
     * @return collection if found, otherwise null.
     */
    fun getUserGroups(user: PFUserDO?): Collection<Long>? {
        user ?: return null
        checkRefresh()
        return getUserGroupIdMap()!![user.id]
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

    private fun getGroupMap(): Map<Long, GroupDO>? {
        checkRefresh()
        return groupMap
    }

    fun getUserGroupIdMap(): Map<Long, MutableSet<Long>>? {
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
        val userId = employeeMap.values.find { it.id == employeeDO.id }?.userId
        return getUser(userId)
    }

    /**
     * Should be called after user modifications.
     */
    fun updateUser(user: PFUserDO) {
        user.id?.let { getUserMap()!![it] = user }
    }

    private fun getUserMap(): MutableMap<Long, PFUserDO?>? {
        checkRefresh()
        return userMap
    }

    /**
     * This method will be called by CacheHelper and is synchronized.
     */
    override fun refresh() {
        val begin = System.currentTimeMillis()
        log.info("Initializing UserGroupCache...")
        // This method must not be synchronized because it works with a new copy of maps.
        val uMap: MutableMap<Long, PFUserDO?> = HashMap()
        // Could not autowire UserDao because of cyclic reference with AccessChecker.
        log.info("Loading all users ...")
        persistenceService.runReadOnly { context ->
            val users = Login.getInstance().allUsers
            for (user in users) {
                user.id ?: continue // Should only occur in test cases.
                user.id?.let {
                    uMap[it] = user
                }
            }
            if (users.size != uMap.size) {
                log.warn("********** Load ${users.size} from the backend, but added only ${uMap.size} users to cache!")
                log.info("For debugging UserCache fuck-up: " + ToStringUtil.toJsonString(users))
                return@runReadOnly
            }
            log.info("Loading all groups ...")
            val groups = Login.getInstance().allGroups
            val gMap: MutableMap<Long, GroupDO> = HashMap()
            val ugIdMap: MutableMap<Long, MutableSet<Long>> = HashMap()
            val nAdminUsers: MutableSet<Long> = HashSet()
            val nFinanceUser: MutableSet<Long> = HashSet()
            val nControllingUsers: MutableSet<Long> = HashSet()
            val nProjectManagers: MutableSet<Long> = HashSet()
            val nProjectAssistants: MutableSet<Long> = HashSet()
            val nMarketingUsers: MutableSet<Long> = HashSet()
            val nOrgaUsers: MutableSet<Long> = HashSet()
            val nhrUsers: MutableSet<Long> = HashSet()
            for (group in groups) {
                val groupId = group.id ?: continue
                gMap[groupId] = group
                group.assignedUsers?.forEach { user ->
                    val userId = user.id ?: return@forEach
                    val groupIdSet = ensureAndGetUserGroupIdMap(ugIdMap, userId)
                    groupIdSet.add(groupId)
                    when {
                        ProjectForgeGroup.ADMIN_GROUP.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as administrator.")
                            nAdminUsers.add(userId)
                        }

                        ProjectForgeGroup.FINANCE_GROUP.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' for finance.")
                            nFinanceUser.add(userId)
                        }

                        ProjectForgeGroup.CONTROLLING_GROUP.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' for controlling.")
                            nControllingUsers.add(userId)
                        }

                        ProjectForgeGroup.PROJECT_MANAGER.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as project manager.")
                            nProjectManagers.add(userId)
                        }

                        ProjectForgeGroup.PROJECT_ASSISTANT.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as project assistant.")
                            nProjectAssistants.add(userId)
                        }

                        ProjectForgeGroup.MARKETING_GROUP.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as marketing user.")
                            nMarketingUsers.add(userId)
                        }

                        ProjectForgeGroup.ORGA_TEAM.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as orga user.")
                            nOrgaUsers.add(userId)
                        }

                        ProjectForgeGroup.HR_GROUP.matches(group.name) -> {
                            log.debug("Adding user '" + user.username + "' as hr user.")
                            nhrUsers.add(userId)
                        }
                    }
                }
            }
            userMap = uMap
            groupMap = gMap
            val nEmployeeMap = mutableMapOf<Long?, EmployeeDO>()
            employeeDao.internalLoadAll(context).forEach { employeeDO ->
                nEmployeeMap[employeeDO.userId] = employeeDO
                employeeDao.setEmployeeStatus(employeeDO)
            }
            employeeMap = nEmployeeMap
            adminUsers = nAdminUsers
            financeUsers = nFinanceUser
            controllingUsers = nControllingUsers
            projectManagers = nProjectManagers
            projectAssistants = nProjectAssistants
            marketingUsers = nMarketingUsers
            orgaUsers = nOrgaUsers
            hrUsers = nhrUsers
            userGroupIdMap = ugIdMap
            val rMap: MutableMap<Long, List<UserRightDO>> = HashMap()
            val rights: List<UserRightDO>
            rights = try {
                userRightDao.internalGetAllOrdered(context)
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
            rightMap = rMap
            log.info("Initializing of UserGroupCache done. Found ${uMap.size} entries.")
            Login.getInstance().afterUserGroupCacheRefresh(users, groups)
        }
        val end = System.currentTimeMillis()
        log.info("UserGroupCache.refresh took: " + (end - begin) + " ms.")
        Thread {
            jobHandler.checkStatus()
        }.start()
    }

    @Synchronized
    fun internalSetAdminUser(adminUser: PFUserDO) {
        checkRefresh()
        adminUser.id?.let {
            adminUsers!!.add(it)
        }
    }

    companion object {
        private const val serialVersionUID = -6501106088529363341L
        private var INSTANCE: UserGroupCache? = null

        @JvmStatic
        fun getInstance(): UserGroupCache {
            return INSTANCE!!
        }

        private fun ensureAndGetUserGroupIdMap(
            ugIdMap: MutableMap<Long, MutableSet<Long>>,
            userId: Long
        ): MutableSet<Long> {
            var set = ugIdMap[userId]
            if (set == null) {
                set = HashSet()
                ugIdMap[userId] = set
            }
            return set
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
