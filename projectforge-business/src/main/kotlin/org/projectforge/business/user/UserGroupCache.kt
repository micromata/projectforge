/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.login.Login
import org.projectforge.business.multitenancy.TenantChecker
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.business.multitenancy.TenantService
import org.projectforge.business.user.filter.UserFilter
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.PFUserDO.Companion.createCopyWithoutSecretFields
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.util.*

/**
 * The group user relations will be cached with this class.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
// Open for mocking in test cases.
open class UserGroupCache(tenant: TenantDO?, applicationContext: ApplicationContext) : AbstractCache() {
    /**
     * @return the tenant
     */
    val tenant: TenantDO?
    /**
     * The key is the user id and the value is a list of assigned groups.
     */
    private var userGroupIdMap: Map<Int, MutableSet<Int>>? = null
    /**
     * The key is the group id.
     */
    private var groupMap: Map<Int, GroupDO>? = null
    /**
     * List of all rights (value) defined for the user ids (key).
     */
    private var rightMap: Map<Int, List<UserRightDO>>? = null
    private var userMap: MutableMap<Int, PFUserDO?>? = null
    /**
     * Key is user id, value is employee id.
     */
    private var employeeMap: MutableMap<Int?, Int>? = null
    private var adminUsers: MutableSet<Int>? = null
    private var financeUsers: Set<Int>? = null
    private var controllingUsers: Set<Int>? = null
    private var projectManagers: Set<Int>? = null
    private var projectAssistants: Set<Int>? = null
    private var marketingUsers: Set<Int>? = null
    private var orgaUsers: Set<Int>? = null
    private var hrUsers: Set<Int>? = null
    private val tenantChecker: TenantChecker
    private val tenantService: TenantService
    private val userRights: UserRightService
    private val userRightDao: UserRightDao
    private val employeeDao: EmployeeDao

    fun getGroup(group: ProjectForgeGroup): GroupDO? {
        checkRefresh()
        return groupMap?.values?.find { group.matches(it.name) }
    }

    fun getGroup(groupId: Int?): GroupDO? {
        checkRefresh()
        return groupMap!![groupId]
    }

    fun getUser(userId: Int?): PFUserDO? {
        if (userId == null) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.let { it[userId] } // Only null in maintenance mode (if t_user isn't readable).
        user?.clearSecretFields()
        return user
    }

    fun getUser(username: String): PFUserDO? {
        if (username.isBlank()) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.values?.find { username == it?.username }
        user?.clearSecretFields()
        return user
    }

    fun getUserByFullname(fullname: String): PFUserDO? {
        if (fullname.isBlank()) {
            return null
        }
        // checkRefresh(); Done by getUserMap().
        val user = getUserMap()?.values?.find { fullname == it?.getFullname() }
        user?.clearSecretFields()
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

    fun getUsername(userId: Int): String? { // checkRefresh(); Done by getUserMap().
        val user = getUserMap()!![userId] ?: return userId.toString()
        return user.username
    }

    /**
     * Check for current logged in user.
     */
    fun isLoggedInUserMemberOfGroup(groupId: Int?): Boolean {
        return isUserMemberOfGroup(ThreadLocalUserContext.getUserId(), groupId)
    }

    fun isUserMemberOfGroup(user: PFUserDO?, groupId: Int?): Boolean {
        return if (user == null) {
            false
        } else isUserMemberOfGroup(user.id, groupId)
    }

    fun isUserMemberOfGroup(userId: Int?, groupId: Int?): Boolean {
        if (groupId == null) {
            return false
        }
        checkRefresh()
        val groupSet: Set<Int>? = getUserGroupIdMap()!![userId]
        return groupSet != null && groupSet.contains(groupId)
    }

    fun isUserMemberOfAtLeastOneGroup(userId: Int?, vararg groupIds: Int?): Boolean {
        if (groupIds.isEmpty()) {
            return false
        }
        checkRefresh()
        val groupSet = getUserGroupIdMap()?.let { it[userId] } ?: return false
        return groupIds.any { groupSet.contains(it) }
    }

    val isUserMemberOfAdminGroup: Boolean
        get() = isUserMemberOfAdminGroup(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfAdminGroup(userId: Int?): Boolean {
        checkRefresh()
        // adminUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return adminUsers != null && adminUsers!!.contains(userId)
    }

    val isUserMemberOfFinanceGroup: Boolean
        get() = isUserMemberOfFinanceGroup(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfFinanceGroup(userId: Int?): Boolean {
        checkRefresh()
        // financeUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return financeUsers != null && financeUsers!!.contains(userId)
    }

    val isUserMemberOfProjectManagers: Boolean
        get() = isUserMemberOfProjectManagers(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfProjectManagers(userId: Int?): Boolean {
        checkRefresh()
        // projectManagers should only be null in maintenance mode (e. g. if user table isn't readable).
        return projectManagers != null && projectManagers!!.contains(userId)
    }

    val isUserMemberOfProjectAssistant: Boolean
        get() = isUserMemberOfProjectAssistant(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfProjectAssistant(userId: Int?): Boolean {
        checkRefresh()
        // projectAssistants should only be null in maintenance mode (e. g. if user table isn't readable).
        return projectAssistants != null && projectAssistants!!.contains(userId)
    }

    fun isUserProjectManagerOrAssistantForProject(projekt: ProjektDO?): Boolean {
        if (projekt?.projektManagerGroupId == null) {
            return false
        }
        val userId = ThreadLocalUserContext.getUserId()
        return if (!isUserMemberOfProjectAssistant(userId) && !isUserMemberOfProjectManagers(userId)) {
            false
        } else isUserMemberOfGroup(userId, projekt.projektManagerGroupId)
    }

    val isUserMemberOfControllingGroup: Boolean
        get() = isUserMemberOfControllingGroup(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfControllingGroup(userId: Int?): Boolean {
        checkRefresh()
        // controllingUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return controllingUsers != null && controllingUsers!!.contains(userId)
    }

    val isUserMemberOfMarketingGroup: Boolean
        get() = isUserMemberOfMarketingGroup(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfMarketingGroup(userId: Int?): Boolean {
        checkRefresh()
        return marketingUsers!!.contains(userId)
    }

    val isUserMemberOfOrgaGroup: Boolean
        get() = isUserMemberOfOrgaGroup(ThreadLocalUserContext.getUserId())

    fun isUserMemberOfOrgaGroup(userId: Int?): Boolean {
        checkRefresh()
        // orgaUsers should only be null in maintenance mode (e. g. if user table isn't readable).
        return orgaUsers != null && orgaUsers!!.contains(userId)
    }

    fun isUserMemberOfHRGroup(userId: Int?): Boolean {
        checkRefresh()
        return hrUsers != null && hrUsers!!.contains(userId)
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

    fun getUserRights(userId: Int?): List<UserRightDO>? {
        return userRightMap!![userId]
    }

    fun getUserRight(userId: Int?, rightId: UserRightId): UserRightDO? {
        val rights = getUserRights(userId) ?: return null
        return rights.find { it.rightIdString == rightId.id }
    }

    private val userRightMap: Map<Int, List<UserRightDO>>?
        get() {
            checkRefresh()
            return rightMap
        }

    /**
     * Returns a collection of group id's to which the user is assigned to.
     *
     * @return collection if found, otherwise null.
     */
    fun getUserGroups(user: PFUserDO): Collection<Int>? {
        checkRefresh()
        return getUserGroupIdMap()!![user.id]
    }

    private fun getGroupMap(): Map<Int, GroupDO>? {
        checkRefresh()
        return groupMap
    }

    fun getUserGroupIdMap(): Map<Int, MutableSet<Int>>? {
        checkRefresh()
        return userGroupIdMap
    }

    fun getEmployeeId(userId: Int?): Int? {
        userId ?: return null
        checkRefresh()
        var employeeId = employeeMap!![userId]
        if (employeeId == null) {
            employeeId = employeeDao.getEmployeeIdByByUserId(userId, tenant?.tenantId)
            if (employeeId == null) {
                employeeMap!![userId] = Int.MIN_VALUE
                return null
            }
            employeeMap!![userId] = employeeId
        } else if (employeeId == Int.MIN_VALUE) {
            return null
        }
        return employeeId
    }

    /**
     * Should be called after user modifications.
     */
    fun updateUser(user: PFUserDO) {
        user.clearSecretFields()
        getUserMap()!![user.id] = user
    }

    private fun getUserMap(): MutableMap<Int, PFUserDO?>? {
        checkRefresh()
        return userMap
    }

    /**
     * This method will be called by CacheHelper and is synchronized.
     */
    override fun refresh() {
        val begin = System.currentTimeMillis()
        var tenantLog = ""
        if (tenant != null) {
            tenantLog = " for tenant " + tenantService.getLogName(tenant)
        }
        log.info("Initializing UserGroupCache $tenantLog...")
        // This method must not be synchronized because it works with a new copy of maps.
        val uMap: MutableMap<Int, PFUserDO?> = HashMap()
        // Could not autowire UserDao because of cyclic reference with AccessChecker.
        log.info("Loading all users ...")
        val users = Login.getInstance().allUsers
        for (user in users) {
            user.id ?: continue // Should only occur in test cases.
            if (tenant != null) {
                if (!tenantChecker.isPartOfTenant(tenant, user)) { // Ignore users not assigned to current tenant.
                    log.warn("********** User ignored, it seems not to be part of the current tenant: $user")
                    continue
                }
            }
            val copiedUser = createCopyWithoutSecretFields(user)
            uMap[user.id] = copiedUser
        }
        if (users.size != uMap.size) {
            log.warn("********** Load ${users.size} from the backend, but added only ${uMap.size} users to cache!")
            log.info("For debugging UserCache fuck-up: " + ToStringUtil.toJsonString(users))
            return
        }
        log.info("Loading all groups ...")
        val groups = Login.getInstance().allGroups
        val gMap: MutableMap<Int, GroupDO> = HashMap()
        val ugIdMap: MutableMap<Int, MutableSet<Int>> = HashMap()
        val nAdminUsers: MutableSet<Int> = HashSet()
        val nFinanceUser: MutableSet<Int> = HashSet()
        val nControllingUsers: MutableSet<Int> = HashSet()
        val nProjectManagers: MutableSet<Int> = HashSet()
        val nProjectAssistants: MutableSet<Int> = HashSet()
        val nMarketingUsers: MutableSet<Int> = HashSet()
        val nOrgaUsers: MutableSet<Int> = HashSet()
        val nhrUsers: MutableSet<Int> = HashSet()
        for (group in groups) {
            if (tenant != null) {
                if (!tenantChecker.isPartOfTenant(tenant.id, group)) { // Ignore groups not assigned to current tenant.
                    log.warn("********** Group ignored, it seems not to be part of the current tenant: $group")
                    continue
                }
            }
            gMap[group.id] = group
            group.assignedUsers?.forEach { user ->
                val groupIdSet = ensureAndGetUserGroupIdMap(ugIdMap, user.id)
                groupIdSet.add(group.id)
                when {
                    ProjectForgeGroup.ADMIN_GROUP.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as administrator.")
                        nAdminUsers.add(user.id)
                    }
                    ProjectForgeGroup.FINANCE_GROUP.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' for finance.")
                        nFinanceUser.add(user.id)
                    }
                    ProjectForgeGroup.CONTROLLING_GROUP.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' for controlling.")
                        nControllingUsers.add(user.id)
                    }
                    ProjectForgeGroup.PROJECT_MANAGER.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as project manager.")
                        nProjectManagers.add(user.id)
                    }
                    ProjectForgeGroup.PROJECT_ASSISTANT.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as project assistant.")
                        nProjectAssistants.add(user.id)
                    }
                    ProjectForgeGroup.MARKETING_GROUP.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as marketing user.")
                        nMarketingUsers.add(user.id)
                    }
                    ProjectForgeGroup.ORGA_TEAM.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as orga user.")
                        nOrgaUsers.add(user.id)
                    }
                    ProjectForgeGroup.HR_GROUP.matches(group.name) -> {
                        log.debug("Adding user '" + user.username + "' as hr user.")
                        nhrUsers.add(user.id)
                    }
                }
            }
        }
        userMap = uMap
        groupMap = gMap
        employeeMap = HashMap()
        adminUsers = nAdminUsers
        financeUsers = nFinanceUser
        controllingUsers = nControllingUsers
        projectManagers = nProjectManagers
        projectAssistants = nProjectAssistants
        marketingUsers = nMarketingUsers
        orgaUsers = nOrgaUsers
        hrUsers = nhrUsers
        userGroupIdMap = ugIdMap
        val rMap: MutableMap<Int, List<UserRightDO>> = HashMap()
        val rights: List<UserRightDO>
        rights = try {
            userRightDao.internalGetAllOrdered()
        } catch (ex: Exception) {
            log.error("******* Exception while getting user rights from data-base (only OK for migration from older versions): "
                    + ex.message,
                    ex)
            ArrayList()
        }
        var list: MutableList<UserRightDO>? = null
        var userId: Int? = null
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
                    && userRights.getRight(right.rightIdString).isAvailable(this, right.user)) {
                list!!.add(right)
            }
        }
        rightMap = rMap
        log.info("Initializing of UserGroupCache done$tenantLog. Found ${uMap.size} entries.")
        Login.getInstance().afterUserGroupCacheRefresh(users, groups)
        val end = System.currentTimeMillis()
        log.info("UserGroupCache.refresh took: " + (end - begin) + " ms.")
    }

    @Synchronized
    fun internalSetAdminUser(adminUser: PFUserDO) {
        check(UserFilter.isUpdateRequiredFirst()) { "Can't set admin user internally! This method is only available if system is under maintenance (update required first is true)!" }
        checkRefresh()
        adminUsers!!.add(adminUser.id)
    }

    companion object {
        @JvmStatic
        val tenantInstance: UserGroupCache
            get() = TenantRegistryMap.getInstance().tenantRegistry.userGroupCache

        private const val serialVersionUID = -6501106088529363341L
        private val log = LoggerFactory.getLogger(UserGroupCache::class.java)
        private fun ensureAndGetUserGroupIdMap(ugIdMap: MutableMap<Int, MutableSet<Int>>, userId: Int): MutableSet<Int> {
            var set = ugIdMap[userId]
            if (set == null) {
                set = HashSet()
                ugIdMap[userId] = set
            }
            return set
        }
    }

    init {
        setExpireTimeInHours(1)
        this.tenant = tenant
        tenantChecker = applicationContext.getBean(TenantChecker::class.java)
        userRights = applicationContext.getBean(UserRightService::class.java)
        userRightDao = applicationContext.getBean(UserRightDao::class.java)
        tenantService = applicationContext.getBean(TenantService::class.java)
        employeeDao = applicationContext.getBean(EmployeeDao::class.java)
    }
}
