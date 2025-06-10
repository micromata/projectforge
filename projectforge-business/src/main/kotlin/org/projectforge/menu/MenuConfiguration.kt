/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.menu

import jakarta.annotation.PostConstruct
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class MenuConfiguration {
    @Value("\${projectforge.menu.visibility.access}")
    private var accessVisibility: String? = null

    @Value("\${projectforge.menu.visibility.addresses}")
    private var addressesVisibility: String? = null

    @Value("\${projectforge.menu.visibility.addressbooks}")
    private var addressbooksVisibility: String? = null

    @Value("\${projectforge.menu.visibility.books}")
    private var booksVisibility: String? = null

    @Value("\${projectforge.menu.visibility.calendar}")
    private var calendarVisibility: String? = null

    @Value("\${projectforge.menu.visibility.calendarList}")
    private var calendarListVisibility: String? = null

    @Value("\${projectforge.menu.visibility.changePassword}")
    private var changePasswordVisibility: String? = null

    @Value("\${projectforge.menu.visibility.changeWlanPassword}")
    private var changeWlanPasswordVisibility: String? = null

    @Value("\${projectforge.menu.visibility.feedback}")
    private var feedbackVisibility: String? = null

    @Value("\${projectforge.menu.visibility.gantt}")
    private var ganttVisibility: String? = null

    @Value("\${projectforge.menu.visibility.groups}")
    private var groupsVisibility: String? = null

    @Value("\${projectforge.menu.visibility.hrPlanning}")
    private var hrPlanningVisibility: String? = null

    @Value("\${projectforge.menu.visibility.hrView}")
    private var hrViewVisibility: String? = null

    @Value("\${projectforge.menu.visibility.monthlyEmployeeReport}")
    private var monthlyEmployeeReportVisibility: String? = null

    @Value("\${projectforge.menu.visibility.myAccount}")
    private var myAccountVisibility: String? = null

    @Value("\${projectforge.menu.visibility.myMenu}")
    private var myMenuVisibility: String? = null

    @Value("\${projectforge.menu.visibility.my2FA}")
    private var my2FAVisibility: String? = null

    @Value("\${projectforge.menu.visibility.my2FASetup}")
    private var my2FASetupVisibility: String? = null

    @Value("\${projectforge.menu.visibility.myScripts}")
    private var myScriptsVisibility: String? = null

    @Value("\${projectforge.menu.visibility.myPreferences}")
    private var myPreferencesVisibility: String? = null

    @Value("\${projectforge.menu.visibility.personalStatistics}")
    private var personalStatisticsVisibility: String? = null

    @Value("\${projectforge.menu.visibility.phoneCall}")
    private var phoneCallVisibility: String? = null

    @Value("\${projectforge.menu.visibility.poll}")
    private var pollVisibility: String? = null

    @Value("\${projectforge.menu.visibility.sendSms}")
    private var sendSmsVisibility: String? = null

    @Value("\${projectforge.menu.visibility.search}")
    private var searchVisibility: String? = null

    @Value("\${projectforge.menu.visibility.taskTree}")
    private var taskTreeVisibility: String? = null

    @Value("\${projectforge.menu.visibility.timesheets}")
    private var timesheetsVisibility: String? = null

    @Value("\${projectforge.menu.visibility.users}")
    private var usersVisibility: String? = null

    @Value("\${projectforge.menu.visibility.vacation}")
    private var vacationVisibility: String? = null

    @Value("\${projectforge.menu.visibility.vacationAccount}")
    private var vacationAccountVisibility: String? = null

    @Value("\${projectforge.menu.visibility.systemStatistics}")
    private var systemStatisticsVisibility: String? = null

    // *******************
    // Built-in plugins:
    // *******************
    @Value("\${projectforge.menu.visibility.addressCampaign}")
    private var addressCampaignVisibility: String? = null

    @Value("\${projectforge.menu.visibility.addressCampaignValues}")
    private var addressCampaignValuesVisibility: String? = null

    @Value("\${projectforge.menu.visibility.datatransfer}")
    private var datatransferVisibility: String? = null

    @Value("\${projectforge.menu.visibility.ihk}")
    private var ihkVisibility: String? = null

    @Value("\${projectforge.menu.visibility.licenseManagement}")
    private var licenseManagementVisibility: String? = null

    @Value("\${projectforge.menu.visibility.memo}")
    private var memoVisibility: String? = null

    @Value("\${projectforge.menu.visibility.merlin}")
    private var merlinVisibility: String? = null

    @Value("\${projectforge.menu.visibility.skillmatrix}")
    private var skillmatrixVisibility: String? = null

    @Value("\${projectforge.menu.visibility.todo}")
    private var todoVisibility: String? = null


    private val registry = mutableListOf<MenuVisibility>()

    fun getMenuVisibility(id: String?): MenuVisibility? {
        return registry.find { it.id == id }
    }

    /**
     * Check if the menu item is visible. If not found, the menu item is visible!
     * @param menuItemDef The menu item to check.
     * @return true if the menu item is visible.
     */
    fun isVisible(menuItemDef: MenuItemDef?): Boolean {
        val menuItemDefId = menuItemDef?.menuItemDefId
        if (menuItemDefId != null) {
            return registry.find { it.menuItemDefId == menuItemDefId }?.isVisible() ?: true
        }
        return getMenuVisibility(menuItemDef?.id)?.isVisible() ?: true
    }

    @PostConstruct
    private fun postConstruct() {
        instance = this
        registry.add(MenuVisibility("access", accessVisibility, MenuItemDefId.ACCESS_LIST))
        registry.add(MenuVisibility("addresses", addressesVisibility, MenuItemDefId.ADDRESS_LIST))
        registry.add(MenuVisibility("addressbooks", addressbooksVisibility, MenuItemDefId.ADDRESSBOOK_LIST))
        registry.add(MenuVisibility("books", booksVisibility, MenuItemDefId.BOOK_LIST))
        registry.add(MenuVisibility("calendar", calendarVisibility, MenuItemDefId.CALENDAR))
        registry.add(MenuVisibility("calendarList", calendarListVisibility, MenuItemDefId.CALENDAR_LIST))
        registry.add(MenuVisibility("changePassword", changePasswordVisibility, MenuItemDefId.CHANGE_PASSWORD))
        registry.add(
            MenuVisibility(
                "changeWlanPassword",
                changeWlanPasswordVisibility,
                MenuItemDefId.CHANGE_WLAN_PASSWORD
            )
        )
        registry.add(MenuVisibility("feedback", feedbackVisibility, MenuItemDefId.FEEDBACK))
        registry.add(MenuVisibility("gantt", ganttVisibility, MenuItemDefId.GANTT))
        registry.add(MenuVisibility("groups", groupsVisibility, MenuItemDefId.GROUP_LIST))
        registry.add(MenuVisibility("hrPlanning", hrPlanningVisibility, MenuItemDefId.HR_PLANNING_LIST))
        registry.add(MenuVisibility("hrView", hrViewVisibility, MenuItemDefId.HR_VIEW))
        registry.add(
            MenuVisibility(
                "monthlyEmployeeReport", monthlyEmployeeReportVisibility, MenuItemDefId.MONTHLY_EMPLOYEE_REPORT
            )
        )
        registry.add(MenuVisibility("myAccount", myAccountVisibility, MenuItemDefId.MY_ACCOUNT))
        registry.add(MenuVisibility("customizeMenu", myAccountVisibility, MenuItemDefId.CUSTOMIZE_MENU))
        registry.add(MenuVisibility("my2FA", my2FAVisibility, MenuItemDefId.MY_2FA))
        registry.add(MenuVisibility("my2FASetup", my2FASetupVisibility, MenuItemDefId.MY_2FA_SETUP))
        registry.add(MenuVisibility("myScripts", myScriptsVisibility, MenuItemDefId.MY_SCRIPT_LIST))
        registry.add(MenuVisibility("myPreferences", myPreferencesVisibility, MenuItemDefId.MY_PREFERENCES))
        registry.add(
            MenuVisibility(
                "personalStatistics",
                personalStatisticsVisibility,
                MenuItemDefId.PERSONAL_STATISTICS
            )
        )
        registry.add(MenuVisibility("phoneCall", phoneCallVisibility, MenuItemDefId.PHONE_CALL))
        registry.add(MenuVisibility("poll", pollVisibility, MenuItemDefId.POLL))
        registry.add(MenuVisibility("sendSms", sendSmsVisibility, MenuItemDefId.SEND_SMS))
        registry.add(MenuVisibility("search", searchVisibility, MenuItemDefId.SEARCH))
        registry.add(MenuVisibility("taskTree", taskTreeVisibility, MenuItemDefId.TASK_TREE))
        registry.add(MenuVisibility("timesheets", timesheetsVisibility, MenuItemDefId.TIMESHEET_LIST))
        registry.add(MenuVisibility("users", usersVisibility, MenuItemDefId.USER_LIST))
        registry.add(MenuVisibility("vacation", vacationVisibility, MenuItemDefId.VACATION))
        registry.add(MenuVisibility("vacationAccount", vacationAccountVisibility, MenuItemDefId.VACATION_ACCOUNT))
        registry.add(MenuVisibility("systemStatistics", systemStatisticsVisibility, MenuItemDefId.SYSTEM_STATISTICS))

        // Built-in plugins:
        registry.add(MenuVisibility("addressCampaign", addressCampaignVisibility))
        registry.add(MenuVisibility("addressCampaignValues", addressCampaignValuesVisibility))
        registry.add(MenuVisibility("datatransfer", datatransferVisibility))
        registry.add(MenuVisibility("ihk", ihkVisibility))
        registry.add(MenuVisibility("licenseManagement", licenseManagementVisibility))
        registry.add(MenuVisibility("memo", memoVisibility))
        registry.add(MenuVisibility("merlin", merlinVisibility))
        registry.add(MenuVisibility("skillmatrix", skillmatrixVisibility))
        registry.add(MenuVisibility("todo", todoVisibility))

    }

    companion object {
        lateinit var instance: MenuConfiguration
            private set
    }
}
