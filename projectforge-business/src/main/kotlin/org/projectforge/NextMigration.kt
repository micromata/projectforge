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
 * @author Kai Reinhard (k.reinhard@micromata.de)
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
     * @param legacyApp The frontend this page was migrated from, i.e. the one its way back leads to.
     * @param legacyRoute Route of the same list page in [legacyApp], without the app prefix. Only
     * needed if the page doesn't follow that app's mount point convention.
     * @param legacyEditRoute Route of the legacy edit page with [ID_PLACEHOLDER], without the app
     * prefix. Only needed if the page doesn't follow the convention either.
     */
    class NextPage(
        val route: String,
        editRoute: String? = null,
        newEntryRoute: String? = null,
        val legacyApp: LegacyApp = LegacyApp.REACT,
        val legacyRoute: String? = null,
        val legacyEditRoute: String? = null,
    ) {
        val editRoute: String = editRoute ?: "$route/edit/$ID_PLACEHOLDER"
        val newEntryRoute: String = newEntryRoute ?: "$route/edit"
    }

    /**
     * REST category -> page in projectforge-next. One entry means: this page is served by
     * projectforge-next, so the menu entry and all server side redirects point to `/next`.
     */
    private val MIGRATED = mapOf(
        // Hand built feature, so its routes are /book, /book/new and /book/<id>.
        "book" to NextPage(route = "book", editRoute = "book/$ID_PLACEHOLDER", newEntryRoute = "book/new"),
        // Migrated from Wicket, which the React migration never reached (see MenuItemDefId.COST1_LIST,
        // which pointed at wa/cost1List): the way back leads to Wicket, not to the React page - that one
        // exists as a layout (Kost1PagesRest) but was never mounted in the menu.
        "cost1" to NextPage(
            route = "cost1",
            editRoute = "cost1/$ID_PLACEHOLDER",
            newEntryRoute = "cost1/new",
            legacyApp = LegacyApp.WICKET,
        ),
    )

    /**
     * @return true, if the page of the given REST category is served by projectforge-next.
     */
    fun isMigrated(category: String): Boolean {
        return MIGRATED.containsKey(category)
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
     * @return The frontend url template of the edit page with [ID_PLACEHOLDER] for the id, e.g.
     * `next/book/:id` or `react/address/edit/:id`.
     */
    fun standardEditPage(category: String): String {
        val route = nextPage(category)?.editRoute ?: "$category/edit/$ID_PLACEHOLDER"
        return "${appPath(category)}$route"
    }

    /**
     * @return The frontend url for creating a new entry, e.g. `next/book/new` or
     * `react/address/edit`.
     */
    fun newEntryUrl(category: String): String {
        val route = nextPage(category)?.newEntryRoute ?: "$category/edit"
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
     * `wa/cost1List`.
     */
    fun legacyListUrl(category: String): String {
        val page = nextPage(category)
        val app = page?.legacyApp ?: LegacyApp.REACT
        return "${app.appPath}${page?.legacyRoute ?: app.listRoute(category)}"
    }

    /**
     * @return The frontend url template of the legacy edit page with [ID_PLACEHOLDER] for the id,
     * e.g. `react/book/edit/:id` or `wa/cost1Edit?id=:id`.
     */
    fun legacyEditPage(category: String): String {
        val page = nextPage(category)
        val app = page?.legacyApp ?: LegacyApp.REACT
        return "${app.appPath}${page?.legacyEditRoute ?: app.editRoute(category)}"
    }

    /**
     * @return The frontend url of the legacy page for creating a new entry, e.g. `react/book/edit`
     * or `wa/cost1Edit`.
     */
    fun legacyNewEntryUrl(category: String): String {
        val page = nextPage(category)
        val app = page?.legacyApp ?: LegacyApp.REACT
        return "${app.appPath}${app.newEntryRoute(category)}"
    }
}
