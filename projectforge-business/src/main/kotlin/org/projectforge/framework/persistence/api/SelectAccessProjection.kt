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


package org.projectforge.framework.persistence.api

/**
 * What a dao's per-row select access ([BaseDao.hasSelectAccess]) reads of a row, for checking it on a projection
 * ([BaseDao.selectDistinct]) instead of on loaded entities: the [paths] of the values the check depends on, and a
 * stub carrying them to check instead of the row. Rows with the same values are checked once.
 *
 * @param paths The properties the access check reads, e.g. `task.id` and `user.id` of a timesheet.
 * @param stubOf An object with the values of [paths] set (in their order), passed to [BaseDao.hasSelectAccess].
 */
class SelectAccessProjection<O>(
    val paths: List<String>,
    val stubOf: (values: List<Any?>) -> O,
)
