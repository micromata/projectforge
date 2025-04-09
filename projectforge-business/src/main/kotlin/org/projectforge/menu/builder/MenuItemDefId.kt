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

package org.projectforge.menu.builder

import org.projectforge.Constants

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
    BOOK_LIST("menu.bookList", getReactListUrl("book")), //
    CALENDAR("menu.calendar", getReactListUrl("calendar")), //
    CALENDAR_LIST("menu.plugins.teamcal", getReactListUrl("teamCal")), //
    CHANGE_PASSWORD("menu.changePassword", getReactDynamicPageUrl("changePassword")), //
    CHANGE_WLAN_PASSWORD("menu.changeWlanPassword", getReactDynamicPageUrl("changeWlanPassword")), //
    CONFIGURATION("menu.configuration", "wa/configuration"), //
    CONTRACTS("menu.contracts", getReactListUrl("contract")), //
    COST1_LIST("menu.fibu.kost1", "wa/cost1List"), // getReactListUrl("cost1")), //
    COST2_LIST("menu.fibu.kost2", "wa/cost2List"), //
    COST2_TYPE_LIST("menu.fibu.kost2arten", "wa/cost2TypeList"), //
    CUSTOMER_LIST("menu.fibu.kunden", "wa/customerList"), //
    //CUSTOMER_LIST("menu.fibu.kunden", getReactListUrl("customer")), // Doesn't work yet

    DATEV_IMPORT("menu.fibu.datevImport", "wa/datevImport"), //
    DVELOP("menu.dvelop", getReactDynamicPageUrl("dvelop")), //
    EMPLOYEE_LIST("menu.fibu.employees", getReactListUrl("employee")), //
    EMPLOYEE_SALARY_LIST("menu.fibu.employeeSalaries", "wa/employeeSalaryList"), //
    EMPLOYEE_SALARY_IMPORT("menu.fibu.employeeSalariesImport", "wa/wicket/bookmarkable/org.projectforge.web.fibu.EmployeeSalaryImportPage"), //
    EMPLOYEE_LEAVE_ACCOUNT_ENTRIES("menu.vacation.leaveAccountEntry", getReactListUrl("leaveAccountEntry")), //
    FEEDBACK("menu.gear.feedback", url = "wa/feedback"), //
    GANTT("menu.gantt", "wa/ganttList"), //
    GROUP_LIST("menu.groupList", getReactListUrl("group")), //
    HR_PLANNING_LIST("menu.hrPlanningList", "wa/hrPlanningList"), //
    HR_VIEW("menu.hrList", "wa/hrList"), //
    INBOX_LIST("menu.orga.posteingang", getReactListUrl("incomingMail")), //
    INCOMING_INVOICE_LIST("menu.fibu.eingangsrechnungen", "wa/incomingInvoiceList"), //
    JOB_MONITOR("jobs.monitor.title", getReactDynamicPageUrl("jobsMonitor")), //
    LOGOUT("menu.logout", url = "logout"), //
    MONTHLY_EMPLOYEE_REPORT("menu.monthlyEmployeeReport", "wa/monthlyEmployeeReport"), //
    MY_ACCOUNT("menu.myAccount", getReactDynamicPageUrl("myAccount")), //
    MY_MENU("menu.myMenu", getReactDynamicPageUrl("myMenu")), //
    CUSTOMIZE_MENU("menu.customizeMenu", "${Constants.REACT_APP_PATH}customizeMenu"), //
    MY_2FA("menu.2FA", getReactDynamicPageUrl(TWO_FACTOR_AUTHENTIFICATION_SUB_URL_PRIV)), //
    MY_2FA_SETUP("menu.2FASetup", getReactDynamicPageUrl("2FASetup")), //
    MY_SCRIPT_LIST("menu.myScriptList", getReactListUrl("myscript")), //
    MY_PREFERENCES("menu.myPreferences", "wa/userPrefList"), //
    ORDER_LIST("menu.fibu.orderbook", "wa/orderBookList"), //
    OUTBOX_LIST("menu.orga.postausgang", getReactListUrl("outgoingMail")), //
    OUTGOING_INVOICE_LIST("menu.fibu.rechnungen", "wa/outgoingInvoiceList"), //
    PERSONAL_STATISTICS("menu.personalStatistics", "wa/personalStatistics"), //
    PHONE_CALL("menu.phoneCall", "wa/phoneCall"), //
    POLL("menu.poll", getReactListUrl("poll")), //
    PROJECT_LIST("menu.fibu.projekte", getReactListUrl("project")), //
    REPORT_OBJECTIVES("menu.fibu.reporting.reportObjectives", "wa/reportObjectives"), //
    SEND_SMS("menu.sendSms", "wa/sendSms"), //
    SCRIPT_LIST("menu.scriptList", getReactListUrl("script")), //
    SEARCH("menu.search", "wa/search"), //
    TASK_TREE("menu.taskTree", "wa/taskTree"), //
    TIMESHEET_LIST("menu.timesheetList", "wa/timesheetList"), //
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

private fun getReactListUrl(name: String): String {
    return "${Constants.REACT_APP_PATH}$name"
}

fun getReactDynamicPageUrl(name: String): String {
    return "${Constants.REACT_APP_PATH}$name/dynamic"
}
