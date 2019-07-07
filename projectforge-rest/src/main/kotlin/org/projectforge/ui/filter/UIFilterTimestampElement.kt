/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui.filter

/**
 * An element for the UI specifying a filter attribute which may be added by the user to the search string.
 * Filter attributes are e. g. title or authors for books as well as modifiedInIntervall or modifiedByUser.
 */
open class UIFilterTimestampElement(id: String,
                                    label: String? = null,
                                    /**
                                     * openInterval means, that begin or end of interval is nullable.
                                     */
                                    var openInterval: Boolean = true,
                                    /**
                                     * The provided quickselectors for time intervals.
                                     */
                                    var selectors: List<QuickSelector>? = null)
    : UIFilterElement(id, filterType = FilterType.TIME_STAMP, label = label) {
    enum class QuickSelector {
        /**
         * Quick select of year (01/01/2019 0:00 until 31/12/2019 24:00) with scrolling buttons.
         */
        YEAR,
        /**
         * Quick select of month (01/03/2019 0:00 until 31/03/2019 24:00) with scrolling buttons.
         */
        MONTH,
        /**
         * Quick select of whole week (sunday until saturday or monday until sunday) with scrolling buttons.
         */
        WEEK,
        /**
         * Quick select of whole day with scrolling buttons.
         */
        DAY,
        /**
         * Quick select of last x minutes, hours, days, weeks, months, ...
         */
        UNTIL_NOW
    }
}
