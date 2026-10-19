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

import org.projectforge.Constants
import org.projectforge.NextMigration

/**
 * Derives the paths of a feature (see [FeatureDefinition]) from the url of its menu entry.
 *
 * - `next/<route>`: the next route, the REST categories of the migrated pages with this route
 *   ([NextMigration.categoriesOfRoute]) or, for a route of one segment, the route itself as category
 *   (`next/search` -> `rs/search`).
 * - `react/<category>/...`: the category (`react/contract` -> `rs/contract`).
 * - Others (`logout`, `wa/...`): no paths.
 *
 * Every REST category also gets the next pages of the category (its route, see [NextMigration.nextPage], or the
 * generic page `next/<category>`) and its multi-selection (`<category>Selected`).
 */
object FeatureDerivation {
    /** `AbstractMultiSelectedPage.URL_SUFFIX_SELECTED` (projectforge-rest). */
    private const val URL_SUFFIX_SELECTED = "Selected"

    /**
     * @param restPaths Additional REST paths below `rs` (e.g. `addressImport`, `task/tree`), or below `rsPublic`
     * with this prefix (`rsPublic/...`).
     * @param nextRoutes Additional next routes (e.g. `creditor-invoice-import`).
     * @param sharedRestPaths See [FeatureDefinition.sharedPaths], given like [restPaths].
     */
    fun derive(
        id: String,
        url: String?,
        restPaths: Collection<String> = emptyList(),
        nextRoutes: Collection<String> = emptyList(),
        sharedRestPaths: Collection<String> = emptyList(),
    ): FeatureDefinition {
        val categories = mutableSetOf<String>()
        val routes = mutableSetOf<String>()
        val path = url?.removePrefix("/")?.substringBefore('?')?.trimEnd('/')
        if (path != null) {
            if (path.startsWith(Constants.NEXT_APP_PATH)) {
                val route = path.removePrefix(Constants.NEXT_APP_PATH)
                if (route.isNotEmpty()) {
                    routes.add(route)
                    categories.addAll(NextMigration.categoriesOfRoute(route).ifEmpty {
                        if (route.contains('/')) emptySet() else setOf(route)
                    })
                }
            } else if (path.startsWith(Constants.REACT_APP_PATH)) {
                path.removePrefix(Constants.REACT_APP_PATH).substringBefore('/').takeIf { it.isNotEmpty() }
                    ?.let { categories.add(it) }
            }
        }
        categories.addAll(restPaths)
        routes.addAll(nextRoutes.map { it.trim('/') })
        val paths = mutableSetOf<String>()
        categories.forEach { category ->
            paths.add(restKey(category))
            if (category.contains('/')) {
                return@forEach // A sub path (task/tree), no category of its own.
            }
            paths.add(restKey("$category$URL_SUFFIX_SELECTED"))
            val page = NextMigration.nextPage(category)
            if (page == null) {
                routes.add(category) // The generic page of projectforge-next.
            } else {
                routes.add(page.route)
                routes.add(routeOf(page.editRoute))
                routes.add(routeOf(page.newEntryRoute))
            }
        }
        routes.filter { it.isNotEmpty() }.forEach { paths.add("${FeatureDefinition.NEXT_PREFIX}$it") }
        return FeatureDefinition(id, paths, sharedRestPaths.map { restKey(it) }.toSet())
    }

    /**
     * @param path A REST path below `rs` (`address`, `task/tree`) or below `rsPublic` with this prefix.
     * @return The path key, e.g. `rs/address`.
     */
    fun restKey(path: String): String {
        val trimmed = path.trim('/')
        return if (trimmed.startsWith(FeatureDefinition.REST_PUBLIC_PREFIX) || trimmed.startsWith(FeatureDefinition.REST_PREFIX)) {
            trimmed
        } else {
            "${FeatureDefinition.REST_PREFIX}$trimmed"
        }
    }

    /** `address/edit/:id` -> `address/edit`. */
    private fun routeOf(route: String): String {
        return route.substringBefore(NextMigration.ID_PLACEHOLDER).substringBefore('?').trimEnd('/')
    }
}
