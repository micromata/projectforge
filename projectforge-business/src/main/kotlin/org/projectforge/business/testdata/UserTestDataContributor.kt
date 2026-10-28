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


package org.projectforge.business.testdata

import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserPasswordDao
import org.projectforge.business.user.UserRightDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.database.DatabaseService
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Locale
import java.util.TimeZone

/**
 * Configuration, the users of "Example Consulting", their groups and rights.
 */
@Service
open class UserTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userPasswordDao: UserPasswordDao

    @Autowired
    private lateinit var userRightDao: UserRightDao

    override val order = 10

    private class User(
        val username: String,
        val firstname: String,
        val lastname: String,
        val description: String,
        val groups: List<ProjectForgeGroup> = emptyList(),
        val rights: Map<UserRightId, UserRightValue> = emptyMap(),
    )

    override fun seed(ctx: TestDataContext) {
        configure()
        USERS.forEach { u ->
            val user = PFUserDO().also {
                it.username = u.username
                it.firstname = u.firstname
                it.lastname = u.lastname
                it.description = u.description
                it.email = "${u.firstname.lowercase()}.${u.lastname.lowercase()}@example.com"
                it.organization = ORGANIZATION
                it.locale = Locale.ENGLISH
                it.timeZone = TimeZone.getTimeZone(ctx.zone)
                it.localUser = true
            }
            userDao.insert(user, checkAccess = false)
            ctx.users[u.username] = user
            u.rights.forEach { (rightId, value) ->
                userRightDao.insert(UserRightDO(user, rightId, value), checkAccess = false)
            }
            ctx.password?.let { userPasswordDao.encryptAndSavePassword(user.id!!, it.toCharArray(), false) }
            val groupIds = u.groups.mapNotNull { userGroupCache.getGroup(it)?.id }
            if (groupIds.isNotEmpty()) {
                groupDao.assignGroupByIds(user, groupIds, null, false)
            }
        }
        // With the administrator: members of the same group see each other's vacations and availability entries, so
        // the administrator's calendar, vacation list and team vacation calendar show those of the team.
        addGroup(
            ctx, ORGANIZATION, "All employees and the administrator",
            USERS.map { it.username } + DatabaseService.DEFAULT_ADMIN_USER,
        )
        addGroup(ctx, "Delivery leads", "Project leads with access to all projects", listOf("laura", "joe"))
    }

    private fun addGroup(ctx: TestDataContext, name: String, description: String, usernames: List<String>) {
        val group = GroupDO().also {
            it.name = name
            it.description = description
            it.organization = ORGANIZATION
            it.localGroup = true
            it.assignedUsers = usernames.map { username -> ctx.user(username) }.toMutableSet()
        }
        groupDao.insert(group, checkAccess = false)
        ctx.groups[name] = group
    }

    private fun configure() {
        configurationDao.checkAndUpdateDatabaseEntries()
        configurationDao.getEntry(ConfigurationParam.ORGANIZATION)?.let {
            it.stringValue = ORGANIZATION
            configurationDao.update(it, checkAccess = false)
        }
        configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)?.let {
            it.booleanValue = true
            configurationDao.update(it, checkAccess = false)
        }
        configurationDao.getEntry(ConfigurationParam.FIBU_DEFAULT_VAT)?.let {
            it.floatValue = BigDecimal("0.19")
            configurationDao.update(it, checkAccess = false)
        }
    }

    companion object {
        const val ORGANIZATION = "Example Consulting"

        /** Every right a user can hold, at its widest value. */
        private val ALL_RIGHTS = mapOf(
            UserRightId.FIBU_AUSGANGSRECHNUNGEN to UserRightValue.READWRITE,
            UserRightId.FIBU_EINGANGSRECHNUNGEN to UserRightValue.READWRITE,
            UserRightId.FIBU_COST_UNIT to UserRightValue.READWRITE,
            UserRightId.FIBU_ACCOUNTS to UserRightValue.READWRITE,
            UserRightId.FIBU_CURRENCY_CONVERSION to UserRightValue.READWRITE,
            UserRightId.FIBU_CUSTOMER to UserRightValue.READWRITE,
            UserRightId.FIBU_DATEV_IMPORT to UserRightValue.TRUE,
            UserRightId.ORGA_CONTRACTS to UserRightValue.READWRITE,
            UserRightId.ORGA_INCOMING_MAIL to UserRightValue.READWRITE,
            UserRightId.ORGA_OUTGOING_MAIL to UserRightValue.READWRITE,
            UserRightId.ORGA_VISITORBOOK to UserRightValue.READWRITE,
            UserRightId.PM_ORDER_BOOK to UserRightValue.READWRITE,
            UserRightId.PM_PROJECT to UserRightValue.READWRITE,
            UserRightId.PM_HR_PLANNING to UserRightValue.READWRITE,
            UserRightId.HR_EMPLOYEE to UserRightValue.READWRITE,
            UserRightId.HR_EMPLOYEE_SALARY to UserRightValue.READWRITE,
            UserRightId.HR_VACATION to UserRightValue.READWRITE,
        )

        private val PROJECT_MANAGER_RIGHTS = mapOf(
            UserRightId.PM_ORDER_BOOK to UserRightValue.READWRITE,
            UserRightId.PM_PROJECT to UserRightValue.READWRITE,
            UserRightId.PM_HR_PLANNING to UserRightValue.READWRITE,
        )

        private val FINANCE_RIGHTS = mapOf(
            UserRightId.FIBU_AUSGANGSRECHNUNGEN to UserRightValue.READWRITE,
            UserRightId.FIBU_EINGANGSRECHNUNGEN to UserRightValue.READWRITE,
            UserRightId.FIBU_COST_UNIT to UserRightValue.READWRITE,
            UserRightId.FIBU_ACCOUNTS to UserRightValue.READWRITE,
            UserRightId.FIBU_CUSTOMER to UserRightValue.READWRITE,
            UserRightId.FIBU_DATEV_IMPORT to UserRightValue.TRUE,
            UserRightId.PM_ORDER_BOOK to UserRightValue.READWRITE,
            UserRightId.PM_PROJECT to UserRightValue.READWRITE,
        )

        /** Laura is the user of the website screenshots: she sees everything. */
        private val USERS = listOf(
            User("laura", "Laura", "Becker", "Head of Delivery", ProjectForgeGroup.entries, ALL_RIGHTS),
            User(
                "joe", "Joe", "Stone", "Project Manager",
                listOf(ProjectForgeGroup.PROJECT_MANAGER), PROJECT_MANAGER_RIGHTS,
            ),
            User(
                "michael", "Michael", "Evers", "Finance",
                listOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP), FINANCE_RIGHTS,
            ),
            User("julia", "Julia", "White", "Consultant", listOf(ProjectForgeGroup.PROJECT_ASSISTANT)),
            User("alex", "Alex", "Peters", "Senior Developer"),
            User("ann", "Ann", "Ville", "UX Designer"),
            User("chris", "Chris", "Clark", "Developer"),
            User("max", "Max", "Bach", "Developer"),
            User("mona", "Mona", "Nike", "Developer"),
        )

        /** The users with an employee record and time sheets, by position. */
        val EMPLOYEES = USERS.map { it.username to it.description }
    }
}
