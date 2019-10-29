/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.Const
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

/**
 * The menu creator contain all menu entries and provides the method [build] for building the user's customized menu.
 */
@Component
class MenuCreator {

    private val log = org.slf4j.LoggerFactory.getLogger(MenuCreator::class.java)

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
        // Check if ID already exists
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
                .add(MenuItemDef(MenuItemDefId.CALENDAR, "${REACT_PREFIX}calendar"))
                .add(MenuItemDef(MenuItemDefId.TEAMCALENDAR, "wa/wicket/bookmarkable/org.projectforge.web.teamcal.admin.TeamCalListPage")) // teamCal
                .add(MenuItemDef(MenuItemDefId.VACATION, "wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationListPage",
                        badgeCounter = { vacationService.getOpenLeaveApplicationsForUser(ThreadLocalUserContext.getUser()).toInt() }))
                .add(MenuItemDef(MenuItemDefId.BOOK_LIST, "${REACT_PREFIX}book"))
                .add(MenuItemDef(MenuItemDefId.ADDRESSBOOK_LIST, "${REACT_PREFIX}addressBook"))
                .add(MenuItemDef(MenuItemDefId.ADDRESS_LIST, "${REACT_PREFIX}address"))
        if (configurationService.telephoneSystemUrl.isNotEmpty())
            commonMenu.add(MenuItemDef(MenuItemDefId.PHONE_CALL, "wa/phoneCall"))
        if (smsSenderConfig.isSmsConfigured())
            commonMenu.add(MenuItemDef(MenuItemDefId.SEND_SMS, "wa/sendSms"))
        if (Configuration.getInstance().isMebConfigured)
            commonMenu.add(MenuItemDef(MenuItemDefId.MEB, "wa/mebList",
                    badgeCounter = { mebDao.getRecentMEBEntries(null) })) // MenuNewCounterMeb
        commonMenu.add(MenuItemDef(MenuItemDefId.SEARCH, "wa/search"))

        //////////////////////////////////////
        //
        // Project management
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.PROJECT_MANAGEMENT))
                .add(MenuItemDef(MenuItemDefId.TASK_TREE, "wa/taskTree"))
                .add(MenuItemDef(MenuItemDefId.TIMESHEET_LIST, "wa/timesheetList"))
                .add(MenuItemDef(MenuItemDefId.MONTHLY_EMPLOYEE_REPORT, "wa/monthlyEmployeeReport"))
                .add(MenuItemDef(MenuItemDefId.PERSONAL_STATISTICS, "wa/personalStatistics"))
                .add(MenuItemDef(MenuItemDefId.HR_VIEW, "wa/hrList",
                        requiredUserRightId = HRPlanningDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.HR_PLANNING_LIST, "wa/hrPlanningList"))
                .add(MenuItemDef(MenuItemDefId.GANTT, "wa/ganttList"))
                .add(MenuItemDef(MenuItemDefId.ORDER_LIST, "wa/orderBookList",
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
                .add(MenuItemDef(MenuItemDefId.EMPLOYEE_LIST, "wa/employeeList", // new: employee
                        requiredUserRightId = EmployeeDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.EMPLOYEE_SALARY_LIST, "wa/employeeSalaryList", // new: employeeSalary
                        requiredUserRightId = EmployeeSalaryDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))

        //////////////////////////////////////
        //
        // Financial and administrative
        //
        val fibuMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.FIBU,
                checkAccess = { isInGroup(*FIBU_ORGA_GROUPS) }))
                .add(MenuItemDef(MenuItemDefId.OUTGOING_INVOICE_LIST, "wa/outgoingInvoiceList",
                        checkAccess = {
                            hasRight(RechnungDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.INCOMING_INVOICE_LIST, "wa/incomingInvoiceList",
                        checkAccess = {
                            hasRight(EingangsrechnungDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
        if (Configuration.getInstance().isCostConfigured) {
            fibuMenu.add(MenuItemDef(MenuItemDefId.CUSTOMER_LIST, "wa/customerList", // new: customer
                    checkAccess = { isInGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP) }))
                    .add(MenuItemDef(MenuItemDefId.PROJECT_LIST, "wa/projectList",
                            checkAccess = {
                                hasRight(ProjektDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                        isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                            }))
        }
        // MenuNewCounterOrder, tooltip = "menu.fibu.orderbook.htmlSuffixTooltip"
        fibuMenu.add(MenuItemDef(MenuItemDefId.ORDER_LIST, "wa/orderBookList",
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
                .add(MenuItemDef(MenuItemDefId.ACCOUNT_LIST, "${REACT_PREFIX}konto",
                        checkAccess =
                        {
                            hasRight(KontoDao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.COST1_LIST, "${REACT_PREFIX}kost1",
                        checkAccess =
                        {
                            hasRight(Kost1Dao.USER_RIGHT_ID, *READONLY_READWRITE) ||
                                    isInGroup(ProjectForgeGroup.CONTROLLING_GROUP)
                        }))
                .add(MenuItemDef(MenuItemDefId.COST2_LIST, "wa/cost2List",
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
                .add(MenuItemDef(MenuItemDefId.SCRIPT_LIST, "wa/scriptList",
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
                .add(MenuItemDef(MenuItemDefId.SCRIPTING, "wa/scripting",
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
                .add(MenuItemDef(MenuItemDefId.REPORT_OBJECTIVES, "wa/reportObjectives",
                        requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)))
        // Only visible if cost is configured:
        reportingMenu.add(MenuItemDef(MenuItemDefId.ACCOUNTING_RECORD_LIST, "wa/accountingRecordList",
                requiredGroups = *arrayOf(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP),
                checkAccess =
                { Configuration.getInstance().isCostConfigured }))
                .add(MenuItemDef(MenuItemDefId.DATEV_IMPORT, "wa/datevImport",
                        requiredUserRightId = DatevImportDao.USER_RIGHT_ID, requiredUserRightValues = arrayOf(UserRightValue.TRUE),
                        checkAccess =
                        { Configuration.getInstance().isCostConfigured }))

        //////////////////////////////////////
        //
        // ORGA
        //
        menuItemDefHolder.add(MenuItemDef(MenuItemDefId.ORGA,
                requiredGroups = *FIBU_ORGA_HR_GROUPS))
                .add(MenuItemDef(MenuItemDefId.OUTBOX_LIST, "${REACT_PREFIX}outgoingMail",
                        requiredUserRightId = PostausgangDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.INBOX_LIST, "${REACT_PREFIX}incomingMail",
                        requiredUserRightId = PosteingangDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.CONTRACTS, "${REACT_PREFIX}contract",
                        requiredUserRightId = ContractDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))
                .add(MenuItemDef(MenuItemDefId.VISITORBOOK, "wa/wicket/bookmarkable/org.projectforge.web.orga.VisitorbookListPage",
                        requiredUserRightId = VisitorbookDao.USER_RIGHT_ID, requiredUserRightValues = READONLY_READWRITE))

        //////////////////////////////////////
        //
        // ADMINISTRATION
        //
        val adminMenu = menuItemDefHolder.add(MenuItemDef(MenuItemDefId.ADMINISTRATION, visibleForRestrictedUsers = true))
                .add(MenuItemDef(MenuItemDefId.MY_ACCOUNT, "wa/myAccount"))
                .add(MenuItemDef(MenuItemDefId.MY_PREFERENCES, "wa/userPrefList"))
                .add(MenuItemDef(MenuItemDefId.VACATION_VIEW, "wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationViewPage",
                        checkAccess =
                        {
                            vacationService.couldUserUseVacationService(ThreadLocalUserContext.getUser(), false)
                        }))
                .add(MenuItemDef(MenuItemDefId.CHANGE_PASSWORD, "wa/changePassword",
                        checkAccess =
                        {
                            // The visibility of this menu entry is evaluated by the login handler implementation.
                            Login.getInstance().isPasswordChangeSupported(ThreadLocalUserContext.getUser())
                        }))
                .add(MenuItemDef(MenuItemDefId.CHANGE_WLAN_PASSWORD, "wa/wicket/bookmarkable/org.projectforge.web.user.ChangeWlanPasswordPage",
                        checkAccess =
                        {
                            // The visibility of this menu entry is evaluated by the login handler implementation.
                            Login.getInstance().isWlanPasswordChangeSupported(ThreadLocalUserContext.getUser())
                        }))
                .add(MenuItemDef(MenuItemDefId.USER_LIST, "wa/userList")) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.GROUP_LIST, "wa/groupList")) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.ACCESS_LIST, "wa/accessList")) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.SYSTEM, "wa/admin", requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))

        if (configurationService.securityConfig?.isSqlConsoleAvailable == true) {
            // Only available in development mode or if SQL console is configured in SecurityConfig.
            adminMenu.add(MenuItemDef(MenuItemDefId.SQL_CONSOLE, "wa/wicket/bookmarkable/org.projectforge.web.admin.SqlConsolePage",
                    requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                    .add(MenuItemDef(MenuItemDefId.GROOVY_CONSOLE, "wa/wicket/bookmarkable/org.projectforge.web.admin.GroovyConsolePage",
                            requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                    .add(MenuItemDef(MenuItemDefId.LUCENE_CONSOLE, "wa/wicket/bookmarkable/org.projectforge.web.admin.LuceneConsolePage",
                            requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
        }
        adminMenu.add(MenuItemDef(MenuItemDefId.SYSTEM_UPDATE, "wa/systemUpdate", requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                .add(MenuItemDef(MenuItemDefId.SYSTEM_STATISTICS, "wa/systemStatistics")) // Visible for all.
                .add(MenuItemDef(MenuItemDefId.CONFIGURATION, "wa/configuration", requiredGroups = *arrayOf(ProjectForgeGroup.ADMIN_GROUP)))
                .add(MenuItemDef(MenuItemDefId.PLUGIN_ADMIN, "wa/wicket/bookmarkable/org.projectforge.web.admin.PluginListPage",
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
