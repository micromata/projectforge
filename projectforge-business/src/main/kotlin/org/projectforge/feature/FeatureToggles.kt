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
 * The configured allow- and denylist of the features (see [FeatureService]), without any knowledge of the menu.
 *
 * @param enabledIds The allowlist, null for all.
 * @param disabledIds The denylist: always wins.
 */
class FeatureToggles(
    val enabledIds: Set<String>?,
    val disabledIds: Set<String>,
) {
    val isRestricted: Boolean
        get() = enabledIds != null || disabledIds.isNotEmpty()

    /**
     * @param chain The id of the feature followed by the ids it belongs to: the ancestors in the menu (`MISC`) and
     * its plugin (`marketing`). Listing one of them switches on (or off) the feature.
     */
    fun isEnabled(chain: List<String>): Boolean {
        if (chain.any { it in disabledIds }) {
            return false
        }
        return enabledIds == null || chain.any { it in enabledIds }
    }

    companion object {
        const val ALL = "all"

        /**
         * @param config Comma separated ids, or [ALL] (allowlist only).
         * @return The ids, null for [ALL].
         */
        fun parse(config: String?): Set<String>? {
            val ids = config?.split(',', ';', ' ')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            if (ids.any { it.equals(ALL, ignoreCase = true) }) {
                return null
            }
            return ids.toSet()
        }
    }
}
