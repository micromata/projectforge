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
    // Migrated to projectforge-next (hand built, GroupAccessEntityRest); wa/accessList is redirected by
    // OrphanedLinkFilter.
    ACCESS_LIST("menu.accessList", getListUrl("access")), //
    ACCOUNT_LIST("menu.fibu.konten", getListUrl("account")), //
    // Migrated to projectforge-next; wa/accountingRecordList is redirected by OrphanedLinkFilter.
    ACCOUNTING_RECORD_LIST("menu.fibu.buchungssaetze", getListUrl("accountingRecord")), //
    ADDRESSBOOK_LIST("menu.addressbookList", getReactListUrl("addressBook")), //
    ADDRESS_LIST("menu.addressList", getReactListUrl("address")), //
    ADMIN_ERRORS("system.admin.adminErrors.title", "next/adminErrors"), //
    ADMIN_LOG_VIEWER("system.admin.logViewer.title", "next/adminLogViewer"), //
    BANK_ACCOUNT_LIST("menu.finance.bankAccounts"), //
    BIRTHDAY_BUTLER("menu.birthdayButler", getReactDynamicPageUrl("birthdayButler")), //
    BOOK_LIST("menu.bookList", getListUrl("book")), //
    CALENDAR("menu.calendar", getListUrl("calendar")), //
    CALENDAR_LIST("menu.plugins.teamcal", getReactListUrl("teamCal")), //
    CHANGE_PASSWORD("menu.changePassword", getReactDynamicPageUrl("changePassword")), //
    CHANGE_WLAN_PASSWORD("menu.changeWlanPassword", getReactDynamicPageUrl("changeWlanPassword")), //
    CONFIGURATION("menu.configuration", getListUrl("configuration")), //
    CONTRACTS("menu.contracts", getReactListUrl("contract")), //
    // Migrated to projectforge-next; the Wicket pages (wa/cost1List, wa/cost2List, wa/cost2TypeList) were removed.
    COST1_LIST("menu.fibu.kost1", getListUrl("cost1")), //
    COST2_LIST("menu.fibu.kost2", getListUrl("cost2")), //
    COST2_TYPE_LIST("menu.fibu.kost2arten", getListUrl("cost2Type")), //
    COST_SEARCH("menu.fibu.kostSearch", getReactDynamicPageUrl("costSearch")), //

    // Migrated to projectforge-next; the Wicket pages (wa/customerList) were removed.
    CUSTOMER_LIST("menu.fibu.kunden", getListUrl("customer")), //
    CUSTOMER_GROUPS("menu.fibu.customerGroups", "next/customerGroups"), //

    DATEV_IMPORT("menu.fibu.datevImport", "next/datev-import"), //
    DVELOP("menu.dvelop", getReactDynamicPageUrl("dvelop")), //
    E_INVOICE_CHECKER("menu.fibu.eInvoiceChecker", getReactDynamicPageUrl("eInvoiceChecker")), //
    EMPLOYEE_LIST("menu.fibu.employees", getReactListUrl("employee")), //
    // Migrated to projectforge-next; the Wicket pages (wa/employeeSalaryList, import included) were removed.
    // The import is reached as a button in the salary list.
    EMPLOYEE_SALARY_LIST("menu.fibu.employeeSalaries", getListUrl("employeeSalary")), //
    EMPLOYEE_LEAVE_ACCOUNT_ENTRIES("menu.vacation.leaveAccountEntry", getReactListUrl("leaveAccountEntry")), //
    // Migrated to projectforge-next (FeedbackPageRest); the Wicket page (wa/feedback) was removed.
    FEEDBACK("menu.gear.feedback", "next/feedback"), //
    GANTT("menu.gantt", getListUrl("gantt")), //
    // Migrated to projectforge-next, list and form; react/group stays reachable through the escape hatch,
    // see NextMigration.legacyListUrl.
    GROUP_LIST("menu.groupList", getListUrl("group")), //
    HR_PLANNING_LIST("menu.hrPlanningList", getListUrl("hrPlanning")), //
    HR_VIEW("menu.hrList", "next/hrList"), //
    INBOX_LIST("menu.orga.posteingang", getReactListUrl("incomingMail")), //
    // Migrated to projectforge-next, list and form; the Wicket pages (wa/incomingInvoiceList/Edit) were removed.
    INCOMING_INVOICE_LIST("menu.fibu.eingangsrechnungen", getListUrl("incomingInvoice")), //
    CURRENCY_PAIR_LIST("menu.fibu.currencyPair", getReactListUrl("currencyPair")), //
    JOB_MONITOR("jobs.monitor.title", getReactDynamicPageUrl("jobsMonitor")), //
    LOGOUT("menu.logout", url = "logout"), //
    MONTHLY_EMPLOYEE_REPORT("menu.monthlyEmployeeReport", "next/monthlyEmployeeReport"), //
    MY_ACCOUNT("menu.myAccount", getReactDynamicPageUrl("myAccount")), //
    CUSTOMIZE_MENU("menu.customizeMenu", "${Constants.REACT_APP_PATH}customizeMenu"), //
    MY_2FA("menu.2FA", getReactDynamicPageUrl(TWO_FACTOR_AUTHENTIFICATION_SUB_URL_PRIV)), //
    MY_2FA_SETUP("menu.2FASetup", getReactDynamicPageUrl("2FASetup")), //
    MY_SCRIPT_LIST("menu.myScriptList", getReactListUrl("myscript")), //
    // Migrated to projectforge-next; the Wicket pages (wa/orderBookList) were removed.
    ORDER_LIST("menu.fibu.orderbook", getListUrl("order")), //
    // Next only: forecast and contribution margin of the order book, with a filter of their own.
    ORDER_STATISTICS("menu.fibu.orderStatistics", "next/orderStatistics"), //
    OUTBOX_LIST("menu.orga.postausgang", getReactListUrl("outgoingMail")), //
    // Migrated to projectforge-next, list and form; the Wicket pages (wa/outgoingInvoiceList) were removed.
    OUTGOING_INVOICE_LIST("menu.fibu.rechnungen", getListUrl("outgoingInvoice")), //
    PERSONAL_STATISTICS("menu.personalStatistics", "next/personalStatistics"), //
    // Migrated to projectforge-next (PhoneCallRest); wa/phoneCall is redirected by OrphanedLinkFilter.
    PHONE_CALL("menu.phoneCall", "next/phoneCall"), //
    POLL("menu.poll", getReactListUrl("poll")), //
    PROJECT_LIST("menu.fibu.projekte", getListUrl("project")), //
    // Migrated to projectforge-next (ReportObjectivesPageRest); the Wicket page (wa/reportObjectives) was removed.
    REPORT_OBJECTIVES("menu.fibu.reporting.reportObjectives", "next/reportObjectives"), //
    // Migrated to projectforge-next (SendTextMessageRest); the Wicket page (wa/sendSms) was removed.
    SEND_SMS("menu.sendSms", "next/sendTextMessage"), //
    SCRIPT_LIST("menu.scriptList", getReactListUrl("script")), //
    // Migrated to projectforge-next (SearchRest); the Wicket search page was removed. A bookmarked
    // /wa/search is still redirected to next by OrphanedLinkFilter.
    SEARCH("menu.search", "${Constants.NEXT_APP_PATH}search"), //
    // Migrated to projectforge-next; wa/taskTree is redirected by OrphanedLinkFilter. The tree and not the
    // category's list (next/task, NextMigration.listUrl): the entity has two perspectives in projectforge-next.
    TASK_TREE("menu.taskTree", "${Constants.NEXT_APP_PATH}taskTree"), //
    TIMESHEET_LIST("menu.timesheetList", getListUrl("timesheet")), //
    USER_LIST("menu.userList", getReactListUrl("user")), //
    VACATION("menu.vacation", getReactListUrl("vacation")), //
    VACATION_ACCOUNT("menu.vacation.leaveaccount", getReactDynamicPageUrl("vacationAccount")), //
    VISITORBOOK("menu.orga.visitorbook", getReactListUrl("visitorbook")), //

    PLUGIN_ADMIN("menu.pluginAdmin", "next/plugins"), //
    SYSTEM("menu.system", "next/system"), //
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
