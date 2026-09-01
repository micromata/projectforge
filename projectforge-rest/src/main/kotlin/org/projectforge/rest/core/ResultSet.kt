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

package org.projectforge.rest.core

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import java.io.Serializable

/**
 * Contains the data including the result list (matching the filter) served by getList methods ([getInitialList] and [getList]).
 * @param origResultSet Should be given, if ResultSet is converted to a new one (preselected ids will be preserved). Might be null,
 * if a new one should be created.
 */
class ResultSet<O : Any>(
    var resultSet: List<O>,
    origResultSet: ResultSet<*>?,
    var totalSize: Int? = null,
    var highlightRowId: Long? = null,
    var selectedEntityIds: Collection<Serializable>? = null,
    magicFilter: MagicFilter, // only needed to check if the result set was truncated (has size of magicFilter.maxRows).
    /**
     * The offset of the first row of [resultSet] within the whole filtered result, or null for a non-paged
     * result (`POST list`). When set, [totalSize] is the size of the whole result and [resultSet] is one page.
     */
    var offset: Int? = null,
    /**
     * The page size requested for a server-side paged result (see [offset]), or null for a non-paged result.
     */
    var limit: Int? = null,
    /**
     * True if [totalSize] is the exact count of the whole result; false if the underlying id list was
     * truncated at the row cap, so more rows may exist. Meaningful only for a paged result ([offset] set).
     */
    var totalSizeExact: Boolean = true,
) {
    /**
     * Result info as mark down to display. Is usable for statistics as well as for important note, that the
     * result set was runcated due to maxRows limitation.
     */
    var resultInfo: String? = null
        internal set

    /**
     * If true, signals to the frontend that the UI should be reloaded (e.g., filter definitions changed).
     */
    var reloadUI: Boolean = false

    /**
     * Aggregates of the whole result set as structured data, for a client that formats and colours them
     * itself: the sums and counters of the order book (see `OrderEntityRest.postProcessResultSet`).
     *
     * The typed counterpart of [resultInfo], which carries the same numbers as markdown with inline styles
     * for the legacy React app. A hand built page in projectforge-next needs the values, not the markup —
     * currency and separators are the user's and are formatted there (`lib/format.ts`), and a colour
     * belongs to a css token rather than to a `<span style=…>` from the server.
     *
     * Untyped on purpose: what is worth aggregating is the entity's business (an order has six sums, a
     * timesheet has one duration), so the type lives with the entity's rest class.
     */
    var statistics: Any? = null

    val size = resultSet.size

    var paginationPageSize = magicFilter.paginationPageSize

    /**
     * True if the result was capped by the row limit, so more rows match the filter than were returned.
     * A typed flag beside [resultInfo]: a hand built page in projectforge-next wants the fact, not the
     * server's red-span markdown, so it can render the warning itself (see the list toolbar there).
     *
     * Derived for both paths: the non-paged [getList] returns the whole result, so a full page (size
     * equals [MagicFilter.maxRows]) means the cap was hit; the paged [listPage] knows it exactly, from
     * whether [totalSize] is the exact count ([totalSizeExact]).
     */
    var resultSetTruncated: Boolean = false
        internal set

    init {
        if (origResultSet != null && selectedEntityIds == null) {
            selectedEntityIds = origResultSet.selectedEntityIds
        }
        // Only for the non-paged path: there the page equals the whole result, so a full page means the cap
        // was hit. For a paged result the truncation is known exactly (totalSizeExact), and a full 50-row page
        // is the normal case, not a truncation.
        if (offset == null) {
            if (resultSet.size == magicFilter.maxRows) {
                resultSetTruncated = true
                val msg = translateMsg("search.maxRowsExceeded", magicFilter.maxRows)
                resultInfo = "<span style=\"color:red; font-weight: bold;\">$msg</span>"
            }
        } else {
            resultSetTruncated = !totalSizeExact
        }
    }

    fun addResultInfo(info: String?) {
        if (info.isNullOrBlank()) {
            return
        }
        resultInfo.let { value ->
            if (resultInfo.isNullOrBlank()) {
                resultInfo = info
            } else {
                resultInfo = "$value\n\n$info"
            }
        }
    }

}
