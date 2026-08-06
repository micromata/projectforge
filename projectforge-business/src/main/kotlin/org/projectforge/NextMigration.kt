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
 * Note that the REST category and the route of the next page may differ: the category is derived
 * from the `@RequestMapping` of the `*PagesRest` class (`book`), while projectforge-next uses the
 * plural noun as its route (`books`). That is exactly the drift this mapping removes.
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
     * A page served by projectforge-next.
     *
     * @param route The route of the list page inside projectforge-next, without the `next/` prefix,
     * e.g. `books`. May differ from the REST category (`book`).
     * @param editRoute Route of the edit page with [ID_PLACEHOLDER] for the id, e.g. `books/:id`.
     * Defaults to the shape of the generic UILayout routes (`<route>/edit/:id`), which mirrors the
     * legacy React app. Hand built pages may deviate.
     * @param newEntryRoute Route for creating a new entry, e.g. `books/new`. Defaults to the
     * generic shape `<route>/edit`.
     */
    class NextPage(
        val route: String,
        editRoute: String? = null,
        newEntryRoute: String? = null,
    ) {
        val editRoute: String = editRoute ?: "$route/edit/$ID_PLACEHOLDER"
        val newEntryRoute: String = newEntryRoute ?: "$route/edit"
    }

    /**
     * REST category -> page in projectforge-next. One entry means: this page is served by
     * projectforge-next, so the menu entry and all server side redirects point to `/next`.
     */
    private val MIGRATED = mapOf(
        // Hand built feature, so its routes are /books, /books/new and /books/<id>.
        "book" to NextPage(route = "books", editRoute = "books/$ID_PLACEHOLDER", newEntryRoute = "books/new"),
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
     * @return The frontend url of the list page without leading slash, e.g. `next/books`
     * or `react/address`.
     */
    fun listUrl(category: String): String {
        return "${appPath(category)}${routeOrCategory(category)}"
    }

    /**
     * @return The frontend url template of the edit page with [ID_PLACEHOLDER] for the id, e.g.
     * `next/books/:id` or `react/address/edit/:id`.
     */
    fun standardEditPage(category: String): String {
        val route = nextPage(category)?.editRoute ?: "$category/edit/$ID_PLACEHOLDER"
        return "${appPath(category)}$route"
    }

    /**
     * @return The frontend url for creating a new entry, e.g. `next/books/new` or
     * `react/address/edit`.
     */
    fun newEntryUrl(category: String): String {
        val route = nextPage(category)?.newEntryRoute ?: "$category/edit"
        return "${appPath(category)}$route"
    }
}
