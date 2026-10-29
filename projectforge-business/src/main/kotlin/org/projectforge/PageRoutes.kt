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


package org.projectforge

/**
 * Single source of truth for the pages of the web frontend (projectforge-next, served under
 * [Constants.NEXT_APP_PATH]): which route a REST category has, where its edit and add pages are, and which old
 * urls of the removed frontends (Wicket, `projectforge-webapp`) are still redirected onto them.
 *
 * The frontend url of a page is generated in two unrelated places - the menu (`MenuItemDefId`) and every
 * `ResponseAction(url = ...)` redirect issued after save/cancel/delete (via `PagesResolver` in projectforge-rest).
 * If those disagreed, the user would be thrown to a wrong page in the middle of a workflow. Both therefore ask this
 * object instead of building urls themselves.
 *
 * Note that the REST category and the route of the page may differ - a page is free to mount itself where it likes,
 * e.g. under a route the category doesn't name. The hand built pages deliberately use the category itself (`book`,
 * `cost1`), so no plural has to be remembered.
 *
 * This object lives in projectforge-business (next to [Constants]) because `MenuItemDefId` does, and
 * projectforge-rest depends on projectforge-business, not the other way round.
 *
 * @author Kai Reinhard
 */
object PageRoutes {
    /**
     * Placeholder for the id in [standardEditPage]. The frontend replaces it per row, so this is
     * part of the contract with the frontend and must not be changed.
     */
    const val ID_PLACEHOLDER = ":id"

    /**
     * The frontend an old link came from. The routes are the mount point conventions of the two removed apps, so a
     * page normally needs nothing but this value: React mounted `<category>`, `<category>/edit/<id>`, while Wicket
     * mounted `<category>List` and `<category>Edit?id=<id>` (see `WebRegistry.addMountPages`).
     */
    enum class LegacyApp(val appPath: String) {
        REACT(Constants.REACT_APP_PATH),
        WICKET(Constants.WICKET_APPLICATION_PATH);

        internal fun listRoute(category: String): String {
            return if (this == WICKET) "${category}List" else category
        }

        internal fun editRoute(category: String): String {
            return if (this == WICKET) "${category}Edit?id=$ID_PLACEHOLDER" else "$category/edit/$ID_PLACEHOLDER"
        }

        internal fun newEntryRoute(category: String): String {
            return if (this == WICKET) "${category}Edit" else "$category/edit"
        }
    }

    /**
     * A page served by projectforge-next.
     *
     * @param route The route of the list page inside projectforge-next, without the `next/` prefix,
     * e.g. `book`. May differ from the REST category.
     * @param editRoute Route of the edit page with [ID_PLACEHOLDER] for the id, e.g. `book/:id`.
     * Defaults to the shape of the removed React app (`<route>/edit/:id`), which no page of
     * projectforge-next serves: a page with an edit page sets it.
     * @param newEntryRoute Route for creating a new entry, e.g. `book/new`. Defaults to the
     * React shape `<route>/edit`, see [editRoute].
     * @param legacyApp The frontend whose old links are redirected onto this page by `OrphanedLinkFilter`, or null.
     * Set only if the links have left ProjectForge (sent by mail, e.g. a vacation notification, or shared by a data
     * transfer area); a page only ever linked from within ProjectForge gets null.
     * @param legacyRoute Route of the old list page in [legacyApp], without the app prefix. Only
     * needed if the page doesn't follow that app's mount point convention.
     * @param legacyEditRoute Route of the old edit page with [ID_PLACEHOLDER], without the app
     * prefix. Only needed if the page doesn't follow the convention either.
     * @param legacyNewEntryRoute Route of the old page for creating an entry, without the app prefix.
     * Only needed if the page doesn't follow the convention.
     */
    class Page(
        val route: String,
        editRoute: String? = null,
        newEntryRoute: String? = null,
        val legacyApp: LegacyApp? = null,
        val legacyRoute: String? = null,
        val legacyEditRoute: String? = null,
        val legacyNewEntryRoute: String? = null,
    ) {
        val editRoute: String = editRoute ?: "$route/edit/$ID_PLACEHOLDER"
        val newEntryRoute: String = newEntryRoute ?: "$route/edit"
    }

    /**
     * REST category -> page in projectforge-next.
     */
    private val PAGES = mapOf(
        // The access-rights management (group/task permission matrix), migrated from Wicket (MenuItemDefId.ACCESS_LIST
        // pointed at wa/accessList). Hand built (GroupAccessEntityRest, no layout) because the permission matrix is no
        // field a generic UILayout page can render.
        "access" to Page(
            route = "access",
            editRoute = "access/$ID_PLACEHOLDER",
            newEntryRoute = "access/new",
        ),
        "account" to Page(
            route = "account",
            editRoute = "account/$ID_PLACEHOLDER",
            newEntryRoute = "account/new",
        ),
        // The DATEV accounting-record list ("Buchungssätze", MenuItemDefId.ACCOUNTING_RECORD_LIST pointed at
        // wa/accountingRecordList), migrated from Wicket with its filter, its BWA (business assessment) and the report
        // drill-down. Hand built (AccountingRecordEntityRest, no layout) so the list keeps the invoice-style date-
        // period filter and the BWA summary above the table.
        "accountingRecord" to Page(
            route = "accounting-record",
            editRoute = "accounting-record/$ID_PLACEHOLDER",
            newEntryRoute = "accounting-record/new",
        ),
        // The address campaigns of the marketing plugin, migrated from Wicket (the menu entry pointed at the
        // bookmarkable AddressCampaignListPage). Hand built (AddressCampaignEntityRest, no layout; the server laid out
        // React page of AddressCampaignPagesRest is gone).
        "addressCampaign" to Page(
            route = "address-campaign",
            editRoute = "address-campaign/$ID_PLACEHOLDER",
            newEntryRoute = "address-campaign/new",
        ),
        // The values of the address campaigns (marketing plugin), migrated from React: hand built list, form and mass
        // update (AddressCampaignValueEntityRest, components/features/address-campaign-value). A row of an address
        // without a value yet has a synthetic (negative) id and opens the form for a new value (`address-campaign-
        // value/new?campaignId=&addressId=`).
        "addressCampaignValue" to Page(
            route = "address-campaign-value",
            editRoute = "address-campaign-value/$ID_PLACEHOLDER",
            newEntryRoute = "address-campaign-value/new",
        ),
        // Hand built list, view and form of the addresses (AddressEntityRest, components/features/address), migrated
        // from React together with the VCF import (next/address/import, AddressImportRest). A row, a search hit and a
        // calendar birthday open the view (`address/:id`, see AddressEntityRest.getStandardEditPage); the form is a
        // sub route of it, as for `script`.
        "address" to Page(
            route = "address",
            editRoute = "address/$ID_PLACEHOLDER/edit",
            newEntryRoute = "address/new",
        ),
        // Hand built list and edit page of the address books (AddressbookEntityRest, components/features/addressBook),
        // migrated from React.
        "addressBook" to Page(
            route = "addressBook",
            editRoute = "addressBook/$ID_PLACEHOLDER",
            newEntryRoute = "addressBook/new",
        ),
        // The import of the transactions is the page `bankAccount/import` (no category of its own).
        "bankAccount" to Page(
            route = "bankAccount",
            editRoute = "bankAccount/$ID_PLACEHOLDER",
            newEntryRoute = "bankAccount/new",
        ),
        "bankAccountRecord" to Page(
            route = "bankAccountRecord",
            editRoute = "bankAccountRecord/$ID_PLACEHOLDER",
            newEntryRoute = "bankAccountRecord/new",
        ),
        // Hand built feature, so its routes are /book, /book/new and /book/<id>.
        "book" to Page(
            route = "book",
            editRoute = "book/$ID_PLACEHOLDER",
            newEntryRoute = "book/new",
        ),
        // The system-configuration page, migrated from Wicket (MenuItemDefId.CONFIGURATION pointed at
        // wa/configuration). Hand built (ConfigurationEntityRest, no layout): the value of a parameter is polymorphic
        // per ConfigurationType, so its edit field is rendered per type, which no generic UILayout page can do. The
        // parameter set is fixed - there is no add and no delete. The newEntryRoute is never asked for (insert is
        // denied) but is kept for the contract.
        "configuration" to Page(
            route = "configuration",
            editRoute = "configuration/$ID_PLACEHOLDER",
            newEntryRoute = "configuration/new",
        ),
        // Hand built calendar (the default page after login), migrated from the React app. No edit page of its own - a
        // new or clicked entry opens a timesheet or team event through their own routes - so the inherited editRoute /
        // newEntryRoute defaults (calendar/edit/:id, calendar/edit) are dead and never asked for.
        // react/calendar is redirected to next although no mail linked it: it was the page after login, so it is what
        // browsers offer and users keep as their start page.
        "calendar" to Page(
            route = "calendar",
            legacyApp = LegacyApp.REACT,
        ),
        // Migrated from Wicket, which the React migration never reached (see MenuItemDefId.COST1_LIST, which pointed
        // at wa/cost1List). Kost1EntityRest serves no layout, so there was never a React page.
        "cost1" to Page(
            route = "cost1",
            editRoute = "cost1/$ID_PLACEHOLDER",
            newEntryRoute = "cost1/new",
        ),
        // Migrated from Wicket (MenuItemDefId.COST2_LIST pointed at wa/cost2List). Kost2EntityRest serves no layout,
        // so there was never a React page.
        "cost2" to Page(
            route = "cost2",
            editRoute = "cost2/$ID_PLACEHOLDER",
            newEntryRoute = "cost2/new",
        ),
        // The cost-2 types ("Kost2-Arten"), migrated from Wicket (MenuItemDefId.COST2_TYPE_LIST pointed at
        // wa/cost2TypeList). Kost2ArtEntityRest serves no layout, so there was never a React page.
        "cost2Type" to Page(
            route = "cost2Type",
            editRoute = "cost2Type/$ID_PLACEHOLDER",
            newEntryRoute = "cost2Type/new",
        ),
        // Migrated from Wicket (MenuItemDefId.CUSTOMER_LIST pointed at wa/customerList; the React page exists as a
        // layout, CustomerPagesRest, but was never mounted in the menu - "Doesn't work yet"). Hand built rather than
        // generic so the list keeps its filter row, saved filters, gear and Excel export. Customer favorites
        // (UserPrefArea.KUNDE_FAVORITE) are not migrated.
        "customer" to Page(
            route = "customer",
            editRoute = "customer/$ID_PLACEHOLDER",
            newEntryRoute = "customer/new",
        ),
        // Hand built list and edit page of the employees (EmployeeEntityRest, components/features/employee), migrated
        // from React; the time-dependent attributes (status, weekly hours, annual leave) are edited in dialogs of the
        // form (`employee/validSinceAttr/...`).
        "employee" to Page(
            route = "employee",
            editRoute = "employee/$ID_PLACEHOLDER",
            newEntryRoute = "employee/new",
        ),
        // Hand built list and edit page of the currency pairs (CurrencyPairEntityRest, components/features/currency-
        // pair), migrated from React; the conversion rates of a pair are edited in dialogs of the form
        // (`currencyPair/rate/...`).
        "currencyPair" to Page(
            route = "currencyPair",
            editRoute = "currencyPair/$ID_PLACEHOLDER",
            newEntryRoute = "currencyPair/new",
        ),
        // Migrated from Wicket (MenuItemDefId.EMPLOYEE_SALARY_LIST pointed at wa/employeeSalaryList).
        // EmployeeSalaryEntityRest serves no layout, so there is no React page.
        "employeeSalary" to Page(
            route = "employeeSalary",
            editRoute = "employeeSalary/$ID_PLACEHOLDER",
            newEntryRoute = "employeeSalary/new",
        ),
        // The Gantt charts, migrated from Wicket (MenuItemDefId.GANTT pointed at wa/ganttList). GanttChartEntityRest
        // serves no layout, so there is no React page; the chart is still rendered by Batik on the server
        // (GanttServicesRest).
        "gantt" to Page(
            route = "gantt",
            editRoute = "gantt/$ID_PLACEHOLDER",
            newEntryRoute = "gantt/new",
        ),
        // Migrated from the React app (MenuItemDefId.GROUP_LIST pointed at react/group).
        "group" to Page(
            route = "group",
            editRoute = "group/$ID_PLACEHOLDER",
            newEntryRoute = "group/new",
        ),
        // The HR planning ("Wochenplanung"), migrated from Wicket (MenuItemDefId.HR_PLANNING_LIST pointed at
        // wa/hrPlanningList) with its weekly form. Hand built: the list shows the entries of the planned weeks
        // (HRPlanningEntryEntityRest, category hrPlanningEntry) while a row opens the week itself
        // (HRPlanningEntityRest).
        "hrPlanning" to Page(
            route = "hrPlanning",
            editRoute = "hrPlanning/$ID_PLACEHOLDER",
            newEntryRoute = "hrPlanning/new",
        ),
        // The list of the HR planning above, under the category of its rows (HRPlanningEntryEntityRest): its list
        // metadata is the one of this category. Without an entry here the category counted as a page of the React app,
        // which has none, and the list offered a dead "classic version" link (react/hrPlanningEntry). The entries have
        // no page of their own (a row opens its week by the planning's id, not the entry's, so no editRoute is given),
        // and there is nothing to go back to either.
        "hrPlanningEntry" to Page(
            route = "hrPlanning",
            newEntryRoute = "hrPlanning/new",
        ),
        "license" to Page(
            route = "license",
            editRoute = "license/$ID_PLACEHOLDER",
            newEntryRoute = "license/new",
        ),
        // The liquidity-planning plugin, the first plugin migrated (from Wicket, list and forecast, form included).
        // Hand built (LiquidityEntityRest, no layout).
        "liquidity" to Page(
            route = "liquidity",
            editRoute = "liquidity/$ID_PLACEHOLDER",
            newEntryRoute = "liquidity/new",
        ),
        // The rules of the notification system, new in next (NotificationRuleEntityRest, no layout): there is no
        // legacy page.
        "notificationRule" to Page(
            route = "notificationRule",
            editRoute = "notificationRule/$ID_PLACEHOLDER",
            newEntryRoute = "notificationRule/new",
        ),
        // Migrated from Wicket as well (MenuItemDefId.ORDER_LIST pointed at wa/orderBookList). Both legacy routes have
        // to be spelled out: the Wicket mount points are orderBookList / orderBookEdit (WebRegistry,
        // DaoConst.ORDERBOOK), so the convention would build orderList. The links of the order change mails
        // (wa/orderBookEdit?id=) are redirected by OrphanedLinkFilter.
        "order" to Page(
            route = "order",
            editRoute = "order/$ID_PLACEHOLDER",
            newEntryRoute = "order/new",
            legacyApp = LegacyApp.WICKET,
            legacyRoute = "orderBookList",
            legacyEditRoute = "orderBookEdit?id=$ID_PLACEHOLDER",
            legacyNewEntryRoute = "orderBookEdit",
        ),
        // Migrated from Wicket, form included: the three document functions that used to be Wicket's alone - the Word
        // export, the XRechnung/ZUGFeRD export and the invoice PDF upload - are REST endpoints of
        // OutgoingInvoiceEntityRest now and are on the next form. The route is `invoice`, not the category: the entity
        // is "Rechnung" to its users, and which side of it the category names (outgoing vs. incoming) is what the menu
        // says, not what the url has to spell out.
        "outgoingInvoice" to Page(
            route = "invoice",
            editRoute = "invoice/$ID_PLACEHOLDER",
            newEntryRoute = "invoice/new",
        ),
        // Migrated from Wicket, list and form. The route is `creditor-invoice`, not the category: `invoice` is the
        // outgoing side, and this is the incoming (creditor) one - which side the category names is what the menu
        // says. Nothing is left behind on Wicket: the SEPA transfer export sits on the edit page and the mass update,
        // the CSV        // import is next/creditor-invoice-import.
        "incomingInvoice" to Page(
            route = "creditor-invoice",
            editRoute = "creditor-invoice/$ID_PLACEHOLDER",
            newEntryRoute = "creditor-invoice/new",
        ),
        // The project list was the generic React page (ProjectPagesRest's layout), its form the Wicket ProjektEditPage
        // - the React form never got past "Not yet implemented". Project favorites (UserPrefArea.PROJEKT_FAVORITE) are
        // not migrated.
        "project" to Page(
            route = "project",
            editRoute = "project/$ID_PLACEHOLDER",
            newEntryRoute = "project/new",
        ),
        // Migrated from Wicket (MenuItemDefId.TASK_TREE pointed at wa/taskTree). This entry is the *list* perspective
        // of the entity, /next/task, as for every other page - the structure tree is a second next page of the same
        // entity, under a route of its own (/next/taskTree, served by TaskServicesRest rather than by a list layout).
        // Only one of the two can be a NextPage.route, and it has to be the list: every server side redirect for the
        // category `task` goes through [listUrl], and a redirect after a save must not land on the tree. The menu
        // opens the tree (MenuItemDefId.TASK_TREE names next/taskTree itself).
        "task" to Page(
            route = "task",
            editRoute = "task/$ID_PLACEHOLDER",
            newEntryRoute = "task/new",
        ),
        // Hand built list and edit page (next/timesheet, next/timesheet/:id, next/timesheet/new). The two most-used
        // calendar editors were migrated ahead of the list; the list followed, so the menu entry now resolves through
        // [listUrl] (MenuItemDefId.TIMESHEET_LIST = getListUrl("timesheet")) rather than pointing at wa/timesheetList.
        "timesheet" to Page(
            route = "timesheet",
            editRoute = "timesheet/$ID_PLACEHOLDER",
            newEntryRoute = "timesheet/new",
        ),
        // Hand built list and edit page of the team calendars (TeamCalEntityRest, components/features/teamCal),
        // migrated from React. The subscription info of a calendar (feed url, QR code) travels with the edit DTO, the
        // export feeds of the list (time sheets, week of years, holidays) come from calendarSubscription/info. (The
        // feeds are /export/ProjectForge.ics, not the pages.)
        "teamCal" to Page(
            route = "teamCal",
            editRoute = "teamCal/$ID_PLACEHOLDER",
            newEntryRoute = "teamCal/new",
        ),
        // Hand built list and edit page of the users (UserEntityRest, components/features/user), migrated from React.
        // The rights of the form depend on the assigned groups and come from user/rights (instead of the watch field
        // re-layout of the React page).
        "user" to Page(
            route = "user",
            editRoute = "user/$ID_PLACEHOLDER",
            newEntryRoute = "user/new",
        ),
        // Hand built *edit* page only (next/teamEvent/:id, next/teamEvent/new), alongside the timesheet editor: the
        // two most-used calendar editors are being migrated ahead of any list. There is no team event list of this app
        // - the event is reached through the calendar (see use-calendar-action.ts) - so this repoints only the edit
        // routes.
        "teamEvent" to Page(
            route = "teamEvent",
            editRoute = "teamEvent/$ID_PLACEHOLDER",
            newEntryRoute = "teamEvent/new",
        ),
        // The to-dos (plugin todo), migrated straight from Wicket. The legacy edit route is the link of the notification
        // mails sent before, which OrphanedLinkFilter redirects.
        "todo" to Page(
            route = "todo",
            editRoute = "todo/$ID_PLACEHOLDER",
            newEntryRoute = "todo/new",
            legacyApp = LegacyApp.WICKET,
            legacyRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoListPage",
            legacyEditRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage?id=$ID_PLACEHOLDER",
            legacyNewEntryRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage",
        ),
        // The data transfer areas (plugin datatransfer), migrated from React and hand built: the list, the admin form
        // (DataTransferAreaEntityRest) and the file view of an area (DataTransferFilesRest). The row click leads to
        // the file view (`datatransfer/:id`), which everybody with access to the area gets; the admin form is a sub
        // route of it. The external access is next/public/datatransfer/<token>, which the area's link points to; links
        // sent before (react/public/datatransfer/dynamic/<token>) are redirected by redirectPublicDataTransferPage.
        "datatransfer" to Page(
            route = "datatransfer",
            editRoute = "datatransfer/$ID_PLACEHOLDER/edit",
            newEntryRoute = "datatransfer/new",
        ),
        // The scripts of financial and controlling staff (ScriptEntityRest, ScriptExecuteRest), migrated from React
        // and hand built. As in React, the row click leads to the execution (`script/:id`); the form is a sub route of
        // it, the ad-hoc editor is `script/execute`.
        "script" to Page(
            route = "script",
            editRoute = "script/$ID_PLACEHOLDER/edit",
            newEntryRoute = "script/new",
        ),
        // The Word® templates of the plugin merlin (MerlinEntityRest, MerlinExecuteRest, components/features/merlin),
        // migrated from React and hand built like "script": the row click leads to the execution (`merlin/:id`), the
        // admin form is a sub route of it.
        "merlin" to Page(
            route = "merlin",
            editRoute = "merlin/$ID_PLACEHOLDER/edit",
            newEntryRoute = "merlin/new",
        ),
        // Hand built list and edit page of the vacation entries (VacationEntityRest, components/features/vacation),
        // migrated from React. The live statistics of the form (working days, days left, conflicts with the
        // replacements, the allowed statuses) come from `vacation/stats`, the replacement of the React watch fields.
        // The links of the vacation mails sent before (react/vacation/edit/<id>) are redirected by OrphanedLinkFilter.
        "vacation" to Page(
            route = "vacation",
            editRoute = "vacation/$ID_PLACEHOLDER",
            newEntryRoute = "vacation/new",
            legacyApp = LegacyApp.REACT,
        ),
        // Hand built list and edit page of the leave account entries ("Urlaubskontokorrekturen",
        // LeaveAccountEntryEntityRest, components/features/leave-account-entry), migrated from React. The vacation
        // account opens an entry with `?returnTo=/vacationAccount`.
        "leaveAccountEntry" to Page(
            route = "leaveAccountEntry",
            editRoute = "leaveAccountEntry/$ID_PLACEHOLDER",
            newEntryRoute = "leaveAccountEntry/new",
        ),
        // Hand built list and edit page of the contracts (ContractEntityRest, components/features/contract), migrated
        // from React. The types to choose from come from `contract/types` (configuration parameter
        // orga.contractTypes).
        "contract" to Page(
            route = "contract",
            editRoute = "contract/$ID_PLACEHOLDER",
            newEntryRoute = "contract/new",
        ),
        // Hand built list and edit pages of the inbox and the outbox ("Posteingang", "Postausgang",
        // PosteingangEntityRest, PostausgangEntityRest, components/features/post), migrated from React.
        "incomingMail" to Page(
            route = "incomingMail",
            editRoute = "incomingMail/$ID_PLACEHOLDER",
            newEntryRoute = "incomingMail/new",
        ),
        "outgoingMail" to Page(
            route = "outgoingMail",
            editRoute = "outgoingMail/$ID_PLACEHOLDER",
            newEntryRoute = "outgoingMail/new",
        ),
        // Hand built list and edit page of the visitor book (VisitorbookEntityRest, components/features/visitorbook),
        // the visits in a dialog of the edit page, migrated from React.
        "visitorbook" to Page(
            route = "visitorbook",
            editRoute = "visitorbook/$ID_PLACEHOLDER",
            newEntryRoute = "visitorbook/new",
        ),
        // The answer page (/poll/<id>/answer, PollAnswerRest) is a page of its own; the links of the poll mails to the
        // React answer page are redirected by OrphanedLinkFilter.
        "poll" to Page(
            route = "poll",
            editRoute = "poll/$ID_PLACEHOLDER",
            newEntryRoute = "poll/new",
        ),
        // Hand built list and edit page of the skill matrix (plugin skillmatrix, SkillEntryEntityRest,
        // components/features/skill-entry), migrated from React.
        "skillentry" to Page(
            route = "skillentry",
            editRoute = "skillentry/$ID_PLACEHOLDER",
            newEntryRoute = "skillentry/new",
        ),
        // Hand built list and edit page of the cases of the CVE-Radar (plugin cveradar, CvrCaseEntityRest,
        // components/features/cve-radar/cases), new in next: no legacy page, so no way back and no redirect
        // (legacyApp = null). A row opens the closure overview (`cveradar/closure/<id>`).
        "cvrcase" to Page(
            route = "cvrcase",
            editRoute = "cvrcase/$ID_PLACEHOLDER",
            newEntryRoute = "cvrcase/new",
            legacyApp = null,
        ),
        // The vulnerabilities imported from the NVD by the CVE-Radar (plugin cveradar, CvrVulnerabilityEntityRest),
        // listed on its start page (components/features/cve-radar/vulnerabilities). A row opens the file of the
        // vulnerability (`cveradar/vulnerability/<id>`); new in next: no legacy page, so no way back and no redirect
        // (legacyApp = null).
        "cvrvulnerability" to Page(
            route = "cveradar",
            editRoute = "cveradar/vulnerability/$ID_PLACEHOLDER",
            newEntryRoute = "cveradar/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the memos (plugin memo, MemoEntityRest, components/features/memo), migrated
        // from React.
        "memo" to Page(
            route = "memo",
            editRoute = "memo/$ID_PLACEHOLDER",
            newEntryRoute = "memo/new",
        ),
        "availability" to Page(
            route = "availability",
            editRoute = "availability/$ID_PLACEHOLDER",
            newEntryRoute = "availability/new",
        ),
        // The scripts every user may execute by the script configuration (MyScriptEntityRest, MyScriptExecuteRest): a
        // list whose row leads to the execution (`myscript/:id`), there is no form. Migrated from React like "script",
        // see there.
        "myscript" to Page(
            route = "myscript",
            editRoute = "myscript/$ID_PLACEHOLDER",
        // No form to add one: an old add link lands on the list.
            newEntryRoute = "myscript",
        ),
    )

    /**
     * The REST categories served by projectforge-next.
     *
     * Handed to projectforge-next by `UserStatusRest`; `PageRoutesTest` asserts that every route
     * of these has a page there.
     */
    val categories: Set<String>
        get() = PAGES.keys

    /**
     * @return true, if the page of the given REST category is served by projectforge-next.
     */
    fun isMigrated(category: String): Boolean {
        return PAGES.containsKey(category)
    }

    /**
     * Whether a frontend url points into projectforge-next, no matter which category produced it.
     *
     * Needed where only the url is left and the category isn't known any more - the menu is built from
     * [org.projectforge.menu.builder.MenuItemDefId], which carries the url of a page, not its category.
     *
     * @param url A frontend url with or without leading slash, e.g. `next/order`.
     */
    @JvmStatic
    fun isNextUrl(url: String?): Boolean {
        return url?.removePrefix("/")?.startsWith(Constants.NEXT_APP_PATH) == true
    }

    /**
     * @return The page of the given REST category in projectforge-next, or null, if there is none.
     */
    fun page(category: String): Page? {
        return PAGES[category]
    }

    /**
     * The REST categories whose page has the given route, e.g. `hrPlanning` for the route `hrPlanning`
     * or `outgoingInvoice` for the route `invoice`. Empty, if no page has this route.
     */
    fun categoriesOfRoute(route: String): Set<String> {
        return PAGES.filterValues { it.route == route }.keys
    }

    /**
     * @return [Constants.NEXT_APP_PATH] for categories with a page, [Constants.REACT_APP_PATH] otherwise.
     */
    fun appPath(category: String): String {
        return if (isMigrated(category)) Constants.NEXT_APP_PATH else Constants.REACT_APP_PATH
    }

    /**
     * The route segment to use in urls: the route of the page, or the REST category if there is no page.
     */
    fun routeOrCategory(category: String): String {
        return page(category)?.route ?: category
    }

    /**
     * @param category The REST category, e.g. `book` or `address`.
     * @return The frontend url of the list page without leading slash, e.g. `next/book`.
     */
    fun listUrl(category: String): String {
        return "${appPath(category)}${routeOrCategory(category)}"
    }

    /**
     * @return The frontend url template of the edit page with [ID_PLACEHOLDER] for the id, e.g.
     * `next/book/:id`.
     */
    fun standardEditPage(category: String): String {
        val route = page(category)?.editRoute ?: "$category/edit/$ID_PLACEHOLDER"
        return "${appPath(category)}$route"
    }

    /**
     * @return The frontend url for creating a new entry, e.g. `next/book/new`.
     */
    fun newEntryUrl(category: String): String {
        val route = page(category)?.newEntryRoute ?: "$category/edit"
        return "${appPath(category)}$route"
    }

    /**
     * An old list/edit/add page of a page with a [Page.legacyApp] and where it now leads, for `OrphanedLinkFilter`
     * to bend a bookmarked or emailed link onto projectforge-next.
     *
     * @property category The REST category of the page, e.g. `order`.
     * @property legacyApp The frontend the old urls belong to. Decides how the edit page carries the
     * id: Wicket as the `id` query parameter, the React app as a path segment (`/edit/<id>`).
     * @property legacyListPath The old list url without leading slash, e.g. `wa/orderBookList` or
     * `react/group`.
     * @property legacyEditPath The old edit url without leading slash and without the id, e.g.
     * `wa/orderBookEdit` or `react/group/edit`. The add page is mounted here too (no id).
     * @property nextListUrl The next list url with leading slash, e.g. `/next/order`.
     * @property nextEditUrl The next edit url with leading slash and [ID_PLACEHOLDER], e.g. `/next/order/:id`.
     * @property nextNewEntryUrl The next add url with leading slash, e.g. `/next/order/new`.
     */
    class OrphanedLink(
        val category: String,
        val legacyApp: LegacyApp,
        val legacyListPath: String,
        val legacyEditPath: String,
        val nextListUrl: String,
        val nextEditUrl: String,
        val nextNewEntryUrl: String,
    )

    /**
     * One [OrphanedLink] per page with a [Page.legacyApp]. Derived from [PAGES], so a page joins the moment it
     * declares one - nothing to maintain here. A category is redirected in its own legacy app only: a page from
     * Wicket orphans `/wa/...` links, one from React orphans `/react/...` links - that is where its old links pointed.
     */
    @JvmStatic
    fun orphanedLinks(): List<OrphanedLink> {
        return PAGES.entries.mapNotNull { (category, page) ->
            val app = page.legacyApp ?: return@mapNotNull null
            val editRoute = page.legacyEditRoute ?: app.editRoute(category)
            OrphanedLink(
                category = category,
                legacyApp = app,
                legacyListPath = "${app.appPath}${page.legacyRoute ?: app.listRoute(category)}",
                // The path only, no query: `wa/orderBookEdit?id=:id` -> `wa/orderBookEdit`, and
                // `react/group/edit/:id` -> `react/group/edit`. A request URI carries no query string.
                legacyEditPath = "${app.appPath}$editRoute".substringBefore('?').substringBefore(ID_PLACEHOLDER)
                    .trimEnd('/'),
                nextListUrl = "/${listUrl(category)}",
                nextEditUrl = "/${standardEditPage(category)}",
                nextNewEntryUrl = "/${newEntryUrl(category)}",
            )
        }
    }
}
