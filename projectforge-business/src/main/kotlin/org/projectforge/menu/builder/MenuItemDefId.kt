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

package org.projectforge.menu.builder

import org.projectforge.Constants
import org.projectforge.NextMigration

private const val TWO_FACTOR_AUTHENTIFICATION_SUB_URL_PRIV = "2FA"

enum class MenuItemDefId constructor(val i18nKey: String, val url: String? = null) {
    // Main menus in alphabetical order
    ADMINISTRATION("menu.administration"), //
    COMMON("menu.common"), //
    COST("menu.fibu.kost"), //
    FIBU("menu.fibu"), //
    HR("menu.hr"), //
    MISC("menu.misc"), //
    ORGA("menu.orga"), //
    PROJECT_MANAGEMENT("menu.projectmanagement"), //
    REPORTING("menu.reporting"), //

    // Sub menus in alphabetical order:
    ACCESS_LIST("menu.accessList", "wa/accessList"), //
    ACCOUNT_LIST("menu.fibu.konten", getReactListUrl("account")), //
    ACCOUNTING_RECORD_LIST("menu.fibu.buchungssaetze", "wa/accountingRecordList"), //
    ADDRESSBOOK_LIST("menu.addressbookList", getReactListUrl("addressBook")), //
    ADDRESS_LIST("menu.addressList", getReactListUrl("address")), //
    ADMIN_LOG_VIEWER("system.admin.logViewer.title", "${getReactDynamicPageUrl("adminLogViewer")}/-1"), //
    BANK_ACCOUNT_LIST("menu.finance.bankAccounts"), //
    BIRTHDAY_BUTLER("menu.birthdayButler", getReactDynamicPageUrl("birthdayButler")), //
    BOOK_LIST("menu.bookList", getListUrl("book")), //
    CALENDAR("menu.calendar", getListUrl("calendar")), //
    CALENDAR_LIST("menu.plugins.teamcal", getReactListUrl("teamCal")), //
    CHANGE_PASSWORD("menu.changePassword", getReactDynamicPageUrl("changePassword")), //
    CHANGE_WLAN_PASSWORD("menu.changeWlanPassword", getReactDynamicPageUrl("changeWlanPassword")), //
    CONFIGURATION("menu.configuration", "wa/configuration"), //
    CONTRACTS("menu.contracts", getReactListUrl("contract")), //
    // Migrated to projectforge-next; the Wicket page (wa/cost1List) stays reachable through the escape
    // hatch next to the page title, see NextMigration.legacyListUrl.
    COST1_LIST("menu.fibu.kost1", getListUrl("cost1")), //
    COST2_LIST("menu.fibu.kost2", "wa/cost2List"), //
    COST2_TYPE_LIST("menu.fibu.kost2arten", "wa/cost2TypeList"), //
    COST_SEARCH("menu.fibu.kostSearch", getReactDynamicPageUrl("costSearch")), //

    CUSTOMER_LIST("menu.fibu.kunden", "wa/customerList"), //
    //CUSTOMER_LIST("menu.fibu.kunden", getReactListUrl("customer")), // Doesn't work yet

    DATEV_IMPORT("menu.fibu.datevImport", "wa/datevImport"), //
    DVELOP("menu.dvelop", getReactDynamicPageUrl("dvelop")), //
    E_INVOICE_CHECKER("menu.fibu.eInvoiceChecker", getReactDynamicPageUrl("eInvoiceChecker")), //
    EMPLOYEE_LIST("menu.fibu.employees", getReactListUrl("employee")), //
    EMPLOYEE_SALARY_LIST("menu.fibu.employeeSalaries", "wa/employeeSalaryList"), //
    EMPLOYEE_SALARY_IMPORT("menu.fibu.employeeSalariesImport", "wa/wicket/bookmarkable/org.projectforge.web.fibu.EmployeeSalaryImportPage"), //
    EMPLOYEE_LEAVE_ACCOUNT_ENTRIES("menu.vacation.leaveAccountEntry", getReactListUrl("leaveAccountEntry")), //
    FEEDBACK("menu.gear.feedback", url = "wa/feedback"), //
    GANTT("menu.gantt", "wa/ganttList"), //
    // Migrated to projectforge-next, list and form; react/group stays reachable through the escape hatch,
    // see NextMigration.legacyListUrl.
    GROUP_LIST("menu.groupList", getListUrl("group")), //
    HR_PLANNING_LIST("menu.hrPlanningList", "wa/hrPlanningList"), //
    HR_VIEW("menu.hrList", "wa/hrList"), //
    INBOX_LIST("menu.orga.posteingang", getReactListUrl("incomingMail")), //
    // Migrated to projectforge-next, list and form; wa/incomingInvoiceList stays reachable through the
    // escape hatch, see NextMigration.legacyListUrl.
    INCOMING_INVOICE_LIST("menu.fibu.eingangsrechnungen", getListUrl("incomingInvoice")), //
    CURRENCY_PAIR_LIST("menu.fibu.currencyPair", getReactListUrl("currencyPair")), //
    JOB_MONITOR("jobs.monitor.title", getReactDynamicPageUrl("jobsMonitor")), //
    LOGOUT("menu.logout", url = "logout"), //
    MONTHLY_EMPLOYEE_REPORT("menu.monthlyEmployeeReport", "wa/monthlyEmployeeReport"), //
    MY_ACCOUNT("menu.myAccount", getReactDynamicPageUrl("myAccount")), //
    CUSTOMIZE_MENU("menu.customizeMenu", "${Constants.REACT_APP_PATH}customizeMenu"), //
    MY_2FA("menu.2FA", getReactDynamicPageUrl(TWO_FACTOR_AUTHENTIFICATION_SUB_URL_PRIV)), //
    MY_2FA_SETUP("menu.2FASetup", getReactDynamicPageUrl("2FASetup")), //
    MY_SCRIPT_LIST("menu.myScriptList", getReactListUrl("myscript")), //
    MY_PREFERENCES("menu.myPreferences", "wa/userPrefList"), //
    // Migrated to projectforge-next; wa/orderBookList stays reachable through the escape hatch, see
    // NextMigration.legacyListUrl.
    ORDER_LIST("menu.fibu.orderbook", getListUrl("order")), //
    OUTBOX_LIST("menu.orga.postausgang", getReactListUrl("outgoingMail")), //
    // Migrated to projectforge-next, list and form; wa/outgoingInvoiceList stays reachable through the
    // escape hatch, see NextMigration.legacyListUrl.
    OUTGOING_INVOICE_LIST("menu.fibu.rechnungen", getListUrl("outgoingInvoice")), //
    PERSONAL_STATISTICS("menu.personalStatistics", "wa/personalStatistics"), //
    PHONE_CALL("menu.phoneCall", "wa/phoneCall"), //
    POLL("menu.poll", getReactListUrl("poll")), //
    PROJECT_LIST("menu.fibu.projekte", getReactListUrl("project")), //
    REPORT_OBJECTIVES("menu.fibu.reporting.reportObjectives", "wa/reportObjectives"), //
    SEND_SMS("menu.sendSms", "wa/sendSms"), //
    SCRIPT_LIST("menu.scriptList", getReactListUrl("script")), //
    SEARCH("menu.search", "wa/search"), //
    // Migrated to projectforge-next; wa/taskTree stays reachable through the escape hatch, see
    // NextMigration.legacyListUrl. Nothing waits on the task favourites (UserPrefArea.TASK_FAVORITE):
    // they are a Wicket affair, replaced in React and next by the quick access of the select fields
    // themselves (the tree with its search, EntityAutocomplete for a user).
    // The tree and not the category's list, because the entity has two perspectives in
    // projectforge-next (see NextMigration.nextRouteUrl).
    TASK_TREE("menu.taskTree", NextMigration.nextRouteUrl("task", "taskTree", "wa/taskTree")), //
    TIMESHEET_LIST("menu.timesheetList", getListUrl("timesheet")), //
    USER_LIST("menu.userList", getReactListUrl("user")), //
    VACATION("menu.vacation", getReactListUrl("vacation")), //
    VACATION_ACCOUNT("menu.vacation.leaveaccount", getReactDynamicPageUrl("vacationAccount")), //
    VISITORBOOK("menu.orga.visitorbook", getReactListUrl("visitorbook")), //

    PLUGIN_ADMIN("menu.pluginAdmin", "wa/wicket/bookmarkable/org.projectforge.web.admin.PluginListPage"), //
    SYSTEM("menu.system", "wa/admin"), //
    SYSTEM_STATISTICS("menu.systemStatistics", getReactDynamicPageUrl("systemStatistics"));

    /**
     * @return name().
     */
    val id: String
        get() = name

    companion object {
        const val TWO_FACTOR_AUTHENTIFICATION_SUB_URL = TWO_FACTOR_AUTHENTIFICATION_SUB_URL_PRIV
    }
}

/**
 * Url of a list page, pointing at whichever frontend currently serves it. [NextMigration] decides:
 * migrated pages resolve to `next/<route>`, all others to `react/<category>`. Switching a page is
 * therefore an entry in [NextMigration], not an edit here - that keeps the menu url and the server
 * side redirect targets (see `PagesResolver`) from drifting apart.
 *
 * @param category The REST category (derived from the `@RequestMapping` of the `*PagesRest` class),
 * e.g. `book` - *not* the route of the next page, which may differ.
 */
private fun getListUrl(category: String): String {
    return NextMigration.listUrl(category)
}

private fun getReactListUrl(name: String): String {
    return "${Constants.REACT_APP_PATH}$name"
}

fun getReactDynamicPageUrl(name: String): String {
    return "${Constants.REACT_APP_PATH}$name/dynamic"
}
