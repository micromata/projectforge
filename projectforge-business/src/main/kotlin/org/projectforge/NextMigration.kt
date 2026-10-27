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
 * Single source of truth for the per-page frontend choice during the migration of the web frontend
 * from the legacy React app (`projectforge-webapp`, served under [Constants.REACT_APP_PATH], removed) to
 * projectforge-next (served under [Constants.NEXT_APP_PATH]).
 *
 * Every page was switched on its own by adding a single entry to [MIGRATED]. The entries stay: they
 * hold the routes of the next pages and the redirects of old links (see [NextPage.legacyApp]).
 *
 * Why this has to be the only place deciding: the frontend url of a page is generated in two
 * unrelated places - the menu (`MenuItemDefId`) and every `ResponseAction(url = ...)` redirect
 * issued after save/cancel/delete (via `PagesResolver` in projectforge-rest). If those disagree,
 * the user is thrown from one frontend into the other in the middle of a workflow. Both therefore
 * ask this object instead of building urls themselves.
 *
 * Note that the REST category and the route of the next page may differ - a next page is free to
 * mount itself where it likes, e.g. under a route the category doesn't name. The hand built pages
 * deliberately use the category itself (`book`, `cost1`), so no plural has to be remembered.
 *
 * This object lives in projectforge-business (next to [Constants]) because `MenuItemDefId` does,
 * and projectforge-rest depends on projectforge-business, not the other way round.
 *
 * @author Kai Reinhard
 */
object NextMigration {
    /**
     * Placeholder for the id in [standardEditPage]. The frontend replaces it per row, so this is
     * part of the contract with both frontends and must not be changed.
     */
    const val ID_PLACEHOLDER = ":id"

    /**
     * The frontend a page came from. Not every page reached projectforge-next through the legacy
     * React app: the migration Wicket -> React never covered all of them, so some are migrated
     * straight from Wicket (`account` e.g.), and their way back leads there.
     *
     * The routes are the mount point conventions of the two apps, so a page normally needs nothing
     * but this value: React mounts `<category>`, `<category>/edit/<id>`, while Wicket mounts
     * `<category>List` and `<category>Edit?id=<id>` (see `WebRegistry.addMountPages`).
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
     * Defaults to the shape of the legacy React app (`<route>/edit/:id`), which no page of
     * projectforge-next serves: a page with an edit page sets it.
     * @param newEntryRoute Route for creating a new entry, e.g. `book/new`. Defaults to the
     * React shape `<route>/edit`, see [editRoute].
     * @param listOnly Only the list page is migrated, the form is still the legacy one: every url of
     * the edit page stays in [legacyApp], so a row click, the add button and every server side
     * redirect after a save lead there ([standardEditPage] and [newEntryUrl] answer the legacy url).
     * Set together with [legacyApp] - a page whose legacy implementation is gone cannot keep its form
     * there. [editRoute] / [newEntryRoute] are not what a row click leads to, but they are still read:
     * [nextEditPage] answers them, for the legacy form that links to a part of the next form it lacks
     * itself. No page is in that state at the moment - the invoice was the last one and is fully
     * migrated now - so this stays for the next list whose form follows a release later.
     * @param legacyApp The frontend this page was migrated from, i.e. the one its way back leads to,
     * or null once that page is gone: a page whose legacy implementation has been removed has no way
     * back, and [legacyListUrl] & co. answer null for it. Kept for a removed page only if its links have left
     * ProjectForge (sent by mail, e.g. a vacation notification, or shared by a data transfer area), so
     * [orphanedLinks] still bends them onto next; a page only ever linked from within ProjectForge gets null.
     * @param legacyRoute Route of the same list page in [legacyApp], without the app prefix. Only
     * needed if the page doesn't follow that app's mount point convention.
     * @param legacyEditRoute Route of the legacy edit page with [ID_PLACEHOLDER], without the app
     * prefix. Only needed if the page doesn't follow the convention either.
     * @param legacyNewEntryRoute Route of the legacy page for creating an entry, without the app prefix.
     * Only needed if the page doesn't follow the convention.
     * @param legacyListInMenu Whether the way back to the legacy *list* page is offered as an entry of
     * the list's gear menu instead of the prominent button (see `LegacyPageLink` / `ListGearMenu` in
     * projectforge-next). The escape hatch has to be loud while the new page still has gaps, so the
     * default is the button; once a page has been in use long enough to be trusted, this demotes it into
     * the menu per entity. Only the list page is affected - edit and other pages keep the button, so this
     * is read only next to [legacyListUrl] (see `ListMetaData.legacyListInMenu`), not for the edit page.
     * Moot once [offerLegacyLink] is false: with no way back offered, there is nothing to place.
     * @param offerLegacyLink Whether the way back to the legacy page (the "classic version" link) is
     * offered to the user at all - on the list *and* the edit page. The final step of a page's migration:
     * once the new page is trusted enough that the escape hatch is no longer wanted, this turns it off and
     * [legacyListUrl] / [legacyEditPage] / [legacyNewEntryUrl] answer null, so projectforge-next renders
     * neither button nor menu entry. Deliberately separate from [legacyApp]: the page keeps its
     * [legacyApp] so [orphanedLinks] still bends a bookmarked or emailed legacy url onto next - only the
     * *offer* of the way back is withdrawn, not the redirect of stale links. Always effectively true for a
     * page that isn't migrated - it is served by the legacy app itself.
     */
    class NextPage(
        val route: String,
        editRoute: String? = null,
        newEntryRoute: String? = null,
        val listOnly: Boolean = false,
        val legacyApp: LegacyApp? = LegacyApp.REACT,
        val legacyRoute: String? = null,
        val legacyEditRoute: String? = null,
        val legacyNewEntryRoute: String? = null,
        val legacyListInMenu: Boolean = false,
        val offerLegacyLink: Boolean = true,
    ) {
        val editRoute: String = editRoute ?: "$route/edit/$ID_PLACEHOLDER"
        val newEntryRoute: String = newEntryRoute ?: "$route/edit"

        init {
            require(!listOnly || legacyApp != null) {
                "A list only page keeps its form in the legacy app, so legacyApp must be given: route=$route"
            }
            require(legacyApp != LegacyApp.WICKET || !offerLegacyLink) {
                "Wicket is no way back any more (it is being removed), so offerLegacyLink must be false: route=$route"
            }
        }
    }

    /**
     * REST category -> page in projectforge-next. One entry means: this page is served by
     * projectforge-next, so the menu entry and all server side redirects point to `/next`.
     */
    private val MIGRATED = mapOf(
        // The access-rights management (group/task permission matrix), migrated from Wicket (MenuItemDefId.ACCESS_LIST
        // pointed at wa/accessList). Hand built (GroupAccessEntityRest, no layout) because the permission matrix is no
        // field a generic UILayout page can render. The Wicket pages are gone: no way back and no redirect of old links
        // (legacyApp = null), as no link of them ever left ProjectForge.
        "access" to NextPage(
            route = "access",
            editRoute = "access/$ID_PLACEHOLDER",
            newEntryRoute = "access/new",
            legacyApp = null,
        ),
        // The accounts ("Konten"), migrated from the React app (MenuItemDefId.ACCOUNT_LIST pointed at react/account),
        // whose page is gone with the layout of KontoPagesRest (now KontoEntityRest, no layout). The legacy pages are
        // gone: no way back and no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "account" to NextPage(
            route = "account",
            editRoute = "account/$ID_PLACEHOLDER",
            newEntryRoute = "account/new",
            legacyApp = null,
        ),
        // The DATEV accounting-record list ("Buchungssätze", MenuItemDefId.ACCOUNTING_RECORD_LIST pointed at
        // wa/accountingRecordList), migrated from Wicket with its filter, its BWA (business assessment) and the report
        // drill-down. Hand built (AccountingRecordEntityRest, no layout) so the list keeps the invoice-style
        // date-period filter and the BWA summary above the table. The Wicket pages are gone: no way back and no
        // redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "accountingRecord" to NextPage(
            route = "accounting-record",
            editRoute = "accounting-record/$ID_PLACEHOLDER",
            newEntryRoute = "accounting-record/new",
            legacyApp = null,
        ),
        // The address campaigns of the marketing plugin, migrated from Wicket (the menu entry pointed at the
        // bookmarkable AddressCampaignListPage). Hand built (AddressCampaignEntityRest, no layout; the server laid out
        // React page of AddressCampaignPagesRest is gone). The Wicket pages have been removed: no way back and no
        // redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "addressCampaign" to NextPage(
            route = "address-campaign",
            editRoute = "address-campaign/$ID_PLACEHOLDER",
            newEntryRoute = "address-campaign/new",
            legacyApp = null,
        ),
        // The values of the address campaigns (marketing plugin), migrated from React: hand built list, form and mass
        // update (AddressCampaignValueEntityRest, components/features/address-campaign-value). A row of an address
        // without a value yet has a synthetic (negative) id and opens the form for a new value
        // (`address-campaign-value/new?campaignId=&addressId=`). The React page is gone (no UILayout served any more):
        // no way back and no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "addressCampaignValue" to NextPage(
            route = "address-campaign-value",
            editRoute = "address-campaign-value/$ID_PLACEHOLDER",
            newEntryRoute = "address-campaign-value/new",
            legacyApp = null,
        ),
        // Hand built list, view and form of the addresses (AddressEntityRest, components/features/address), migrated
        // from React together with the VCF import (next/address/import, AddressImportRest). A row, a search hit and a
        // calendar birthday open the view (`address/:id`, see AddressEntityRest.getStandardEditPage); the form is a sub
        // route of it, as for `script`. No way back and no redirect of old links (legacyApp = null), as no link of them
        // ever left ProjectForge.
        "address" to NextPage(
            route = "address",
            editRoute = "address/$ID_PLACEHOLDER/edit",
            newEntryRoute = "address/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the address books (AddressbookEntityRest, components/features/addressBook),
        // migrated from React. The React pages are gone (no UILayout served any more): no way back and no redirect of
        // old links (legacyApp = null), as no link of them ever left ProjectForge.
        "addressBook" to NextPage(
            route = "addressBook",
            editRoute = "addressBook/$ID_PLACEHOLDER",
            newEntryRoute = "addressBook/new",
            legacyApp = null,
        ),
        // The bank accounts and their transactions of the banking plugin (BankAccountEntityRest,
        // BankAccountRecordEntityRest, components/features/bank-account*): hand built, the React pages are removed, so
        // there is no way back and no redirect of old links (legacyApp = null). The import of the transactions is the
        // page `bankAccount/import` (no category of its own).
        "bankAccount" to NextPage(
            route = "bankAccount",
            editRoute = "bankAccount/$ID_PLACEHOLDER",
            newEntryRoute = "bankAccount/new",
            legacyApp = null,
        ),
        "bankAccountRecord" to NextPage(
            route = "bankAccountRecord",
            editRoute = "bankAccountRecord/$ID_PLACEHOLDER",
            newEntryRoute = "bankAccountRecord/new",
            legacyApp = null,
        ),
        // Hand built feature, so its routes are /book, /book/new and /book/<id>. The React page it was
        // migrated from is removed (its layout is gone with BookEntityRest.createListLayout), so there is
        // no way back: legacyApp = null.
        "book" to NextPage(
            route = "book",
            editRoute = "book/$ID_PLACEHOLDER",
            newEntryRoute = "book/new",
            legacyApp = null,
        ),
        // The system-configuration page, migrated from Wicket (MenuItemDefId.CONFIGURATION pointed at
        // wa/configuration). Hand built (ConfigurationEntityRest, no layout): the value of a parameter is polymorphic
        // per ConfigurationType, so its edit field is rendered per type, which no generic UILayout page can do. The
        // parameter set is fixed - there is no add and no delete. The newEntryRoute is never asked for (insert is
        // denied) but is kept for the contract. The Wicket pages have been removed: no way back and no redirect of old
        // links (legacyApp = null), as no link of them ever left ProjectForge.
        "configuration" to NextPage(
            route = "configuration",
            editRoute = "configuration/$ID_PLACEHOLDER",
            newEntryRoute = "configuration/new",
            legacyApp = null,
        ),
        // Hand built calendar (the default page after login), migrated from the React app. No way back:
        // its time sheet and team event forms are served by layout free EntityRests only, so the React
        // calendar could not open an entry any more. react/calendar is redirected to next although no mail linked
        // it: it was the page after login, so it is what browsers offer and users keep as their start page. No edit
        // page of its own - a new or clicked entry opens a timesheet or team event through their own
        // routes - so the inherited editRoute / newEntryRoute defaults (calendar/edit/:id, calendar/edit)
        // are dead and never asked for.
        "calendar" to NextPage(
            route = "calendar",
            legacyApp = LegacyApp.REACT,
            offerLegacyLink = false,
        ),
        // Migrated from Wicket, which the React migration never reached (see MenuItemDefId.COST1_LIST, which pointed at
        // wa/cost1List). Kost1EntityRest serves no layout, so there was never a React page. The Wicket pages have been
        // removed: no way back and no redirect of old links (legacyApp = null), as no link of them ever left
        // ProjectForge.
        "cost1" to NextPage(
            route = "cost1",
            editRoute = "cost1/$ID_PLACEHOLDER",
            newEntryRoute = "cost1/new",
            legacyApp = null,
        ),
        // Migrated from Wicket (MenuItemDefId.COST2_LIST pointed at wa/cost2List). Kost2EntityRest serves no layout, so
        // there was never a React page. The Wicket pages have been removed: no way back and no redirect of old links
        // (legacyApp = null), as no link of them ever left ProjectForge.
        "cost2" to NextPage(
            route = "cost2",
            editRoute = "cost2/$ID_PLACEHOLDER",
            newEntryRoute = "cost2/new",
            legacyApp = null,
        ),
        // The cost-2 types ("Kost2-Arten"), migrated from Wicket (MenuItemDefId.COST2_TYPE_LIST pointed at
        // wa/cost2TypeList). Kost2ArtEntityRest serves no layout, so there was never a React page. The Wicket pages
        // have been removed: no way back and no redirect of old links (legacyApp = null), as no link of them ever left
        // ProjectForge.
        "cost2Type" to NextPage(
            route = "cost2Type",
            editRoute = "cost2Type/$ID_PLACEHOLDER",
            newEntryRoute = "cost2Type/new",
            legacyApp = null,
        ),
        // Migrated from Wicket (MenuItemDefId.CUSTOMER_LIST pointed at wa/customerList; the React page exists as a
        // layout, CustomerPagesRest, but was never mounted in the menu - "Doesn't work yet"). Hand built rather than
        // generic so the list keeps its filter row, saved filters, gear and Excel export. Customer favorites
        // (UserPrefArea.KUNDE_FAVORITE) are not migrated. The Wicket pages have been removed: no way back and no
        // redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "customer" to NextPage(
            route = "customer",
            editRoute = "customer/$ID_PLACEHOLDER",
            newEntryRoute = "customer/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the cases of the CVE-Radar (plugin cveradar, CvrCaseEntityRest,
        // components/features/cve-radar/cases), new in next: no legacy page, so no way back and no redirect
        // (legacyApp = null). A row opens the closure overview (`cveradar/closure/<id>`).
        "cvrcase" to NextPage(
            route = "cvrcase",
            editRoute = "cvrcase/$ID_PLACEHOLDER",
            newEntryRoute = "cvrcase/new",
            legacyApp = null,
        ),
        // The vulnerabilities imported from the NVD by the CVE-Radar (plugin cveradar, CvrVulnerabilityEntityRest),
        // listed on its start page (components/features/cve-radar/vulnerabilities). Read only and without a page of
        // their own yet; new in next: no legacy page, so no way back and no redirect (legacyApp = null).
        "cvrvulnerability" to NextPage(
            route = "cveradar",
            editRoute = "cveradar/$ID_PLACEHOLDER",
            newEntryRoute = "cveradar/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the employees (EmployeeEntityRest, components/features/employee), migrated
        // from React; the time-dependent attributes (status, weekly hours, annual leave) are edited in dialogs of the
        // form (`employee/validSinceAttr/...`). The React pages are gone (no UILayout served any more): no way back and
        // no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "employee" to NextPage(
            route = "employee",
            editRoute = "employee/$ID_PLACEHOLDER",
            newEntryRoute = "employee/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the currency pairs (CurrencyPairEntityRest,
        // components/features/currency-pair), migrated from React; the conversion rates of a pair are edited in dialogs
        // of the form (`currencyPair/rate/...`). The React pages are gone (no UILayout served any more): no way back
        // and no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "currencyPair" to NextPage(
            route = "currencyPair",
            editRoute = "currencyPair/$ID_PLACEHOLDER",
            newEntryRoute = "currencyPair/new",
            legacyApp = null,
        ),
        // Migrated from Wicket (MenuItemDefId.EMPLOYEE_SALARY_LIST pointed at wa/employeeSalaryList).
        // EmployeeSalaryEntityRest serves no layout, so there is no React page. The Wicket pages (the salary import
        // included, now a button in the next list) have been removed: no way back and no redirect of old links
        // (legacyApp = null), as no link of them ever left ProjectForge.
        "employeeSalary" to NextPage(
            route = "employeeSalary",
            editRoute = "employeeSalary/$ID_PLACEHOLDER",
            newEntryRoute = "employeeSalary/new",
            legacyApp = null,
        ),
        // The Gantt charts, migrated from Wicket (MenuItemDefId.GANTT pointed at wa/ganttList). GanttChartEntityRest
        // serves no layout, so there is no React page; the chart is still rendered by Batik on the server
        // (GanttServicesRest). The Wicket pages have been removed: no way back and no redirect of old links (legacyApp
        // = null), as no link of them ever left ProjectForge.
        "gantt" to NextPage(
            route = "gantt",
            editRoute = "gantt/$ID_PLACEHOLDER",
            newEntryRoute = "gantt/new",
            legacyApp = null,
        ),
        // Migrated from the React app (MenuItemDefId.GROUP_LIST pointed at react/group). The React pages are gone
        // (GroupEntityRest serves no UILayout): no way back and no redirect of old links (legacyApp = null), as no
        // link of them ever left ProjectForge.
        "group" to NextPage(
            route = "group",
            editRoute = "group/$ID_PLACEHOLDER",
            newEntryRoute = "group/new",
            legacyApp = null,
        ),
        // The HR planning ("Wochenplanung"), migrated from Wicket (MenuItemDefId.HR_PLANNING_LIST pointed at
        // wa/hrPlanningList) with its weekly form. Hand built: the list shows the entries of the planned weeks
        // (HRPlanningEntryEntityRest, category hrPlanningEntry) while a row opens the week itself
        // (HRPlanningEntityRest). The Wicket pages have been removed: no way back and no redirect of old links
        // (legacyApp = null), as no link of them ever left ProjectForge.
        "hrPlanning" to NextPage(
            route = "hrPlanning",
            editRoute = "hrPlanning/$ID_PLACEHOLDER",
            newEntryRoute = "hrPlanning/new",
            legacyApp = null,
        ),
        // The list of the HR planning above, under the category of its rows (HRPlanningEntryEntityRest): its list
        // metadata is the one of this category. Without an entry here the category counted as a page of the React
        // app, which has none, and the list offered a dead "classic version" link (react/hrPlanningEntry). The
        // entries have no page of their own (a row opens its week by the planning's id, not the entry's, so no
        // editRoute is given), and there is nothing to go back to either.
        "hrPlanningEntry" to NextPage(
            route = "hrPlanning",
            newEntryRoute = "hrPlanning/new",
            legacyApp = null,
        ),
        // The licenses (plugin licensemanagement) are migrated straight from Wicket and their Wicket pages are removed:
        // no way back and no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "license" to NextPage(
            route = "license",
            editRoute = "license/$ID_PLACEHOLDER",
            newEntryRoute = "license/new",
            legacyApp = null,
        ),
        // The liquidity-planning plugin, the first plugin migrated (from Wicket, list and forecast, form included).
        // Hand built (LiquidityEntityRest, no layout). The Wicket pages have been removed: no way back and no redirect
        // of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "liquidity" to NextPage(
            route = "liquidity",
            editRoute = "liquidity/$ID_PLACEHOLDER",
            newEntryRoute = "liquidity/new",
            legacyApp = null,
        ),
        // The rules of the notification system, new in next (NotificationRuleEntityRest, no layout): there is no
        // legacy page.
        "notificationRule" to NextPage(
            route = "notificationRule",
            editRoute = "notificationRule/$ID_PLACEHOLDER",
            newEntryRoute = "notificationRule/new",
            legacyApp = null,
        ),
        // Migrated from Wicket as well (MenuItemDefId.ORDER_LIST pointed at wa/orderBookList). Both legacy
        // routes have to be spelled out: the Wicket mount points are orderBookList / orderBookEdit
        // (WebRegistry, DaoConst.ORDERBOOK), so the convention would build orderList and the way back
        // would 404.
        "order" to NextPage(
            route = "order",
            editRoute = "order/$ID_PLACEHOLDER",
            newEntryRoute = "order/new",
            legacyApp = LegacyApp.WICKET,
            legacyRoute = "orderBookList",
            legacyEditRoute = "orderBookEdit?id=$ID_PLACEHOLDER",
            legacyNewEntryRoute = "orderBookEdit",
            // Trusted now: the way back is no longer offered (neither list menu nor edit button). legacyApp stays
            // so OrphanedLinkFilter still redirects the links of the order change mails (wa/orderBookEdit?id=).
            offerLegacyLink = false,
        ),
        // Migrated from Wicket, form included: the three document functions that used to be Wicket's alone - the Word
        // export, the XRechnung/ZUGFeRD export and the invoice PDF upload - are REST endpoints of
        // OutgoingInvoiceEntityRest now and are on the next form. The route is `invoice`, not the category: the entity
        // is "Rechnung" to its users, and which side of it the category names (outgoing vs. incoming) is what the menu
        // says, not what the url has to spell out. The Wicket pages have been removed: no way back and no redirect of
        // old links (legacyApp = null), as no link of them ever left ProjectForge.
        "outgoingInvoice" to NextPage(
            route = "invoice",
            editRoute = "invoice/$ID_PLACEHOLDER",
            newEntryRoute = "invoice/new",
            legacyApp = null,
        ),
        // Migrated from Wicket, list and form. The route is `creditor-invoice`, not the category: `invoice` is the
        // outgoing side, and this is the incoming (creditor) one - which side the category names is what the menu says.
        // Nothing is left behind on Wicket: the SEPA transfer export sits on the edit page and the mass update, the CSV        // import is next/creditor-invoice-import. The Wicket pages have been removed: no way back
        // and no redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "incomingInvoice" to NextPage(
            route = "creditor-invoice",
            editRoute = "creditor-invoice/$ID_PLACEHOLDER",
            newEntryRoute = "creditor-invoice/new",
            legacyApp = null,
        ),
        // The project list was the generic React page (ProjectPagesRest's layout), its form the Wicket ProjektEditPage
        // - the React form never got past "Not yet implemented". Both halves are hand built in projectforge-next now
        // and ProjectEntityRest serves no layout any more, so the React page is gone, and the Wicket list and form have
        // been removed as well: no way back and no redirect of old links (legacyApp = null), as no link of them ever
        // left ProjectForge. Project favorites (UserPrefArea.PROJEKT_FAVORITE) are not migrated.
        "project" to NextPage(
            route = "project",
            editRoute = "project/$ID_PLACEHOLDER",
            newEntryRoute = "project/new",
            legacyApp = null,
        ),
        // Migrated from Wicket (MenuItemDefId.TASK_TREE pointed at wa/taskTree). This entry is the *list* perspective
        // of the entity, /next/task, as for every other page - the structure tree is a second next page of the same
        // entity, under a route of its own (/next/taskTree, served by TaskServicesRest rather than by a list layout).
        // Only one of the two can be a NextPage.route, and it has to be the list: every server side redirect for the
        // category `task` goes through [listUrl], and a redirect after a save must not land on the tree. The menu opens
        // the tree (MenuItemDefId.TASK_TREE names next/taskTree itself). The legacy pages are gone: no way back and no
        // redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "task" to NextPage(
            route = "task",
            editRoute = "task/$ID_PLACEHOLDER",
            newEntryRoute = "task/new",
            legacyApp = null,
        ),
        // Hand built list and edit page (next/timesheet, next/timesheet/:id, next/timesheet/new). The two most-used
        // calendar editors were migrated ahead of the list; the list followed, so the menu entry now resolves through
        // [listUrl] (MenuItemDefId.TIMESHEET_LIST = getListUrl("timesheet")) rather than pointing at wa/timesheetList.
        // The legacy pages are gone: no way back and no redirect of old links (legacyApp = null), as no link of them
        // ever left ProjectForge.
        "timesheet" to NextPage(
            route = "timesheet",
            editRoute = "timesheet/$ID_PLACEHOLDER",
            newEntryRoute = "timesheet/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the team calendars (TeamCalEntityRest, components/features/teamCal),
        // migrated from React. The subscription info of a calendar (feed url, QR code) travels with the edit DTO, the
        // export feeds of the list (time sheets, week of years, holidays) come from calendarSubscription/info. The
        // React pages are gone (no UILayout served any more): no way back and no redirect of old links (legacyApp =
        // null), as no link of them ever left ProjectForge. (The feeds are /export/ProjectForge.ics, not the pages.)
        "teamCal" to NextPage(
            route = "teamCal",
            editRoute = "teamCal/$ID_PLACEHOLDER",
            newEntryRoute = "teamCal/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the users (UserEntityRest, components/features/user), migrated from React.
        // The rights of the form depend on the assigned groups and come from user/rights (instead of the watch field
        // re-layout of the React page). The React pages are gone (no UILayout served any more): no way back and no
        // redirect of old links (legacyApp = null), as no link of them ever left ProjectForge.
        "user" to NextPage(
            route = "user",
            editRoute = "user/$ID_PLACEHOLDER",
            newEntryRoute = "user/new",
            legacyApp = null,
        ),
        // Hand built *edit* page only (next/teamEvent/:id, next/teamEvent/new), alongside the timesheet
        // editor: the two most-used calendar editors are being migrated ahead of any list. There is no team
        // event list of this app - the event is reached through the calendar (see use-calendar-action.ts) -
        // so this repoints only the edit routes. The React team event form was rendered from a server side UILayout
        // under calendar/teamEvent, which is no longer served: no way back and no redirect of old links
        // (legacyApp = null), as no link of it ever left ProjectForge (the invitation mails link the /cal servlet).
        "teamEvent" to NextPage(
            route = "teamEvent",
            editRoute = "teamEvent/$ID_PLACEHOLDER",
            newEntryRoute = "teamEvent/new",
            legacyApp = null,
        ),
        // The to-dos (plugin todo) are migrated straight from Wicket and their Wicket pages are removed, as the
        // licenses. legacyApp and the legacy routes stay: the legacy edit route is the link of the notification
        // mails sent before, which OrphanedLinkFilter redirects.
        "todo" to NextPage(
            route = "todo",
            editRoute = "todo/$ID_PLACEHOLDER",
            newEntryRoute = "todo/new",
            legacyApp = LegacyApp.WICKET,
            legacyRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoListPage",
            legacyEditRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage?id=$ID_PLACEHOLDER",
            legacyNewEntryRoute = "wicket/bookmarkable/org.projectforge.plugins.todo.ToDoEditPage",
            offerLegacyLink = false,
        ),
        // The data transfer areas (plugin datatransfer), migrated from React and hand built: the list, the admin form
        // (DataTransferAreaEntityRest) and the file view of an area (DataTransferFilesRest). The row click leads to the
        // file view (`datatransfer/:id`), which everybody with access to the area gets; the admin form is a sub route
        // of it. The React pages are gone (no UILayout served any more), so there is no way back. The list and the
        // admin form were never linked from outside (legacyApp = null), but the file view, the activities and the
        // personal box were (react/datatransferfiles/dynamic/<id> & co., sent by the notification mails): they are
        // redirected by OrphanedLinkFilter.redirectDataTransferPage. The external access is
        // next/public/datatransfer/<token>, which the area's link points to; links sent before
        // (react/public/datatransfer/dynamic/<token>) are redirected by redirectPublicDataTransferPage.
        "datatransfer" to NextPage(
            route = "datatransfer",
            editRoute = "datatransfer/$ID_PLACEHOLDER/edit",
            newEntryRoute = "datatransfer/new",
            legacyApp = null,
        ),
        // The scripts of financial and controlling staff (ScriptEntityRest, ScriptExecuteRest), migrated from
        // React and hand built. As in React, the row click leads to the execution (`script/:id`); the form is a
        // sub route of it, the ad-hoc editor is `script/execute`. The React pages are gone (no UILayout served
        // any more), so there is no way back, and no link of them ever left ProjectForge (no mail, no data
        // transfer): old links are not redirected (legacyApp = null).
        "script" to NextPage(
            route = "script",
            editRoute = "script/$ID_PLACEHOLDER/edit",
            newEntryRoute = "script/new",
            legacyApp = null,
        ),
        // The Word® templates of the plugin merlin (MerlinEntityRest, MerlinExecuteRest, components/features/merlin),
        // migrated from React and hand built like "script": the row click leads to the execution (`merlin/:id`), the
        // admin form is a sub route of it. The React pages are gone (no UILayout served any more), so there is no
        // way back, and no link of them ever left ProjectForge (no mail, no data transfer): old links are not
        // redirected (legacyApp = null).
        "merlin" to NextPage(
            route = "merlin",
            editRoute = "merlin/$ID_PLACEHOLDER/edit",
            newEntryRoute = "merlin/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the vacation entries (VacationEntityRest, components/features/vacation),
        // migrated from React. The live statistics of the form (working days, days left, conflicts with the
        // replacements, the allowed statuses) come from `vacation/stats`, the replacement of the React watch
        // fields. The React pages are gone (no UILayout served any more), so there is no way back; the legacy app
        // is kept for the redirect of the links of the vacation mails sent before (react/vacation/edit/<id>).
        "vacation" to NextPage(
            route = "vacation",
            editRoute = "vacation/$ID_PLACEHOLDER",
            newEntryRoute = "vacation/new",
            legacyApp = LegacyApp.REACT,
            offerLegacyLink = false,
        ),
        // Hand built list and edit page of the leave account entries ("Urlaubskontokorrekturen",
        // LeaveAccountEntryEntityRest, components/features/leave-account-entry), migrated from React. The vacation
        // account opens an entry with `?returnTo=/vacationAccount`. The React pages are gone (no UILayout served any
        // more): no way back and no redirect of old links (legacyApp = null), as no link of them ever left
        // ProjectForge.
        "leaveAccountEntry" to NextPage(
            route = "leaveAccountEntry",
            editRoute = "leaveAccountEntry/$ID_PLACEHOLDER",
            newEntryRoute = "leaveAccountEntry/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the contracts (ContractEntityRest, components/features/contract), migrated
        // from React. The types to choose from come from `contract/types` (configuration parameter orga.contractTypes).
        // The React pages are gone (no UILayout served any more): no way back and no redirect of old links (legacyApp =
        // null), as no link of them ever left ProjectForge.
        "contract" to NextPage(
            route = "contract",
            editRoute = "contract/$ID_PLACEHOLDER",
            newEntryRoute = "contract/new",
            legacyApp = null,
        ),
        // Hand built list and edit pages of the inbox and the outbox ("Posteingang", "Postausgang",
        // PosteingangEntityRest, PostausgangEntityRest, components/features/post), migrated from React. The React
        // pages are gone (no UILayout served any more), so there is no way back, and no link of them ever left
        // ProjectForge (no mail, no data transfer): old links are not redirected (legacyApp = null).
        "incomingMail" to NextPage(
            route = "incomingMail",
            editRoute = "incomingMail/$ID_PLACEHOLDER",
            newEntryRoute = "incomingMail/new",
            legacyApp = null,
        ),
        "outgoingMail" to NextPage(
            route = "outgoingMail",
            editRoute = "outgoingMail/$ID_PLACEHOLDER",
            newEntryRoute = "outgoingMail/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the visitor book (VisitorbookEntityRest, components/features/visitorbook),
        // the visits in a dialog of the edit page, migrated from React. The React pages are gone (no UILayout served
        // any more): no way back and no redirect of old links (legacyApp = null), as no link of them ever left
        // ProjectForge.
        "visitorbook" to NextPage(
            route = "visitorbook",
            editRoute = "visitorbook/$ID_PLACEHOLDER",
            newEntryRoute = "visitorbook/new",
            legacyApp = null,
        ),
        // The polls (PollEntityRest, components/features/poll): hand built, the React pages are removed. The list and
        // the form were never linked from outside (legacyApp = null). The answer page (/poll/<id>/answer,
        // PollAnswerRest) is a page of its own; the links of the poll mails to the React answer page are redirected by
        // OrphanedLinkFilter.
        "poll" to NextPage(
            route = "poll",
            editRoute = "poll/$ID_PLACEHOLDER",
            newEntryRoute = "poll/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the skill matrix (plugin skillmatrix, SkillEntryEntityRest,
        // components/features/skill-entry), migrated from React. The React pages are gone (no UILayout served any
        // more): no way back and no redirect of old links (legacyApp = null), as no link of them ever left
        // ProjectForge.
        "skillentry" to NextPage(
            route = "skillentry",
            editRoute = "skillentry/$ID_PLACEHOLDER",
            newEntryRoute = "skillentry/new",
            legacyApp = null,
        ),
        // Hand built list and edit page of the memos (plugin memo, MemoEntityRest, components/features/memo), migrated
        // from React. The React pages are gone (no UILayout served any more): no way back and no redirect of old
        // links (legacyApp = null), as no link of them ever left ProjectForge.
        "memo" to NextPage(
            route = "memo",
            editRoute = "memo/$ID_PLACEHOLDER",
            newEntryRoute = "memo/new",
            legacyApp = null,
        ),
        // The availability entries (AvailabilityEntityRest, components/features/availability): hand built, migrated
        // from React before they were ever released: no way back and no redirect of old links (legacyApp = null), as no
        // link of them ever left ProjectForge.
        "availability" to NextPage(
            route = "availability",
            editRoute = "availability/$ID_PLACEHOLDER",
            newEntryRoute = "availability/new",
            legacyApp = null,
        ),
        // The scripts every user may execute by the script configuration (MyScriptEntityRest,
        // MyScriptExecuteRest): a list whose row leads to the execution (`myscript/:id`), there is no form.
        // Migrated from React like "script", see there.
        "myscript" to NextPage(
            route = "myscript",
            editRoute = "myscript/$ID_PLACEHOLDER",
            // No form to add one: an old add link lands on the list.
            newEntryRoute = "myscript",
            legacyApp = null,
        ),
    )

    /**
     * The REST categories served by projectforge-next.
     *
     * Handed to projectforge-next by `UserStatusRest`; `NextMigrationTest` asserts that every route
     * of these has a page there.
     */
    val categories: Set<String>
        get() = MIGRATED.keys

    /**
     * @return true, if the page of the given REST category is served by projectforge-next.
     */
    fun isMigrated(category: String): Boolean {
        return MIGRATED.containsKey(category)
    }

    /**
     * Whether the legacy page reachable at [path] has a projectforge-next version, i.e. whether the old
     * frontends should point the user to the new version from it. The old frontends ([LegacyApp.WICKET]
     * and [LegacyApp.REACT]) show the "use the new version" hint only where there actually is one.
     *
     * [path] is the request path of the legacy page without the app prefix and without query, e.g.
     * `cost1List`, `orderBookEdit`, `group` or `group/edit/42`. It is matched against the legacy list,
     * edit and add routes of every migrated page of [legacyApp] (the same routes [orphanedLinks] bends
     * onto next), so both the list and the edit/add page of a migrated entity count.
     */
    @JvmStatic
    fun hasNextVersion(legacyApp: LegacyApp, path: String): Boolean {
        val normalized = path.removePrefix("/").removePrefix(legacyApp.appPath).substringBefore('?').trimEnd('/')
        return MIGRATED.any { (category, page) ->
            if (page.legacyApp != legacyApp) return@any false
            val listRoute = (page.legacyRoute ?: legacyApp.listRoute(category)).trimEnd('/')
            val editRoute = (page.legacyEditRoute ?: legacyApp.editRoute(category))
                .substringBefore('?').substringBefore(ID_PLACEHOLDER).trimEnd('/')
            normalized == listRoute || normalized == editRoute || normalized.startsWith("$editRoute/")
        }
    }

    /**
     * Whether a frontend url points into projectforge-next, no matter which category produced it.
     *
     * Needed where only the url is left and the category isn't known any more - the menu is built from
     * [org.projectforge.menu.builder.MenuItemDefId], which carries the url of a page, not its category
     * (see `WicketMenuBuilder`).
     *
     * @param url A frontend url with or without leading slash, e.g. `next/order` or `/wa/cost1List`.
     */
    @JvmStatic
    fun isNextUrl(url: String?): Boolean {
        return url?.removePrefix("/")?.startsWith(Constants.NEXT_APP_PATH) == true
    }

    /**
     * @return The page of the given REST category in projectforge-next, or null, if not migrated.
     */
    fun nextPage(category: String): NextPage? {
        return MIGRATED[category]
    }

    /**
     * The REST categories whose next page has the given route, e.g. `hrPlanning` for the route `hrPlanning`
     * or `outgoingInvoice` for the route `invoice`. Empty, if no migrated page has this route.
     */
    fun categoriesOfRoute(route: String): Set<String> {
        return MIGRATED.filterValues { it.route == route }.keys
    }

    /**
     * @return [Constants.NEXT_APP_PATH] for migrated pages, [Constants.REACT_APP_PATH] otherwise.
     */
    fun appPath(category: String): String {
        return if (isMigrated(category)) Constants.NEXT_APP_PATH else Constants.REACT_APP_PATH
    }

    /**
     * The route segment to use in urls: the next route for migrated pages, the REST category
     * otherwise (the legacy React app uses the category as its route).
     */
    fun routeOrCategory(category: String): String {
        return nextPage(category)?.route ?: category
    }

    /**
     * @param category The REST category, e.g. `book` or `address`.
     * @return The frontend url of the list page without leading slash, e.g. `next/book`
     * or `react/address`.
     */
    fun listUrl(category: String): String {
        return "${appPath(category)}${routeOrCategory(category)}"
    }

    /**
     * @return The frontend url template of the edit page with [ID_PLACEHOLDER] for the id, e.g.
     * `next/book/:id` or `react/address/edit/:id`. For a [NextPage.listOnly] page the legacy edit page,
     * e.g. `wa/<category>Edit?id=:id`: that is where its form is.
     */
    fun standardEditPage(category: String): String {
        val page = nextPage(category)
        if (page?.listOnly == true) {
            // Raw, not the offer-gated one: a listOnly page's form is the legacy one, so this is a
            // functional route (where a row click leads), not the escape hatch that [offerLegacyLink]
            // may have withdrawn. Not null: a listOnly page has a legacyApp (see NextPage.init).
            return legacyEditPageRaw(category)!!
        }
        val route = page?.editRoute ?: "$category/edit/$ID_PLACEHOLDER"
        return "${appPath(category)}$route"
    }

    /**
     * The edit page in projectforge-next, whether or not it is the one a row click leads to.
     *
     * Not [standardEditPage], which answers the legacy url for a [NextPage.listOnly] page: this one names
     * the next form even then. For the legacy form that links into it for a part it lacks itself, as
     * `RechnungEditForm` does for the invoice's attachments - that link stays useful now that the invoice
     * is fully migrated, since Wicket's form is still reachable through the escape hatch.
     *
     * @return e.g. `next/invoice/:id` with [ID_PLACEHOLDER] for the id, or null if the category is not
     * migrated at all.
     */
    @JvmStatic
    fun nextEditPage(category: String): String? {
        val page = nextPage(category) ?: return null
        return "${Constants.NEXT_APP_PATH}${page.editRoute}"
    }

    /**
     * @return The frontend url for creating a new entry, e.g. `next/book/new` or
     * `react/address/edit`, and the legacy one for a [NextPage.listOnly] page.
     */
    fun newEntryUrl(category: String): String {
        val page = nextPage(category)
        if (page?.listOnly == true) {
            // Raw, not the offer-gated one: the add destination of a listOnly page is a functional route,
            // not the escape hatch [offerLegacyLink] may have withdrawn (see standardEditPage).
            return legacyNewEntryUrlRaw(category)!!
        }
        val route = page?.newEntryRoute ?: "$category/edit"
        return "${appPath(category)}$route"
    }

    /**
     * The way back: the same list page in the frontend the page came from, offered on the next page
     * as an escape hatch while the migration runs (see `LegacyPageLink` in projectforge-next).
     *
     * Needed as its own mapping because [listUrl] is a one way function once a page is migrated -
     * the app is not recoverable from the next url: a page may have been migrated straight from
     * Wicket, which the React migration never reached (`cost1`), and its route need not name the
     * category either.
     *
     * @return The frontend url of the legacy list page without leading slash, e.g. `react/book` or
     * `wa/cost1List`, or null if this page has no legacy counterpart any more.
     */
    fun legacyListUrl(category: String): String? {
        if (!offersLegacyLink(category)) return null
        return legacyListUrlRaw(category)
    }

    /**
     * The legacy list url regardless of whether the way back is still offered to the user (see
     * [offerLegacyLink]): the raw counterpart of [legacyListUrl], for [orphanedLinks], which must keep
     * bending bookmarked legacy urls onto next even for a page whose escape hatch has been withdrawn.
     */
    private fun legacyListUrlRaw(category: String): String? {
        val page = nextPage(category)
        val app = legacyApp(category) ?: return null
        return "${app.appPath}${page?.legacyRoute ?: app.listRoute(category)}"
    }

    /**
     * Whether the way back to the legacy *list* page is offered in the list's gear menu instead of the
     * prominent button (see [NextPage.legacyListInMenu]). False for a page that isn't migrated - it is
     * served by the legacy app itself and has no such choice to make - and for one that no longer offers
     * the way back at all (see [offerLegacyLink]).
     */
    fun legacyListInMenu(category: String): Boolean {
        val page = nextPage(category) ?: return false
        return page.offerLegacyLink && page.legacyListInMenu
    }

    /**
     * Whether the way back to the legacy page is offered to the user for this category (see
     * [NextPage.offerLegacyLink]). Always true for a page that isn't migrated - it is served by the
     * legacy app itself.
     */
    private fun offersLegacyLink(category: String): Boolean {
        return nextPage(category)?.offerLegacyLink != false
    }

    /**
     * The frontend the legacy urls of this category point into: the one the page was migrated from, or
     * [LegacyApp.REACT] for a page that isn't migrated at all (it is served by the React app, so that
     * *is* its current frontend).
     *
     * @return null if the page is migrated and its legacy implementation has been removed.
     */
    private fun legacyApp(category: String): LegacyApp? {
        val page = nextPage(category) ?: return LegacyApp.REACT
        return page.legacyApp
    }

    /**
     * @return The frontend url template of the legacy edit page with [ID_PLACEHOLDER] for the id,
     * e.g. `react/book/edit/:id` or `wa/cost1Edit?id=:id`, or null if this page has no legacy
     * counterpart any more.
     */
    fun legacyEditPage(category: String): String? {
        if (!offersLegacyLink(category)) return null
        return legacyEditPageRaw(category)
    }

    /**
     * The legacy edit url regardless of whether the way back is still offered (see [offerLegacyLink]):
     * the raw counterpart of [legacyEditPage], for [orphanedLinks] and for the row-click destination of a
     * [NextPage.listOnly] page (whose form is the legacy one and must stay reachable even with the escape
     * hatch withdrawn).
     */
    private fun legacyEditPageRaw(category: String): String? {
        val page = nextPage(category)
        val app = legacyApp(category) ?: return null
        return "${app.appPath}${page?.legacyEditRoute ?: app.editRoute(category)}"
    }

    /**
     * @return The frontend url of the legacy page for creating a new entry, e.g. `react/book/edit`
     * or `wa/cost1Edit`, or null if this page has no legacy counterpart any more.
     */
    fun legacyNewEntryUrl(category: String): String? {
        if (!offersLegacyLink(category)) return null
        return legacyNewEntryUrlRaw(category)
    }

    /**
     * The legacy add url regardless of whether the way back is still offered (see [offerLegacyLink]): the
     * raw counterpart of [legacyNewEntryUrl], for the add destination of a [NextPage.listOnly] page, whose
     * form is the legacy one and must stay reachable even with the escape hatch withdrawn.
     */
    private fun legacyNewEntryUrlRaw(category: String): String? {
        val page = nextPage(category)
        val app = legacyApp(category) ?: return null
        return "${app.appPath}${page?.legacyNewEntryRoute ?: app.newEntryRoute(category)}"
    }

    /**
     * Query parameter that marks a link as the "way back" escape hatch (see `LegacyPageLink` in
     * projectforge-next): `OrphanedLinkFilter` lets a request carrying it through to the legacy page
     * instead of bending it back to projectforge-next. Every other request to a migrated legacy url is a
     * bookmark or a link from an e-mail sent before the migration, and gets redirected.
     */
    const val ESCAPE_HATCH_PARAM = "legacyEscape"

    /**
     * Marks a legacy url as the escape hatch, so `OrphanedLinkFilter` passes it through. Applied to the
     * "way back" urls the server hands projectforge-next (`ListMetaData.legacyListPage`,
     * `legacyEditPage`, `legacyNewEntryPage`), which is where every escape hatch reads them from.
     *
     * @return [legacyUrl] with [ESCAPE_HATCH_PARAM] appended, or null if [legacyUrl] is null.
     */
    fun withEscapeHatchMarker(legacyUrl: String?): String? {
        legacyUrl ?: return null
        val separator = if (legacyUrl.contains('?')) '&' else '?'
        return "$legacyUrl$separator$ESCAPE_HATCH_PARAM"
    }

    /**
     * A legacy list/edit/add page of a migrated entity and where it now leads, for `OrphanedLinkFilter`
     * to bend a bookmarked or emailed link onto projectforge-next.
     *
     * @property category The REST category of the page, e.g. `order`.
     * @property legacyApp The frontend the legacy urls belong to. Decides how the edit page carries the
     * id: Wicket as the `id` query parameter, the React app as a path segment (`/edit/<id>`).
     * @property legacyListPath The legacy list url without leading slash, e.g. `wa/orderBookList` or
     * `react/group`.
     * @property legacyEditPath The legacy edit url without leading slash and without the id, e.g.
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
     * One [OrphanedLink] per migrated page whose form was migrated too. Derived from [MIGRATED], so a page
     * joins the moment it is migrated - nothing to maintain here.
     *
     * A [NextPage.listOnly] page keeps its legacy form, so its edit link must not be bent away from it and
     * it is left out. A category is redirected in its own legacy app only: a page migrated from Wicket
     * orphans `/wa/...` links, one migrated from React orphans `/react/...` links - that is where its old
     * links pointed. A page whose legacy implementation is gone (`legacyApp == null`) has no legacy url to
     * orphan.
     */
    @JvmStatic
    fun orphanedLinks(): List<OrphanedLink> {
        return MIGRATED.entries.mapNotNull { (category, page) ->
            val app = page.legacyApp
            if (app == null || page.listOnly) {
                return@mapNotNull null
            }
            OrphanedLink(
                category = category,
                legacyApp = app,
                // Raw, not the offer-gated accessors: a bookmarked or emailed legacy url is redirected onto
                // next whether or not the way back is still offered (see offerLegacyLink). Non-null:
                // legacyApp is set (see legacyListUrlRaw / legacyEditPageRaw).
                legacyListPath = legacyListUrlRaw(category)!!,
                // The path only, no query: `wa/orderBookEdit?id=:id` -> `wa/orderBookEdit`, and
                // `react/group/edit/:id` -> `react/group/edit`. A request URI carries no query string.
                legacyEditPath = legacyEditPageRaw(category)!!.substringBefore('?').substringBefore(ID_PLACEHOLDER).trimEnd('/'),
                nextListUrl = "/${listUrl(category)}",
                nextEditUrl = "/${standardEditPage(category)}",
                nextNewEntryUrl = "/${newEntryUrl(category)}",
            )
        }
    }
}
