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

package org.projectforge.rest.dto

import java.math.BigDecimal

/**
 * The HR view ("Personalplanung", successor of Wicket's `HRListPage`): the planned and booked man days of the
 * employees in a period, as a matrix of users (rows) and projects or customers (columns). Computed by
 * `org.projectforge.business.humanresources.HRViewDao`; this only reshapes its result into plain data.
 *
 * Days are rounded to two fraction digits; a zero is sent as null (an empty cell).
 */
class HRView(
    /** The effective filter: the request's values completed by the stored ones and the defaults. */
    val filter: HRViewFilter,
    /** The calendar weeks of the period, e.g. "KW 09" or "KW 09-12". */
    val calendarWeeks: String,
    /** The project columns (sorted by display name), followed by the customer columns. */
    val columns: List<HRViewColumn>,
    /** One row per employee, sorted by full name. */
    val rows: List<HRViewRow>,
    /** Employees taking part in the HR planning without planned days in the period. Empty if only my projects. */
    val unplannedUsers: List<HRViewUser>,
    /** True if the logged-in user may write plannings: only then the users lead to their planned week. */
    val fullAccess: Boolean,
)

/**
 * The options of the HR view. Also the user preference the page reopens with (dates as ISO strings).
 */
class HRViewFilter(
    var startDay: String? = null,
    var stopDay: String? = null,
    var showPlanning: Boolean = true,
    var showBookedTimesheets: Boolean = false,
    var onlyMyProjects: Boolean = false,
    var allProjectsGroupedByCustomer: Boolean = false,
    var otherProjectsGroupedByCustomer: Boolean = false,
)

class HRViewColumn(
    /** `p<project id>` or `k<customer number>`, the key of the row's [HRViewRow.cells]. */
    val key: String,
    /** The project's or customer's identifier, or its name if it has none. */
    val label: String?,
    /** The task of a project: its booked days lead to the time sheets of the task. Null for a customer. */
    val taskId: Long? = null,
)

class HRViewCell(
    val planned: BigDecimal? = null,
    val actual: BigDecimal? = null,
)

class HRViewRow(
    val userId: Long?,
    val userName: String?,
    /** The planned week of the user, if the period has one: the user's link leads to it. */
    val planningId: Long?,
    /** The planning is marked as deleted. */
    val deleted: Boolean,
    val sum: HRViewCell,
    /** The days of no shown project or customer (status entries, other projects). */
    val rest: HRViewCell,
    /** The cells by [HRViewColumn.key]; a column without days is missing. */
    val cells: Map<String, HRViewCell>,
)

class HRViewUser(
    val id: Long?,
    val displayName: String?,
)
