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
 * from the legacy React app (`projectforge-webapp`, served under [Constants.REACT_APP_PATH]) to
 * projectforge-next (served under [Constants.NEXT_APP_PATH]).
 *
 * Both frontends render the same server side generated `UILayout`, so a page can be switched
 * independently of all others. Switching one page means adding a single entry to [MIGRATED];
 * rolling it back means removing that entry again.
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
     * straight from Wicket (`cost1` e.g.), and their way back leads there.
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
     * Defaults to the shape of the generic UILayout routes (`<route>/edit/:id`), which mirrors the
     * legacy React app. Hand built pages may deviate.
     * @param newEntryRoute Route for creating a new entry, e.g. `book/new`. Defaults to the
     * generic shape `<route>/edit`.
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
     * back, and [legacyListUrl] & co. answer null for it.
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
    ) {
        val editRoute: String = editRoute ?: "$route/edit/$ID_PLACEHOLDER"
        val newEntryRoute: String = newEntryRoute ?: "$route/edit"

        init {
            require(!listOnly || legacyApp != null) {
                "A list only page keeps its form in the legacy app, so legacyApp must be given: route=$route"
            }
        }
    }

    /**
     * REST category -> page in projectforge-next. One entry means: this page is served by
     * projectforge-next, so the menu entry and all server side redirects point to `/next`.
     */
    private val MIGRATED = mapOf(
        // Hand built feature, so its routes are /book, /book/new and /book/<id>. The React page it was
        // migrated from is removed (its layout is gone with BookEntityRest.createListLayout), so there is
        // no way back: legacyApp = null.
        "book" to NextPage(
            route = "book",
            editRoute = "book/$ID_PLACEHOLDER",
            newEntryRoute = "book/new",
            legacyApp = null,
        ),
        // Hand built calendar (the default page after login), migrated from the React app, which is where
        // the way back leads (react/calendar). It has no edit page of its own - a new or clicked entry
        // opens a timesheet or team event through their own dynamic routes - so the inherited editRoute /
        // newEntryRoute defaults (calendar/edit/:id, calendar/edit) are dead and never asked for.
        "calendar" to NextPage(
            route = "calendar",
            legacyApp = LegacyApp.REACT,
        ),
        // Migrated from Wicket, which the React migration never reached (see MenuItemDefId.COST1_LIST,
        // which pointed at wa/cost1List): the way back leads to Wicket, not to the React page - that one
        // exists as a layout (Kost1PagesRest) but was never mounted in the menu.
        "cost1" to NextPage(
            route = "cost1",
            editRoute = "cost1/$ID_PLACEHOLDER",
            newEntryRoute = "cost1/new",
            legacyApp = LegacyApp.WICKET,
        ),
        // Migrated from the React app (MenuItemDefId.GROUP_LIST pointed at react/group), which is where the
        // way back leads. Hand built rather than generic because the React list has a filter of its own
        // (the group type) and an Excel export, neither of which the generic UILayout route renders.
        "group" to NextPage(
            route = "group",
            editRoute = "group/$ID_PLACEHOLDER",
            newEntryRoute = "group/new",
            legacyApp = LegacyApp.REACT,
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
            // In use long enough to trust the new list: the way back moves into the gear menu.
            legacyListInMenu = true,
        ),
        // Migrated from Wicket, form included: the three document functions that used to be Wicket's alone
        // - the Word export, the XRechnung/ZUGFeRD export and the invoice PDF upload - are REST endpoints
        // of OutgoingInvoiceEntityRest now and are on the next form, so nothing is lost by clicking a row
        // (this is what dropped `listOnly`; wa/outgoingInvoiceEdit stays reachable through the escape
        // hatch, see legacyEditPage).
        // The route is `invoice`, not the category: the entity is "Rechnung" to its users, and which side
        // of it the category names (outgoing vs. incoming) is what the menu says, not what the url has to
        // spell out. Wicket's mount points follow the convention (DaoConst.OUTGOING_INVOICE +
        // List/Edit), so no legacy route has to be spelled out.
        "outgoingInvoice" to NextPage(
            route = "invoice",
            editRoute = "invoice/$ID_PLACEHOLDER",
            newEntryRoute = "invoice/new",
            legacyApp = LegacyApp.WICKET,
            // In use long enough to trust the new list: the way back moves into the gear menu.
            legacyListInMenu = true,
        ),
        // Migrated from Wicket, list and form. The route is `creditor-invoice`, not the category: `invoice`
        // is the outgoing side, and this is the incoming (creditor) one - which side the category names is
        // what the menu says. The CSV/SEPA import wizard and the SEPA transfer export stay on Wicket for now
        // (see MIGRATION.md), so wa/incomingInvoiceEdit stays reachable through the escape hatch. Wicket's
        // mount points follow the convention (DaoConst.INCOMING_INVOICE + List/Edit), so no legacy route has
        // to be spelled out.
        "incomingInvoice" to NextPage(
            route = "creditor-invoice",
            editRoute = "creditor-invoice/$ID_PLACEHOLDER",
            newEntryRoute = "creditor-invoice/new",
            legacyApp = LegacyApp.WICKET,
            // In use long enough to trust the new list: the way back moves into the gear menu.
            legacyListInMenu = true,
        ),
        // Migrated from Wicket (MenuItemDefId.TASK_TREE pointed at wa/taskTree). This entry is the
        // *list* perspective of the entity, /next/task, as for every other page - the structure tree is
        // a second next page of the same entity, under a route of its own (/next/taskTree, served by
        // TaskServicesRest rather than by a list layout). Only one of the two can be a NextPage.route,
        // and it has to be the list: every server side redirect for the category `task` goes through
        // [listUrl], and a redirect after a save must not land on the tree.
        // Which of the two the menu opens is [nextRouteUrl]'s answer, not this route's (see
        // MenuItemDefId.TASK_TREE). The legacy routes follow Wicket's convention - taskList / taskEdit
        // are its mount points (WebRegistry, DaoConst.TASK) - so none has to be spelled out; the tree
        // page names wa/taskTree itself, which is where the two entries of that page next has not
        // migrated still are (the task favourites and the task wizard).
        "task" to NextPage(
            route = "task",
            editRoute = "task/$ID_PLACEHOLDER",
            newEntryRoute = "task/new",
            legacyApp = LegacyApp.WICKET,
        ),
        // Hand built list and edit page (next/timesheet, next/timesheet/:id, next/timesheet/new). The two
        // most-used calendar editors were migrated ahead of the list; the list followed, so the menu entry
        // now resolves through [listUrl] (MenuItemDefId.TIMESHEET_LIST = getListUrl("timesheet")) rather
        // than pointing at wa/timesheetList. The way back leads to the React app, whose timesheet list and
        // form were rendered from the same UILayout before.
        "timesheet" to NextPage(
            route = "timesheet",
            editRoute = "timesheet/$ID_PLACEHOLDER",
            newEntryRoute = "timesheet/new",
            legacyApp = LegacyApp.REACT,
        ),
        // Hand built *edit* page only (next/teamEvent/:id, next/teamEvent/new), alongside the timesheet
        // editor: the two most-used calendar editors are being migrated ahead of any list. There is no team
        // event list of this app - the event is reached through the calendar (see use-calendar-action.ts) -
        // so this repoints only the edit routes. The way back leads to the React app, whose team event form
        // was rendered from the same UILayout under calendar/teamEvent (TeamEventPagesRest.getRestEditPath).
        "teamEvent" to NextPage(
            route = "teamEvent",
            editRoute = "teamEvent/$ID_PLACEHOLDER",
            newEntryRoute = "teamEvent/new",
            legacyApp = LegacyApp.REACT,
            legacyEditRoute = "calendar/teamEvent/edit/$ID_PLACEHOLDER",
            legacyNewEntryRoute = "calendar/teamEvent/edit",
        ),
    )

    /**
     * The REST categories served by projectforge-next.
     *
     * Exposed for `NextMigrationTest`, which asserts that this set is the one
     * `HAND_BUILT_CATEGORIES` in projectforge-next names: the two lists encode the same decision in
     * two languages, and a category missing from either one fails silently - the frontend serves the
     * generic UILayout page under `next/<category>`, or the menu points at a page that isn't there.
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
     * A *second* next page of an already migrated entity, under a route the category doesn't name.
     *
     * Only for a page whose entity has more than one perspective in projectforge-next: the task has its
     * list (`next/task`, the [NextPage.route]) and its structure tree (`next/taskTree`), and the menu
     * entry opens the tree while every redirect for the category goes to the list. The route cannot be
     * derived from the category, so the caller names it - and it stays tied to [MIGRATED] all the same:
     * as long as the entity isn't migrated, the answer is the legacy url the caller passes.
     *
     * @param category The REST category the page belongs to, e.g. `task`.
     * @param route The route inside projectforge-next, without the `next/` prefix, e.g. `taskTree`.
     * @param legacyUrl The url to use while [category] is not migrated, e.g. `wa/taskTree`.
     * @return e.g. `next/taskTree`, or [legacyUrl].
     */
    fun nextRouteUrl(category: String, route: String, legacyUrl: String): String {
        return if (isMigrated(category)) "${Constants.NEXT_APP_PATH}$route" else legacyUrl
    }

    /**
     * @return The frontend url template of the edit page with [ID_PLACEHOLDER] for the id, e.g.
     * `next/book/:id` or `react/address/edit/:id`. For a [NextPage.listOnly] page the legacy edit page,
     * e.g. `wa/<category>Edit?id=:id`: that is where its form is.
     */
    fun standardEditPage(category: String): String {
        val page = nextPage(category)
        if (page?.listOnly == true) {
            // Not null: a listOnly page has a legacyApp (see NextPage.init).
            return legacyEditPage(category)!!
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
            return legacyNewEntryUrl(category)!!
        }
        val route = page?.newEntryRoute ?: "$category/edit"
        return "${appPath(category)}$route"
    }

    /**
     * The generic React url of the edit page, for a list request that came from the legacy React app
     * itself (see `AbstractPagesRest.getEditPage`).
     *
     * Not [legacyEditPage]: that one names the page the *way back* leads to, which may be a Wicket
     * page. Wicket renders its own pages server side and never asks this REST for a layout, so the
     * only non-next caller is the React app - and a user clicking a row there stays there.
     *
     * @return e.g. `react/book/edit/:id` or `react/cost1/edit/:id`.
     */
    fun reactEditPage(category: String): String {
        return "${Constants.REACT_APP_PATH}${LegacyApp.REACT.editRoute(category)}"
    }

    /**
     * The generic React url for adding an entry, for a request from the React app itself.
     *
     * @return e.g. `react/book/edit`.
     */
    fun reactNewEntryUrl(category: String): String {
        return "${Constants.REACT_APP_PATH}${LegacyApp.REACT.newEntryRoute(category)}"
    }

    /**
     * The generic React url of the list page, for a request from the React app itself.
     *
     * @return e.g. `react/book`.
     */
    fun reactListUrl(category: String): String {
        return "${Constants.REACT_APP_PATH}${LegacyApp.REACT.listRoute(category)}"
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
        val page = nextPage(category)
        val app = legacyApp(category) ?: return null
        return "${app.appPath}${page?.legacyRoute ?: app.listRoute(category)}"
    }

    /**
     * Whether the way back to the legacy *list* page is offered in the list's gear menu instead of the
     * prominent button (see [NextPage.legacyListInMenu]). False for a page that isn't migrated - it is
     * served by the legacy app itself and has no such choice to make.
     */
    fun legacyListInMenu(category: String): Boolean {
        return nextPage(category)?.legacyListInMenu == true
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
        val page = nextPage(category)
        val app = legacyApp(category) ?: return null
        return "${app.appPath}${page?.legacyEditRoute ?: app.editRoute(category)}"
    }

    /**
     * @return The frontend url of the legacy page for creating a new entry, e.g. `react/book/edit`
     * or `wa/cost1Edit`, or null if this page has no legacy counterpart any more.
     */
    fun legacyNewEntryUrl(category: String): String? {
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
     * "way back" urls the server hands projectforge-next ([AbstractPagesRest]'s `legacyUrl`,
     * `legacyEditPage`, `legacyNewEntryPage`), which is where every escape hatch - hand built pages
     * included - reads them from.
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
                legacyApp = app,
                // Non-null: legacyApp is set (see legacyListUrl / legacyEditPage).
                legacyListPath = legacyListUrl(category)!!,
                // The path only, no query: `wa/orderBookEdit?id=:id` -> `wa/orderBookEdit`, and
                // `react/group/edit/:id` -> `react/group/edit`. A request URI carries no query string.
                legacyEditPath = legacyEditPage(category)!!.substringBefore('?').substringBefore(ID_PLACEHOLDER).trimEnd('/'),
                nextListUrl = "/${listUrl(category)}",
                nextEditUrl = "/${standardEditPage(category)}",
                nextNewEntryUrl = "/${newEntryUrl(category)}",
            )
        }
    }
}
