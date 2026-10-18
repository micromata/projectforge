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

package org.projectforge.business.fibu.numberrange

/**
 * Number ranges (Nummernkreise) of customers and projects and their mapping to Kost2. Without any configured range,
 * the formerly built-in ranges 4 and 5 ([NumberRanges.legacyRanges]) apply only to an installation that used them
 * (see [NumberRangeService]), otherwise customer and project numbers are free.
 *
 * Example (the ranges used by Micromata):
 * ```
 * projectforge.fibu.number-ranges.allow-free-numbers=false
 * projectforge.fibu.number-ranges.ranges[0].prefix=4
 * projectforge.fibu.number-ranges.ranges[0].name=Intern
 * projectforge.fibu.number-ranges.ranges[0].project-digits=5
 * projectforge.fibu.number-ranges.ranges[1].prefix=5
 * projectforge.fibu.number-ranges.ranges[1].customer-min=0
 * projectforge.fibu.number-ranges.ranges[1].customer-max=999
 * projectforge.fibu.number-ranges.ranges[1].project-digits=2
 * projectforge.fibu.number-ranges.ranges[2].prefix=1
 * projectforge.fibu.number-ranges.ranges[2].customer-min=10000
 * projectforge.fibu.number-ranges.ranges[2].customer-max=19999
 * projectforge.fibu.number-ranges.ranges[2].project-digits=1
 * projectforge.fibu.number-ranges.ranges[2].segments=1,4,1,2
 * ```
 * See [NumberRange] for the meaning of the fields. Bound by [NumberRangeService] from the environment (prefix
 * [PREFIX]).
 */
class NumberRangeConfig {
    /**
     * If false, every customer number must lie in a configured range (protects against typos); applies to the former
     * ranges 4 and 5 as well, ignored if no range applies.
     */
    var allowFreeNumbers: Boolean = true

    /** Segments of Kost2 numbers whose prefix belongs to no range (cost units without project). */
    var defaultSegments: List<Int> = NumberRange.DEFAULT_SEGMENTS

    var ranges: MutableList<NumberRange> = mutableListOf()

    companion object {
        const val PREFIX = "projectforge.fibu.number-ranges"
    }
}
