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

package org.projectforge.plugins.liquidityplanning

import org.projectforge.framework.time.RecurrenceFrequency

/**
 * The "repeat" block the new-entry form posts to create a recurring series from a single entry (a transient
 * part of [LiquidityEntryDO], never persisted). [count] is null for an endless series; [frequency] is
 * MONTHLY for now (the field exists so a later extension needs no wire change).
 *
 * @author Kai Reinhard
 */
class LiquidityRepeatConfig {
    var enabled: Boolean = false
    var intervalMonths: Int = 1
    var count: Int? = null
    var frequency: RecurrenceFrequency? = RecurrenceFrequency.MONTHLY
}
