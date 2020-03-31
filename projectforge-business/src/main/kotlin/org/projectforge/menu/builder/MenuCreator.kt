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

package org.projectforge.menu.builder

import mu.KotlinLogging
import org.projectforge.Const
import org.projectforge.SystemStatus
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.datev.DatevImportDao
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.business.login.Login
import org.projectforge.business.meb.MebDao
import org.projectforge.business.orga.ContractDao
import org.projectforge.business.orga.PostausgangDao
import org.projectforge.business.orga.PosteingangDao
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.service.VacationMenuCounterCache
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.api.UserRightService.*
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * The menu creator contain all menu entries and provides the method [build] for building the user's customized menu.
 */
@Component
class MenuCreator {

    internal class MenuItemDefHolder {
        internal val menuItems: MutableList<MenuItemDef> = mutableListOf()
        fun add(menuItem: MenuItemDef): MenuItemDef {
            menuItems.add(menuItem)
            return menuItem
        }
    }

    private val menuItemDefHolder = MenuItemDefHolder()

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var vacationService: VacationService

    @Autowired
    private lateinit var vacationMenuCounterCache: VacationMenuCounterCache

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var mebDao: MebDao

    private var initialized = false

    companion object {
        const val REACT_PREFIX: String = Const.REACT_APP_PATH

        /**
         * If test cases fails, try to set testCase to true.
         */
        var testCase = false
    }

    fun refresh() {
        initialized = false
        initialize()
    }

    /**
     * Registers menu entry definition. It's important that a parent menu entry item definition is registered before its
     * sub menu entry items.
     *
     * @param menuItemDef
     * @return this for chaining.
     */
    fun addTopLevelMenu(menuItemDef: MenuItemDef) {
        initialize()
        // Check if ID already exists
        menuItemDefHolder.menuItems.forEach {
            if (it.id == menuItemDef.id)
                throw IllegalArgumentException(("Duplicated menu ID '${menuItemDef.id}' for entry '${menuItemDef.i18nKey}'"))
        }
        menuItemDefHolder.add(menuItemDef)
    }

    /**
     * Registers menu entry definition. It's important that a parent menu entry item definition is registered before its
     * sub menu entry items.
     *
     * @param menuItemDef
     * @return this for chaining.
     */
    fun add(parentId: String, menuItemDef: MenuItemDef): MenuItemDef {
        val parent = findById(parentId)
                ?: throw java.lang.IllegalArgumentException("Can't append menu '${menuItemDef.id}' to parent '$parentId'. Parent not found.")
        return add(parent, menuItemDef)
    }

    /**
     * Registers menu entry definition. It's important that a parent menu entry item definition is registered before its
     * sub menu entry items.
     *
     * @param menuItemDef
     * @return this for chaining.
     */
    fun add(parentId: MenuItemDefId, menuItemDef: MenuItemDef): MenuItemDef {
        val parent = findById(parentId)
                ?: throw java.lang.IllegalArgumentException("Can't append menu '${menuItemDef.id}' to parent '$parentId'. Parent not found.")
        return add(parent, menuItemDef)
    }

    /**
     * Registers menu entry definition. It's important that a parent menu entry item definition is registered before its
     * sub menu entry items.
     *
     * @param menuItemDef
     * @return this for chaining.
     */
    fun add(parent: MenuItemDef, menuItemDef: MenuItemDef): MenuItemDef {
        if (findById(parent, menuItemDef.id) != null) {
            throw IllegalArgumentException(("Duplicated menu ID '${menuItemDef.id}' for entry '${menuItemDef.i18nKey}'"))
        }
        parent.add(menuItemDef)
        return menuItemDef
    }

    fun findById(menuItemDefId: MenuItemDefId): MenuItemDef? {
        return findById(menuItemDefId.id)
    }

    fun findById(id: String): MenuItemDef? {
        initialize()
        menuItemDefHolder.menuItems.forEach {
            if (it.id == id)
                return it
            val menuItemDef = findById(it, id)
            if (menuItemDef != null)
                return menuItemDef
        }
        return null
    }

    private fun findById(parent: MenuItemDef, id: String): MenuItemDef? {
        parent.children?.forEach {
            if (it.id == id)
                return it
            val menuItemDef = findById(it, id)
            if (menuItemDef != null)
                return menuItemDef
        }
        return null
    }

    @Synchronized
    private fun initialize() {
        if (initialized)
            return
        initialized = true
        if (!this::configurationService.isInitialized) {
            if (testCase) {
                menuItemDefHolder.add(MenuItemDef(MenuItemDefId.COMMON))
                return // This should only occur in test cases.
            }
            log.error("Oups, shouldn't occur. Spring bean not correctly initialized.")
        }
        //////////////////////////////////////
        //
        // COMMON
        //
        val commonMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.COMMON))
                .add(MenuItemDef(MenuItemDefId.CALENDAR))
                .add(MenuItemDef(MenuItemDefId.TEAMCALENDAR))
                .add(MenuItemDef(MenuItemDefId.VACATION,
                        badgeCounter = { vacationMenuCounterCache.getOpenLeaveApplicationsForUser(ThreadLocalUserContext.getUser()) }))
                .add(MenuItemDef(MenuItemDefId.BOOK_LIST))
                .add(MenuItemDef(MenuItemDefId.ADDRESSBOOK_LIST))
                .add(MenuItemDef(MenuItemDefId.ADDRESS_LIST))
        if (configurationService.telephoneSystemUrl.isNotEmpty())
            commonMenu.add(MenuItemDef(MenuItemDefId.PHONE_CALL))
        if (smsSenderConfig.isSmsConfigured())
            commonMenu.add(MenuItemDef(MenuItemDefId.SEND_SMS))
        if (Configuration.getInstance().isMebConfigured)
            commonMenu.add(MenuItemDef(MenuItemDefId.MEB,
                    badgeCounter = { mebDao.getRecentMEBEntries(null) })) // MenuNewCounterMeb
        commonMenu.add(MenuItemDef(MenuItemDefId.SEARCH))

        //////////////////////////////////////
        //
        // Project management
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.PROJECT_MANAGEMENT))
                .add(MenuItemDef(MenuItemDefId.TASK_TREE))
                .add(MenuItemDef(MenuItemDefId.TIMESHEET_LIST))
                .add(MenuItemDef(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT))
                .add(MenuItemDef(MenuItemDefId.PERSONAL_STATISTICS))
                .add(MenuItemDef(MenuItemDefId.HR_VIEW,
                        requiredUserRightId = HRPlanningDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.HR_PLANNING_LIST))
                .add(MenuItemDef(MenuItemDefId.GANTT))
                .add(MenuItemDef(MenuItemDefId.ORDER_LIST,
                        checkAccess =
                        {
                            hasRight(AuftragDao.USER_RIGHT_ID, *READONLY_PARTLYREADWRITE_READWRITE) &&
                                    !isInGroup(*FIBU_ORGA_GROUPS) // Orderbook is shown under menu FiBu for FiBu users
                        },
                        badgeCounter = {
                            if (isInGroup(*FIBU_ORGA_GROUPS))
                                auftragDao.abgeschlossenNichtFakturiertAnzahl
                            else
                                0
                        }))

        //////////////////////////////////////
        //
        // Human resources
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.HR,
                        checkAccess =
                        { isInGroup(ProjectForgeGroup.HR_GROUP) }))
                .add(MenuItemDef(MenuItemDefId.EMPLOYEE_LIST,
                        requiredUserRightId = EmployeeDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.EMPLOYEE_SALARY_LIST,
                        requiredUserRightId = EmployeeSalaryDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.EMPLOYEE_LEAVE_ACCOUNT_ENTRIES,
                        requiredUserRightId = EmployeeDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))

        //////////////////////////////////////
        //
        // Financial and administrative
        //
        val fibuMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.FIBU,
                        checkAccess = { isInGroup(*FIBU_ORGA_GROUPS) }))
                .add(MenuItemDef(MenuItemDefId.OUTGOING_INVOICE_LIST,
                        checkAccess = {
                            hasRight(RechnungDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.INCOMING_INVOICE_LIST,
                        checkAccess = {
                            hasRight(EingangsrechnungDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
        if (Configuration.getInstance().isCostConfigured) {
            fibuMenu.add(MenuItemDef(MenuItemDefId.CUSTOMER_LIST,
                            checkAccess = { isInGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP) }))
                    .add(MenuItemDef(MenuItemDefId.PROJECT_LIST,
                            checkAccess = {
                                hasRight(ProjektDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                        isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                            }))
        }
        // MenuNewCounterOrder, tooltip = "menu.fibu.orderbook.htmlSuffixTooltip"
        fibuMenu.add(MenuItemDef(MenuItemDefId.ORDER_LIST,
                requiredGroups = *FIBU_ORGA_GROUPS,
                badgeCounter =
                { auftragDao.abgeschlossenNichtFakturiertAnzahl }))

        //////////////////////////////////////
        //
        // COST
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.COST, requiredGroups = *FIBU_ORGA_HR_GROUPS,
                        checkAccess =
                        { Configuration.getInstance().isCostConfigured }))
                .add(MenuItemDef(MenuItemDefId.ACCOUNT_LIST,
                        checkAccess =
                        {
                            hasRight(KontoDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.COST1_LIST,
                        checkAccess =
                        {
                            hasRight(Kost1Dao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.COST2_LIST,
                        checkAccess =
                        {
                            hasRight(Kost2Dao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.COST2_TYPE_LIST,
                        checkAccess =
                        {
                            hasRight(Kost2Dao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))

        //////////////////////////////////////
        //
        // REPORTING
        //
        val reportingMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.REPORTING,
                        checkAccess = {
                            isInGroup(*FIBU_ORGA_HR_GROUPS)
                        }))
                .add(MenuItemDef(MenuItemDefId.SCRIPT_LIST,
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
                .add(MenuItemDef(MenuItemDefId.SCRIPTING,
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
                .add(MenuItemDef(MenuItemDefId.REPORT_OBJECTIVES,
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
        // Only visible if cost is configured:
        reportingMenu.add(MenuItemDef(MenuItemDefId.ACCOUNTING_RECORD_LIST,
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
                        checkAccess =
                        { Configuration.getInstance().isCostConfigured }))
                .add(MenuItemDef(MenuItemDefId.DATEV_IMPORT,
                        requiredUserRightId = DatevImportDao.USER_RIGHT_ID, requiredUserRightValues = arrayOf(UserRightValue.TRUE),
                        checkAccess =
                        { Configuration.getInstance().isCostConfigured }))

        //////////////////////////////////////
        //
        // ORGA
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.ORGA,
                        requiredGroups = *FIBU_ORGA_HR_GROUPS))
                .add(MenuItemDef(MenuItemDefId.OUTBOX_LIST,
                        requiredUserRightId = PostausgangDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.INBOX_LIST,
                        requiredUserRightId = PosteingangDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.CONTRACTS,
                        requiredUserRightId = ContractDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.VISITORBOOK,
                        requiredUserRightId = VisitorbookDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))

        //////////////////////////////////////
        //
        // ADMINISTRATION
        //
        val adminMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.ADMINISTRATION, visibleForRestrictedUsers = true))
        if (SystemStatus.isDevelopmentMode()) {
            log.warn("********** React version of my account is only available in development mode.")
            adminMenu.add(MenuItemDef("MY_ACCOUNT", "menu.myAccount", "${REACT_PREFIX}myAccount/dynamic"))
        } else {
            adminMenu.add(MenuItemDef(MenuItemDefId.MY_ACCOUNT))
        }
        adminMenu
                .add(MenuItemDef(MenuItemDefId.MY_PREFERENCES))
                .add(MenuItemDef(MenuItemDefId.VACATION_ACCOUNT,
                        checkAccess =
                        {
                            vacationService.hasAccessToVacationService(ThreadLocalUserContext.getUser(), false)
                        }))
                .add(MenuItemDef(MenuItemDefId.CHANGE_PASSWORD,
                        checkAccess =
                        {
                            // The visibility of this menu entry is evaluated by the login handler implementation.
                            Login.getInstance().isPasswordChangeSupported(ThreadLocalUserContext.getUser())
                        }))
                .add(MenuItemDef(MenuItemDefId.CHANGE_WLAN_PASSWORD,
                        checkAccess =
                        {
                            // The visibility of this menu entry is evaluated by the login handler implementation.
                            Login.getInstance().isWlanPasswordChangeSupported(ThreadLocalUserContext.getUser())
                        }))
                .add(MenuItemDef(MenuItemDefId.USER_LIST)) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.GROUP_LIST)) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.ACCESS_LIST)) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.SYSTEM, requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))

        if (configurationService.securityConfig?.isSqlConsoleAvailable == true) {
            // Only available in development mode or if SQL console is configured in SecurityConfig.
            adminMenu.add(MenuItemDef(MenuItemDefId.SQL_CONSOLE,
                            requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                    .add(MenuItemDef(MenuItemDefId.GROOVY_CONSOLE,
                            requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                    .add(MenuItemDef(MenuItemDefId.LUCENE_CONSOLE,
                            requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
        }
        adminMenu.add(MenuItemDef(MenuItemDefId.SYSTEM_STATISTICS)) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.CONFIGURATION, requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                .add(MenuItemDef(MenuItemDefId.PLUGIN_ADMIN,
                        requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))


        //////////////////////////////////////
        //
        // MISC
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.MISC))
    }

    /**
     * Builds the user's menu.
     */
    fun build(menuCreatorContext: MenuCreatorContext): Menu {
        initialize()
        val menu = Menu()
        menuItemDefHolder.menuItems.forEach { menuItemDef ->
            menu.add(build(null, menuItemDef, menuCreatorContext))
        }
        menu.postProcess()
        return menu
    }

    private fun build(parent: MenuItem?, menuItemDef: MenuItemDef, menuCreatorContext: MenuCreatorContext): MenuItem? {
        if (!checkAccess(menuCreatorContext, menuItemDef))
            return null // No access
        val menuItem = menuItemDef.createMenu(parent, menuCreatorContext)

        parent?.add(menuItem)
        menuItemDef.children?.forEach { childMenuItemDef ->
            build(menuItem, childMenuItemDef, menuCreatorContext)
        }
        return menuItem
    }

    private fun checkAccess(menuBuilderContext: MenuCreatorContext, menuItemDef: MenuItemDef): Boolean {
        if (menuItemDef.checkAccess?.invoke() == false)
            return false
        if (accessChecker.isRestrictedUser && !menuItemDef.visibleForRestrictedUsers)
            return false
        if (!menuItemDef.requiredGroups.isNullOrEmpty() && !isInGroup(menuBuilderContext, menuItemDef.requiredGroups!!)) {
            return false
        }
        var userRightId = menuItemDef.requiredUserRightId
        if (userRightId == null && menuItemDef.requiredUserRight != null)
            userRightId = menuItemDef.requiredUserRight?.id
        if (userRightId != null && !hasRight(menuBuilderContext, userRightId, menuItemDef.requiredUserRightValues)) {
            return false
        }
        return true
    }

    private fun hasRight(menuBuilderContext: MenuCreatorContext, rightId: IUserRightId, values: Array<UserRightValue>?): Boolean {
        if (values.isNullOrEmpty()) {
            log.warn("Can't check user right '$rightId' against null values.")
            return false
        }
        return accessChecker.hasRight(menuBuilderContext.user, rightId, false, *values)
    }

    private fun hasRight(rightId: IUserRightId, vararg values: UserRightValue): Boolean {
        return accessChecker.hasLoggedInUserRight(rightId, false, *values)
    }

    private fun isInGroup(menuBuilderContext: MenuCreatorContext, groups: Array<ProjectForgeGroup>): Boolean {
        return accessChecker.isUserMemberOfGroup(menuBuilderContext.user, *groups)
    }

    private fun isInGroup(vararg groups: ProjectForgeGroup): Boolean {
        return accessChecker.isLoggedInUserMemberOfGroup(*groups)
    }
}
