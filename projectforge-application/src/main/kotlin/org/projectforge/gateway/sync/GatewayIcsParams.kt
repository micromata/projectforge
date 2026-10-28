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

package org.projectforge.gateway.sync

import org.projectforge.common.StringHelper

/**
 * Key of a calendar subscription in the gateway's ICS cache: the decrypted parameters of its url without the
 * token. The encrypted parameter q can't be the key, the same subscription has different q values: the subscription
 * dialog offers each team calendar with `exportReminders=true` and `=false`.
 *
 * `exportReminders` is left out: the main instance ignores it (`ICalGenerator.exportVAlarms` isn't evaluated, both urls
 * get the same ICS), so the push generates each calendar once for both urls. Should reminders ever be exported, the
 * push has to generate both variants and this parameter has to become part of the key.
 */
object GatewayIcsParams {
    private const val TOKEN = "token"
    private const val EXPORT_REMINDERS = "exportReminders"

    /**
     * @param params The decrypted parameters, e.g. `token=abc&teamCals=42&exportReminders=true`.
     * @return The parameters without token and exportReminders, sorted by name, e.g. `teamCals=42`, or null if the
     * parameters aren't of the form key=value.
     */
    fun normalize(params: String): String? {
        return StringHelper.getKeyValues(params, "&")?.let { normalize(it) }
    }

    fun normalize(params: Map<String, String>): String {
        return params
            .filter { (key, _) -> key != TOKEN && key != EXPORT_REMINDERS }
            .toSortedMap()
            .entries.joinToString("&") { (key, value) -> "$key=$value" }
    }

    /**
     * @return The names of the parameters, sorted, without the token and their values (for logging).
     */
    fun names(params: Map<String, String>): String {
        return params.keys.filter { it != TOKEN }.sorted().joinToString(",")
    }
}
