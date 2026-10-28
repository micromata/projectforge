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


package org.projectforge.feature

import org.projectforge.menu.builder.MenuItemDefId

/**
 * The features of the core menu entries ([MenuItemDefId]): the paths derived from their urls (see
 * [FeatureDerivation]) plus the ones declared here, which the url doesn't reveal (imports, services, pages of
 * their own).
 *
 * `FeatureCoverageTest` checks that every REST category and every next route belongs to a feature, to a plugin
 * or to the core (always reachable, see `FeaturePathIndex.CORE_REST`).
 */
object CoreFeatures {
    /** Always on, no feature of their own. */
    val ALWAYS_ENABLED = setOf(MenuItemDefId.LOGOUT.id)

    private class Extras(
        val rest: List<String> = emptyList(),
        val next: List<String> = emptyList(),
        val shared: List<String> = emptyList(),
    )

    private val CALENDAR_SUBSCRIPTION = listOf("calendarSubscription")

    /** The project wizard, offered by the lists of customers, projects and tasks. */
    private val PROJECT_WIZARD = listOf("projectWizard")

    private val EXTRAS = mapOf(
        MenuItemDefId.ADDRESS_LIST to Extras(rest = listOf("addressImport", "cardDAVInfo")),
        MenuItemDefId.ADMIN_ERRORS to Extras(rest = listOf("adminErrors")),
        // The subscription dialog (with its QR code) is offered by the calendar, the calendar list and the timesheets.
        MenuItemDefId.CALENDAR to Extras(rest = listOf("calendarSettings", "teamEvent") + CALENDAR_SUBSCRIPTION),
        MenuItemDefId.CALENDAR_LIST to Extras(rest = listOf("teamCalImport", "teamEvent") + CALENDAR_SUBSCRIPTION),
        // The rooms, the direct messages and the confidential conversations of the chat.
        MenuItemDefId.CHAT to Extras(rest = listOf("chatDirect", "chatRoom", "chatSecret")),
        MenuItemDefId.CURRENCY_PAIR_LIST to Extras(rest = listOf("currencyConversionRate")),
        // The number ranges of the number fields of customers and projects.
        MenuItemDefId.CUSTOMER_LIST to Extras(rest = listOf("numberRanges") + PROJECT_WIZARD, next = PROJECT_WIZARD),
        MenuItemDefId.CUSTOMIZE_MENU to Extras(rest = listOf("menu/customized", "menu/default", "menu/reset")),
        MenuItemDefId.DATEV_IMPORT to Extras(rest = listOf("datevAccountImport", "datevRecordImport")),
        MenuItemDefId.EMPLOYEE_SALARY_LIST to Extras(rest = listOf("employeeSalaryImport")),
        MenuItemDefId.GANTT to Extras(rest = listOf("ganttServices")),
        MenuItemDefId.HR_VIEW to Extras(rest = listOf("hrView")),
        MenuItemDefId.INCOMING_INVOICE_LIST to Extras(
            rest = listOf("incomingInvoiceImport"),
            next = listOf("creditor-invoice-import"),
        ),
        // The job monitor's jobs are polled by every page (toasts of the running jobs).
        MenuItemDefId.JOB_MONITOR to Extras(shared = listOf("jobsMonitor/jobs", "jobsMonitor/cancel")),
        MenuItemDefId.MY_SCRIPT_LIST to Extras(rest = listOf("myScriptExecute")),
        MenuItemDefId.ORDER_STATISTICS to Extras(rest = listOf("orderStatistics")),
        MenuItemDefId.OUTGOING_INVOICE_LIST to Extras(rest = listOf("invoice")),
        MenuItemDefId.PHONE_CALL to Extras(rest = listOf("sipgate")),
        MenuItemDefId.PLUGIN_ADMIN to Extras(rest = listOf("pluginList")),
        MenuItemDefId.POLL to Extras(rest = listOf("pollAnswer")),
        MenuItemDefId.PROJECT_LIST to Extras(rest = listOf("numberRanges") + PROJECT_WIZARD, next = PROJECT_WIZARD),
        MenuItemDefId.SCRIPT_LIST to Extras(rest = listOf("scriptExecute")),
        MenuItemDefId.SYSTEM to Extras(
            rest = listOf(
                "admin", "adminCaches", "adminLogViewer", "adminScheduler", "logViewer", "propertiesEditor",
                "systemStatistics",
            ),
            next = listOf("logViewer"),
        ),
        // The task picker of other pages (timesheets, orders, ...) uses the tree and the favorites.
        // The ad hoc booking grants are managed in the task form.
        MenuItemDefId.TASK_TREE to Extras(
            rest = listOf("task", "taskBookingGrant", "taskWizard") + PROJECT_WIZARD,
            next = listOf("task", "taskWizard") + PROJECT_WIZARD,
            shared = listOf("task/tree", "task/favorites", "task/recent", "task/info", "task/kost2Preview"),
        ),
        MenuItemDefId.TIMESHEET_LIST to Extras(rest = CALENDAR_SUBSCRIPTION),
        MenuItemDefId.VACATION to Extras(rest = listOf("vacationExport")),
    )

    /** The features of all [MenuItemDefId]s (also of the main menu groups, which have no paths). */
    val definitions: List<FeatureDefinition> by lazy {
        MenuItemDefId.entries.filter { it.id !in ALWAYS_ENABLED }.map { defId ->
            val extras = EXTRAS[defId]
            FeatureDerivation.derive(
                defId.id,
                defId.url,
                restPaths = extras?.rest.orEmpty(),
                nextRoutes = extras?.next.orEmpty(),
                sharedRestPaths = extras?.shared.orEmpty(),
            )
        }
    }
}
