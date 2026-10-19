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

/**
 * A function of ProjectForge which can be switched off (see [FeatureService]): one per menu entry.
 *
 * @param id The id of the menu entry ([org.projectforge.menu.builder.MenuItemDefId.name] or the id of a
 * plugin's menu entry), or the id of a plugin.
 * @param paths The request paths of the feature without leading slash, below `rs`, `rsPublic` and `next`,
 * e.g. `rs/address`, `rs/addressImport`, `next/address`. A request path belongs to the feature if one of these
 * is a prefix of it, compared segment by segment (`rs/address` covers `rs/address/list`, but not
 * `rs/addressBook`).
 * @param sharedPaths Paths below [paths] used by other features too (e.g. the task tree of the task picker):
 * they stay reachable if the feature is switched off.
 */
class FeatureDefinition(
    val id: String,
    val paths: Set<String>,
    val sharedPaths: Set<String> = emptySet(),
) {
    /** The routes of the next pages, without `next/`, e.g. `address`. */
    val nextRoutes: List<String>
        get() = paths.filter { it.startsWith(NEXT_PREFIX) }.map { it.removePrefix(NEXT_PREFIX) }

    override fun toString(): String {
        return "FeatureDefinition(id=$id, paths=$paths, sharedPaths=$sharedPaths)"
    }

    companion object {
        const val REST_PREFIX = "rs/"
        const val REST_PUBLIC_PREFIX = "rsPublic/"
        const val NEXT_PREFIX = "next/"
    }
}
